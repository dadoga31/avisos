/* run.js — ejecuta todas las pruebas (*.test.js) o las que se pasen por
   nombre: node run.js gestos ficha */
'use strict';

var fs = require('fs');
var path = require('path');
var L = require('./lib');

async function main() {
  var filtro = process.argv.slice(2);
  var ficheros = fs.readdirSync(__dirname)
    .filter(function (f) { return /\.test\.js$/.test(f); })
    .filter(function (f) { return !filtro.length || filtro.some(function (x) { return f.indexOf(x) !== -1; }); })
    .sort();

  var srv = await L.servidor();
  var bien = 0, mal = 0;

  for (var i = 0; i < ficheros.length; i++) {
    var pruebas = require(path.join(__dirname, ficheros[i]));
    for (var nombre in pruebas) {
      var t0 = Date.now();
      try {
        await pruebas[nombre](srv.url);
        bien++;
        console.log('  ✓ ' + ficheros[i].replace('.test.js', '') + ' › ' + nombre + ' (' + (Date.now() - t0) + ' ms)');
      } catch (e) {
        mal++;
        console.log('  ✗ ' + ficheros[i].replace('.test.js', '') + ' › ' + nombre);
        console.log('    ' + String(e && e.stack || e).split('\n').slice(0, 6).join('\n    '));
      }
    }
  }

  await L.terminar();
  srv.cerrar();
  console.log('\n' + bien + ' bien, ' + mal + ' mal');
  process.exit(mal ? 1 : 0);
}

main().catch(function (e) { console.error(e); process.exit(1); });
