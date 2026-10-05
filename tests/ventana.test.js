/* ventana.test.js — el fallo de iOS 26 (WebKit 301108): con la app instalada,
   a veces la web arranca más corta que la pantalla y lo fijo abajo sube. Aquí
   se simula con una ventana más baja que la pantalla y, para la barra de
   estado translúcida, con el margen seguro de arriba de un iPhone. */
'use strict';

var L = require('./lib');
var assert = L.assert;

function estado(p) {
  return p.evaluate(function () {
    var v = Ios.ventana();
    var barra = document.getElementById('tabbar').getBoundingClientRect();
    return {
      corta: v.corta, translucida: v.translucida, reintentos: v.reintentos,
      clase: document.documentElement.classList.contains('vp-corta'),
      hueco: Math.round(innerHeight - barra.bottom),
      viewport: document.querySelector('meta[name="viewport"]').getAttribute('content')
    };
  });
}

module.exports = {

  'ventana completa: no se toca nada': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-15' });
    try {
      var e = await estado(p);
      assert.strictEqual(e.corta, false);
      assert.strictEqual(e.clase, false);
      assert.strictEqual(e.reintentos, 0, 'no hay que recolocar nada');
      assert.strictEqual(e.hueco, 23, 'barra concéntrica con la pantalla: max(12, 55 − 32)');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'barra translúcida y ventana corta (tu caso): se reintenta y la barra baja': async function (base) {
    /* iPhone de 852 puntos con la web 55 puntos más corta y la barra de estado
       translúcida, como la de la captura. */
    var p = await L.abrir(base, { modelo: 'iphone-15', altoVentana: 797, margenes: { top: 59, bottom: 34 } });
    try {
      await p.waitForTimeout(600);
      var e = await estado(p);
      assert.strictEqual(e.translucida, true);
      assert.strictEqual(e.corta, true);
      assert.ok(e.reintentos >= 1, 'tiene que pedir a WebKit que vuelva a medir');
      assert.match(e.viewport, /viewport-fit=cover/, 'el viewport tiene que quedar como estaba');
      assert.strictEqual(e.clase, true);
      assert.strictEqual(e.hueco, 8, 'mientras siga corta, la barra se pega abajo');
      await L.ir(p, '#/ajustes');
      assert.strictEqual((await p.locator('[data-diag="barra"] .fila__valor').textContent()).trim(), 'Antigua');
      assert.match(await p.locator('[data-diag="ventana"] .fila__valor').textContent(), /corta/);
      assert.match(await p.locator('#view').textContent(), /vuelve a añadirla desde Safari/);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'barra normal y ventana corta: también se detecta': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-15', altoVentana: 740 });
    try {
      await p.waitForTimeout(600);
      var e = await estado(p);
      assert.strictEqual(e.translucida, false);
      assert.strictEqual(e.corta, true);
      assert.strictEqual(e.hueco, 8);
      await L.ir(p, '#/ajustes');
      assert.strictEqual((await p.locator('[data-diag="barra"] .fila__valor').textContent()).trim(), 'Normal');
      assert.doesNotMatch(await p.locator('#view').textContent(), /vuelve a añadirla desde Safari/,
        'con la barra normal no hay que reinstalar');
    } finally { await L.cerrar(p); }
  },

  'cuando iOS la corrige, la barra vuelve a su sitio': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-15', altoVentana: 797, margenes: { top: 59, bottom: 34 } });
    try {
      await p.waitForTimeout(600);
      assert.strictEqual((await estado(p)).clase, true);
      await p.setViewportSize({ width: 393, height: 852 });
      await p.waitForTimeout(300);
      var e = await estado(p);
      assert.strictEqual(e.corta, false);
      assert.strictEqual(e.clase, false);
      assert.strictEqual(e.hueco, 23);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'con el teclado abierto no se toca el viewport': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-15' });
    try {
      await p.focus('#q');
      await p.setViewportSize({ width: 393, height: 500 });     // el teclado ocupa la mitad
      await p.waitForTimeout(300);
      var e = await estado(p);
      assert.strictEqual(e.clase, false, 'escribiendo no es ventana corta');
      assert.strictEqual(e.reintentos, 0);
    } finally { await L.cerrar(p); }
  },

  'en Safari (sin instalar) no se diagnostica nada': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-15', altoVentana: 700, instalada: false, ruta: '#/ajustes' });
    try {
      assert.strictEqual((await estado(p)).corta, false, 'en Safari la barra del navegador ocupa sitio, es normal');
      assert.strictEqual(await p.locator('[data-diag]').count(), 0);
    } finally { await L.cerrar(p); }
  }
};
