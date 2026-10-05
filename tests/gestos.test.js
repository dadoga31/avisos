/* gestos.test.js — deslizar las filas con el dedo: a la izquierda «hecho»,
   a la derecha «en curso» o «cancelar», y siempre con «Deshacer». */
'use strict';

var L = require('./lib');
var assert = L.assert;

async function primeraAbierta(p) {
  return p.locator('#view [data-swipe]:not([data-hecho="0"])').first().getAttribute('data-swipe');
}

module.exports = {

  'a la izquierda lo da por hecho, y se puede deshacer': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await primeraAbierta(p);
      var previo = await L.estado(p, id);
      var ancho = (await p.locator('[data-swipe="' + id + '"]').boundingBox()).width;

      await L.deslizar(p, '[data-swipe="' + id + '"]', -ancho * 0.7);
      await p.waitForFunction(function (i) { return Store.byId(Store.state.avisos, i).estado === 'resuelto'; }, id);
      await p.waitForSelector('#toast >> text=hecho');
      assert.strictEqual(await p.locator('#view [data-swipe="' + id + '"]').count(), 0, 'la fila tiene que salir de la agenda');

      await p.locator('#toast button').tap();
      await p.waitForFunction(function (a) {
        return Store.byId(Store.state.avisos, a.i).estado === a.e;
      }, { i: id, e: previo });
      await p.waitForSelector('#view [data-swipe="' + id + '"]');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'un deslizamiento corto no hace nada': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await primeraAbierta(p);
      var previo = await L.estado(p, id);
      await L.deslizar(p, '[data-swipe="' + id + '"]', -40);
      await p.waitForTimeout(400);
      assert.strictEqual(await L.estado(p, id), previo);
      assert.ok(await p.locator('#view [data-swipe="' + id + '"]').isVisible());
    } finally { await L.cerrar(p); }
  },

  'a la derecha enseña las acciones y «En curso» funciona': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await p.evaluate(function () {
        var a = Store.state.avisos.filter(function (x) { return Store.abierto(x) && x.estado !== 'en_curso'; });
        return a.length ? a[0].id : null;
      });
      await L.ir(p, '#/avisos');
      var fila = '[data-swipe="' + id + '"]';
      await p.locator(fila).scrollIntoViewIfNeeded();
      await L.deslizar(p, fila, 200);
      var boton = p.locator(fila + ' [data-estado="en_curso"]');
      await boton.waitFor({ state: 'visible' });
      await p.waitForTimeout(300);
      await boton.tap();
      await p.waitForFunction(function (i) { return Store.byId(Store.state.avisos, i).estado === 'en_curso'; }, id);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'con las acciones abiertas, tocar la fila solo las cierra': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await primeraAbierta(p);
      var fila = '[data-swipe="' + id + '"]';
      await L.deslizar(p, fila, 200);
      await p.waitForSelector(fila + '.swipe--abierta');
      await p.waitForTimeout(500);              // que termine de abrirse
      await p.locator(fila + ' [data-aviso]').tap();
      await p.waitForTimeout(250);
      assert.strictEqual(await p.evaluate(function () { return location.hash; }), '#/agenda', 'no debería abrir la ficha');
      assert.strictEqual(await p.locator(fila + '.swipe--abierta').count(), 0);
    } finally { await L.cerrar(p); }
  },

  'en el histórico un aviso cerrado se reabre deslizando': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await p.evaluate(function () { return Store.cerrados()[0].id; });
      await L.ir(p, '#/historico');
      var fila = '[data-swipe="' + id + '"]';
      await p.locator(fila).scrollIntoViewIfNeeded();
      await L.deslizar(p, fila, 160);
      var boton = p.locator(fila + ' [data-estado="pendiente"]');
      await boton.waitFor({ state: 'visible' });
      await p.waitForTimeout(300);
      await boton.tap();
      await p.waitForFunction(function (i) { return Store.byId(Store.state.avisos, i).estado === 'pendiente'; }, id);
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  }
};
