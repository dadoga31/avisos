/* diseno.test.js — lo que hace que parezca una app del iPhone y es fácil de
   romper sin darse cuenta: la barra de pestañas, la lente, la barra de estado,
   las esquinas, los bloques que aparecen y la hoja que entra y sale. */
'use strict';

var L = require('./lib');
var assert = L.assert;

function centroX(caja) { return caja.x + caja.width / 2; }

module.exports = {

  'la barra tiene Agenda, Avisos, +, Hechos y Ajustes, con el + en el centro': async function (base) {
    var p = await L.abrir(base);
    try {
      var tabs = await p.locator('#tabbar .tab').evaluateAll(function (ts) { return ts.map(function (t) { return t.dataset.tab; }); });
      assert.deepStrictEqual(tabs, ['agenda', 'avisos', 'historico', 'ajustes']);
      var mas = await p.locator('#fab').boundingBox();
      var ancho = p.viewportSize().width;
      assert.ok(Math.abs(centroX(mas) - ancho / 2) < 1.5, 'el + está en ' + centroX(mas) + ' y el centro en ' + ancho / 2);
    } finally { await L.cerrar(p); }
  },

  'la lente queda bajo la pestaña activa al cambiar de sección': async function (base) {
    var p = await L.abrir(base, { movimiento: true });
    try {
      var rutas = [['#/avisos', 'avisos'], ['#/historico', 'historico'], ['#/ajustes', 'ajustes'], ['#/equipo', 'ajustes'], ['#/agenda', 'agenda']];
      for (var i = 0; i < rutas.length; i++) {
        await L.ir(p, rutas[i][0]);
        await p.waitForTimeout(800);            // que acabe el muelle
        var tab = await p.locator('#tabbar .tab[data-tab="' + rutas[i][1] + '"]').boundingBox();
        var lente = await p.locator('#lente').boundingBox();
        assert.ok(Math.abs(tab.x - lente.x) < 1.5 && Math.abs(tab.width - lente.width) < 1.5,
          rutas[i][0] + ': lente en ' + lente.x + '/' + lente.width + ', pestaña en ' + tab.x + '/' + tab.width);
      }
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'Equipo se abre desde Ajustes y vuelve': async function (base) {
    var p = await L.abrir(base, { ruta: '#/ajustes' });
    try {
      await L.tocar(p, '#view a.fila[href="#/equipo"]');
      await p.waitForFunction(function () { return location.hash === '#/equipo'; });
      await p.waitForSelector('#navTitle >> text=Equipo');
      assert.ok(await p.locator('#btnBack').isVisible(), 'en Equipo tiene que haber botón de volver');
      assert.strictEqual(await p.evaluate(function () { return document.documentElement.dataset.vista; }), 'apilada');
      await L.tocar(p, '#btnBack');
      await p.waitForFunction(function () { return location.hash === '#/ajustes'; });
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'en el formulario no hay barra de pestañas': async function (base) {
    var p = await L.abrir(base, { ruta: '#/nuevo' });
    try {
      assert.ok(await p.locator('#tabbar').isHidden());
      await L.ir(p, '#/agenda');
      assert.ok(await p.locator('#tabbar').isVisible());
    } finally { await L.cerrar(p); }
  },

  'la barra de estado sigue al tema y se oscurece con la hoja': async function (base) {
    var p = await L.abrir(base);
    function color() {
      return p.evaluate(function () {
        return Array.prototype.map.call(document.querySelectorAll('meta[name="theme-color"]'), function (m) {
          return m.getAttribute('content') + (m.hasAttribute('media') ? ' (media)' : '');
        });
      });
    }
    try {
      assert.deepStrictEqual(await color(), ['#f2f2f7', '#f2f2f7']);
      await L.conEjemplos(p);
      await L.ir(p, '#/avisos');
      await L.tocar(p, '[data-mas]');
      await p.waitForSelector('#sheet:not([hidden])');
      assert.deepStrictEqual(await color(), ['#b6b6b9', '#b6b6b9']);
      await p.keyboard.press('Escape');
      await p.waitForSelector('#sheet', { state: 'hidden' });
      assert.deepStrictEqual(await color(), ['#f2f2f7', '#f2f2f7']);
      await p.evaluate(function () { return Store.guardarAjustes({ tema: 'dark' }).then(App.aplicarTema); });
      assert.deepStrictEqual(await color(), ['#000000', '#000000']);
      assert.strictEqual(await p.getAttribute('meta[name="apple-mobile-web-app-status-bar-style"]', 'content'), 'default');
    } finally { await L.cerrar(p); }
  },

  'esquinas concéntricas solo en la app instalada de un iPhone sin botón': async function (base) {
    var p = await L.abrir(base, { modelo: 'iphone-17' });
    var q = await L.abrir(base, { modelo: 'iphone-17', instalada: false });
    try {
      assert.strictEqual(await p.evaluate(function () { return document.documentElement.style.getPropertyValue('--screen-radius'); }), '62px');
      assert.ok(await p.evaluate(function () { return document.documentElement.hasAttribute('data-screen-corners'); }));
      assert.ok(!(await q.evaluate(function () { return document.documentElement.hasAttribute('data-screen-corners'); })),
        'en Safari no hay esquinas con las que casar');
      assert.strictEqual(await p.evaluate(function () { return Ios.radioPantalla(375, 667, 2, false); }), 0);
      assert.strictEqual(await p.evaluate(function () { return Ios.radioPantalla(390, 844, 3, true); }), 47.33);
    } finally { await L.cerrar(p); await L.cerrar(q); }
  },

  'con animaciones, nada se queda escondido al recorrer cada pantalla': async function (base) {
    var p = await L.abrir(base, { movimiento: true });
    try {
      await L.conEjemplos(p);
      var rutas = ['#/agenda', '#/avisos', '#/historico', '#/ajustes', '#/equipo'];
      for (var i = 0; i < rutas.length; i++) {
        await L.ir(p, rutas[i]);
        await p.waitForTimeout(300);
        /* Bajando poco a poco, como con el dedo */
        var alto = await p.evaluate(function () { return document.documentElement.scrollHeight; });
        for (var y = 0; y <= alto; y += 250) {
          await p.evaluate(function (v) { window.scrollTo(0, v); }, y);
          await p.waitForTimeout(60);
        }
        await p.waitForTimeout(400);
        var quedan = await p.locator('[data-reveal="pending"]').count();
        assert.strictEqual(quedan, 0, rutas[i] + ': ' + quedan + ' bloques sin aparecer');
      }
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'con animaciones, la hoja baja antes de cerrarse y otra puede abrirse detrás': async function (base) {
    var p = await L.abrir(base, { movimiento: true, ruta: '#/ajustes' });
    try {
      await L.tocar(p, '[data-reinstalar]');
      await p.waitForSelector('#sheet:not([hidden]) [data-si]');
      await L.tocar(p, '#sheet [data-no]');
      assert.ok(await p.locator('#sheet.sheet--cerrando').count(), 'tendría que estar bajando');
      await p.waitForSelector('#sheet', { state: 'hidden' });
      /* Cerrar una y abrir otra al momento (lo hace «Eliminar técnico») */
      await p.evaluate(function () {
        UI.abrirSheet('Primera', '<p>uno</p>');
        UI.cerrarSheet();
        UI.abrirSheet('Segunda', '<p>dos</p>');
      });
      await p.waitForTimeout(500);
      assert.ok(await p.locator('#sheet').isVisible(), 'la segunda hoja no puede desaparecer con el cierre de la primera');
      assert.strictEqual((await p.locator('#sheetTitle').textContent()).trim(), 'Segunda');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'la tarjeta de hoy cambia de color con los vencidos': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var clase = function () { return p.locator('#view .hero').getAttribute('class'); };
      assert.ok(/hero--exceso/.test(await clase()), 'con vencidos tiene que ir en rojo');
      await p.evaluate(function () {
        var hoy = Store.hoyISO();
        return Promise.all(Store.state.avisos.filter(function (a) { return Store.abierto(a) && a.fecha && a.fecha < hoy; })
          .map(function (a) { a.estado = 'resuelto'; return Store.guardarAviso(a); })).then(function () { App.render(); });
      });
      assert.ok(!/hero--exceso/.test(await clase()));
    } finally { await L.cerrar(p); }
  }
};
