/* flujos.test.js — lo que se hace a diario con la app: abrirla, crear un
   aviso, verlo, apuntar algo y moverse por las secciones. */
'use strict';

var L = require('./lib');
var assert = L.assert;

module.exports = {

  'arranca vacía, sin errores, y carga los ejemplos': async function (base) {
    var p = await L.abrir(base);
    try {
      assert.ok(await p.locator('.empty, [data-accion="ejemplo"]').count(), 'sin avisos debería verse el estado vacío');
      await L.tocar(p, '[data-accion="ejemplo"]');
      await p.waitForSelector('[data-aviso]');
      assert.ok(await p.locator('[data-aviso]').count() > 3, 'tras cargar ejemplos tiene que haber avisos en la agenda');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'crea un aviso desde el + y lo abre': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.tocar(p, '#fab');
      await p.waitForSelector('#fAviso');
      assert.strictEqual(await p.evaluate(function () { return location.hash; }), '#/nuevo');
      await p.fill('#titulo', 'Cámara del portal sin imagen');
      await p.fill('#cl_nombre', 'Comunidad Prueba 12');
      await p.locator('#fAviso [type="submit"]').tap();
      await p.waitForFunction(function () { return /^#\/aviso\//.test(location.hash); });
      await p.waitForSelector('text=Cámara del portal sin imagen');
      var n = await p.evaluate(function () { return Store.state.avisos.length; });
      assert.strictEqual(n, 1);
      await p.waitForSelector('#toast >> text=creado');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'el formulario no deja crear un aviso sin título': async function (base) {
    var p = await L.abrir(base, { ruta: '#/nuevo' });
    try {
      await p.waitForSelector('#fAviso');
      await p.locator('#fAviso [type="submit"]').tap();
      await p.waitForSelector('#toast >> text=título');
      assert.strictEqual(await p.evaluate(function () { return Store.state.avisos.length; }), 0);
    } finally { await L.cerrar(p); }
  },

  'en la ficha se cambia el estado y se añade una nota': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await p.locator('[data-aviso]').first().getAttribute('data-aviso');
      await L.tocar(p, '[data-aviso="' + id + '"]');
      await p.waitForFunction(function (i) { return location.hash === '#/aviso/' + i; }, id);

      await L.tocar(p, '#view [data-estado="en_espera"]');
      await p.waitForFunction(function (i) { return Store.byId(Store.state.avisos, i).estado === 'en_espera'; }, id);

      await L.tocar(p, '[data-nota]');
      await p.waitForSelector('#sheet:not([hidden]) #pt');
      await p.fill('#pt', 'Falta la fuente de alimentación');
      await L.tocar(p, '#sheet [data-si]');
      await p.waitForSelector('#view >> text=Falta la fuente de alimentación');
      assert.ok(await p.locator('#sheet').isHidden(), 'la hoja debería cerrarse al guardar');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'la hoja se cierra con su botón y con Escape': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      await L.ir(p, '#/avisos');
      await L.tocar(p, '[data-mas]');
      await p.waitForSelector('#sheet:not([hidden])');
      await L.tocar(p, '#sheet .sheet__panel [data-close]');
      await p.waitForSelector('#sheet', { state: 'hidden' });

      await L.tocar(p, '[data-mas]');
      await p.waitForSelector('#sheet:not([hidden])');
      await p.keyboard.press('Escape');
      await p.waitForSelector('#sheet', { state: 'hidden' });
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'la búsqueda filtra la lista de avisos': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      await L.ir(p, '#/avisos');
      var antes = await p.locator('[data-aviso]').count();
      var cliente = await p.evaluate(function () {
        return Store.state.avisos.filter(Store.abierto)[0].cliente.nombre;
      });
      await p.fill('#q', cliente);
      await p.waitForFunction(function (n) {
        return document.querySelectorAll('[data-aviso]').length < n;
      }, antes);
      assert.ok(await p.locator('[data-aviso]').count() >= 1, 'el cliente buscado tiene que salir');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'equipo: se añade un técnico': async function (base) {
    var p = await L.abrir(base, { ruta: '#/equipo' });
    try {
      await L.tocar(p, '#view [data-nuevo]');
      await p.waitForSelector('#sheet:not([hidden]) #t_n');
      await p.fill('#t_n', 'Lucía Prueba');
      await L.tocar(p, '#sheet [data-ok]');
      await p.waitForSelector('#view >> text=Lucía Prueba');
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'ajustes: el tema oscuro se aplica': async function (base) {
    var p = await L.abrir(base, { ruta: '#/ajustes' });
    try {
      await p.locator('input[name="tema"][value="dark"]').check({ force: true });
      await p.waitForFunction(function () { return document.documentElement.getAttribute('data-theme') === 'dark'; });
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  },

  'todas las secciones se abren sin errores': async function (base) {
    var p = await L.abrir(base);
    try {
      await L.conEjemplos(p);
      var id = await p.evaluate(function () { return Store.state.avisos[0].id; });
      var rutas = ['#/agenda', '#/avisos', '#/historico', '#/equipo', '#/ajustes', '#/nuevo', '#/aviso/' + id, '#/editar/' + id];
      for (var i = 0; i < rutas.length; i++) {
        await L.ir(p, rutas[i]);
        await p.waitForSelector('#view > *');
        var titulo = await p.locator('#topTitle').textContent();
        assert.ok(titulo && titulo.trim(), 'sin título en ' + rutas[i]);
      }
      L.sinErrores(p);
    } finally { await L.cerrar(p); }
  }
};
