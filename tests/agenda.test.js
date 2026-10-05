/* agenda.test.js — la Agenda es a la vez la lista de lo abierto y el
   buscador: que no se pierda ningún aviso y que se entre y salga de la
   búsqueda sin perderse. */
'use strict';

var L = require('./lib');
var assert = L.assert;

module.exports = {

  'un aviso a más de 7 días sale en «Más adelante»': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await p.evaluate(function () {
        var a = Store.nuevoAvisoVacio();
        a.titulo = 'Revisión anual del videoportero';
        a.fecha = Store.sumaDias(Store.hoyISO(), 20);
        return Store.guardarAviso(a).then(function (g) { App.render(); return g.id; });
      });
      var seccion = p.locator('#view .section', { has: p.locator('[data-aviso="' + id + '"]') });
      assert.strictEqual(await seccion.count(), 1, 'el aviso tiene que estar en la agenda');
      assert.match(await seccion.locator('.section__title').textContent(), /Más adelante/);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'cada aviso abierto sale una sola vez en la agenda': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var abiertos = await p.evaluate(function () { return Store.state.avisos.filter(Store.abierto).length; });
      var filas = await p.locator('#view [data-aviso]').evaluateAll(function (fs) { return fs.map(function (f) { return f.dataset.aviso; }); });
      assert.strictEqual(filas.length, abiertos);
      assert.strictEqual(new Set(filas).size, filas.length, 'hay filas repetidas');
    } finally { await L.cerrar(p); }
  },

  'al buscar salen también los cerrados, y Cancelar vuelve a la agenda': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var cerrado = await p.evaluate(function () { var a = Store.cerrados()[0]; return { id: a.id, titulo: a.titulo }; });
      assert.ok(await p.locator('#view .hero').count(), 'sin buscar se ve la tarjeta de hoy');
      await p.fill('#q', cerrado.titulo.slice(0, 12));
      await p.waitForSelector('[data-clear]');
      assert.strictEqual(await p.locator('#view .hero').count(), 0, 'buscando, la tarjeta se quita');
      await p.waitForSelector('#view [data-aviso="' + cerrado.id + '"]');
      assert.strictEqual(await p.evaluate(function () { return document.activeElement && document.activeElement.id; }), 'q',
        'el foco tiene que seguir en el buscador mientras se escribe');

      await L.tocar(p, '[data-clear]');
      await p.waitForSelector('#view .hero');
      assert.strictEqual(await p.inputValue('#q'), '');
      assert.strictEqual(await p.locator('[data-clear]').count(), 0);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'los accesos de la tarjeta de hoy abren la lista filtrada': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      await L.tocar(p, '.hero__stat[data-k="vencidos"]');
      await p.waitForSelector('.chip[data-chip="vencidos"][aria-pressed="true"]');
      var ids = await p.locator('#view [data-aviso]').evaluateAll(function (fs) { return fs.map(function (f) { return f.dataset.aviso; }); });
      var vencidos = await p.evaluate(function () { return Store.state.avisos.filter(Store.vencido).map(function (a) { return a.id; }); });
      assert.deepStrictEqual(ids.sort(), vencidos.sort());
      await L.tocar(p, '[data-clear]');
      await p.waitForSelector('#view .hero');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'los enlaces antiguos a #/avisos llevan a la agenda filtrada': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      await L.ir(p, '#/avisos?v=sinasignar');
      await p.waitForFunction(function () { return location.hash === '#/agenda'; });
      await p.waitForSelector('.chip[data-chip="sinasignar"][aria-pressed="true"]');
      var sinAsignar = await p.evaluate(function () {
        return Store.state.avisos.filter(function (a) { return Store.abierto(a) && !a.asignadoA; }).length;
      });
      assert.strictEqual(await p.locator('#view [data-aviso]').count(), sinAsignar);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'el globo de la Agenda cuenta los vencidos': async function (base) {
    var p = await L.abrir(base);
    try {
      assert.ok(await p.locator('#badgeAgenda').isHidden(), 'sin avisos no hay globo');
      await L.conEjemplos(p);
      var vencidos = await p.evaluate(function () { return Store.resumen().vencidos; });
      assert.ok(vencidos > 0);
      assert.strictEqual((await p.locator('#badgeAgenda').textContent()).trim(), String(vencidos));
    } finally { await L.cerrar(p); }
  }
};
