/* publicacion.test.js — que lo que se publica esté completo y coherente.
   Un fichero que falta en la caché o una versión desparejada dejan la app
   a medias en el iPhone (fallo de la 1.8.1). */
'use strict';

var fs = require('fs');
var path = require('path');
var http = require('http');
var L = require('./lib');
var assert = L.assert;

function leer(f) { return fs.readFileSync(path.join(L.RAIZ, f), 'utf8'); }

function pedir(url) {
  return new Promise(function (resolve, reject) {
    http.get(url, function (res) { res.resume(); resolve(res.statusCode); }).on('error', reject);
  });
}

function shell() {
  var sw = leer('sw.js');
  var bloque = sw.slice(sw.indexOf('var SHELL'), sw.indexOf('];', sw.indexOf('var SHELL')));
  return (bloque.match(/'[^']+'/g) || []).map(function (s) { return s.slice(1, -1); });
}

module.exports = {

  'la versión del service worker y la de la app coinciden': async function () {
    var sw = (leer('sw.js').match(/VERSION\s*=\s*'avisos-v([\d.]+)'/) || [])[1];
    var app = (leer('assets/js/app.js').match(/APP_VERSION\s*=\s*'([\d.]+)'/) || [])[1];
    assert.ok(sw && app, 'no encuentro las versiones');
    assert.strictEqual(sw, app, 'sw.js dice ' + sw + ' y app.js dice ' + app);
  },

  'todo lo que guarda el service worker existe': async function (base) {
    var rutas = shell();
    assert.ok(rutas.length > 10);
    for (var i = 0; i < rutas.length; i++) {
      var codigo = await pedir(base + rutas[i].replace(/^\.\//, ''));
      assert.strictEqual(codigo, 200, rutas[i] + ' responde ' + codigo);
    }
  },

  'cada script de la página está en la caché del service worker': async function () {
    var enCache = shell();
    var scripts = (leer('index.html').match(/<script src="([^"]+)"/g) || []).map(function (s) {
      return './' + s.match(/src="([^"]+)"/)[1];
    });
    assert.ok(scripts.length > 5);
    scripts.forEach(function (s) {
      assert.ok(enCache.indexOf(s) !== -1, s + ' se carga en index.html pero no está en SHELL de sw.js');
    });
  }
};
