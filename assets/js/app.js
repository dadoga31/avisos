/* app.js — arranque, enrutado por hash y montaje de la interfaz. */
(function (global) {
  'use strict';

  var APP_VERSION = '1.2.0';
  global.APP_VERSION = APP_VERSION;

  var S = global.Store, U = global.UI, V = global.Views;

  var elView = document.getElementById('view');
  var elTitle = document.getElementById('topTitle');
  var elSub = document.getElementById('topSub');
  var elBack = document.getElementById('btnBack');
  var elActions = document.getElementById('topActions');
  var elFab = document.getElementById('fab');
  var elTabbar = document.getElementById('tabbar');

  var rutaActual = '';
  var scrolls = {};
  var promptInstalar = null;

  /* ---------- enrutado ---------- */

  function parseHash() {
    var h = (location.hash || '#/agenda').replace(/^#\/?/, '');
    var partes = h.split('?');
    var segs = partes[0].split('/').filter(Boolean);
    var params = {};
    (partes[1] || '').split('&').filter(Boolean).forEach(function (kv) {
      var p = kv.split('=');
      params[decodeURIComponent(p[0])] = decodeURIComponent(p[1] || '');
    });
    return { seg: segs, params: params, raw: partes[0] };
  }

  function resolver(r) {
    var s = r.seg;
    switch (s[0]) {
      case 'avisos':  return { vista: V.avisos(r.params), tab: 'avisos' };
      case 'aviso':   return { vista: V.detalle({ id: s[1] }), tab: 'avisos', sinFab: true };
      case 'nuevo':   return { vista: V.formulario(null), tab: null, sinFab: true };
      case 'editar':  return { vista: V.formulario({ id: s[1] }), tab: null, sinFab: true };
      case 'equipo':  return { vista: V.equipo(), tab: 'equipo', sinFab: true };
      case 'ajustes': return { vista: V.ajustes(), tab: 'ajustes', sinFab: true };
      default:        return { vista: V.agenda(), tab: 'agenda' };
    }
  }

  function render(opts) {
    opts = opts || {};
    var r = parseHash();
    var clave = r.raw;
    var mismaRuta = clave === rutaActual;

    if (!mismaRuta && rutaActual) scrolls[rutaActual] = global.scrollY;
    var scrollPrevio = mismaRuta ? global.scrollY : (opts.restaurar ? (scrolls[clave] || 0) : 0);

    V.liberarURLs();

    var res;
    try {
      res = resolver(r);
    } catch (e) {
      console.error(e);
      elView.innerHTML = U.vacioHTML({ titulo: 'Algo ha fallado', texto: String(e && e.message || e) });
      return;
    }
    var v = res.vista;

    elTitle.textContent = v.titulo || 'Avisos';
    elSub.textContent = v.sub || '';
    elSub.hidden = !v.sub;
    elBack.hidden = !v.atras;
    elBack.dataset.atras = v.atras || '';
    elActions.innerHTML = v.acciones || '';
    elView.innerHTML = v.html || '';

    U.$$('.tab', elTabbar).forEach(function (t) {
      if (t.dataset.tab === res.tab) t.setAttribute('aria-current', 'page');
      else t.removeAttribute('aria-current');
    });
    elFab.hidden = !!res.sinFab;

    if (v.mount) v.mount(elView);
    conectarFilas(elView);
    conectarAcciones(r);

    global.scrollTo(0, scrollPrevio);
    rutaActual = clave;

    if (opts.mantenerFoco) {
      var f = elView.querySelector(opts.mantenerFoco);
      if (f) { f.focus(); try { f.setSelectionRange(f.value.length, f.value.length); } catch (e) {} }
    }
    actualizarBadge();
  }

  function conectarFilas(root) {
    U.$$('[data-aviso]', root).forEach(function (b) {
      b.addEventListener('click', function () { location.hash = '#/aviso/' + b.dataset.aviso; });
    });
  }

  function conectarAcciones(r) {
    var ed = elActions.querySelector('[data-editar]');
    if (ed) ed.addEventListener('click', function () { location.hash = '#/editar/' + r.seg[1]; });
    var mn = elActions.querySelector('[data-menu]');
    if (mn) mn.addEventListener('click', function () {
      var a = S.byId(S.state.avisos, r.seg[1]);
      if (a) V.menuAviso(a);
    });
  }

  function actualizarBadge() {
    var n = S.resumen().abiertos;
    var b = document.getElementById('badgeAbiertos');
    b.textContent = n > 99 ? '99+' : n;
    b.hidden = !n;
  }

  /* ---------- tema ---------- */

  function aplicarTema() {
    var t = S.state.ajustes.tema || 'auto';
    if (t === 'auto') document.documentElement.removeAttribute('data-theme');
    else document.documentElement.setAttribute('data-theme', t);
  }

  /* ---------- eventos globales ---------- */

  function conectarChasis() {
    global.addEventListener('hashchange', function () { render({ restaurar: true }); });

    elBack.addEventListener('click', function () {
      if (history.length > 1) history.back();
      else location.hash = elBack.dataset.atras || '#/agenda';
    });

    elFab.addEventListener('click', function () { location.hash = '#/nuevo'; });

    U.$$('[data-close]', document.getElementById('sheet')).forEach(function (n) {
      n.addEventListener('click', U.cerrarSheet);
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') U.cerrarSheet();
    });

    /* si el móvil se queda abierto y cambia el día, «Hoy» debe seguir siendo hoy */
    var diaVisible = S.hoyISO();
    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState !== 'visible') return;
      if (S.hoyISO() === diaVisible) return;
      diaVisible = S.hoyISO();
      render({ restaurar: true });
    });

    global.addEventListener('beforeinstallprompt', function (e) {
      e.preventDefault();
      promptInstalar = e;
      var b = document.getElementById('btnInstalar');
      if (b) mostrarInstalar(b);
    });
  }

  function mostrarInstalar(b) {
    b.hidden = false;
    b.addEventListener('click', function () {
      if (!promptInstalar) return;
      promptInstalar.prompt();
      promptInstalar.userChoice.then(function () { promptInstalar = null; b.hidden = true; });
    }, { once: true });
  }

  /* observa la aparición del botón de instalar al entrar en Ajustes */
  var observador = new MutationObserver(function () {
    var b = document.getElementById('btnInstalar');
    if (b && b.hidden && promptInstalar) mostrarInstalar(b);
  });

  /* ---------- arranque ---------- */

  function arrancar() {
    S.load().then(function () {
      aplicarTema();
      conectarChasis();
      observador.observe(elView, { childList: true });
      if (!location.hash) location.replace('#/agenda');
      render();
    }).catch(function (e) {
      console.error(e);
      elView.innerHTML = U.vacioHTML({
        titulo: 'No se pudo abrir la base de datos',
        texto: 'Si estás en modo incógnito o con el almacenamiento bloqueado, la app no puede guardar datos. ' + (e && e.message || '')
      });
    });

    if ('serviceWorker' in navigator && location.protocol !== 'file:') {
      global.addEventListener('load', function () {
        navigator.serviceWorker.register('sw.js').catch(function (e) {
          console.warn('Service worker no registrado:', e);
        });
      });
    }
  }

  global.App = { render: render, aplicarTema: aplicarTema, version: APP_VERSION };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', arrancar);
  else arrancar();
})(window);
