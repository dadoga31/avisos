/* ui.js — utilidades de interfaz: formato, componentes, hoja inferior y avisos flotantes. */
(function (global) {
  'use strict';

  var S = global.Store;

  /* ---------- básicos ---------- */

  function esc(v) {
    return String(v == null ? '' : v)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function el(html) {
    var t = document.createElement('template');
    t.innerHTML = String(html).trim();
    return t.content.firstElementChild;
  }

  function frag(html) {
    var t = document.createElement('template');
    t.innerHTML = String(html);
    return t.content;
  }

  function $(sel, root) { return (root || document).querySelector(sel); }
  function $$(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  /* ---------- fechas ---------- */

  var DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
  var MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

  function aDate(iso) {
    var p = String(iso).split('-');
    return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
  }

  function fmtFecha(iso, opts) {
    if (!iso) return 'Sin fecha';
    opts = opts || {};
    var hoy = S.hoyISO();
    if (iso === hoy) return 'Hoy';
    if (iso === S.sumaDias(hoy, 1)) return 'Mañana';
    if (iso === S.sumaDias(hoy, -1)) return 'Ayer';
    var d = aDate(iso);
    var txt = DIAS[d.getDay()] + ' ' + d.getDate() + ' ' + MESES[d.getMonth()];
    if (!opts.corto && d.getFullYear() !== new Date().getFullYear()) txt += ' ' + d.getFullYear();
    return txt;
  }

  function fmtFechaLarga(iso) {
    if (!iso) return 'Sin fecha';
    var d = aDate(iso);
    var dias = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
    var meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio',
      'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    return dias[d.getDay()] + ', ' + d.getDate() + ' de ' + meses[d.getMonth()] + ' de ' + d.getFullYear();
  }

  function fmtCuando(a) {
    var f = fmtFecha(a.fecha, { corto: true });
    return a.hora ? f + ' · ' + a.hora : f;
  }

  function fmtSello(ts) {
    if (!ts) return '';
    var d = new Date(ts);
    var iso = S.hoyISO(d);
    var hm = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
    var dia = fmtFecha(iso, { corto: true });
    return dia.charAt(0).toLowerCase() + dia.slice(1) + ' · ' + hm;
  }

  function diasDe(iso) {
    if (!iso) return 0;
    var ms = aDate(S.hoyISO()) - aDate(iso);
    return Math.round(ms / 86400000);
  }

  function plural(n, singular, pluralS) {
    return n + ' ' + (Math.abs(n) === 1 ? singular : (pluralS || singular + 's'));
  }

  function fmtHoras(n) {
    if (!n) return '0 h';
    return (Math.round(n * 100) / 100).toString().replace('.', ',') + ' h';
  }

  /* ---------- componentes ---------- */

  var ICO_CHECK = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7"/></svg>';
  var ICO_CURSO = '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/></svg>';
  var ICO_CANCEL = '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="m8.7 8.7 6.6 6.6"/></svg>';

  function pill(estadoId) {
    var e = S.catalogo(S.ESTADOS, estadoId);
    return '<span class="pill pill--' + esc(estadoId) + '">' + esc(e.label) + '</span>';
  }

  function who(tecnicoId, opts) {
    opts = opts || {};
    var t = S.tecnico(tecnicoId);
    if (!t) {
      return '<span class="who who--none"><span class="who__dot">?</span>' +
        (opts.soloAvatar ? '' : '<span class="who__name">Sin asignar</span>') + '</span>';
    }
    return '<span class="who"><span class="who__dot" style="background:' + esc(t.color || '#71717a') + '">' +
      esc(S.iniciales(t.nombre)) + '</span>' +
      (opts.soloAvatar ? '' : '<span class="who__name">' + esc(t.nombre) + '</span>') + '</span>';
  }

  function avisoRow(a, opts) {
    opts = opts || {};
    var tarde = S.vencido(a);
    var cuando = fmtCuando(a);
    var sub = [];
    if (a.cliente && a.cliente.nombre) sub.push(a.cliente.nombre);
    sub.push(S.catalogo(S.TIPOS, a.tipo).label + ' · ' + S.catalogo(S.SISTEMAS, a.sistema).label);

    var prio = (a.prioridad === 'urgente' || a.prioridad === 'alta')
      ? '<span class="prio prio--' + esc(a.prioridad) + '">' + esc(S.catalogo(S.PRIORIDADES, a.prioridad).label) + '</span>'
      : '';

    var fila = '' +
      '<button class="avrow avrow--e-' + esc(a.estado) + '" data-aviso="' + esc(a.id) + '" type="button">' +
        '<span class="avrow__flag" aria-hidden="true"></span>' +
        '<span class="avrow__main">' +
          '<span class="avrow__top">' +
            '<span class="avrow__ref">' + esc(a.ref || '—') + '</span>' +
            pill(a.estado) + prio +
          '</span>' +
          '<span class="avrow__title">' + esc(a.titulo || '(sin título)') + '</span>' +
          '<span class="avrow__sub">' + esc(sub.join(' — ')) + '</span>' +
        '</span>' +
        '<span class="avrow__side">' +
          '<span class="avrow__when' + (tarde ? ' avrow__when--late' : '') + '">' +
            (tarde ? '⚠ ' : '') + esc(cuando) +
          '</span>' +
          who(a.asignadoA, { soloAvatar: true }) +
        '</span>' +
      '</button>';

    if (!opts.swipe) return fila;
    /* Los avisos ya cerrados no se deslizan: no hay nada que cambiar. */
    if (!S.abierto(a)) return '<div class="swipe swipe--fija">' + fila + '</div>';

    return '<div class="swipe" data-swipe="' + esc(a.id) + '">' +
      '<div class="swipe__acciones">' +
        '<button class="swipe__acc swipe__acc--curso" data-estado="en_curso" type="button">' +
          ICO_CURSO + '<span>En curso</span></button>' +
        '<button class="swipe__acc swipe__acc--cancel" data-estado="cancelado" type="button">' +
          ICO_CANCEL + '<span>Cancelar</span></button>' +
      '</div>' +
      '<div class="swipe__hecho">' + ICO_CHECK + '<span>Hecho</span></div>' +
      '<div class="swipe__front">' + fila + '</div>' +
    '</div>';
  }

  function lista(avisos, vacio, opts) {
    if (!avisos.length) return vacioHTML(vacio);
    return '<div class="avlist">' + avisos.map(function (a) {
      return avisoRow(a, opts);
    }).join('') + '</div>';
  }

  function vacioHTML(cfg) {
    cfg = cfg || {};
    return '<div class="empty">' +
      '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v14H4z"/><path d="M8 10h8M8 14h5"/></svg>' +
      '<h3>' + esc(cfg.titulo || 'Nada por aquí') + '</h3>' +
      '<p>' + esc(cfg.texto || 'Cuando añadas avisos aparecerán en esta lista.') + '</p>' +
      (cfg.accion ? '<div class="spacer"></div><button class="btn btn--sm" data-accion="' + esc(cfg.accion) + '">' + esc(cfg.accionLabel || 'Añadir') + '</button>' : '') +
      '</div>';
  }

  function seccion(titulo, contenidoHTML, meta) {
    return '<section class="section">' +
      '<div class="section__head">' +
        '<h2 class="section__title">' + titulo + '</h2>' +
        (meta ? '<span class="section__meta">' + esc(meta) + '</span>' : '') +
      '</div>' + contenidoHTML + '</section>';
  }

  function opciones(catalogo, seleccion, vacioLabel) {
    var out = vacioLabel ? '<option value="">' + esc(vacioLabel) + '</option>' : '';
    return out + catalogo.map(function (c) {
      return '<option value="' + esc(c.id) + '"' + (c.id === seleccion ? ' selected' : '') + '>' + esc(c.label) + '</option>';
    }).join('');
  }

  /* ---------- avisos flotantes ---------- */

  var toastT = null;
  function ocultarToast() {
    document.getElementById('toast').classList.remove('toast--on');
  }

  function toast(msg, opts) {
    opts = opts || {};
    var n = document.getElementById('toast');
    n.innerHTML = '';
    n.appendChild(document.createTextNode(msg));
    n.classList.toggle('toast--accion', !!opts.accion);
    if (opts.accion) {
      var b = document.createElement('button');
      b.className = 'toast__btn';
      b.type = 'button';
      b.textContent = opts.accion;
      b.addEventListener('click', function () {
        clearTimeout(toastT);
        ocultarToast();
        if (opts.alPulsar) opts.alPulsar();
      });
      n.appendChild(b);
    }
    n.classList.add('toast--on');
    clearTimeout(toastT);
    toastT = setTimeout(ocultarToast, opts.accion ? 5500 : 2600);
  }

  /* ---------- hoja inferior ---------- */

  var sheetCierre = null;

  function abrirSheet(titulo, contenido, onMount) {
    var s = document.getElementById('sheet');
    document.getElementById('sheetTitle').textContent = titulo;
    var body = document.getElementById('sheetBody');
    body.innerHTML = '';
    if (typeof contenido === 'string') body.appendChild(frag(contenido));
    else body.appendChild(contenido);
    s.hidden = false;
    document.body.style.overflow = 'hidden';
    document.body.classList.add('sheet-abierta');
    if (onMount) onMount(body);
    var primero = body.querySelector('input,textarea,select,button');
    if (primero && !('ontouchstart' in window)) primero.focus();
    return body;
  }

  function cerrarSheet() {
    var s = document.getElementById('sheet');
    if (s.hidden) return;
    s.hidden = true;
    document.getElementById('sheetBody').innerHTML = '';
    document.body.style.overflow = '';
    document.body.classList.remove('sheet-abierta');
    if (sheetCierre) { var f = sheetCierre; sheetCierre = null; f(null); }
  }

  function confirmar(titulo, texto, opts) {
    opts = opts || {};
    return new Promise(function (resolve) {
      sheetCierre = resolve;
      abrirSheet(titulo,
        '<p class="small muted" style="margin-bottom:16px">' + esc(texto) + '</p>' +
        '<div class="btnrow btnrow--split">' +
          '<button class="btn" data-no type="button">' + esc(opts.cancelar || 'Cancelar') + '</button>' +
          '<button class="btn ' + (opts.peligro ? 'btn--danger' : 'btn--primary') + '" data-si type="button">' + esc(opts.aceptar || 'Aceptar') + '</button>' +
        '</div>',
        function (body) {
          body.querySelector('[data-si]').addEventListener('click', function () {
            sheetCierre = null; cerrarSheet(); resolve(true);
          });
          body.querySelector('[data-no]').addEventListener('click', function () {
            sheetCierre = null; cerrarSheet(); resolve(false);
          });
        });
    });
  }

  function pedirTexto(titulo, cfg) {
    cfg = cfg || {};
    return new Promise(function (resolve) {
      sheetCierre = resolve;
      var campo = cfg.multilinea
        ? '<textarea class="textarea" id="pt" placeholder="' + esc(cfg.placeholder || '') + '">' + esc(cfg.valor || '') + '</textarea>'
        : '<input class="input" id="pt" type="' + esc(cfg.tipo || 'text') + '" value="' + esc(cfg.valor || '') + '" placeholder="' + esc(cfg.placeholder || '') + '">';
      abrirSheet(titulo,
        '<div class="field">' +
          (cfg.label ? '<label class="field__label" for="pt">' + esc(cfg.label) + '</label>' : '') +
          campo +
        '</div>' +
        '<div class="btnrow btnrow--split">' +
          '<button class="btn" data-no type="button">Cancelar</button>' +
          '<button class="btn btn--primary" data-si type="button">' + esc(cfg.aceptar || 'Guardar') + '</button>' +
        '</div>',
        function (body) {
          var input = body.querySelector('#pt');
          function ok() {
            var v = input.value;
            sheetCierre = null; cerrarSheet(); resolve(v);
          }
          body.querySelector('[data-si]').addEventListener('click', ok);
          body.querySelector('[data-no]').addEventListener('click', function () {
            sheetCierre = null; cerrarSheet(); resolve(null);
          });
          if (!cfg.multilinea) {
            input.addEventListener('keydown', function (e) { if (e.key === 'Enter') ok(); });
          }
          input.focus();
          if (input.setSelectionRange && input.value) {
            try { input.setSelectionRange(input.value.length, input.value.length); } catch (e) {}
          }
        });
    });
  }

  function descargar(nombre, contenido, mime) {
    var blob = contenido instanceof Blob ? contenido : new Blob([contenido], { type: mime || 'application/json' });
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url; a.download = nombre;
    document.body.appendChild(a);
    a.click();
    setTimeout(function () { URL.revokeObjectURL(url); a.remove(); }, 400);
  }

  global.UI = {
    esc: esc, el: el, frag: frag, $: $, $$: $$,
    fmtFecha: fmtFecha, fmtFechaLarga: fmtFechaLarga, fmtCuando: fmtCuando, fmtSello: fmtSello,
    fmtHoras: fmtHoras, diasDe: diasDe, plural: plural,
    pill: pill, who: who, avisoRow: avisoRow, lista: lista, vacioHTML: vacioHTML,
    seccion: seccion, opciones: opciones,
    toast: toast, abrirSheet: abrirSheet, cerrarSheet: cerrarSheet,
    confirmar: confirmar, pedirTexto: pedirTexto, descargar: descargar
  };
})(window);
