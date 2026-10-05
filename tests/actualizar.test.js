/* actualizar.test.js — que una versión nueva entre entera, nunca mezclada con
   la anterior. Se sirve la web con las mismas cabeceras de caché que pone
   Vercel (vercel.json), se publican dos versiones seguidas y se comprueba
   que la página y sus .js son de la misma.

   Fallo de la 2.1.0: con dos versiones publicadas en menos de una hora, el
   service worker guardó la página nueva con los .js de la caché HTTP del
   navegador, que eran los de antes. */
'use strict';

var fs = require('fs');
var os = require('os');
var path = require('path');
var http = require('http');
var L = require('./lib');
var assert = L.assert;

var WEB = ['index.html', 'manifest.webmanifest', 'sw.js', 'assets', 'api'];

function copiar(origen, destino) {
  if (fs.statSync(origen).isDirectory()) {
    fs.mkdirSync(destino, { recursive: true });
    fs.readdirSync(origen).forEach(function (f) { copiar(path.join(origen, f), path.join(destino, f)); });
  } else {
    fs.copyFileSync(origen, destino);
  }
}

/* La web en una carpeta aparte, para poder «publicar» otra versión
   cambiando ficheros sin tocar el repositorio. */
function publicacion() {
  var dir = fs.mkdtempSync(path.join(os.tmpdir(), 'avisos-pub-'));
  WEB.forEach(function (f) { copiar(path.join(L.RAIZ, f), path.join(dir, f)); });
  return dir;
}

/* Las cabeceras de vercel.json, tal cual. */
function cabeceras(ruta) {
  var reglas = JSON.parse(fs.readFileSync(path.join(L.RAIZ, 'vercel.json'), 'utf8')).headers || [];
  var h = {};
  reglas.forEach(function (r) {
    if (new RegExp('^' + r.source + '$').test(ruta)) r.headers.forEach(function (x) { h[x.key] = x.value; });
  });
  return h;
}

var TIPOS = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.webmanifest': 'application/manifest+json', '.svg': 'image/svg+xml', '.png': 'image/png' };

function servidorComoVercel(dir) {
  return new Promise(function (resolve) {
    var srv = http.createServer(function (req, res) {
      var ruta = decodeURIComponent(req.url.split('?')[0]);
      var fichero = path.join(dir, ruta === '/' ? 'index.html' : ruta);
      fs.readFile(fichero, function (err, datos) {
        if (err) { res.writeHead(404); res.end(); return; }
        var h = cabeceras(ruta);
        h['Content-Type'] = TIPOS[path.extname(fichero)] || 'application/octet-stream';
        res.writeHead(200, h);
        res.end(datos);
      });
    });
    srv.listen(0, '127.0.0.1', function () {
      resolve({ url: 'http://127.0.0.1:' + srv.address().port + '/', cerrar: function () { srv.close(); } });
    });
  });
}

/* «Publica» otra versión: otro número, y una marca en la página y en app.js
   para saber de qué versión es cada uno. */
function publicarOtra(dir, version) {
  function cambiar(f, fn) { var p = path.join(dir, f); fs.writeFileSync(p, fn(fs.readFileSync(p, 'utf8'))); }
  cambiar('sw.js', function (s) { return s.replace(/var VERSION = 'avisos-v[\d.]+';/, "var VERSION = 'avisos-v" + version + "';"); });
  cambiar('assets/js/app.js', function (s) {
    return s.replace(/var APP_VERSION = '[\d.]+';/, "var APP_VERSION = '" + version + "';")
      .replace("'use strict';", "'use strict';\n  window.__JS = '" + version + "';");
  });
  cambiar('index.html', function (s) { return s.replace('<body>', '<body data-pagina="' + version + '">'); });
}

async function versiones(p) {
  return p.evaluate(function () {
    return { pagina: document.body.dataset.pagina || 'original', js: window.__JS || 'original', app: window.APP_VERSION };
  });
}

module.exports = {

  'dos versiones seguidas: la página y sus .js entran juntos': async function () {
    var dir = publicacion();
    var srv = await servidorComoVercel(dir);
    var p = await L.abrir(srv.url, { serviceWorker: true });
    try {
      /* Primera visita: se instala el service worker de la versión de hoy. */
      await p.waitForFunction(function () {
        return navigator.serviceWorker.getRegistration().then(function (r) { return !!(r && r.active); });
      }, null, { timeout: 15000 });
      await p.reload();
      await p.waitForFunction(function () { return !!navigator.serviceWorker.controller; }, null, { timeout: 15000, polling: 100 });

      /* Sale la versión «8.0.0» al rato (dentro de la hora de caché). */
      publicarOtra(dir, '8.0.0');
      await p.evaluate(function () { return navigator.serviceWorker.getRegistration().then(function (r) { return r.update(); }); });
      await p.waitForSelector('#toast.toast--fijo .toast__btn', { timeout: 15000 });
      await Promise.all([
        p.waitForNavigation({ timeout: 15000 }),
        p.locator('#toast .toast__btn').click()
      ]);
      await p.waitForFunction(function () { return window.App && window.Store; });
      var v = await versiones(p);
      assert.deepStrictEqual(v, { pagina: '8.0.0', js: '8.0.0', app: '8.0.0' },
        'mezcla de versiones tras actualizar: ' + JSON.stringify(v));
      L.sinErrores(p);
    } finally {
      await L.cerrar(p);
      srv.cerrar();
      fs.rmSync(dir, { recursive: true, force: true });
    }
  },

  'Reinstalar la app baja todo de nuevo, sin restos de la caché': async function () {
    var dir = publicacion();
    var srv = await servidorComoVercel(dir);
    var p = await L.abrir(srv.url, { serviceWorker: true });
    try {
      await p.waitForFunction(function () {
        return navigator.serviceWorker.getRegistration().then(function (r) { return !!(r && r.active); });
      }, null, { timeout: 15000 });
      publicarOtra(dir, '9.0.0');
      await Promise.all([
        p.waitForNavigation({ timeout: 15000 }),
        p.evaluate(function () { App.reinstalar(); })
      ]);
      await p.waitForFunction(function () { return window.App && window.Store; });
      var v = await versiones(p);
      assert.deepStrictEqual(v, { pagina: '9.0.0', js: '9.0.0', app: '9.0.0' },
        'mezcla de versiones tras reinstalar: ' + JSON.stringify(v));
    } finally {
      await L.cerrar(p);
      srv.cerrar();
      fs.rmSync(dir, { recursive: true, force: true });
    }
  }
};
