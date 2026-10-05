/* lib.js — lo que comparten las pruebas: un servidor de los ficheros de la
   web y un iPhone emulado con la app «instalada» en la pantalla de inicio. */
'use strict';

var http = require('http');
var fs = require('fs');
var path = require('path');
var assert = require('assert');
var chromium = require('playwright-core').chromium;

var RAIZ = path.resolve(__dirname, '..');

/* Chromium viene instalado en el entorno; si no está ahí, se puede indicar
   otro con CHROMIUM=/ruta/al/chrome. */
var CHROMIUM = process.env.CHROMIUM || [
  '/opt/pw-browsers/chromium-1194/chrome-linux/chrome',
  '/opt/pw-browsers/chromium/chrome-linux/chrome'
].find(function (p) { return fs.existsSync(p); });

var TIPOS = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json',
  '.webmanifest': 'application/manifest+json', '.svg': 'image/svg+xml',
  '.png': 'image/png', '.ico': 'image/x-icon'
};

/* Los modelos con los que se comprueba el diseño. */
var MODELOS = {
  'iphone-se': { width: 375, height: 667, dpr: 2 },
  'iphone-13-mini': { width: 375, height: 812, dpr: 3 },
  'iphone-15': { width: 393, height: 852, dpr: 3 },
  'iphone-17': { width: 402, height: 874, dpr: 3 },
  'iphone-15-pro-max': { width: 430, height: 932, dpr: 3 }
};

var UA = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 ' +
  '(KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1';

function servidor() {
  return new Promise(function (resolve) {
    var srv = http.createServer(function (req, res) {
      var ruta = decodeURIComponent(req.url.split('?')[0]);
      if (ruta === '/') ruta = '/index.html';
      var fichero = path.join(RAIZ, ruta);
      if (fichero.indexOf(RAIZ) !== 0) { res.writeHead(403); res.end(); return; }
      fs.readFile(fichero, function (err, datos) {
        if (err) { res.writeHead(404); res.end('no'); return; }
        res.writeHead(200, {
          'Content-Type': TIPOS[path.extname(fichero)] || 'application/octet-stream',
          'Cache-Control': 'no-store'
        });
        res.end(datos);
      });
    });
    srv.listen(0, '127.0.0.1', function () {
      resolve({ url: 'http://127.0.0.1:' + srv.address().port + '/', cerrar: function () { srv.close(); } });
    });
  });
}

var navegador = null;
function lanzar() {
  if (!navegador) navegador = chromium.launch({ executablePath: CHROMIUM, headless: true });
  return navegador;
}

/* Abre la app en un iPhone limpio (sin datos de antes). Devuelve la página
   con los errores de consola recogidos en page.errores. */
async function abrir(base, opts) {
  opts = opts || {};
  var m = MODELOS[opts.modelo || 'iphone-15'];
  var nav = await lanzar();
  var ctx = await nav.newContext({
    viewport: { width: m.width, height: m.height },
    screen: { width: m.width, height: m.height },
    deviceScaleFactor: m.dpr,
    isMobile: true,
    hasTouch: true,
    userAgent: UA,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    colorScheme: opts.tema || 'light',
    serviceWorkers: opts.serviceWorker ? 'allow' : 'block',
    reducedMotion: opts.movimiento ? 'no-preference' : 'reduce'
  });
  if (opts.instalada !== false) {
    await ctx.addInitScript(function () {
      Object.defineProperty(navigator, 'standalone', { get: function () { return true; } });
    });
  }
  var page = await ctx.newPage();
  page.errores = [];
  page.on('console', function (msg) {
    if (msg.type() === 'error') page.errores.push(msg.text());
  });
  page.on('pageerror', function (e) { page.errores.push(String(e && e.stack || e)); });
  await page.goto(base + (opts.ruta || '#/agenda'));
  await page.waitForFunction(function () { return window.Store && window.App && Store.state; });
  await page.waitForSelector('#view > *');
  return page;
}

async function cerrar(page) {
  await page.context().close();
}

async function conEjemplos(page) {
  await page.evaluate(function () { return Store.datosDeEjemplo().then(function () { App.render(); }); });
  await page.waitForSelector('[data-aviso]');
}

async function ir(page, hash) {
  await page.evaluate(function (h) { location.hash = h; }, hash);
  await page.waitForTimeout(80);
}

function estado(page, id) {
  return page.evaluate(function (i) { return Store.byId(Store.state.avisos, i).estado; }, id);
}

/* Desliza con el dedo de verdad: eventos táctiles por CDP, que Chromium
   convierte en Pointer Events igual que Safari. */
async function deslizar(page, selector, dx, opts) {
  opts = opts || {};
  var caja = await page.locator(selector).first().boundingBox();
  assert.ok(caja, 'no encuentro ' + selector + ' para deslizar');
  var x0 = caja.x + (dx < 0 ? caja.width * 0.8 : caja.width * 0.2);
  var y = caja.y + caja.height / 2;
  var cdp = await page.context().newCDPSession(page);
  var pasos = opts.pasos || 14;
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: x0, y: y }] });
  for (var i = 1; i <= pasos; i++) {
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x0 + dx * i / pasos, y: y }] });
    await page.waitForTimeout(12);
  }
  await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await cdp.detach();
}

async function tocar(page, selector) {
  await page.locator(selector).first().tap();
}

function sinErrores(page) {
  assert.deepStrictEqual(page.errores, [], 'errores en la consola:\n' + page.errores.join('\n'));
}

module.exports = {
  RAIZ: RAIZ, MODELOS: MODELOS, servidor: servidor, lanzar: lanzar, abrir: abrir, cerrar: cerrar,
  conEjemplos: conEjemplos, ir: ir, estado: estado, deslizar: deslizar, tocar: tocar,
  sinErrores: sinErrores, assert: assert,
  terminar: function () { return navegador ? navegador.then(function (n) { return n.close(); }) : null; }
};
