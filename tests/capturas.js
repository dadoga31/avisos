/* capturas.js — fotos de cada pantalla en varios iPhone, en claro y oscuro,
   para revisar el diseño a ojo. No es una prueba: no falla, solo guarda.

     node capturas.js [carpeta] [modelos,separados] [temas] [pantallas]
     node capturas.js /tmp/fotos iphone-15,iphone-se light,dark agenda,ficha

   Con la barra de estado de iOS simulada: una franja del color de
   theme-color encima, como la pinta el iPhone con la app instalada. */
'use strict';

var fs = require('fs');
var path = require('path');
var L = require('./lib');

var PANTALLAS = {
  agenda: '#/agenda',
  busqueda: 'busqueda',
  hechos: '#/historico',
  ajustes: '#/ajustes',
  equipo: '#/equipo',
  nuevo: '#/nuevo',
  ficha: 'ficha',
  hoja: 'hoja',
  recogido: 'recogido',
  gesto: 'gesto'
};

/* Alto de la barra de estado según el modelo (sin isla: 20). */
function altoBarra(m) { return m.height <= 736 ? 20 : (m.height >= 852 ? 54 : 47); }

async function foto(page, destino, modelo) {
  var m = L.MODELOS[modelo];
  var color = await page.evaluate(function () {
    var meta = document.querySelector('meta[name="theme-color"]');
    return meta ? meta.getAttribute('content') : '#ffffff';
  });
  var oscuro = await page.evaluate(function () {
    var t = document.documentElement.getAttribute('data-theme');
    return t ? t === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
  });
  var buf = await page.screenshot();
  /* La captura va debajo de la franja: así se ve si empalman. */
  var b64 = buf.toString('base64');
  var h = altoBarra(m);
  var html = '<body style="margin:0;background:#000">' +
    '<div style="height:' + h + 'px;background:' + color + ';color:' + (oscuro ? '#fff' : '#000') +
    ';font:600 15px -apple-system,sans-serif;display:flex;align-items:center;justify-content:space-between;padding:0 28px">' +
    '<span>9:41</span><span>▮▮▮ ◔</span></div>' +
    '<img style="display:block;width:' + m.width + 'px" src="data:image/png;base64,' + b64 + '"></body>';
  var marco = await page.context().browser().newPage({
    viewport: { width: m.width, height: m.height + h }, deviceScaleFactor: m.dpr
  });
  await marco.setContent(html);
  await marco.screenshot({ path: destino });
  await marco.close();
}

async function preparar(page, nombre) {
  var ruta = PANTALLAS[nombre];
  if (ruta.charAt(0) === '#') { await L.ir(page, ruta); await page.waitForTimeout(250); return; }
  if (nombre === 'ficha') {
    var id = await page.evaluate(function () {
      return Store.state.avisos.filter(function (a) { return (a.notas || []).length; })[0].id;
    });
    await L.ir(page, '#/aviso/' + id);
  } else if (nombre === 'busqueda') {
    await L.ir(page, '#/agenda');
    await page.fill('#q', 'alarma');
    await page.waitForSelector('[data-clear]');
  } else if (nombre === 'hoja') {
    await L.ir(page, '#/agenda');
    await page.locator('[data-mas]').tap();
  } else if (nombre === 'recogido') {
    await L.ir(page, '#/agenda');
    await page.evaluate(function () { window.scrollTo(0, 420); });
  } else if (nombre === 'gesto') {
    await L.ir(page, '#/agenda');
    var fila = await page.locator('#view [data-swipe]').first().getAttribute('data-swipe');
    await L.deslizar(page, '[data-swipe="' + fila + '"]', 200);
  }
  await page.waitForTimeout(700);
}

async function main() {
  var carpeta = process.argv[2] || path.join(require('os').tmpdir(), 'capturas-avisos');
  var modelos = (process.argv[3] || 'iphone-13-mini,iphone-15,iphone-17,iphone-15-pro-max').split(',');
  var temas = (process.argv[4] || 'light,dark').split(',');
  var pantallas = (process.argv[5] || Object.keys(PANTALLAS).join(',')).split(',');
  fs.mkdirSync(carpeta, { recursive: true });

  var srv = await L.servidor();
  for (var i = 0; i < modelos.length; i++) {
    for (var j = 0; j < temas.length; j++) {
      var page = await L.abrir(srv.url, { modelo: modelos[i], tema: temas[j] });
      await L.conEjemplos(page);
      for (var k = 0; k < pantallas.length; k++) {
        await preparar(page, pantallas[k]);
        var f = path.join(carpeta, pantallas[k] + '-' + modelos[i] + '-' + temas[j] + '.png');
        await foto(page, f, modelos[i]);
        await page.evaluate(function () {
          if (window.UI) UI.cerrarSheet();
          var c = document.querySelector('[data-clear]');
          if (c) c.click();
          window.scrollTo(0, 0);
        });
      }
      if (page.errores.length) console.log('⚠ errores en ' + modelos[i] + '/' + temas[j] + ':\n  ' + page.errores.join('\n  '));
      await L.cerrar(page);
    }
  }
  await L.terminar();
  srv.cerrar();
  console.log('Capturas en ' + carpeta);
}

main().catch(function (e) { console.error(e); process.exit(1); });
