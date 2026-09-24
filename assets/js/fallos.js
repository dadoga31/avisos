/* fallos.js — recoge los errores que se escapan y los deja a la vista.
   Sin esto, un fallo dentro de la app se traga en la consola, que en un
   móvil no hay forma de mirar. Se carga el primero, para pillar también
   los de los demás módulos. */
(function (global) {
  'use strict';

  var CLAVE = 'avisos_fallos';
  var MAX = 12;

  function leer() {
    try {
      var crudo = localStorage.getItem(CLAVE);
      return crudo ? (JSON.parse(crudo) || []) : [];
    } catch (e) { return []; }
  }

  function escribir(lista) {
    try { localStorage.setItem(CLAVE, JSON.stringify(lista.slice(0, MAX))); } catch (e) {}
  }

  function registrar(mensaje, donde) {
    var texto = String(mensaje || 'Error desconocido');
    var lista = leer();

    /* El mismo fallo repetido no llena la lista: se cuenta. */
    if (lista[0] && lista[0].mensaje === texto) {
      lista[0].veces = (lista[0].veces || 1) + 1;
      lista[0].ts = new Date().toISOString();
    } else {
      lista.unshift({ ts: new Date().toISOString(), mensaje: texto, donde: String(donde || ''), veces: 1 });
    }
    escribir(lista);

    if (global.UI && UI.toast) {
      UI.toast('Ha fallado algo: ' + texto.slice(0, 80), {
        accion: 'Ver',
        alPulsar: function () { if (global.App) location.hash = '#/ajustes'; }
      });
    }
    return texto;
  }

  function lista() { return leer(); }
  function limpiar() { escribir([]); }

  function comoTexto() {
    var app = global.APP_VERSION || '?';
    return leer().map(function (f) {
      return f.ts + (f.veces > 1 ? ' (x' + f.veces + ')' : '') + '\n' + f.mensaje +
        (f.donde ? '\n' + f.donde : '');
    }).join('\n\n') || 'Sin fallos registrados.'
      + '\n\nVersión ' + app;
  }

  global.addEventListener('error', function (e) {
    /* Los fallos al cargar una imagen o un script no traen mensaje. */
    if (e && e.target && e.target !== global && e.target.tagName) {
      registrar('No se pudo cargar ' + e.target.tagName.toLowerCase(), e.target.src || e.target.href || '');
      return;
    }
    var m = (e && e.message) || 'Error';
    var donde = e && e.filename ? (e.filename.split('/').pop() + ':' + e.lineno) : '';
    registrar(m, donde);
  }, true);

  global.addEventListener('unhandledrejection', function (e) {
    var r = e && e.reason;
    var m = (r && (r.message || r.name)) || String(r || 'Promesa rechazada');
    registrar(m, r && r.stack ? String(r.stack).split('\n')[1] || '' : '');
  });

  global.Fallos = {
    registrar: registrar,
    lista: lista,
    limpiar: limpiar,
    comoTexto: comoTexto
  };
})(window);
