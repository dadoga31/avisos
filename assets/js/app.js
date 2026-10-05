/* app.js — arranque, enrutado por hash y montaje de la interfaz. */
(function (global) {
  'use strict';

  var APP_VERSION = '2.1.1';
  global.APP_VERSION = APP_VERSION;

  var S = global.Store, U = global.UI, V = global.Views;

  var elView = document.getElementById('view');
  var elTitle = document.getElementById('topTitle');
  var elSub = document.getElementById('topSub');
  var elNavTitle = document.getElementById('navTitle');
  var elNavSub = document.getElementById('navSub');
  var elBack = document.getElementById('btnBack');
  var elActions = document.getElementById('topActions');
  var elFab = document.getElementById('fab');
  var elTabbar = document.getElementById('tabbar');
  var raiz = document.documentElement;
  var mirarTitulo = function () {};
  var tabActual = null;

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
      case 'aviso':   return { vista: V.detalle({ id: s[1] }), tab: 'agenda' };
      /* En los formularios la barra de pestañas estorba: tienen sus botones. */
      case 'nuevo':   return { vista: V.formulario(null, Atajos.deObjeto(r.params)), tab: null, sinBarra: true };
      case 'editar':  return { vista: V.formulario({ id: s[1] }), tab: null, sinBarra: true };
      case 'historico': return { vista: V.historico(), tab: 'historico' };
      case 'equipo':  return { vista: V.equipo(), tab: 'equipo' };
      case 'ajustes': return { vista: V.ajustes(), tab: 'ajustes' };
      default:        return { vista: V.agenda(), tab: 'agenda' };
    }
  }

  /* Un atajo del iPhone puede pedir que el aviso se cree sin preguntar
     (#/nuevo?...&crear=1). Se hace una sola vez por dirección: si no, recargar
     la página crearía el aviso otra vez. */
  var atajoHecho = '';

  function atenderAtajo(r) {
    if (r.seg[0] !== 'nuevo' || !r.params.crear) return false;
    /* En Safari del iPhone el aviso iría a parar a otro sitio distinto del de
       la app instalada: mejor enseñar el formulario con la advertencia que
       crearlo a escondidas donde no lo va a ver. */
    if (U.esIOS() && !U.enApp()) return false;
    var huella = location.hash;
    if (atajoHecho === huella) return false;
    atajoHecho = huella;

    var datos = Atajos.deObjeto(r.params);
    if (!datos.titulo) return false;           // sin título no hay aviso que crear
    S.guardarAviso(Atajos.aAviso(datos)).then(function (g) {
      location.replace('#/aviso/' + g.id);     // sin dejar rastro en el historial
      render();
      U.toast('Aviso ' + g.ref + ' creado');
    });
    return true;
  }

  function render(opts) {
    opts = opts || {};
    var r = parseHash();
    if (atenderAtajo(r)) return;
    /* La lista de avisos ahora es la búsqueda de la Agenda. Los enlaces de
       antes (#/avisos?v=vencidos…) llegan a ella ya filtrada. */
    if (r.seg[0] === 'avisos') {
      if (r.params.v) V.verLista(r.params.v);
      location.replace('#/agenda');
      return;
    }
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

    /* Las pantallas de primer nivel llevan título grande; las que se abren
       desde ellas (con «volver»), el título pequeño en la barra. */
    raiz.setAttribute('data-vista', v.atras ? 'apilada' : 'raiz');
    if (res.sinBarra) raiz.setAttribute('data-sin-barra', ''); else raiz.removeAttribute('data-sin-barra');
    elTitle.textContent = v.titulo || 'Avisos';
    elSub.textContent = v.sub || '';
    elSub.hidden = !v.sub;
    elNavTitle.textContent = v.titulo || 'Avisos';
    elNavSub.textContent = v.sub || '';
    elBack.hidden = !v.atras;
    elBack.dataset.atras = v.atras || '';
    elActions.innerHTML = v.acciones || '';
    elView.innerHTML = v.html || '';

    U.$$('.tab', elTabbar).forEach(function (t) {
      if (t.dataset.tab === res.tab) t.setAttribute('aria-current', 'page');
      else t.removeAttribute('aria-current');
    });
    global.Ios.lente(tabActual !== null && tabActual !== res.tab);
    tabActual = res.tab;

    if (v.mount) v.mount(elView);
    conectarFilas(elView);
    conectarAcciones(r);
    global.Ios.segmentados(elView);
    /* Al entrar en una pantalla sus bloques aparecen y sus cifras cuentan;
       si es la misma que se vuelve a pintar, se quedan quietos. */
    if (!mismaRuta) {
      global.Ios.aparecer(elView);
      global.Ios.contar(elView);
    } else {
      global.Ios.contar(elView, { sinAnimar: true });
    }

    global.scrollTo(0, scrollPrevio);
    mirarTitulo();
    rutaActual = clave;

    if (opts.mantenerFoco) {
      var f = elView.querySelector(opts.mantenerFoco);
      if (f) { f.focus(); try { f.setSelectionRange(f.value.length, f.value.length); } catch (e) {} }
    }
    actualizarBadge();
    publicarResumen();
  }

  /* Alimenta los contadores del widget de Android. */
  function publicarResumen() {
    if (!global.Nativo || !Nativo.disponible()) return;
    var r = S.resumen();
    Nativo.publicarResumen({
      dia: S.hoyISO(),
      hoy: r.hoy,
      vencidos: r.vencidos,
      abiertos: r.abiertos,
      hechosHoy: r.hechosHoy
    });
  }

  function conectarFilas(root) {
    U.$$('[data-aviso]', root).forEach(function (b) {
      b.addEventListener('click', function () { location.hash = '#/aviso/' + b.dataset.aviso; });
    });
  }

  function conectarAcciones(r) {
    var ed = elActions.querySelector('[data-editar]');
    if (ed) ed.addEventListener('click', function () { location.hash = '#/editar/' + r.seg[1]; });
    var pg = elActions.querySelector('[data-pegar]');
    if (pg) pg.addEventListener('click', function () { V.pegarAviso({}); });
    var mn = elActions.querySelector('[data-menu]');
    if (mn) mn.addEventListener('click', function () {
      var a = S.byId(S.state.avisos, r.seg[1]);
      if (a) V.menuAviso(a);
    });
  }

  /* El globo rojo de la Agenda cuenta lo vencido: lo que pide atención. */
  function actualizarBadge() {
    var n = S.resumen().vencidos;
    var b = document.getElementById('badgeAgenda');
    if (!b) return;
    b.textContent = n > 99 ? '99+' : n;
    b.hidden = !n;
  }

  /* ---------- tema ---------- */

  function aplicarTema() {
    var t = S.state.ajustes.tema || 'auto';
    if (t === 'auto') document.documentElement.removeAttribute('data-theme');
    else document.documentElement.setAttribute('data-theme', t);
    global.Ios.barraEstado();
  }

  /* ---------- eventos globales ---------- */

  function conectarChasis() {
    global.addEventListener('hashchange', function () { render({ restaurar: true }); });

    elBack.addEventListener('click', function () {
      if (history.length > 1) history.back();
      else location.hash = elBack.dataset.atras || '#/agenda';
    });

    elFab.addEventListener('click', function () { global.Ios.vibrar(); location.hash = '#/nuevo'; });

    U.$$('.tab', elTabbar).forEach(function (t) {
      t.addEventListener('click', function () {
        if (t.getAttribute('aria-current') !== 'page') global.Ios.vibrar();
      });
    });

    /* Con el tema en automático, la barra de estado sigue al sistema. */
    if (global.matchMedia) {
      var mq = global.matchMedia('(prefers-color-scheme: dark)');
      var alCambiar = function () { global.Ios.barraEstado(); };
      if (mq.addEventListener) mq.addEventListener('change', alCambiar);
      else if (mq.addListener) mq.addListener(alCambiar);
    }
    global.addEventListener('resize', function () { global.Ios.lente(false); });

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
      /* Al volver a la app puede haber entrado correo, o un aviso dejado por
         un atajo, mientras estaba fuera. */
      recogerCorreo();
      recogerBuzon();
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

  /* Los correos los descarga la parte nativa; el aviso se crea aquí, en
     cuanto la app está delante. */
  function recogerCorreo() {
    if (!global.Correo || !Correo.disponible()) return;
    Correo.procesarPendientes().then(function (r) {
      if (!r.creados) return;
      U.toast(r.creados === 1 ? 'Nuevo aviso desde el correo' : r.creados + ' avisos nuevos desde el correo');
      render({ restaurar: true });
    }).catch(function (e) { console.warn('Correo no procesado:', e); });
  }

  /* Los avisos que deja un atajo del iPhone esperan en el servidor hasta que
     alguien abre la app: el buzón entrega y vacía de una vez. */
  var recogiendo = false;

  function recogerBuzon(opts) {
    opts = opts || {};
    if (recogiendo || !global.Buzon || !Buzon.activo()) return Promise.resolve(null);
    recogiendo = true;
    return Buzon.recoger().then(function (r) {
      recogiendo = false;
      if (r && r.creados) {
        U.toast(r.creados === 1 ? 'Nuevo aviso desde el atajo' : r.creados + ' avisos nuevos desde el atajo');
        render({ restaurar: true });
      } else if (opts.avisar) {
        U.toast('No había ningún aviso esperando');
        render({ restaurar: true });
      }
      return r;
    }, function (e) {
      recogiendo = false;
      console.warn('Buzón no recogido:', e);
      if (opts.avisar) { U.toast('El buzón falla: ' + (e && e.message || e)); render({ restaurar: true }); }
      throw e;
    });
  }

  /* ---------- arranque ---------- */

  function arrancar() {
    global.Ios.esquinas();
    global.Ios.luz();
    global.Ios.barraEstado();
    mirarTitulo = global.Ios.vigilarTitulo();
    S.load().then(function () {
      aplicarTema();
      conectarChasis();
      observador.observe(elView, { childList: true });
      if (!location.hash) location.replace('#/agenda');
      render();
      recogerCorreo();
      recogerBuzon();
    }).catch(function (e) {
      console.error(e);
      elView.innerHTML = U.vacioHTML({
        titulo: 'No se pudo abrir la base de datos',
        texto: 'Si estás en modo incógnito o con el almacenamiento bloqueado, la app no puede guardar datos. ' + (e && e.message || '')
      });
    });

    /* En la app de Android los archivos ya son locales: el service worker
       solo añadiría una caché que puede quedarse vieja. */
    var nativo = global.Nativo && Nativo.disponible();
    if ('serviceWorker' in navigator && location.protocol !== 'file:' && !nativo) {
      global.addEventListener('load', prepararServiceWorker);
    }
  }

  /* Publicada en Vercel, la app se actualiza sola pero sin sobresaltos: se
     avisa y se cambia cuando el usuario quiere. */
  function prepararServiceWorker() {
    var recargando = false;
    /* Que la página esté controlada quiere decir que ya había una versión
       instalada: solo entonces un trabajador en espera es una actualización, y
       no la primera instalación. */
    var controlada = function () { return !!navigator.serviceWorker.controller; };
    var habiaControl = controlada();

    navigator.serviceWorker.addEventListener('controllerchange', function () {
      /* La primera instalación también cambia el control, y ahí no hay nada
         que recargar. Pero si el cambio es porque hemos pedido pasar a la
         versión nueva, sí: en la pantalla siguen los ficheros viejos. */
      if (recargando || (!habiaControl && !pedimosCambio)) return;
      recargando = true;
      location.reload();
    });

    navigator.serviceWorker.register('sw.js').then(function (reg) {
      registro = reg;
      if (reg.waiting && controlada()) avisarDeVersion(reg);

      reg.addEventListener('updatefound', function () {
        var entrante = reg.installing || reg.waiting;
        if (!entrante) return;
        /* Puede haber terminado de instalarse antes de que lleguemos aquí. */
        if (entrante.state === 'installed') { if (controlada()) avisarDeVersion(reg); return; }
        entrante.addEventListener('statechange', function () {
          if (entrante.state === 'installed' && controlada()) avisarDeVersion(reg);
        });
      });

      /* Al volver a la app se mira si hay algo nuevo publicado. */
      document.addEventListener('visibilitychange', function () {
        if (document.visibilityState === 'visible') reg.update().catch(function () {});
      });
    }).catch(function (e) {
      console.warn('Service worker no registrado:', e);
    });
  }

  var registro = null;
  var pedimosCambio = false;

  function avisarDeVersion(reg) {
    U.toast('Hay una versión nueva de la app', {
      accion: 'Actualizar',
      fijo: true,
      alPulsar: function () { aplicarVersion(reg); }
    });
  }

  function aplicarVersion(reg) {
    if (!reg || !reg.waiting) return false;
    pedimosCambio = true;
    reg.waiting.postMessage({ tipo: 'saltar' });
    return true;
  }

  /* Mirar a mano si hay algo nuevo publicado, para no depender de pillar el
     aviso al vuelo. */
  function buscarActualizacion() {
    if (!registro) return Promise.resolve('sin-sw');
    if (aplicarVersion(registro)) return Promise.resolve('aplicando');
    return registro.update().then(function () {
      /* Instalar lleva un momento: se le da margen antes de decir que no hay
         nada, que si no siempre diría que está al día. */
      return new Promise(function (res) {
        var intentos = 0;
        (function mirar() {
          if (aplicarVersion(registro)) return res('aplicando');
          if (++intentos > 12) return res('al-dia');
          setTimeout(mirar, 500);
        })();
      });
    }, function () { return 'fallo'; });
  }

  /* Salida de emergencia: si la caché se queda a medias (una página nueva con
     los ficheros viejos, por ejemplo), esto la vacía y vuelve a empezar. Los
     avisos viven en otro sitio y no se tocan. */
  function reinstalar() {
    var pasos = [];
    if (global.caches) {
      pasos.push(caches.keys().then(function (ks) {
        return Promise.all(ks.map(function (k) { return caches.delete(k); }));
      }));
    }
    if (navigator.serviceWorker) {
      pasos.push(navigator.serviceWorker.getRegistrations().then(function (rs) {
        return Promise.all(rs.map(function (r) { return r.unregister(); }));
      }));
    }
    return Promise.all(pasos).then(function () {
      location.reload();
    });
  }

  global.App = {
    render: render, aplicarTema: aplicarTema, version: APP_VERSION,
    recogerCorreo: recogerCorreo, recogerBuzon: recogerBuzon,
    buscarActualizacion: buscarActualizacion, reinstalar: reinstalar
  };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', arrancar);
  else arrancar();
})(window);
