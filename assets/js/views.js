/* views.js — pantallas de la aplicación. Cada vista devuelve
   { titulo, sub, acciones, html, mount(root) } */
(function (global) {
  'use strict';

  var S = global.Store, U = global.UI;
  var esc = U.esc;

  var ICON = {
    lupa: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="M16 16l4.5 4.5"/></svg>',
    x: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>',
    lapiz: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20l4-1 10-10-3-3L5 16l-1 4z"/><path d="M14.5 5.5l3 3"/></svg>',
    mas: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
    puntos: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="5" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="12" cy="19" r="1.4"/></svg>',
    tel: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h3l1.5 4.5-2 1.5a12 12 0 006.5 6.5l1.5-2L21 15v3a2 2 0 01-2.2 2A16.5 16.5 0 014 5.2 2 2 0 016 3z"/></svg>',
    mapa: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21s7-6.3 7-11a7 7 0 10-14 0c0 4.7 7 11 7 11z"/><circle cx="12" cy="10" r="2.5"/></svg>',
    camara: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 8h3.5L8 5.5h8L17.5 8H21v12H3z"/><circle cx="12" cy="13.5" r="3.5"/></svg>',
    papelera: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13"/></svg>',
    copia: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="9" y="9" width="11" height="11" rx="1"/><path d="M15 6H5v10"/></svg>',
    compartir: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12M8 7l4-4 4 4"/><path d="M5 14v5h14v-5"/></svg>',
    filtro: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16l-6 7v6l-4-2v-4z"/></svg>',
    calendario: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="1"/><path d="M3 10h18M8 3v4M16 3v4M12 14v4M10 16h4"/></svg>',
    clip: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11.5 11.8 19.7a4.6 4.6 0 0 1-6.5-6.5l8.4-8.4a3 3 0 0 1 4.3 4.3l-8.2 8.2a1.5 1.5 0 0 1-2.1-2.1l7.5-7.5"/></svg>',
    sobre: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="1.5"/><path d="m3.5 6.5 8.5 6.5 8.5-6.5"/></svg>',
    abrirFuera: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 4h6v6"/><path d="M20 4l-8.5 8.5"/><path d="M19 14v5H5V5h5"/></svg>',
    desliza: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 8 3.5 11.5 7 15M17 8l3.5 3.5L17 15M3.5 11.5h17"/></svg>',
    chevron: '<svg class="fila__chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5.5 6.5 6.5L9 18.5"/></svg>',
    personas: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8.5" r="3.2"/><path d="M3.5 19.5c0-3.2 2.5-5.3 5.5-5.3s5.5 2.1 5.5 5.3"/><path d="M15.5 5.6a3 3 0 0 1 0 5.8M17.6 14.6c1.8.7 2.9 2.4 2.9 4.9"/></svg>',
    exportar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 15V4M8 8l4-4 4 4"/><path d="M5 13v5.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V13"/></svg>',
    importar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4v11M8 11l4 4 4-4"/><path d="M5 13v5.5A1.5 1.5 0 0 0 6.5 20h11a1.5 1.5 0 0 0 1.5-1.5V13"/></svg>',
    tabla: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="5" width="16" height="14" rx="2"/><path d="M4 10h16M4 14.5h16M10 10v9"/></svg>',
    campana: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 1.5H5z"/><path d="M10 20.5h4"/></svg>',
    movil: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="7" y="3" width="10" height="18" rx="2.5"/><path d="M11 18h2"/></svg>',
    ayuda: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5"/><path d="M9.6 9.5a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4"/><circle cx="12" cy="16.7" r=".6" fill="currentColor"/></svg>',
    actualizar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19.5 9A8 8 0 0 0 5 7.5M4.5 15A8 8 0 0 0 19 16.5"/><path d="M5 3.5v4h4M19 20.5v-4h-4"/></svg>',
    reinstalar: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4.5 12a7.5 7.5 0 1 0 2.2-5.3"/><path d="M4.5 4.5v4h4"/></svg>',
    matraz: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9.5 3.5h5M10.5 3.5v5.2L5.4 17.6A2 2 0 0 0 7.1 20.5h9.8a2 2 0 0 0 1.7-2.9l-5.1-8.9V3.5"/><path d="M7.8 14.5h8.4"/></svg>',
    numero: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9.5 4 8 20M16 4l-1.5 16M4.5 9h15M4 15h15"/></svg>',
    disco: '<svg viewBox="0 0 24 24" aria-hidden="true"><ellipse cx="12" cy="6.5" rx="7" ry="2.8"/><path d="M5 6.5v11c0 1.5 3.1 2.8 7 2.8s7-1.3 7-2.8v-11M5 12c0 1.5 3.1 2.8 7 2.8s7-1.3 7-2.8"/></svg>'
  };

  /* Fila de una lista agrupada, como en Ajustes del iPhone: icono sobre
     color, título, valor a la derecha y flecha si lleva a otra pantalla. */
  function fila(o) {
    var etiqueta = o.href ? 'a' : (o.label ? 'label' : (o.estatica ? 'div' : 'button'));
    var attrs = (o.href ? ' href="' + esc(o.href) + '"' : '') + (etiqueta === 'button' ? ' type="button"' : '') + (o.attrs || '');
    return '<li><' + etiqueta + ' class="fila' + (o.control ? ' fila--control' : '') + (o.clase ? ' ' + o.clase : '') + '"' + attrs + '>' +
      (o.icono ? '<span class="fila__icono" style="--c:' + o.color + '">' + ICON[o.icono] + '</span>' : '') +
      '<span class="fila__main"><span class="fila__titulo">' + o.titulo + '</span>' +
        (o.sub ? '<span class="fila__sub">' + o.sub + '</span>' : '') + '</span>' +
      (o.control || '') +
      (o.valor != null ? '<span class="fila__valor"' + (o.idValor ? ' id="' + o.idValor + '"' : '') + '>' + o.valor + '</span>' : '') +
      (o.chevron ? ICON.chevron : '') +
      '</' + etiqueta + '></li>';
  }

  function bloque(titulo, filas, pie, opts) {
    opts = opts || {};
    return '<section class="bloque">' +
      (titulo ? '<h2 class="grupo__cabecera">' + titulo + '</h2>' : '') +
      '<ul class="grupo' + (opts.sinIconos ? '' : ' grupo--iconos') + '">' + filas + '</ul>' +
      (pie ? '<p class="grupo__pie">' + pie + '</p>' : '') +
      '</section>';
  }

  /* Colores del sistema para los iconos de Ajustes */
  var COLOR = {
    azul: '#007aff', verde: '#34c759', indigo: '#5856d6', naranja: '#ff9500',
    rojo: '#ff3b30', gris: '#8e8e93', turquesa: '#30b0c7', morado: '#af52de'
  };

  /* =========================================================
     AGENDA
     Lo abierto, agrupado por fecha, y encima el buscador. Al escribir o
     filtrar, la misma pantalla pasa a ser la lista de resultados (abiertos y
     cerrados); «Cancelar» la devuelve a la agenda.
     ========================================================= */

  function agenda() {
    var r = S.resumen();
    var buscando = enBusqueda();
    var html = buscador(buscando) + (buscando ? resultados() : agendaDelDia(r));

    return {
      titulo: 'Agenda',
      sub: buscando
        ? 'Buscando en ' + U.plural(S.state.avisos.length, 'aviso')
        : U.plural(r.abiertos, 'aviso abierto', 'avisos abiertos') + ' · ' + r.total + ' en total',
      acciones: '<button class="iconbtn" data-pegar type="button" aria-label="Pegar un aviso copiado">' + ICON.copia + '</button>',
      html: html,
      mount: function (root) {
        montarBuscador(root);
        /* Los accesos de la tarjeta de hoy abren la lista ya filtrada. */
        U.$$('[data-k]', root).forEach(function (b) {
          b.addEventListener('click', function () {
            aplicarVista(b.dataset.k);
            global.scrollTo(0, 0);
            global.App.render();
          });
        });
        var ej = root.querySelector('[data-accion="ejemplo"]');
        if (ej) ej.addEventListener('click', function () {
          S.datosDeEjemplo().then(function () { U.toast('Datos de ejemplo cargados'); global.App.render(); });
        });
        var pista = root.querySelector('[data-pista]');
        if (pista) pista.addEventListener('click', ocultarPista);
        conectarGestos(root);
      }
    };
  }

  function agendaDelDia(r) {
    var hoy = S.hoyISO();
    var abiertos = S.state.avisos.filter(S.abierto);

    function grupo(pred) { return S.ordenar(abiertos.filter(pred), 'fecha'); }

    var vencidos = grupo(function (a) { return a.fecha && a.fecha < hoy; });
    var deHoy = grupo(function (a) { return a.fecha === hoy; });
    var manana = grupo(function (a) { return a.fecha === S.sumaDias(hoy, 1); });
    var proximos = grupo(function (a) { return a.fecha && a.fecha > S.sumaDias(hoy, 1) && a.fecha <= S.sumaDias(hoy, 7); });
    /* Sin este grupo, lo que tiene fecha a más de una semana no se veía en
       ningún sitio de la agenda. */
    var despues = grupo(function (a) { return a.fecha && a.fecha > S.sumaDias(hoy, 7); });
    var sinFecha = grupo(function (a) { return !a.fecha; });

    var html = '';

    if (S.state.avisos.length) html += tarjetaHoy(r, deHoy, vencidos, abiertos);

    if (S.state.avisos.length && S.state.ajustes.pistaGestos !== false) {
      html += '<div class="pista">' +
        '<span class="pista__ico">' + ICON.desliza + '</span>' +
        '<span>Desliza un aviso <b>hacia la izquierda</b> para darlo por hecho, o ' +
        '<b>hacia la derecha</b> para ponerlo en curso o cancelarlo. ' +
        'El color del icono de cada fila indica su estado.</span>' +
        '<button class="iconbtn" data-pista type="button" aria-label="Entendido">' + ICON.x + '</button>' +
        '</div>';
    }

    if (!S.state.avisos.length) {
      return html + U.vacioHTML({
        titulo: 'Todavía no hay avisos',
        texto: 'Crea el primero con el botón + o carga unos datos de ejemplo para ver cómo funciona.',
        accion: 'ejemplo', accionLabel: 'Cargar datos de ejemplo'
      });
    }

    var desliza = { swipe: true };
    if (vencidos.length) {
      html += U.seccion('<span class="section__title--rojo">Vencidos</span>',
        U.lista(vencidos, null, desliza), U.plural(vencidos.length, 'aviso'));
    }
    html += U.seccion('Hoy',
      deHoy.length ? U.lista(deHoy, null, desliza) : '<div class="card card__pad small muted">Nada programado para hoy.</div>',
      deHoy.length ? U.plural(deHoy.length, 'aviso') : '');
    if (manana.length) html += U.seccion('Mañana', U.lista(manana, null, desliza), U.plural(manana.length, 'aviso'));
    if (proximos.length) html += U.seccion('Próximos 7 días', U.lista(proximos, null, desliza), U.plural(proximos.length, 'aviso'));
    if (despues.length) html += U.seccion('Más adelante', U.lista(despues, null, desliza), U.plural(despues.length, 'aviso'));
    if (sinFecha.length) html += U.seccion('Sin fecha', U.lista(sinFecha, null, desliza), U.plural(sinFecha.length, 'aviso'));
    if (!abiertos.length) {
      html += U.vacioHTML({ titulo: 'Todo al día', texto: 'No queda ningún aviso abierto. Buen trabajo.' });
    }

    var cerrados = S.cerrados();
    if (cerrados.length) {
      html += '<a class="cierre" href="#/historico">' +
        '<span>' + (r.hechosHoy
          ? '<b>' + U.plural(r.hechosHoy, 'aviso') + '</b> que has dado por hecho hoy'
          : '<b>' + U.plural(cerrados.length, 'aviso cerrado', 'avisos cerrados') + '</b> en el histórico') +
        '</span><span class="cierre__ir">Ver' + ICON.chevron + '</span></a>';
    }
    return html;
  }

  /* La tarjeta principal: lo que queda para hoy en grande, un anillo con lo
     ya hecho y accesos a lo que pide atención. El color avisa: rojizo si hay
     vencidos, violeta-naranja si hay urgentes, azul si va todo en orden. */
  var VUELTA = 2 * Math.PI * 42;

  function tarjetaHoy(r, deHoy, vencidos, abiertos) {
    var hechos = r.hechosHoy;
    var total = deHoy.length + hechos;
    var parte = total ? hechos / total : 0;
    var urgentes = abiertos.some(function (a) { return a.prioridad === 'urgente'; });
    var tono = vencidos.length ? ' hero--exceso' : (urgentes ? ' hero--tenso' : '');
    var fecha = U.fmtFechaLarga(S.hoyISO()).replace(/ de \d{4}$/, '').replace(',', '');
    fecha = fecha.charAt(0).toUpperCase() + fecha.slice(1);

    function dato(k, n, etiqueta) {
      return '<button class="hero__stat" data-k="' + k + '" type="button">' +
        '<span class="k">' + esc(etiqueta) + '</span><span class="v" data-cuenta="' + n + '">' + n + '</span></button>';
    }

    return '<section class="hero' + tono + '">' +
      '<div class="hero__top">' +
        '<div>' +
          '<p class="hero__eyebrow">' + esc(fecha) + '</p>' +
          '<p class="hero__cifra" data-cuenta="' + deHoy.length + '">' + deHoy.length + '</p>' +
          '<p class="hero__linea">' + (deHoy.length === 1 ? 'aviso para hoy' : 'avisos para hoy') +
            (hechos ? ' · <b>' + U.plural(hechos, 'hecho') + '</b>' : '') + '</p>' +
        '</div>' +
        '<div class="anillo" role="img" aria-label="' + (total ? hechos + ' de ' + total + ' avisos de hoy hechos' : 'Nada para hoy') + '">' +
          '<svg viewBox="0 0 100 100" aria-hidden="true">' +
            '<circle class="anillo__pista" cx="50" cy="50" r="42"/>' +
            '<circle class="anillo__valor" cx="50" cy="50" r="42" stroke-dasharray="' + VUELTA.toFixed(2) + '"' +
              ' stroke-dashoffset="' + (VUELTA * (1 - parte)).toFixed(2) + '" data-anillo="' + parte.toFixed(3) + '"' +
              (total ? '' : ' style="opacity:0"') + '/>' +
          '</svg>' +
          '<span class="anillo__txt"><strong>' + (total ? hechos + '/' + total : '—') + '</strong>' +
            '<span>' + (total ? 'hechos' : 'libre') + '</span></span>' +
        '</div>' +
      '</div>' +
      '<div class="hero__stats">' +
        dato('vencidos', r.vencidos, 'Vencidos') +
        dato('semana', r.semana, '7 días') +
        dato('sinasignar', r.sinAsignar, 'Sin asignar') +
      '</div>' +
    '</section>';
  }

  /* =========================================================
     GESTOS SOBRE LAS FILAS
     ========================================================= */

  function ocultarPista() {
    S.guardarAjustes({ pistaGestos: false }).then(function () { global.App.render(); });
  }

  function conectarGestos(root) {
    if (!global.Swipe) return;
    Swipe.conectar(root, { onEstado: cambiarEstadoRapido });
  }

  var MENSAJE = {
    resuelto:  'Marcado como hecho',
    en_curso:  'Puesto en curso',
    cancelado: 'Aviso cancelado',
    pendiente: 'Aviso reabierto'
  };

  /* Cambio de estado desde el gesto, siempre con opción de deshacer:
     un deslizamiento se dispara sin querer con facilidad. */
  function cambiarEstadoRapido(id, estado) {
    var a = S.byId(S.state.avisos, id);
    if (!a || a.estado === estado) { global.App.render(); return; }

    var previo = a.estado;
    var previoCerrado = a.cerrado;
    a.estado = estado;

    var guardado = S.guardarAviso(a);
    if (S.state.ajustes.pistaGestos !== false) {
      guardado = guardado.then(function () { return S.guardarAjustes({ pistaGestos: false }); });
    }

    guardado.then(function () {
      global.App.render();
      U.toast((a.ref ? a.ref + ': ' : '') + (MENSAJE[estado] || 'Estado actualizado'), {
        accion: 'Deshacer',
        alPulsar: function () {
          a.estado = previo;
          a.cerrado = previoCerrado;
          S.guardarAviso(a).then(function () {
            U.toast('Cambio deshecho');
            global.App.render();
          });
        }
      });
    });
  }

  function kpi(k, n, label, extra) {
    return '<button class="kpi ' + extra + '" data-k="' + k + '" type="button">' +
      '<span class="kpi__n">' + n + '</span><span class="kpi__l">' + esc(label) + '</span></button>';
  }

  /* =========================================================
     BÚSQUEDA (dentro de la Agenda)
     ========================================================= */

  /* Por defecto se busca en todo, también en lo cerrado: buscar a un cliente
     suele ser para ver qué se le hizo. */
  var FILTRO_VACIO = { texto: '', estado: '', prioridad: '', tecnico: '', tipo: '', sistema: '', desde: '', hasta: '', soloAbiertos: false, vencidos: false };
  var filtro = Object.assign({ orden: 'fecha' }, FILTRO_VACIO);

  function enBusqueda() {
    return !!(String(filtro.texto || '').trim() || contarFiltros() || filtro.vencidos || filtro.soloAbiertos);
  }

  function limpiarBusqueda() {
    Object.assign(filtro, FILTRO_VACIO);
  }

  /* Una lista ya filtrada: los accesos de la tarjeta de hoy y los enlaces de
     antes a #/avisos?v=… */
  function aplicarVista(v) {
    limpiarBusqueda();
    filtro.soloAbiertos = true;
    if (v === 'vencidos') filtro.vencidos = true;
    else if (v === 'hoy') { filtro.desde = S.hoyISO(); filtro.hasta = S.hoyISO(); }
    else if (v === 'semana') { filtro.desde = S.hoyISO(); filtro.hasta = S.sumaDias(S.hoyISO(), 7); }
    else if (v === 'sinasignar') filtro.tecnico = '__sin__';
  }

  function buscador(buscando) {
    var activos = contarFiltros();
    return '<div class="searchbar">' +
      '<span class="searchbar__field">' + ICON.lupa +
        '<input id="q" type="search" inputmode="search" enterkeyhint="search" placeholder="Buscar cliente, dirección, ref…" value="' + esc(filtro.texto) + '" autocomplete="off">' +
      '</span>' +
      '<button class="iconbtn searchbar__filtro' + (activos ? ' searchbar__filtro--on' : '') + '" data-mas type="button" aria-label="Filtros' + (activos ? ' (' + activos + ' activos)' : '') + '">' + ICON.filtro + '</button>' +
      (buscando ? '<button class="searchbar__cancelar" data-clear type="button">Cancelar</button>' : '') +
    '</div>';
  }

  function resultados() {
    var res = S.ordenar(S.filtrar(filtro), filtro.orden);

    var vacio = { titulo: 'Sin resultados', texto: 'Prueba a quitar filtros o a buscar otra cosa.' };
    if (!res.length && String(filtro.texto || '').trim()) {
      var sinFiltros = S.filtrar({ texto: filtro.texto });
      if (sinFiltros.length) {
        vacio.texto = sinFiltros.length + (sinFiltros.length === 1 ? ' aviso coincide' : ' avisos coinciden') +
          ' con la búsqueda, pero los filtros activos los ocultan.';
        vacio.accion = 'todos';
        vacio.accionLabel = 'Buscar en todos los avisos';
      }
    }

    return '<div class="chips">' +
        chip('abiertos', filtro.soloAbiertos, 'Solo abiertos') +
        chip('vencidos', filtro.vencidos, 'Vencidos') +
        chip('urgente', filtro.prioridad === 'urgente', 'Urgentes') +
        chip('sinasignar', filtro.tecnico === '__sin__', 'Sin asignar') +
        chip('hoy', filtro.desde === S.hoyISO() && filtro.hasta === S.hoyISO(), 'Hoy') +
      '</div>' +
      '<div class="section__head"><h2 class="section__title">' +
        U.plural(res.length, 'resultado') +
      '</h2><button class="btn btn--ghost btn--sm" data-orden type="button">Orden: ' + esc(nombreOrden(filtro.orden)) + '</button></div>' +
      U.lista(res, vacio, { swipe: true });
  }

  function montarBuscador(root) {
    var q = root.querySelector('#q');
    var t = null;
    q.addEventListener('input', function () {
      clearTimeout(t);
      t = setTimeout(function () { filtro.texto = q.value; global.App.render({ mantenerFoco: '#q' }); }, 220);
    });
    /* «Buscar» en el teclado solo lo esconde: la lista ya está al día. */
    q.addEventListener('keydown', function (e) { if (e.key === 'Enter') q.blur(); });

    var cl = root.querySelector('[data-clear]');
    if (cl) cl.addEventListener('click', function () {
      clearTimeout(t);
      limpiarBusqueda();
      global.App.render();
    });

    U.$$('[data-mas]', root).forEach(function (b) { b.addEventListener('click', sheetFiltros); });
    U.$$('.chip[data-chip]', root).forEach(function (c) {
      c.addEventListener('click', function () { toggleChip(c.dataset.chip); global.App.render(); });
    });

    var todos = root.querySelector('[data-accion="todos"]');
    if (todos) todos.addEventListener('click', function () {
      var texto = filtro.texto;
      limpiarBusqueda();
      filtro.texto = texto;
      global.App.render();
    });
    var orden = root.querySelector('[data-orden]');
    if (orden) orden.addEventListener('click', function () {
      var ordenes = ['fecha', 'prioridad', 'reciente'];
      filtro.orden = ordenes[(ordenes.indexOf(filtro.orden) + 1) % ordenes.length];
      global.App.render();
    });
  }

  function nombreOrden(o) {
    return o === 'prioridad' ? 'prioridad' : (o === 'reciente' ? 'más recientes' : 'fecha');
  }

  function chip(id, activo, label) {
    return '<button class="chip" data-chip="' + id + '" type="button" aria-pressed="' + (activo ? 'true' : 'false') + '">' + esc(label) + '</button>';
  }

  function toggleChip(id) {
    if (id === 'abiertos') filtro.soloAbiertos = !filtro.soloAbiertos;
    else if (id === 'vencidos') filtro.vencidos = !filtro.vencidos;
    else if (id === 'urgente') filtro.prioridad = filtro.prioridad === 'urgente' ? '' : 'urgente';
    else if (id === 'sinasignar') filtro.tecnico = filtro.tecnico === '__sin__' ? '' : '__sin__';
    else if (id === 'hoy') {
      var on = filtro.desde === S.hoyISO() && filtro.hasta === S.hoyISO();
      filtro.desde = on ? '' : S.hoyISO();
      filtro.hasta = on ? '' : S.hoyISO();
    }
  }

  function contarFiltros() {
    var n = 0;
    ['estado', 'prioridad', 'tipo', 'sistema', 'tecnico', 'desde', 'hasta'].forEach(function (k) {
      if (filtro[k]) n++;
    });
    return n;
  }

  function sheetFiltros() {
    var tecs = S.state.tecnicos.map(function (t) { return { id: t.id, label: t.nombre }; });
    tecs.unshift({ id: '__sin__', label: 'Sin asignar' });
    U.abrirSheet('Filtros',
      '<div class="grid2">' +
        campoSelect('f_estado', 'Estado', S.ESTADOS, filtro.estado, 'Cualquiera') +
        campoSelect('f_prioridad', 'Prioridad', S.PRIORIDADES, filtro.prioridad, 'Cualquiera') +
        campoSelect('f_tipo', 'Tipo', S.TIPOS, filtro.tipo, 'Cualquiera') +
        campoSelect('f_sistema', 'Sistema', S.SISTEMAS, filtro.sistema, 'Cualquiera') +
      '</div>' +
      campoSelect('f_tecnico', 'Técnico', tecs, filtro.tecnico, 'Cualquiera') +
      '<div class="grid2">' +
        '<div class="field"><label class="field__label" for="f_desde">Desde</label><input class="input" type="date" id="f_desde" value="' + esc(filtro.desde || '') + '"></div>' +
        '<div class="field"><label class="field__label" for="f_hasta">Hasta</label><input class="input" type="date" id="f_hasta" value="' + esc(filtro.hasta || '') + '"></div>' +
      '</div>' +
      '<div class="btnrow btnrow--split">' +
        '<button class="btn" data-limpiar type="button">Limpiar</button>' +
        '<button class="btn btn--primary" data-aplicar type="button">Aplicar</button>' +
      '</div>',
      function (body) {
        body.querySelector('[data-aplicar]').addEventListener('click', function () {
          filtro.estado = body.querySelector('#f_estado').value;
          filtro.prioridad = body.querySelector('#f_prioridad').value;
          filtro.tipo = body.querySelector('#f_tipo').value;
          filtro.sistema = body.querySelector('#f_sistema').value;
          filtro.tecnico = body.querySelector('#f_tecnico').value;
          filtro.desde = body.querySelector('#f_desde').value;
          filtro.hasta = body.querySelector('#f_hasta').value;
          U.cerrarSheet(); global.App.render();
        });
        body.querySelector('[data-limpiar]').addEventListener('click', function () {
          filtro.estado = filtro.prioridad = filtro.tipo = filtro.sistema = filtro.tecnico = '';
          filtro.desde = filtro.hasta = ''; filtro.vencidos = false;
          U.cerrarSheet(); global.App.render();
        });
      });
  }

  function campoSelect(id, label, cat, val, vacio) {
    return '<div class="field"><label class="field__label" for="' + id + '">' + esc(label) + '</label>' +
      '<select class="select" id="' + id + '">' + U.opciones(cat, val, vacio) + '</select></div>';
  }

  /* =========================================================
     DETALLE DEL AVISO
     ========================================================= */

  function detalle(params) {
    var a = S.byId(S.state.avisos, params.id);
    if (!a) return { titulo: 'Aviso', html: U.vacioHTML({ titulo: 'Aviso no encontrado', texto: 'Puede que se haya eliminado.' }), atras: '#/agenda' };

    var c = a.cliente || {};
    var tel = String(c.telefono || '').replace(/\s+/g, '');
    var dir = [c.nombre, c.direccion].filter(Boolean).join(', ');
    var horasTot = S.totalHoras(a);

    var html = '';

    html += '<h2 class="detail__title">' + esc(a.titulo || '(sin título)') + '</h2>';
    html += '<div class="detail__meta">' +
      U.pill(a.estado) +
      '<span class="tag">' + esc(S.catalogo(S.TIPOS, a.tipo).label) + '</span>' +
      '<span class="tag">' + esc(S.catalogo(S.SISTEMAS, a.sistema).label) + '</span>' +
      '<span class="tag' + (a.prioridad === 'urgente' || a.prioridad === 'alta' ? ' tag--' + a.prioridad : '') + '">' +
        'Prioridad ' + esc(S.catalogo(S.PRIORIDADES, a.prioridad).label.toLowerCase()) + '</span>' +
      '</div>';

    /* estado rápido */
    html += U.seccion('Estado', '<div class="card card__pad"><div class="statusgrid">' +
      S.ESTADOS.map(function (e) {
        return '<button class="e-' + e.id + '" data-estado="' + e.id + '" type="button" aria-pressed="' + (e.id === a.estado ? 'true' : 'false') + '">' + esc(e.label) + '</button>';
      }).join('') + '</div></div>');

    /* cita y asignación */
    var tarde = S.vencido(a);
    html += U.seccion('Cuándo y quién', '<div class="card card__pad">' +
      '<dl class="kv">' +
        '<dt>Fecha</dt><dd' + (tarde ? ' style="color:var(--pr-urgente);font-weight:600"' : '') + '>' +
          esc(U.fmtFechaLarga(a.fecha).replace(' de ' + new Date().getFullYear(), '')) + (tarde ? ' · vencido hace ' + U.plural(U.diasDe(a.fecha), 'día') : '') + '</dd>' +
        '<dt>Hora</dt><dd>' + esc(a.hora || '—') + (a.duracion ? ' · ' + esc(a.duracion) + ' h previstas' : '') + '</dd>' +
        '<dt>Asignado a</dt><dd>' + U.who(a.asignadoA) + '</dd>' +
        '<dt>Horas</dt><dd>' + esc(U.fmtHoras(horasTot)) + '</dd>' +
      '</dl>' +
      '<div class="divider"></div>' +
      '<div class="btnrow"><button class="btn btn--sm" data-asignar type="button">Reasignar</button>' +
      '<button class="btn btn--sm" data-reprogramar type="button">Cambiar fecha</button>' +
      '<button class="btn btn--sm" data-calendario type="button">' + ICON.calendario + 'Al calendario</button></div>' +
      '</div>');

    /* cliente */
    html += U.seccion('Cliente', '<div class="card card__pad">' +
      '<dl class="kv">' +
        '<dt>Nombre</dt><dd>' + esc(c.nombre || '—') + '</dd>' +
        '<dt>Dirección</dt><dd>' + esc(c.direccion || '—') + '</dd>' +
        '<dt>Contacto</dt><dd>' + esc(c.contacto || '—') + '</dd>' +
        '<dt>Teléfono</dt><dd>' + (tel ? '<a href="tel:' + esc(tel) + '">' + esc(c.telefono) + '</a>' : '—') + '</dd>' +
      '</dl>' +
      ((tel || dir) ? '<div class="divider"></div><div class="btnrow">' +
        (tel ? '<a class="btn btn--sm" href="tel:' + esc(tel) + '">' + ICON.tel + 'Llamar</a>' : '') +
        (tel ? '<a class="btn btn--sm" href="https://wa.me/' + esc(tel.replace(/^\+/, '')) + '" target="_blank" rel="noopener">WhatsApp</a>' : '') +
        (dir ? '<a class="btn btn--sm" href="https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(dir) + '" target="_blank" rel="noopener">' + ICON.mapa + 'Ir</a>' : '') +
      '</div>' : '') +
      '</div>');

    if (a.origen === 'correo' && a.correo) {
      html += U.seccion('Llegó por correo', '<div class="card card__pad">' +
        '<dl class="kv">' +
          '<dt>De</dt><dd>' + esc(a.correo.deNombre || a.correo.de) +
            (a.correo.deNombre ? '<br><span class="small muted">' + esc(a.correo.de) + '</span>' : '') + '</dd>' +
          '<dt>Asunto</dt><dd>' + esc(a.correo.asunto || '—') + '</dd>' +
          '<dt>Recibido</dt><dd>' + esc(U.fmtSello(a.correo.fecha)) + '</dd>' +
        '</dl>' +
        '<div class="divider"></div>' +
        '<div class="btnrow">' +
          '<button class="btn btn--sm" data-vercorreo type="button">' + ICON.sobre + 'Ver el correo</button>' +
          '<button class="btn btn--sm" data-ignorar type="button">No crear avisos de este remitente</button>' +
        '</div>' +
        '</div>');
    }

    if (a.descripcion) {
      html += U.seccion('Descripción', '<div class="card card__pad"><p style="white-space:pre-wrap">' + esc(a.descripcion) + '</p></div>');
    }

    /* notas */
    var notas = a.notas || [];
    html += U.seccion('Seguimiento', '<div class="card card__pad">' +
      '<button class="btn btn--sm btn--block" data-nota type="button">' + ICON.mas + 'Añadir nota</button>' +
      (notas.length ? '<div class="divider"></div><ul class="timeline">' + notas.map(function (n) {
        return '<li><time>' + esc(U.fmtSello(n.ts)) + '</time><p>' + esc(n.texto) + '</p>' +
          '<button class="timeline__del" data-delnota="' + esc(n.id) + '" type="button">Eliminar nota</button></li>';
      }).join('') + '</ul>' : '<p class="small muted" style="margin-top:10px">Sin notas todavía.</p>') +
      '</div>', notas.length ? U.plural(notas.length, 'nota') : '');

    /* material */
    var mats = a.materiales || [];
    html += U.seccion('Material', '<div class="card card__pad">' +
      '<button class="btn btn--sm btn--block" data-material type="button">' + ICON.mas + 'Añadir material</button>' +
      (mats.length ? '<div class="divider"></div>' + mats.map(function (m) {
        return '<div class="linerow"><span class="linerow__grow">' + esc(m.desc) + '</span>' +
          '<span class="linerow__qty">×' + esc(m.cantidad) + '</span>' +
          '<button class="iconbtn" data-delmat="' + esc(m.id) + '" type="button" aria-label="Quitar">' + ICON.x + '</button></div>';
      }).join('') : '') +
      '</div>', mats.length ? U.plural(mats.length, 'línea') : '');

    /* horas */
    var hs = a.horas || [];
    html += U.seccion('Horas trabajadas', '<div class="card card__pad">' +
      '<button class="btn btn--sm btn--block" data-horas type="button">' + ICON.mas + 'Imputar horas</button>' +
      (hs.length ? '<div class="divider"></div>' + hs.map(function (h) {
        var t = S.tecnico(h.tecnicoId);
        return '<div class="linerow"><span class="linerow__grow">' + esc(U.fmtFecha(h.fecha)) +
          '<span class="linerow__sub"> · ' + esc(t ? t.nombre : 'sin técnico') + '</span></span>' +
          '<span class="linerow__qty">' + esc(U.fmtHoras(h.horas)) + '</span>' +
          '<button class="iconbtn" data-delhora="' + esc(h.id) + '" type="button" aria-label="Quitar">' + ICON.x + '</button></div>';
      }).join('') : '') +
      '</div>', horasTot ? U.fmtHoras(horasTot) : '');

    /* fotos */
    html += U.seccion('Fotos y adjuntos', '<div class="card card__pad">' +
      '<input type="file" id="fotoInput" accept="image/*" capture="environment" multiple hidden>' +
      '<button class="btn btn--sm btn--block" data-foto type="button">' + ICON.camara + 'Añadir foto</button>' +
      '<div id="fotos" class="photos" style="margin-top:10px"></div>' +
      '<div id="archivos" class="stack stack--tight" style="margin-top:10px"></div>' +
      '</div>');

    html += '<p class="small muted center">Creado ' + esc(U.fmtSello(a.creado)) +
      ' · modificado ' + esc(U.fmtSello(a.actualizado)) +
      (a.cerrado ? ' · cerrado ' + esc(U.fmtSello(a.cerrado)) : '') + '</p>';

    return {
      titulo: a.ref || 'Aviso',
      sub: (c.nombre || 'Sin cliente'),
      atras: '#/agenda',
      acciones: '<button class="iconbtn" data-editar type="button" aria-label="Editar">' + ICON.lapiz + '</button>' +
                '<button class="iconbtn" data-menu type="button" aria-label="Más acciones">' + ICON.puntos + '</button>',
      html: html,
      mount: function (root) { montarDetalle(root, a); }
    };
  }

  function montarDetalle(root, a) {
    function recarga() { global.App.render(); }

    U.$$('[data-estado]', root).forEach(function (b) {
      b.addEventListener('click', function () {
        if (a.estado === b.dataset.estado) return;
        a.estado = b.dataset.estado;
        S.guardarAviso(a).then(function () { U.toast('Estado: ' + S.catalogo(S.ESTADOS, a.estado).label); recarga(); });
      });
    });

    root.querySelector('[data-asignar]').addEventListener('click', function () {
      var cat = S.state.tecnicos.map(function (t) { return { id: t.id, label: t.nombre + ' · ' + U.plural(S.cargaPorTecnico(t.id), 'abierto') }; });
      if (!cat.length) { U.toast('Añade técnicos en Ajustes → Equipo'); return; }
      U.abrirSheet('Asignar aviso',
        campoSelect('as_t', 'Técnico', cat, a.asignadoA, 'Sin asignar') +
        '<button class="btn btn--primary btn--block" data-ok type="button">Guardar</button>',
        function (body) {
          body.querySelector('[data-ok]').addEventListener('click', function () {
            a.asignadoA = body.querySelector('#as_t').value;
            S.guardarAviso(a).then(function () { U.cerrarSheet(); U.toast('Aviso asignado'); recarga(); });
          });
        });
    });

    root.querySelector('[data-reprogramar]').addEventListener('click', function () {
      U.abrirSheet('Cambiar fecha',
        '<div class="grid2">' +
          '<div class="field"><label class="field__label" for="rp_f">Fecha</label><input class="input" type="date" id="rp_f" value="' + esc(a.fecha || '') + '"></div>' +
          '<div class="field"><label class="field__label" for="rp_h">Hora</label><input class="input" type="time" id="rp_h" value="' + esc(a.hora || '') + '"></div>' +
        '</div>' +
        '<div class="btnrow" style="margin-bottom:12px">' +
          '<button class="btn btn--sm" data-rel="0" type="button">Hoy</button>' +
          '<button class="btn btn--sm" data-rel="1" type="button">Mañana</button>' +
          '<button class="btn btn--sm" data-rel="7" type="button">+1 semana</button>' +
        '</div>' +
        '<button class="btn btn--primary btn--block" data-ok type="button">Guardar</button>',
        function (body) {
          U.$$('[data-rel]', body).forEach(function (b) {
            b.addEventListener('click', function () {
              body.querySelector('#rp_f').value = S.sumaDias(S.hoyISO(), Number(b.dataset.rel));
            });
          });
          body.querySelector('[data-ok]').addEventListener('click', function () {
            a.fecha = body.querySelector('#rp_f').value;
            a.hora = body.querySelector('#rp_h').value;
            if (a.estado === 'pendiente' && a.fecha) a.estado = 'programado';
            S.guardarAviso(a).then(function () { U.cerrarSheet(); U.toast('Fecha actualizada'); recarga(); });
          });
        });
    });

    root.querySelector('[data-calendario]').addEventListener('click', function () { sheetCalendario(a); });

    var verCorreo = root.querySelector('[data-vercorreo]');
    if (verCorreo) verCorreo.addEventListener('click', function () {
      U.abrirSheet('Correo original',
        '<dl class="kv" style="margin-bottom:12px">' +
          '<dt>De</dt><dd>' + esc(a.correo.de) + '</dd>' +
          '<dt>Asunto</dt><dd>' + esc(a.correo.asunto || '—') + '</dd>' +
          '<dt>Fecha</dt><dd>' + esc(U.fmtSello(a.correo.fecha)) + '</dd>' +
        '</dl>' +
        '<div class="correo__cuerpo">' + esc(a.correo.cuerpo || '(sin texto)') + '</div>');
    });

    var ignorar = root.querySelector('[data-ignorar]');
    if (ignorar) ignorar.addEventListener('click', function () {
      var de = (a.correo && a.correo.de) || '';
      if (!de) return;
      U.confirmar('Ignorar remitente',
        'Los próximos correos de ' + de + ' no crearán avisos. Puedes quitarlo en Ajustes.',
        { aceptar: 'Ignorar' }).then(function (ok) {
          if (!ok) return;
          var lista = (S.state.ajustes.correoIgnorados || []).slice();
          if (lista.indexOf(de) === -1) lista.push(de);
          S.guardarAjustes({ correoIgnorados: lista }).then(function () {
            U.toast('Remitente ignorado');
          });
        });
    });

    root.querySelector('[data-nota]').addEventListener('click', function () {
      U.pedirTexto('Nueva nota', { label: 'Qué ha pasado', multilinea: true, placeholder: 'Ej.: Sustituida la fuente, pendiente de probar con la CRA' })
        .then(function (txt) {
          if (!txt) return;
          S.addNota(a.id, txt).then(function () { U.toast('Nota añadida'); recarga(); });
        });
    });
    U.$$('[data-delnota]', root).forEach(function (b) {
      b.addEventListener('click', function () {
        S.delNota(a.id, b.dataset.delnota).then(recarga);
      });
    });

    root.querySelector('[data-material]').addEventListener('click', function () {
      U.abrirSheet('Añadir material',
        '<div class="field"><label class="field__label" for="m_d">Descripción</label><input class="input" id="m_d" placeholder="Ej.: Detector PIR grado 2"></div>' +
        '<div class="field"><label class="field__label" for="m_c">Cantidad</label><input class="input" id="m_c" type="number" inputmode="numeric" min="1" step="1" value="1"></div>' +
        '<button class="btn btn--primary btn--block" data-ok type="button">Añadir</button>',
        function (body) {
          body.querySelector('[data-ok]').addEventListener('click', function () {
            S.addMaterial(a.id, body.querySelector('#m_d').value, body.querySelector('#m_c').value)
              .then(function () { U.cerrarSheet(); recarga(); });
          });
        });
    });
    U.$$('[data-delmat]', root).forEach(function (b) {
      b.addEventListener('click', function () { S.delMaterial(a.id, b.dataset.delmat).then(recarga); });
    });

    root.querySelector('[data-horas]').addEventListener('click', function () {
      var cat = S.state.tecnicos.map(function (t) { return { id: t.id, label: t.nombre }; });
      U.abrirSheet('Imputar horas',
        '<div class="grid2">' +
          '<div class="field"><label class="field__label" for="h_f">Fecha</label><input class="input" type="date" id="h_f" value="' + esc(S.hoyISO()) + '"></div>' +
          '<div class="field"><label class="field__label" for="h_h">Horas</label><input class="input" id="h_h" type="text" inputmode="decimal" autocomplete="off" placeholder="2,5"></div>' +
        '</div>' +
        campoSelect('h_t', 'Técnico', cat, a.asignadoA, 'Sin técnico') +
        '<button class="btn btn--primary btn--block" data-ok type="button">Guardar</button>',
        function (body) {
          body.querySelector('[data-ok]').addEventListener('click', function () {
            S.addHoras(a.id, body.querySelector('#h_f').value, body.querySelector('#h_h').value, body.querySelector('#h_t').value)
              .then(function () { U.cerrarSheet(); recarga(); });
          });
        });
    });
    U.$$('[data-delhora]', root).forEach(function (b) {
      b.addEventListener('click', function () { S.delHoras(a.id, b.dataset.delhora).then(recarga); });
    });

    /* fotos */
    var input = root.querySelector('#fotoInput');
    root.querySelector('[data-foto]').addEventListener('click', function () { input.click(); });
    input.addEventListener('change', function () {
      var files = Array.prototype.slice.call(input.files || []);
      if (!files.length) return;
      U.toast('Guardando ' + files.length + ' foto(s)…');
      files.reduce(function (p, f) {
        return p.then(function () { return S.addFoto(a.id, f); });
      }, Promise.resolve()).then(function () {
        input.value = '';
        pintarFotos(root, a.id);
        U.toast('Fotos guardadas');
      }).catch(function (e) { U.toast('No se pudieron guardar: ' + e.message); });
    });
    pintarFotos(root, a.id);
  }

  var urlsVivas = [];
  function liberarURLs() {
    urlsVivas.forEach(function (u) { URL.revokeObjectURL(u); });
    urlsVivas = [];
  }

  function tamanoLegible(bytes) {
    if (!bytes) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB';
    return (bytes / 1048576).toFixed(1).replace('.', ',') + ' MB';
  }

  function pintarFotos(root, avisoId) {
    var rejilla = root.querySelector('#fotos');
    var archivos = root.querySelector('#archivos');
    if (!rejilla) return;

    S.fotosDe(avisoId).then(function (adjuntos) {
      liberarURLs();
      adjuntos.sort(function (x, y) { return String(x.ts).localeCompare(String(y.ts)); });

      var imagenes = adjuntos.filter(function (f) { return S.esImagen(f.mime); });
      var otros = adjuntos.filter(function (f) { return !S.esImagen(f.mime); });

      /* Nada de enlaces a blob: dentro de la app de Android no hay quien
         los abra. Al tocar, lo lleva el visor. */
      rejilla.innerHTML = imagenes.length ? imagenes.map(function (f, i) {
        var url = URL.createObjectURL(f.blob);
        urlsVivas.push(url);
        return '<figure class="photo">' +
          '<button class="photo__ver" data-ver="' + i + '" type="button" aria-label="Ver ' + esc(f.nombre) + '">' +
            '<img src="' + url + '" alt="' + esc(f.nombre) + '" loading="lazy">' +
          '</button>' +
          '<button class="photo__del" data-delfoto="' + esc(f.id) + '" type="button" aria-label="Borrar foto">' + ICON.x + '</button></figure>';
      }).join('') : '';

      archivos.innerHTML = otros.map(function (f, i) {
        return '<div class="archivo" data-abrir="' + i + '" role="button" tabindex="0">' +
          ICON.clip +
          '<span class="archivo__nombre">' + esc(f.nombre) + '</span>' +
          '<span class="archivo__tam">' + esc(tamanoLegible(f.blob.size)) + '</span>' +
          '<span class="archivo__abrir">' + ICON.abrirFuera + '</span>' +
          '<button class="iconbtn" data-delfoto="' + esc(f.id) + '" type="button" aria-label="Borrar adjunto">' + ICON.x + '</button>' +
          '</div>';
      }).join('');

      if (!adjuntos.length) rejilla.innerHTML = '<p class="small muted">Sin fotos ni adjuntos.</p>';

      U.$$('[data-ver]', root).forEach(function (b) {
        b.addEventListener('click', function () {
          Visor.abrir(imagenes, Number(b.dataset.ver));
        });
      });

      U.$$('[data-abrir]', root).forEach(function (fila) {
        function abrir(e) {
          if (e.target.closest('[data-delfoto]')) return;   // el aspa borra
          Visor.abrir(otros, Number(fila.dataset.abrir));
        }
        fila.addEventListener('click', abrir);
        fila.addEventListener('keydown', function (e) {
          if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrir(e); }
        });
      });

      U.$$('[data-delfoto]', root).forEach(function (b) {
        b.addEventListener('click', function (e) {
          e.stopPropagation();
          U.confirmar('Borrar adjunto', 'Se borrará del aviso y no se puede deshacer.', { peligro: true, aceptar: 'Borrar' })
            .then(function (ok) {
              if (ok) S.delFoto(b.dataset.delfoto).then(function () { pintarFotos(root, avisoId); });
            });
        });
      });
    });
  }

  /* =========================================================
     CALENDARIO DEL MÓVIL (.ics)
     ========================================================= */

  var RECORDATORIOS = [
    { id: '0',    label: 'Sin recordatorio' },
    { id: '15',   label: '15 minutos antes' },
    { id: '30',   label: '30 minutos antes' },
    { id: '60',   label: '1 hora antes' },
    { id: '120',  label: '2 horas antes' },
    { id: '1440', label: '1 día antes' }
  ];

  /* En el móvil se comparte el archivo (el sistema ofrece «Calendario»);
     en el escritorio, o si no hay soporte, se descarga. */
  function entregarICS(nombre, texto, tituloCompartir) {
    var blob = new Blob([texto], { type: 'text/calendar;charset=utf-8' });

    if (global.Nativo && Nativo.disponible()) {
      U.descargar(nombre, blob, 'text/calendar', 'abrir');
      return;
    }

    try {
      var archivo = new File([blob], nombre, { type: 'text/calendar' });
      if (navigator.canShare && navigator.canShare({ files: [archivo] })) {
        navigator.share({ files: [archivo], title: tituloCompartir })
          .catch(function (e) {
            if (!e || e.name !== 'AbortError') U.descargar(nombre, blob);
          });
        return;
      }
    } catch (e) { /* sin API de compartir archivos: se descarga */ }
    U.descargar(nombre, blob);
  }

  function opcionesICS() {
    return { recordatorio: Number(S.state.ajustes.recordatorio) || 0 };
  }

  function exportarAlCalendario(lista, nombreArchivo, tituloCompartir) {
    var conFecha = ICS.exportables(lista);
    if (!conFecha.length) {
      U.toast('No hay avisos con fecha para exportar');
      return Promise.resolve(0);
    }
    return S.marcarExportados(conFecha).then(function () {
      entregarICS(nombreArchivo, ICS.calendario(conFecha, opcionesICS()), tituloCompartir);
      return conFecha.length;
    });
  }

  function sheetCalendario(a) {
    if (!a.fecha) {
      U.toast('Ponle una fecha al aviso para llevarlo al calendario');
      return;
    }
    var rec = Number(S.state.ajustes.recordatorio) || 0;
    var cuando = a.hora
      ? U.fmtFechaLarga(a.fecha) + ' a las ' + a.hora +
        (a.duracion ? ' (' + esc(a.duracion) + ' h)' : ' (1 h)')
      : U.fmtFechaLarga(a.fecha) + ', todo el día';
    var aviso = rec ? S.catalogo(RECORDATORIOS, String(rec)).label.toLowerCase() : 'sin recordatorio';

    U.abrirSheet('Llevar al calendario',
      '<p class="small muted" style="margin-bottom:14px">' + esc(cuando) + ' · ' + esc(aviso) + '.<br>' +
        'Si cambias la fecha del aviso, vuelve a exportarlo y el calendario actualizará el mismo evento.</p>' +
      '<div class="stack">' +
        '<button class="btn btn--block btn--primary" data-ics type="button">' + ICON.calendario + 'Añadir al calendario</button>' +
        '<a class="btn btn--block" href="' + esc(ICS.enlaceGoogle(a)) + '" target="_blank" rel="noopener">Abrir en Google Calendar</a>' +
      '</div>',
      function (body) {
        body.querySelector('[data-ics]').addEventListener('click', function () {
          exportarAlCalendario([a], ICS.nombreArchivo(a), titulo0(a)).then(function (n) {
            if (n) { U.cerrarSheet(); U.toast('Archivo de calendario listo'); }
          });
        });
      });
  }

  function titulo0(a) { return (a.ref ? a.ref + ' · ' : '') + (a.titulo || 'Aviso'); }

  function menuAviso(a) {
    U.abrirSheet('Aviso ' + (a.ref || ''),
      '<div class="stack">' +
        '<button class="btn btn--block" data-a="editar" type="button">' + ICON.lapiz + 'Editar aviso</button>' +
        '<button class="btn btn--block" data-a="duplicar" type="button">' + ICON.copia + 'Duplicar</button>' +
        '<button class="btn btn--block" data-a="calendario" type="button">' + ICON.calendario + 'Llevar al calendario</button>' +
        '<button class="btn btn--block" data-a="compartir" type="button">' + ICON.compartir + 'Compartir resumen</button>' +
        '<button class="btn btn--block btn--danger" data-a="borrar" type="button">' + ICON.papelera + 'Eliminar aviso</button>' +
      '</div>',
      function (body) {
        U.$$('[data-a]', body).forEach(function (b) {
          b.addEventListener('click', function () {
            var acc = b.dataset.a;
            U.cerrarSheet();
            if (acc === 'editar') location.hash = '#/editar/' + a.id;
            else if (acc === 'duplicar') S.duplicarAviso(a.id).then(function (n) { U.toast('Aviso duplicado'); location.hash = '#/aviso/' + n.id; });
            else if (acc === 'calendario') sheetCalendario(a);
            else if (acc === 'compartir') compartir(a);
            else if (acc === 'borrar') {
              U.confirmar('Eliminar aviso', 'Se borrará ' + (a.ref || 'el aviso') + ' con sus notas y fotos. No se puede deshacer.', { peligro: true, aceptar: 'Eliminar' })
                .then(function (ok) {
                  if (!ok) return;
                  S.borrarAviso(a.id).then(function () { U.toast('Aviso eliminado'); location.hash = '#/agenda'; });
                });
            }
          });
        });
      });
  }

  function compartir(a) {
    var c = a.cliente || {};
    var t = S.tecnico(a.asignadoA);
    var txt = [
      (a.ref || '') + ' · ' + (a.titulo || ''),
      'Estado: ' + S.catalogo(S.ESTADOS, a.estado).label + ' · Prioridad: ' + S.catalogo(S.PRIORIDADES, a.prioridad).label,
      'Cuándo: ' + U.fmtFechaLarga(a.fecha) + (a.hora ? ' a las ' + a.hora : ''),
      'Técnico: ' + (t ? t.nombre : 'sin asignar'),
      'Cliente: ' + (c.nombre || '-') + (c.direccion ? ' — ' + c.direccion : '') + (c.telefono ? ' — ' + c.telefono : ''),
      'Trabajo: ' + S.catalogo(S.TIPOS, a.tipo).label + ' / ' + S.catalogo(S.SISTEMAS, a.sistema).label,
      a.descripcion ? '\n' + a.descripcion : '',
      (a.materiales || []).length ? '\nMaterial:\n' + a.materiales.map(function (m) { return '- ' + m.desc + ' x' + m.cantidad; }).join('\n') : '',
      S.totalHoras(a) ? '\nHoras: ' + U.fmtHoras(S.totalHoras(a)) : ''
    ].filter(Boolean).join('\n');

    if (navigator.share) {
      navigator.share({ title: a.ref + ' · ' + a.titulo, text: txt }).catch(function () {});
    } else if (navigator.clipboard) {
      navigator.clipboard.writeText(txt).then(function () { U.toast('Resumen copiado al portapapeles'); });
    } else {
      U.abrirSheet('Resumen', '<textarea class="textarea" rows="12" readonly>' + esc(txt) + '</textarea>');
    }
  }

  /* =========================================================
     FORMULARIO
     ========================================================= */

  function formulario(params, prellenado) {
    var editando = !!(params && params.id);
    /* Un aviso que llega de un atajo entra ya escrito en el formulario: se
       repasa de un vistazo y se crea, en vez de fiarse a ciegas. */
    var desdeAtajo = null, porEnlace = false;
    if (!editando) {
      desdeAtajo = pendiente;
      if (!desdeAtajo && prellenado && Object.keys(prellenado).length) {
        desdeAtajo = prellenado;
        porEnlace = true;
      }
      pendiente = null;
    }
    var a = editando
      ? JSON.parse(JSON.stringify(S.byId(S.state.avisos, params.id) || {}))
      : (desdeAtajo ? Atajos.aAviso(desdeAtajo) : S.nuevoAvisoVacio());
    if (editando && !a.id) {
      return { titulo: 'Aviso', html: U.vacioHTML({ titulo: 'Aviso no encontrado', texto: '' }), atras: '#/agenda' };
    }
    var c = a.cliente || (a.cliente = {});
    var tecs = S.state.tecnicos.map(function (t) { return { id: t.id, label: t.nombre }; });

    var html =
      (porEnlace ? avisoDeContenedor() : '') +
      (editando ? '' :
        '<div class="btnrow" style="margin-bottom:22px">' +
          '<button class="btn btn--sm btn--block" data-pegar type="button">' + ICON.copia + 'Pegar un aviso copiado</button>' +
        '</div>') +
      '<form id="fAviso" novalidate>' +
      U.seccion('El trabajo', '<div class="card card__pad">' +
        '<div class="field"><label class="field__label" for="titulo">Título *</label>' +
          '<input class="input" id="titulo" value="' + esc(a.titulo) + '" placeholder="Ej.: Central en fallo de comunicación" required></div>' +
        '<div class="grid2">' +
          campoSelect('tipo', 'Tipo', S.TIPOS, a.tipo) +
          campoSelect('sistema', 'Sistema', S.SISTEMAS, a.sistema) +
        '</div>' +
        '<div class="field"><span class="field__label">Prioridad</span><div class="segmented">' +
          S.PRIORIDADES.map(function (p) {
            return '<label><input type="radio" name="prioridad" value="' + p.id + '"' + (p.id === a.prioridad ? ' checked' : '') + '><span>' + esc(p.label) + '</span></label>';
          }).join('') + '</div></div>' +
        '<div class="field"><label class="field__label" for="descripcion">Descripción</label>' +
          '<textarea class="textarea" id="descripcion" placeholder="Qué falla, qué hay que llevar, indicaciones del cliente…">' + esc(a.descripcion) + '</textarea></div>' +
        '</div>') +

      U.seccion('Cliente', '<div class="card card__pad">' +
        '<div class="field"><label class="field__label" for="cl_nombre">Nombre o empresa</label>' +
          '<input class="input" id="cl_nombre" value="' + esc(c.nombre || '') + '" autocomplete="off" list="clientesPrev"></div>' +
        '<datalist id="clientesPrev">' + clientesPrevios() + '</datalist>' +
        '<div class="field"><label class="field__label" for="cl_direccion">Dirección</label>' +
          '<input class="input" id="cl_direccion" value="' + esc(c.direccion || '') + '" autocomplete="off"></div>' +
        '<div class="grid2">' +
          '<div class="field"><label class="field__label" for="cl_telefono">Teléfono</label>' +
            '<input class="input" id="cl_telefono" type="tel" inputmode="tel" value="' + esc(c.telefono || '') + '"></div>' +
          '<div class="field"><label class="field__label" for="cl_contacto">Contacto</label>' +
            '<input class="input" id="cl_contacto" value="' + esc(c.contacto || '') + '"></div>' +
        '</div></div>') +

      U.seccion('Planificación', '<div class="card card__pad">' +
        '<div class="grid2">' +
          '<div class="field"><label class="field__label" for="fecha">Fecha</label><input class="input" type="date" id="fecha" value="' + esc(a.fecha || '') + '"></div>' +
          '<div class="field"><label class="field__label" for="hora">Hora</label><input class="input" type="time" id="hora" value="' + esc(a.hora || '') + '"></div>' +
        '</div>' +
        '<div class="grid2">' +
          '<div class="field"><label class="field__label" for="duracion">Duración prevista (h)</label>' +
            '<input class="input" id="duracion" type="text" inputmode="decimal" autocomplete="off" placeholder="Ej.: 1,5" value="' + esc(a.duracion || '') + '"></div>' +
          campoSelect('estado', 'Estado', S.ESTADOS, a.estado) +
        '</div>' +
        campoSelect('asignadoA', 'Asignado a', tecs, a.asignadoA, 'Sin asignar') +
        (tecs.length ? '' : '<p class="field__hint">Aún no hay técnicos. Puedes añadirlos en Ajustes → Equipo.</p>') +
        '</div>') +

      '<div class="btnrow btnrow--split">' +
        '<button class="btn" type="button" data-cancelar>Cancelar</button>' +
        '<button class="btn btn--primary" type="submit">' + (editando ? 'Guardar cambios' : 'Crear aviso') + '</button>' +
      '</div>' +
      '</form>';

    return {
      titulo: editando ? 'Editar ' + (a.ref || 'aviso') : 'Nuevo aviso',
      sub: editando ? 'Modifica los datos y guarda'
        : (desdeAtajo ? 'Repásalo y créalo · será ' + S.siguienteRef() : 'Se guardará como ' + S.siguienteRef()),
      atras: editando ? '#/aviso/' + a.id : '#/agenda',
      html: html,
      mount: function (root) {
        var form = root.querySelector('#fAviso');
        root.querySelector('[data-cancelar]').addEventListener('click', function () { history.back(); });
        var pegar = root.querySelector('[data-pegar]');
        if (pegar) pegar.addEventListener('click', function () { pegarAviso({ alFormulario: true }); });
        form.addEventListener('submit', function (ev) {
          ev.preventDefault();
          var titulo = form.querySelector('#titulo');
          if (!titulo.value.trim()) { titulo.focus(); U.toast('Ponle un título al aviso'); return; }
          a.titulo = titulo.value.trim();
          a.tipo = form.querySelector('#tipo').value;
          a.sistema = form.querySelector('#sistema').value;
          a.prioridad = (form.querySelector('input[name="prioridad"]:checked') || {}).value || 'normal';
          a.descripcion = form.querySelector('#descripcion').value.trim();
          a.cliente = {
            nombre: form.querySelector('#cl_nombre').value.trim(),
            direccion: form.querySelector('#cl_direccion').value.trim(),
            telefono: form.querySelector('#cl_telefono').value.trim(),
            contacto: form.querySelector('#cl_contacto').value.trim()
          };
          a.fecha = form.querySelector('#fecha').value;
          a.hora = form.querySelector('#hora').value;
          a.duracion = form.querySelector('#duracion').value.trim().replace('.', ',');
          a.estado = form.querySelector('#estado').value;
          a.asignadoA = form.querySelector('#asignadoA').value;
          S.guardarAviso(a).then(function (g) {
            U.toast(editando ? 'Cambios guardados' : 'Aviso ' + g.ref + ' creado');
            location.hash = '#/aviso/' + g.id;
          });
        });
      }
    };
  }

  /* =========================================================
     PEGAR UN AVISO (atajos del iPhone, correos, WhatsApp…)
     ========================================================= */

  /* Datos a la espera de pintarse en el próximo formulario. Van por aquí y no
     por la dirección para no dejar el teléfono del cliente en el historial. */
  var pendiente = null;

  function pegarAviso(opts) {
    opts = opts || {};
    Atajos.delPortapapeles().then(function (texto) {
      if (!aplicar(texto, opts)) pedirloAMano(opts, texto);
    }, function () {
      /* Safari puede negar la lectura, y algún navegador ni la trae. */
      pedirloAMano(opts, '');
    });
  }

  function aplicar(texto, opts) {
    var d = Atajos.deTexto(texto);
    if (!d) return false;
    pendiente = d;
    if (opts.alFormulario && /^#\/nuevo/.test(location.hash)) global.App.render();
    else location.hash = '#/nuevo';
    U.toast('Aviso pegado: repásalo y créalo');
    return true;
  }

  function pedirloAMano(opts, valor) {
    U.pedirTexto('Pegar un aviso', {
      multilinea: true,
      valor: valor || '',
      label: 'Pega aquí el texto (mantén pulsado → Pegar)',
      placeholder: 'Central en fallo\ncliente: Farmacia Centro\ntel: 611223344\nprioridad: urgente',
      aceptar: 'Usar este texto'
    }).then(function (v) {
      if (v == null) return;
      if (!aplicar(v, opts)) U.toast('No he entendido ningún aviso en ese texto');
    });
  }

  /* En el iPhone, la app de la pantalla de inicio y Safari guardan sus datos
     por separado. Si un atajo abre un enlace, cae en Safari, y el aviso que se
     cree ahí no aparecerá en la app. Más vale decirlo antes de crearlo. */
  function enSitioEquivocado() {
    return U.esIOS() && !U.enApp();
  }

  function avisoDeContenedor() {
    if (!enSitioEquivocado()) return '';
    return '<div class="card card__pad card--ojo" style="margin-bottom:10px">' +
      '<p class="small"><b>Ojo:</b> esto se ha abierto en Safari, no en la app de la pantalla de inicio. ' +
      'En el iPhone cada una guarda sus avisos por su cuenta, así que lo que crees aquí <b>no aparecerá en la app</b>.</p>' +
      '<p class="small muted" style="margin-top:8px">Para que entre bien: cambia el atajo para que <b>copie</b> el aviso en vez de abrir el enlace, abre la app desde la pantalla de inicio y pégalo con el icono de arriba a la derecha.</p>' +
      '</div>';
  }

  function clientesPrevios() {
    var vistos = {};
    S.state.avisos.forEach(function (a) {
      var n = a.cliente && a.cliente.nombre;
      if (n && !vistos[n]) vistos[n] = a.cliente;
    });
    return Object.keys(vistos).sort().map(function (n) {
      return '<option value="' + esc(n) + '"></option>';
    }).join('');
  }

  /* =========================================================
     HISTÓRICO (avisos cerrados)
     ========================================================= */

  var filtroHist = { texto: '', estado: '' };

  function historico() {
    var todos = S.cerrados();
    var hoy = S.hoyISO();

    var lista = todos.filter(function (a) {
      if (filtroHist.estado && a.estado !== filtroHist.estado) return false;
      if (!filtroHist.texto) return true;
      var q = filtroHist.texto.toLowerCase();
      var t = S.tecnico(a.asignadoA);
      return [a.ref, a.titulo, a.cliente && a.cliente.nombre, a.cliente && a.cliente.direccion, t && t.nombre]
        .join(' ').toLowerCase().indexOf(q) !== -1;
    });

    var resueltos = todos.filter(function (a) { return a.estado === 'resuelto'; });
    var desdeSemana = S.sumaDias(hoy, -7);
    var semana = resueltos.filter(function (a) { return S.diaCierre(a) >= desdeSemana; });
    var horasSemana = semana.reduce(function (n, a) { return n + S.totalHoras(a); }, 0);

    var html = '';

    if (!todos.length) {
      return {
        titulo: 'Hechos',
        sub: 'Histórico de avisos cerrados',
        html: U.vacioHTML({
          titulo: 'Todavía no hay nada cerrado',
          texto: 'Cuando des un aviso por hecho o lo canceles, se guardará aquí con su fecha de cierre. No se borra nada.'
        }),
        mount: function () {}
      };
    }

    html += '<div class="kpis">' +
      kpi('hist_hoy', resueltos.filter(function (a) { return S.diaCierre(a) === hoy; }).length, 'Hoy', '') +
      kpi('hist_semana', semana.length, '7 días', '') +
      kpi('hist_horas', U.fmtHoras(horasSemana).replace(' h', ''), 'Horas 7 d', '') +
      kpi('hist_total', resueltos.length, 'Resueltos', '') +
      '</div>';

    html += '<div class="searchbar">' +
        '<span class="searchbar__field">' + ICON.lupa +
          '<input id="qh" type="search" inputmode="search" placeholder="Buscar en el histórico…" value="' + esc(filtroHist.texto) + '" autocomplete="off">' +
        '</span>' +
        (filtroHist.texto ? '<button class="iconbtn searchbar__clear" data-clearh type="button" aria-label="Limpiar búsqueda">' + ICON.x + '</button>' : '') +
      '</div>';

    html += '<div class="chips">' +
      '<button class="chip" data-hf="" type="button" aria-pressed="' + (!filtroHist.estado) + '">Todos<span class="chip__n">' + todos.length + '</span></button>' +
      '<button class="chip" data-hf="resuelto" type="button" aria-pressed="' + (filtroHist.estado === 'resuelto') + '">Resueltos<span class="chip__n">' + resueltos.length + '</span></button>' +
      '<button class="chip" data-hf="cancelado" type="button" aria-pressed="' + (filtroHist.estado === 'cancelado') + '">Cancelados<span class="chip__n">' + (todos.length - resueltos.length) + '</span></button>' +
      '</div>';

    if (!lista.length) {
      html += U.vacioHTML({ titulo: 'Sin resultados', texto: 'Prueba a buscar otra cosa o a quitar el filtro.' });
    } else {
      agruparPorCierre(lista).forEach(function (g) {
        html += U.seccion(g.titulo, U.lista(g.avisos, null, { swipe: true, cierre: true }),
          U.plural(g.avisos.length, 'aviso') + (g.horas ? ' · ' + U.fmtHoras(g.horas) : ''));
      });
    }

    return {
      titulo: 'Hechos',
      sub: U.plural(resueltos.length, 'resuelto') + ' · ' + U.plural(todos.length - resueltos.length, 'cancelado'),
      html: html,
      mount: function (root) {
        var q = root.querySelector('#qh');
        if (q) {
          var t = null;
          q.addEventListener('input', function () {
            clearTimeout(t);
            t = setTimeout(function () {
              filtroHist.texto = q.value;
              global.App.render({ mantenerFoco: '#qh' });
            }, 220);
          });
        }
        var cl = root.querySelector('[data-clearh]');
        if (cl) cl.addEventListener('click', function () { filtroHist.texto = ''; global.App.render(); });

        U.$$('[data-hf]', root).forEach(function (b) {
          b.addEventListener('click', function () {
            filtroHist.estado = b.dataset.hf;
            global.App.render();
          });
        });
        conectarGestos(root);
      }
    };
  }

  /* Agrupa por día los últimos siete y por mes lo anterior. */
  function agruparPorCierre(lista) {
    var hoy = S.hoyISO();
    var limiteDia = S.sumaDias(hoy, -6);
    var grupos = [];
    var indice = {};

    lista.forEach(function (a) {
      var dia = S.diaCierre(a);
      var clave, titulo;
      if (!dia) {
        clave = 'sin'; titulo = 'Sin fecha de cierre';
      } else if (dia >= limiteDia) {
        clave = dia;
        titulo = U.fmtFecha(dia);
        if (dia !== hoy && dia !== S.sumaDias(hoy, -1)) titulo = '<b>' + esc(titulo) + '</b>';
      } else {
        clave = dia.slice(0, 7);
        titulo = '<b>' + esc(nombreMes(clave)) + '</b>';
      }
      if (!indice[clave]) {
        indice[clave] = { titulo: titulo, avisos: [], horas: 0 };
        grupos.push(indice[clave]);
      }
      indice[clave].avisos.push(a);
      indice[clave].horas += S.totalHoras(a);
    });
    return grupos;
  }

  function nombreMes(ym) {
    var meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio',
      'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    var p = ym.split('-');
    var txt = meses[Number(p[1]) - 1] + ' de ' + p[0];
    return txt.charAt(0).toUpperCase() + txt.slice(1);
  }

  /* =========================================================
     EQUIPO
     ========================================================= */

  function equipo() {
    var tecs = S.state.tecnicos.slice().sort(function (x, y) { return x.nombre.localeCompare(y.nombre); });
    var sinAsignar = S.state.avisos.filter(function (a) { return S.abierto(a) && !a.asignadoA; }).length;

    var html = '<button class="btn btn--primary btn--block" data-nuevo type="button" style="margin-bottom:16px">' + ICON.mas + 'Añadir técnico</button>';

    if (!tecs.length) {
      html += U.vacioHTML({ titulo: 'Sin técnicos', texto: 'Añade a las personas que atienden los avisos para poder asignárselos.' });
    } else {
      html += '<div class="avlist">' + tecs.map(function (t) {
        var carga = S.cargaPorTecnico(t.id);
        var venc = S.state.avisos.filter(function (a) { return a.asignadoA === t.id && S.vencido(a); }).length;
        return '<button class="avrow avrow--simple" data-tec="' + esc(t.id) + '" type="button">' +
          '<span class="avrow__avatar" style="--c:' + esc(t.color) + '" aria-hidden="true">' + esc(S.iniciales(t.nombre)) + '</span>' +
          '<span class="avrow__main">' +
            '<span class="avrow__title">' + esc(t.nombre) + '</span>' +
            '<span class="avrow__sub">' + (t.telefono ? esc(t.telefono) + ' · ' : '') +
              U.plural(carga, 'abierto') + (venc ? ' · <span style="color:var(--pr-urgente)">' + U.plural(venc, 'vencido') + '</span>' : '') + '</span>' +
          '</span>' +
          '<span class="avrow__side" style="justify-content:center"><span class="avrow__cuenta">' + carga + '</span></span>' +
        '</button>';
      }).join('') + '</div>';
    }

    if (sinAsignar) {
      html += '<div class="spacer"></div><div class="card card__pad small">' +
        '<b>' + sinAsignar + '</b> ' + (sinAsignar === 1 ? 'aviso abierto sin asignar' : 'avisos abiertos sin asignar') + '. ' +
        '<a href="#/avisos?v=sinasignar" style="color:var(--accent)">Verlos</a></div>';
    }

    return {
      titulo: 'Equipo',
      sub: U.plural(tecs.length, 'técnico'),
      html: html,
      mount: function (root) {
        var nb = root.querySelector('[data-nuevo]');
        if (nb) nb.addEventListener('click', function () { editarTecnico(null); });
        U.$$('[data-tec]', root).forEach(function (b) {
          b.addEventListener('click', function () { editarTecnico(S.tecnico(b.dataset.tec)); });
        });
      }
    };
  }

  function editarTecnico(t) {
    var esNuevo = !t;
    t = t || { nombre: '', telefono: '', color: S.COLORES[S.state.tecnicos.length % S.COLORES.length] };
    U.abrirSheet(esNuevo ? 'Nuevo técnico' : 'Editar técnico',
      '<div class="field"><label class="field__label" for="t_n">Nombre *</label><input class="input" id="t_n" value="' + esc(t.nombre) + '"></div>' +
      '<div class="field"><label class="field__label" for="t_t">Teléfono</label><input class="input" id="t_t" type="tel" inputmode="tel" value="' + esc(t.telefono || '') + '"></div>' +
      '<div class="field"><span class="field__label">Color</span><div class="colores">' +
        S.COLORES.map(function (col) {
          return '<label style="--c:' + col + '"><input type="radio" name="color" value="' + col + '" aria-label="Color ' + col + '"' +
            (col === t.color ? ' checked' : '') + '></label>';
        }).join('') + '</div></div>' +
      '<div class="btnrow btnrow--split">' +
        (esNuevo ? '' : '<button class="btn btn--danger" data-borrar type="button">Eliminar</button>') +
        '<button class="btn btn--primary" data-ok type="button">Guardar</button>' +
      '</div>',
      function (body) {
        body.querySelector('[data-ok]').addEventListener('click', function () {
          var nombre = body.querySelector('#t_n').value.trim();
          if (!nombre) { U.toast('Escribe un nombre'); return; }
          t.nombre = nombre;
          t.telefono = body.querySelector('#t_t').value.trim();
          t.color = (body.querySelector('input[name="color"]:checked') || {}).value || t.color;
          S.guardarTecnico(t).then(function () { U.cerrarSheet(); U.toast('Técnico guardado'); global.App.render(); });
        });
        var del = body.querySelector('[data-borrar]');
        if (del) del.addEventListener('click', function () {
          U.cerrarSheet();
          U.confirmar('Eliminar técnico', 'Sus avisos quedarán sin asignar. ¿Continuar?', { peligro: true, aceptar: 'Eliminar' })
            .then(function (ok) {
              if (ok) S.borrarTecnico(t.id).then(function () { U.toast('Técnico eliminado'); global.App.render(); });
            });
        });
      });
  }

  /* =========================================================
     AJUSTES
     ========================================================= */

  function ajustes() {
    var r = S.resumen();
    var tema = S.state.ajustes.tema || 'auto';

    var conFecha = ICS.exportables(S.state.avisos);
    var abiertosCal = conFecha.filter(S.abierto);
    var limite30 = S.sumaDias(S.hoyISO(), 30);
    var proximos = abiertosCal.filter(function (a) { return a.fecha >= S.hoyISO() && a.fecha <= limite30; });

    var html = '';

    html += bloque('Resumen',
      fila({ estatica: true, titulo: 'Avisos', valor: r.total + ' · ' + r.abiertos + ' abiertos' }) +
      fila({ estatica: true, titulo: 'Vencidos', valor: String(r.vencidos) }) +
      fila({ estatica: true, titulo: 'Urgentes', valor: String(r.urgentes) }) +
      fila({ estatica: true, titulo: 'Resueltos', valor: String(r.resueltos) }) +
      fila({ estatica: true, titulo: 'Espacio', valor: '—', idValor: 'espacio' }),
      '', { sinIconos: true });

    html += bloque('Apariencia',
      '<li class="fila fila--bloque"><div class="segmented" role="radiogroup" aria-label="Tema">' +
        [['auto', 'Automático'], ['light', 'Claro'], ['dark', 'Oscuro']].map(function (o) {
          return '<label><input type="radio" name="tema" value="' + o[0] + '"' + (tema === o[0] ? ' checked' : '') + '><span>' + o[1] + '</span></label>';
        }).join('') + '</div></li>', '', { sinIconos: true });

    html += bloque('Calendario del móvil',
      fila({ label: true, icono: 'campana', color: COLOR.rojo, titulo: 'Aviso previo',
        control: '<select class="en-fila" id="recordatorio">' + U.opciones(RECORDATORIOS, String(Number(S.state.ajustes.recordatorio) || 0)) + '</select>' }) +
      fila({ icono: 'calendario', color: COLOR.rojo, titulo: 'Próximos 30 días', valor: String(proximos.length), attrs: ' data-cal="proximos"', clase: 'fila--accion' }) +
      fila({ icono: 'calendario', color: COLOR.naranja, titulo: 'Todos los abiertos', valor: String(abiertosCal.length), attrs: ' data-cal="abiertos"', clase: 'fila--accion' }) +
      fila({ icono: 'calendario', color: COLOR.gris, titulo: 'Todos con fecha', valor: String(conFecha.length), attrs: ' data-cal="todos"', clase: 'fila--accion' }),
      'Genera un archivo <b>.ics</b> y ábrelo: el móvil te ofrece añadir los avisos a tu calendario. ' +
      'Solo entran los que tienen fecha, y al volver a importarlos se actualizan en vez de duplicarse.');

    html += bloque('Copia de seguridad',
      fila({ icono: 'exportar', color: COLOR.azul, titulo: 'Copia completa', sub: 'Con fotos y adjuntos', attrs: ' data-exp="full"' }) +
      fila({ icono: 'exportar', color: COLOR.turquesa, titulo: 'Solo los datos', sub: 'Sin fotos, ocupa poco', attrs: ' data-exp="light"' }) +
      fila({ icono: 'tabla', color: COLOR.verde, titulo: 'Avisos en CSV', sub: 'Para abrir en Excel', attrs: ' data-exp="csv"' }) +
      fila({ icono: 'importar', color: COLOR.indigo, titulo: 'Importar copia…', attrs: ' data-imp' }) +
      '<li hidden><input type="file" id="impInput" accept="application/json,.json"></li>',
      'Los datos viven solo en este móvil. Haz copias de vez en cuando y guárdalas donde quieras: Archivos, iCloud, el correo.');

    html += U.seccion('Atajos del iPhone', seccionAtajos());
    html += U.seccion('Correo de la empresa', seccionCorreo());

    html += bloque('Numeración',
      fila({ label: true, icono: 'numero', color: COLOR.gris, titulo: 'Prefijo',
        control: '<input class="en-fila" id="prefijo" value="' + esc(S.state.ajustes.prefijoRef || 'AV') + '" maxlength="6" autocapitalize="characters" autocomplete="off">' }),
      'El próximo aviso será <b>' + esc(S.siguienteRef()) + '</b>.');

    html += bloque('Instalación y versión',
      fila({ estatica: true, icono: 'movil', color: COLOR.azul, titulo: 'Versión', valor: '<b>' + esc(global.APP_VERSION || '1.0.0') + '</b>' }) +
      '<li hidden id="filaInstalar"><button class="fila fila--accion" id="btnInstalar" type="button" hidden>' +
        '<span class="fila__icono" style="--c:' + COLOR.azul + '">' + ICON.mas + '</span>' +
        '<span class="fila__main"><span class="fila__titulo">Añadir a la pantalla de inicio</span></span></button></li>' +
      fila({ icono: 'ayuda', color: COLOR.morado, titulo: 'Cómo instalarla', attrs: ' data-ayuda', chevron: true }) +
      fila({ icono: 'actualizar', color: COLOR.verde, titulo: 'Buscar una versión nueva', attrs: ' data-buscar-version' }) +
      fila({ icono: 'reinstalar', color: COLOR.naranja, titulo: 'Reinstalar la app', attrs: ' data-reinstalar' }),
      'Reinstalar vacía lo que la app tiene guardado del propio programa y lo vuelve a bajar. ' +
      '<b>Los avisos, los técnicos y las fotos no se tocan.</b> Úsalo si algo se queda a medias.');

    html += U.seccion('Si algo falla', seccionFallos());

    html += bloque('Datos',
      fila({ icono: 'matraz', color: COLOR.naranja, titulo: 'Cargar datos de ejemplo', attrs: ' data-ejemplo' }));
    html += bloque('',
      fila({ titulo: 'Borrar todos los datos', attrs: ' data-borrartodo', clase: 'fila--peligro' }),
      'Funciona sin conexión. Ningún dato sale del dispositivo.', { sinIconos: true });

    return {
      titulo: 'Ajustes',
      sub: 'Copias, apariencia y datos',
      html: html,
      mount: function (root) { montarAjustes(root); }
    };
  }

  /* =========================================================
     ATAJOS DEL IPHONE
     ========================================================= */

  function seccionAtajos() {
    if (!global.Buzon) {
      return '<div class="card card__pad"><p class="small muted">Esta parte necesita una versión más nueva de la app. Baja a <b>Instalación y versión</b> y toca <b>Buscar una versión nueva</b>.</p></div>';
    }
    var b = Buzon.estado();

    var buzon;
    if (!b.activo) {
      buzon = '<div class="card card__pad">' +
        '<p class="small muted" style="margin-bottom:12px">Con el buzón puesto, el atajo <b>manda el aviso a la app directamente</b>: tú dictas y el aviso aparece solo la próxima vez que abres Avisos. Sin copiar ni pegar nada.</p>' +
        '<button class="btn btn--primary btn--block" data-buzon-activar type="button">Activar el buzón</button>' +
        '<p class="field__hint">Hace falta haber conectado un Redis en Vercel (Storage → Upstash). Al activarlo se comprueba.</p>' +
        '</div>';
    } else {
      buzon = '<div class="card card__pad">' +
        '<dl class="kv">' +
          '<dt>Buzón</dt><dd>en marcha</dd>' +
          '<dt>Última vez</dt><dd>' + (b.ultima ? esc(U.fmtSello(b.ultima)) : 'todavía no ha recogido') + '</dd>' +
          (b.error ? '<dt>Fallo</dt><dd><span style="color:var(--pr-urgente)">' + esc(b.error) + '</span></dd>' : '') +
        '</dl>' +
        '<div class="divider"></div>' +
        '<div class="stack">' +
          '<button class="btn btn--primary btn--block" data-buzon-atajo type="button">' + ICON.copia + 'Ver los datos del atajo</button>' +
          '<button class="btn btn--block" data-buzon-ahora type="button">Recoger ahora</button>' +
          '<button class="btn btn--block btn--danger" data-buzon-quitar type="button">Apagar el buzón</button>' +
        '</div>' +
        '<p class="field__hint">El aviso entra al abrir la app o al volver a ella. Una web no puede recogerlo con la app cerrada.</p>' +
        '</div>';
    }

    return buzon +
      '<div class="card card__pad">' +
      '<span class="field__label">Sin buzón, a mano</span>' +
      '<p class="small muted" style="margin-bottom:12px">También puedes hacer que el atajo <b>copie</b> el aviso y pegarlo aquí con el icono de arriba a la derecha de la Agenda. Vale igual para el texto de un correo o un WhatsApp.</p>' +
      '<div class="stack">' +
        '<button class="btn btn--block" data-atajo-ayuda type="button">Cómo montar el atajo</button>' +
        '<button class="btn btn--block" data-atajo-claves type="button">Ver las palabras que entiende</button>' +
      '</div>' +
      '</div>';
  }

  var CLAVES_ATAJO = [
    ['titulo', 'lo que hay que hacer (también «asunto» o «t»)'],
    ['cliente', 'nombre o empresa (también «c»)'],
    ['dir', 'dirección'],
    ['tel', 'teléfono'],
    ['contacto', 'persona de contacto'],
    ['desc', 'descripción; se puede repetir («nota:»)'],
    ['fecha', 'hoy · mañana · lunes · +3 · 30/9 · 2026-09-30'],
    ['hora', '9 · 9:30 · 0930'],
    ['duracion', 'horas previstas, con coma: 1,5'],
    ['prioridad', 'baja · normal · alta · urgente (también «p»)'],
    ['tipo', 'avería · instalación · mantenimiento · revisión · presupuesto'],
    ['sistema', 'alarma · cctv · accesos · incendios · portero'],
    ['tecnico', 'nombre de alguien del Equipo']
  ];

  function montarAtajos(root) {
    var ayuda = root.querySelector('[data-atajo-ayuda]');
    if (!ayuda) return;

    montarBuzon(root);

    ayuda.addEventListener('click', function () {
      U.abrirSheet('Montar el atajo en el iPhone',
        '<div class="stack small">' +
        '<p class="muted">Esto es el atajo <b>sin buzón</b>: copia el aviso y lo pegas tú. Si tienes el buzón en marcha, usa mejor <i>Ver los datos del atajo</i>, que lo manda solo.</p>' +
        '<p>En la app <b>Atajos</b>, toca <b>+</b> y añade, en este orden:</p>' +
        '<p><b>1.</b> <i>Pedir entrada de texto</i> · pregunta: «¿Qué aviso?».</p>' +
        '<p><b>2.</b> <i>Copiar al portapapeles</i> · con el resultado del paso anterior.</p>' +
        '<p>Ponle nombre («Nuevo aviso») y lánzalo desde Siri, el botón de acción, el widget de Atajos o tocando dos veces la parte de atrás del móvil.</p>' +
        '<p>Después abre <b>Avisos</b> desde la pantalla de inicio y toca el icono de pegar, arriba a la derecha de la Agenda: el aviso entra relleno y solo hay que crearlo.</p>' +
        '<div class="divider"></div>' +
        '<p class="muted">Si dictas y ya está, con el título basta. Si quieres más, dicta por líneas: «Central en fallo», «cliente dos puntos Farmacia Centro», «prioridad dos puntos urgente».</p>' +
        '</div>');
    });

    root.querySelector('[data-atajo-claves]').addEventListener('click', function () {
      U.abrirSheet('Palabras que entiende',
        '<div class="stack small">' +
        '<p class="muted">Una por línea, en cualquier orden: <b>clave: valor</b>. Lo que no encaje se queda como descripción.</p>' +
        '<dl class="kv">' + CLAVES_ATAJO.map(function (c) {
          return '<dt>' + esc(c[0]) + '</dt><dd>' + esc(c[1]) + '</dd>';
        }).join('') + '</dl>' +
        '</div>');
    });
  }

  function montarBuzon(root) {
    var activar = root.querySelector('[data-buzon-activar]');
    if (activar) {
      activar.addEventListener('click', function () {
        activar.disabled = true;
        U.toast('Comprobando el buzón…');
        Buzon.activar().then(function () {
          U.toast('Buzón en marcha');
          global.App.render();
          setTimeout(hojaDelAtajo, 250);
        }, function (e) {
          activar.disabled = false;
          U.abrirSheet('No se ha podido activar',
            '<div class="stack small">' +
            '<p>' + esc(e && e.message || e) + '</p>' +
            (e && e.codigo === 'sin-almacen'
              ? '<div class="divider"></div>' +
                '<p>Falta darle un sitio donde guardar los avisos mientras la app está cerrada:</p>' +
                '<p><b>1.</b> Entra en <b>vercel.com</b> → tu proyecto → pestaña <b>Storage</b>.</p>' +
                '<p><b>2.</b> <i>Create Database</i> → <b>Upstash</b> → <b>Redis</b> (el plan gratuito vale).</p>' +
                '<p><b>3.</b> Conéctalo al proyecto: Vercel pone solo las variables que hacen falta.</p>' +
                '<p><b>4.</b> Vuelve a desplegar (<i>Redeploy</i>) y prueba otra vez aquí.</p>'
              : '<p class="muted">Comprueba que la app está abierta desde su dirección de Vercel y que hay cobertura.</p>') +
            '</div>');
        });
      });
      return;
    }

    var datos = root.querySelector('[data-buzon-atajo]');
    if (!datos) return;
    datos.addEventListener('click', hojaDelAtajo);

    root.querySelector('[data-buzon-ahora]').addEventListener('click', function () {
      U.toast('Recogiendo…');
      global.App.recogerBuzon({ avisar: true }).catch(function () {});
    });

    root.querySelector('[data-buzon-quitar]').addEventListener('click', function () {
      U.confirmar('Apagar el buzón',
        'El atajo dejará de poder mandar avisos. Los que ya estén en la app se quedan como están.',
        { peligro: true, aceptar: 'Apagar' }).then(function (ok) {
          if (!ok) return;
          Buzon.desactivar().then(function () { U.toast('Buzón apagado'); global.App.render(); });
        });
    });
  }

  /* Los dos datos que hay que teclear dentro de Atajos, cada uno con su botón
     de copiar: el token es largo y no se puede escribir a mano sin errar. */
  function hojaDelAtajo() {
    var b = Buzon.estado();
    U.abrirSheet('El atajo que manda el aviso',
      '<div class="stack small">' +
      '<p>En <b>Atajos</b> → <b>+</b>, dos acciones:</p>' +
      '<p><b>1.</b> <i>Pedir entrada de texto</i> · pregunta: «¿Qué aviso?».</p>' +
      '<p><b>2.</b> <i>Obtener contenido de URL</i>, con la dirección de abajo y, tocando en <i>Mostrar más</i>:</p>' +
      '<p style="margin-left:12px">· <b>Método:</b> POST<br>' +
      '· <b>Cabeceras:</b> una, de nombre <code>X-Token</code> y valor el token de abajo<br>' +
      '· <b>Cuerpo de solicitud:</b> Archivo → el <i>Texto proporcionado</i> del paso 1</p>' +
      '<p>Ponle nombre («Nuevo aviso») y ya lo puedes lanzar con Siri, con el botón de acción, desde el widget de Atajos o tocando dos veces la parte de atrás del móvil.</p>' +
      '<div class="divider"></div>' +
      '<span class="field__label">Dirección</span>' +
      '<p class="small" style="word-break:break-all">' + esc(b.direccion) + '</p>' +
      '<button class="btn btn--sm btn--block" data-copiar-dir type="button">' + ICON.copia + 'Copiar la dirección</button>' +
      '<span class="field__label" style="margin-top:8px">Token</span>' +
      '<p class="small" style="word-break:break-all">' + esc(b.token) + '</p>' +
      '<button class="btn btn--sm btn--block" data-copiar-token type="button">' + ICON.copia + 'Copiar el token</button>' +
      '<p class="field__hint">El token es la llave del buzón: quien lo tenga puede meter avisos aquí. No lo publiques.</p>' +
      '<div class="divider"></div>' +
      '<p class="muted">El aviso aparece al abrir la app o al volver a ella. Una web no puede recogerlo con la app cerrada.</p>' +
      '</div>',
      function (body) {
        body.querySelector('[data-copiar-dir]').addEventListener('click', function () {
          copiar(b.direccion).then(function (ok) { U.toast(ok ? 'Dirección copiada' : 'No he podido copiarla'); });
        });
        body.querySelector('[data-copiar-token]').addEventListener('click', function () {
          copiar(b.token).then(function (ok) { U.toast(ok ? 'Token copiado' : 'No he podido copiarlo'); });
        });
      });
  }

  function copiar(texto) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(texto).then(function () { return true; },
        function () { return false; });
    }
    return Promise.resolve(false);
  }

  /* =========================================================
     CUENTA DE CORREO
     ========================================================= */

  var PROTOCOLOS = [{ id: 'imap', label: 'IMAP' }, { id: 'pop3', label: 'POP3' }];
  var SEGURIDADES = [{ id: 'ssl', label: 'SSL/TLS' }, { id: 'starttls', label: 'STARTTLS' }];
  var PUERTOS = { imap: { ssl: 993, starttls: 143 }, pop3: { ssl: 995, starttls: 110 } };

  function seccionCorreo() {
    var e = Correo.estado();

    /* Leer un buzón exige abrir un socket contra el servidor de correo, y eso
       ningún navegador lo permite: ni Safari en el iPhone ni Chrome. Solo la
       app de Android, que envuelve esta misma web, puede hacerlo. */
    if (!e.soportado) {
      return '<div class="card card__pad">' +
        '<p class="small muted" style="margin-bottom:12px">Esta versión se abre desde el navegador, y un navegador no puede conectarse a un buzón de correo. ' +
        'Los avisos que entraban solos desde el correo hay que crearlos aquí a mano.</p>' +
        (UI.esIOS()
          ? '<p class="small muted">En el iPhone no hay forma de leer el correo dentro de la app. Lo que sí puedes hacer: reenviar el aviso a ti mismo y copiarlo, o abrirlo con el botón <b>+</b> de la Agenda, que ya trae los campos preparados.</p>'
          : '<p class="small muted">Si lo necesitas en marcha otra vez, la <b>app de Android</b> sí lee el buzón: instálala y configúralo allí.</p>') +
        '</div>';
    }

    if (!e.usuario) {
      return '<div class="card card__pad">' +
        '<p class="small muted" style="margin-bottom:12px">Conecta el buzón de la empresa y cada correo que llegue se convertirá en un aviso automáticamente.</p>' +
        '<button class="btn btn--primary btn--block" data-correo-config type="button">' + ICON.sobre + 'Conectar una cuenta</button>' +
        '</div>';
    }

    var ignorados = S.state.ajustes.correoIgnorados || [];

    var esperando = Number(e.pendientes) || 0;
    var caidas = Number(e.caidas) || 0;
    var vigila = e.protocolo === 'pop3' ? 'Consultando cada 5 minutos' : 'Escuchando en tiempo real';

    /* Que la conexión se caiga y vuelva es normal en un móvil: no merece
       pintarse en rojo como si el buzón estuviera roto. */
    var estado;
    if (e.error) estado = '<span style="color:var(--pr-urgente)">' + esc(e.error) + '</span>';
    else if (!e.activo) estado = '<span style="color:var(--pr-alta)">En pausa — abre la app o revisa la batería</span>';
    else if (caidas) estado = vigila + ' <span class="muted">· reconectando</span>';
    else estado = vigila;

    return '<div class="card card__pad">' +
      '<dl class="kv">' +
        '<dt>Cuenta</dt><dd>' + esc(e.usuario) + '</dd>' +
        '<dt>Servidor</dt><dd>' + esc(e.servidor || '—') + ' · ' + esc(String(e.protocolo || '').toUpperCase()) + '</dd>' +
        '<dt>Estado</dt><dd>' + estado + '</dd>' +
        '<dt>Última vez</dt><dd>' + (e.ultimaSync
          ? esc(U.fmtSello(e.ultimaSync))
          : '<span style="color:var(--pr-urgente)">nunca ha llegado a consultar</span>') + '</dd>' +
        /* Este número dice dónde se corta la cadena: si hay correos
           esperando, la descarga va bien y falla la conversión. */
        '<dt>Esperando</dt><dd>' + (esperando
          ? '<b>' + esperando + '</b> ' + (esperando === 1 ? 'correo por convertir' : 'correos por convertir')
          : 'ninguno') + '</dd>' +
      '</dl>' +
      '<div class="divider"></div>' +
      '<div class="stack">' +
        '<button class="btn btn--block" data-correo-sync type="button">Sincronizar ahora</button>' +
        '<button class="btn btn--block" data-correo-releer type="button">Volver a revisar los últimos correos</button>' +
        '<button class="btn btn--block" data-correo-config type="button">Cambiar los datos de la cuenta</button>' +
        '<button class="btn btn--block btn--danger" data-correo-quitar type="button">Desconectar el buzón</button>' +
      '</div>' +
      '<div class="divider"></div>' +
      '<span class="field__label">Remitentes ignorados</span>' +
      (ignorados.length
        ? '<div class="stack stack--tight">' + ignorados.map(function (r) {
            return '<div class="archivo"><span class="archivo__nombre" style="color:var(--ink-2);text-decoration:none">' + esc(r) + '</span>' +
              '<button class="iconbtn" data-quitar-ignorado="' + esc(r) + '" type="button" aria-label="Quitar">' + ICON.x + '</button></div>';
          }).join('') + '</div>'
        : '<p class="small muted">Ninguno. Todo lo que entre creará un aviso.</p>') +
      '<button class="btn btn--sm btn--block" data-anadir-ignorado type="button" style="margin-top:8px">' + ICON.mas + 'Ignorar un remitente</button>' +
      '</div>';
  }

  function seccionFallos() {
    var lista = (global.Fallos && Fallos.lista()) || [];
    if (!lista.length) {
      return '<div class="card card__pad"><p class="small muted">' +
        'Nada que contar: la app no ha registrado ningún fallo.</p></div>';
    }
    return '<div class="card card__pad">' +
      '<p class="small muted" style="margin-bottom:10px">Lo último que le ha fallado a la app. ' +
      'Si algo no va, comparte esta lista.</p>' +
      '<ul class="timeline">' + lista.slice(0, 6).map(function (f) {
        return '<li><time>' + esc(U.fmtSello(f.ts)) + (f.veces > 1 ? ' · ' + f.veces + ' veces' : '') + '</time>' +
          '<p>' + esc(f.mensaje) + '</p></li>';
      }).join('') + '</ul>' +
      '<div class="divider"></div>' +
      '<div class="btnrow btnrow--split">' +
        '<button class="btn btn--sm" data-fallos-limpiar type="button">Vaciar</button>' +
        '<button class="btn btn--sm btn--primary" data-fallos-compartir type="button">Compartir</button>' +
      '</div></div>';
  }

  function montarFallos(root) {
    var limpiar = root.querySelector('[data-fallos-limpiar]');
    if (limpiar) limpiar.addEventListener('click', function () {
      Fallos.limpiar();
      U.toast('Lista vaciada');
      global.App.render();
    });
    var compartir = root.querySelector('[data-fallos-compartir]');
    if (compartir) compartir.addEventListener('click', function () {
      var texto = Fallos.comoTexto();
      if (navigator.share) navigator.share({ title: 'Fallos de Avisos', text: texto }).catch(function () {});
      else if (navigator.clipboard) navigator.clipboard.writeText(texto).then(function () { U.toast('Copiado'); });
      else U.abrirSheet('Fallos', '<textarea class="textarea" rows="12" readonly>' + esc(texto) + '</textarea>');
    });
  }

  function sheetCuentaCorreo() {
    var e = Correo.estado();
    var proto = e.protocolo || 'imap';
    var seg = e.seguridad || 'ssl';

    U.abrirSheet('Cuenta de correo',
      '<div class="field"><label class="field__label" for="c_usuario">Dirección de correo</label>' +
        '<input class="input" id="c_usuario" type="email" inputmode="email" autocapitalize="off" autocomplete="off" value="' + esc(e.usuario || '') + '" placeholder="tunombre@tuempresa.es"></div>' +
      '<div class="field"><label class="field__label" for="c_clave">Contraseña</label>' +
        '<input class="input" id="c_clave" type="password" autocomplete="off" placeholder="' + (e.usuario ? 'sin cambios' : '') + '"></div>' +
      '<div class="field"><label class="field__label" for="c_servidor">Servidor de entrada</label>' +
        '<input class="input" id="c_servidor" autocapitalize="off" autocomplete="off" value="' + esc(e.servidor || '') + '" placeholder="mail.tuempresa.es">' +
        '<p class="field__hint">Lo tienes en los ajustes de la cuenta en Outlook, como «servidor de correo entrante».</p></div>' +
      '<div class="field"><span class="field__label">Protocolo</span><div class="segmented">' +
        PROTOCOLOS.map(function (o) {
          return '<label><input type="radio" name="c_proto" value="' + o.id + '"' + (o.id === proto ? ' checked' : '') + '><span>' + o.label + '</span></label>';
        }).join('') + '</div>' +
        '<p class="field__hint">IMAP permite avisos al instante. POP3 solo consulta cada cierto tiempo.</p></div>' +
      '<div class="grid2">' +
        '<div class="field"><span class="field__label">Seguridad</span><div class="segmented">' +
          SEGURIDADES.map(function (o) {
            return '<label><input type="radio" name="c_seg" value="' + o.id + '"' + (o.id === seg ? ' checked' : '') + '><span>' + o.label + '</span></label>';
          }).join('') + '</div></div>' +
        '<div class="field"><label class="field__label" for="c_puerto">Puerto</label>' +
          '<input class="input" id="c_puerto" type="text" inputmode="numeric" value="' + esc(e.puerto || PUERTOS[proto][seg]) + '"></div>' +
      '</div>' +
      '<p class="small muted" id="c_resultado" style="margin-bottom:12px"></p>' +
      '<div class="btnrow btnrow--split">' +
        '<button class="btn" data-probar type="button">Probar</button>' +
        '<button class="btn btn--primary" data-guardar type="button">Guardar y activar</button>' +
      '</div>',
      function (body) {
        var resultado = body.querySelector('#c_resultado');
        var puerto = body.querySelector('#c_puerto');

        function leer() {
          return {
            usuario: body.querySelector('#c_usuario').value.trim(),
            clave: body.querySelector('#c_clave').value,
            servidor: body.querySelector('#c_servidor').value.trim(),
            protocolo: (body.querySelector('input[name="c_proto"]:checked') || {}).value || 'imap',
            seguridad: (body.querySelector('input[name="c_seg"]:checked') || {}).value || 'ssl',
            puerto: Number(puerto.value) || 0
          };
        }

        /* Al cambiar protocolo o seguridad se ofrece el puerto habitual. */
        U.$$('input[name="c_proto"], input[name="c_seg"]', body).forEach(function (i) {
          i.addEventListener('change', function () {
            var c = leer();
            puerto.value = PUERTOS[c.protocolo][c.seguridad];
          });
        });

        function validar(c) {
          if (!c.usuario || c.usuario.indexOf('@') === -1) return 'Escribe la dirección de correo completa';
          if (!c.servidor) return 'Falta el servidor de entrada';
          if (!c.puerto) return 'Falta el puerto';
          if (!c.clave && !e.usuario) return 'Falta la contraseña';
          return '';
        }

        function probar() {
          var c = leer();
          var fallo = validar(c);
          if (fallo) { resultado.textContent = fallo; return Promise.resolve(null); }
          resultado.textContent = 'Conectando con ' + c.servidor + '…';
          return Correo.probar(c).then(function (r) {
            resultado.innerHTML = r.ok
              ? '<span style="color:var(--st-resuelto)">Conexión correcta.</span>'
              : '<span style="color:var(--pr-urgente)">' + esc(r.mensaje || 'No se pudo conectar') + '</span>';
            return r;
          });
        }

        body.querySelector('[data-probar]').addEventListener('click', probar);

        body.querySelector('[data-guardar]').addEventListener('click', function () {
          probar().then(function (r) {
            if (!r || !r.ok) return;
            Correo.guardarCuenta(leer());
            U.cerrarSheet();
            U.toast('Buzón conectado');
            global.App.render();
            /* Lo que ya estuviera descargado debe aparecer sin esperar a
               cerrar y abrir la app. */
            global.App.recogerCorreo();
          });
        });
      });
  }

  function esperar(ms) {
    return new Promise(function (r) { setTimeout(r, ms); });
  }

  function montarCorreo(root) {
    var cfg = root.querySelector('[data-correo-config]');
    if (cfg) cfg.addEventListener('click', sheetCuentaCorreo);

    var sync = root.querySelector('[data-correo-sync]');
    if (sync) sync.addEventListener('click', function () {
      U.toast('Buscando correo nuevo…');
      Correo.sincronizarAhora();

      /* Lo ya descargado se convierte al momento; para lo que traiga esta
         consulta se vuelve a mirar un par de veces, por si el servidor
         tarda. La parte nativa también avisa por su cuenta al terminar. */
      var total = 0;
      function recoger() {
        return Correo.procesarPendientes().then(function (r) { total += r.creados; });
      }
      recoger()
        .then(function () { return esperar(1500).then(recoger); })
        .then(function () { return esperar(3000).then(recoger); })
        .then(function () {
          U.toast(total ? U.plural(total, 'aviso nuevo', 'avisos nuevos') : 'Sin correo nuevo');
          global.App.render();
        });
    });

    var releer = root.querySelector('[data-correo-releer]');
    if (releer) releer.addEventListener('click', function () {
      U.confirmar('Volver a revisar',
        'Se mirarán otra vez los últimos 20 correos del buzón. Los que ya tengan aviso se dejan como están; solo entrarán los que falten.',
        { aceptar: 'Revisar' }).then(function (ok) {
          if (!ok) return;
          Correo.revisarDeNuevo(20);
          U.toast('Revisando el buzón…');
          esperar(4000)
            .then(function () { return Correo.procesarPendientes(); })
            .then(function (r) {
              U.toast(r.creados ? U.plural(r.creados, 'aviso recuperado', 'avisos recuperados') : 'No faltaba ninguno');
              global.App.render();
            });
        });
    });

    var quitar = root.querySelector('[data-correo-quitar]');
    if (quitar) quitar.addEventListener('click', function () {
      U.confirmar('Desconectar el buzón',
        'Dejarán de entrar avisos por correo. Los avisos ya creados se quedan como están.',
        { peligro: true, aceptar: 'Desconectar' }).then(function (ok) {
          if (!ok) return;
          Correo.borrarCuenta();
          U.toast('Buzón desconectado');
          global.App.render();
        });
    });

    var anadir = root.querySelector('[data-anadir-ignorado]');
    if (anadir) anadir.addEventListener('click', function () {
      U.pedirTexto('Ignorar remitente', {
        label: 'Correo o dominio',
        placeholder: 'boletin@ejemplo.com o @publicidad.com'
      }).then(function (valor) {
        var v = (valor || '').trim().toLowerCase();
        if (!v) return;
        var lista = (S.state.ajustes.correoIgnorados || []).slice();
        if (lista.indexOf(v) === -1) lista.push(v);
        S.guardarAjustes({ correoIgnorados: lista }).then(function () { global.App.render(); });
      });
    });

    U.$$('[data-quitar-ignorado]', root).forEach(function (b) {
      b.addEventListener('click', function () {
        var lista = (S.state.ajustes.correoIgnorados || []).filter(function (r) {
          return r !== b.dataset.quitarIgnorado;
        });
        S.guardarAjustes({ correoIgnorados: lista }).then(function () { global.App.render(); });
      });
    });
  }

  function montarAjustes(root) {
    montarCorreo(root);
    montarAtajos(root);
    montarFallos(root);
    /* El botón de instalar lo enseña app.js cuando el navegador lo ofrece;
       su fila se enseña con él. */
    var bi = root.querySelector('#btnInstalar');
    if (bi) new MutationObserver(function () {
      var li = root.querySelector('#filaInstalar');
      if (li) li.hidden = bi.hidden;
    }).observe(bi, { attributes: true, attributeFilter: ['hidden'] });

    DB.estimate().then(function (e) {
      var n = root.querySelector('#espacio');
      if (!n) return;
      if (!e || !e.usage) { n.textContent = 'no disponible'; return; }
      n.textContent = (e.usage / 1048576).toFixed(1) + ' MB usados';
    });

    U.$$('input[name="tema"]', root).forEach(function (i) {
      i.addEventListener('change', function () {
        S.guardarAjustes({ tema: i.value }).then(function () { global.App.aplicarTema(); U.toast('Tema actualizado'); });
      });
    });

    var pref = root.querySelector('#prefijo');
    pref.addEventListener('change', function () {
      var v = pref.value.trim().toUpperCase().replace(/[^A-Z0-9]/g, '') || 'AV';
      S.guardarAjustes({ prefijoRef: v }).then(function () { global.App.render(); });
    });

    var selRec = root.querySelector('#recordatorio');
    selRec.addEventListener('change', function () {
      S.guardarAjustes({ recordatorio: Number(selRec.value) || 0 }).then(function () {
        U.toast('Recordatorio: ' + S.catalogo(RECORDATORIOS, selRec.value).label.toLowerCase());
      });
    });

    U.$$('[data-cal]', root).forEach(function (b) {
      b.addEventListener('click', function () {
        var todos = ICS.exportables(S.state.avisos);
        var lista, nombre;
        if (b.dataset.cal === 'proximos') {
          var hasta = S.sumaDias(S.hoyISO(), 30);
          lista = todos.filter(function (a) {
            return S.abierto(a) && a.fecha >= S.hoyISO() && a.fecha <= hasta;
          });
          nombre = 'avisos-30-dias.ics';
        } else if (b.dataset.cal === 'abiertos') {
          lista = todos.filter(S.abierto);
          nombre = 'avisos-abiertos.ics';
        } else {
          lista = todos;
          nombre = 'avisos-todos.ics';
        }
        exportarAlCalendario(S.ordenar(lista, 'fecha'), nombre, 'Avisos').then(function (n) {
          if (n) U.toast(U.plural(n, 'aviso') + ' en el archivo de calendario');
        });
      });
    });

    U.$$('[data-exp]', root).forEach(function (b) {
      b.addEventListener('click', function () {
        var modo = b.dataset.exp;
        var sello = S.hoyISO();
        if (modo === 'csv') {
          U.descargar('avisos-' + sello + '.csv', '﻿' + csv(), 'text/csv;charset=utf-8');
          U.toast('CSV descargado');
          return;
        }
        U.toast('Preparando la copia…');
        S.exportar(modo === 'full').then(function (data) {
          U.descargar('copia-avisos-' + sello + (modo === 'full' ? '-con-fotos' : '') + '.json',
            JSON.stringify(data), 'application/json');
          U.toast('Copia descargada');
        });
      });
    });

    var imp = root.querySelector('#impInput');
    root.querySelector('[data-imp]').addEventListener('click', function () { imp.click(); });
    imp.addEventListener('change', function () {
      var f = imp.files && imp.files[0];
      if (!f) return;
      var fr = new FileReader();
      fr.onload = function () {
        var datos;
        try { datos = JSON.parse(fr.result); }
        catch (e) { U.toast('El archivo no es válido'); return; }
        U.abrirSheet('Importar copia',
          '<p class="small muted" style="margin-bottom:14px">Contiene ' + ((datos.avisos || []).length) + ' avisos, ' +
            ((datos.tecnicos || []).length) + ' técnicos y ' + ((datos.fotos || []).length) + ' fotos.</p>' +
          '<div class="stack">' +
            '<button class="btn btn--block btn--primary" data-modo="anadir" type="button">Añadir a lo que ya tengo</button>' +
            '<button class="btn btn--block btn--danger" data-modo="reemplazar" type="button">Reemplazar todo</button>' +
          '</div>',
          function (body) {
            U.$$('[data-modo]', body).forEach(function (bt) {
              bt.addEventListener('click', function () {
                U.cerrarSheet();
                S.importar(datos, bt.dataset.modo).then(function (res) {
                  U.toast('Importados ' + U.plural(res.avisos, 'aviso') + ' y ' + U.plural(res.fotos, 'foto'));
                  global.App.render();
                }).catch(function (e) { U.toast('Error: ' + e.message); });
              });
            });
          });
        imp.value = '';
      };
      fr.readAsText(f);
    });

    root.querySelector('[data-buscar-version]').addEventListener('click', function (e) {
      var b = e.currentTarget;
      b.disabled = true;
      U.toast('Mirando si hay algo nuevo…');
      global.App.buscarActualizacion().then(function (r) {
        b.disabled = false;
        if (r === 'aplicando') U.toast('Versión nueva: actualizando…');
        else if (r === 'al-dia') U.toast('Ya tienes la última versión');
        else U.toast('No se ha podido comprobar: mira la cobertura');
      });
    });

    root.querySelector('[data-reinstalar]').addEventListener('click', function () {
      U.confirmar('Reinstalar la app',
        'Se vuelve a bajar el programa entero. Tus avisos, técnicos y fotos se quedan como están: esto solo toca la copia que el móvil guarda de la app.',
        { aceptar: 'Reinstalar' }).then(function (ok) {
          if (!ok) return;
          U.toast('Reinstalando…');
          global.App.reinstalar();
        });
    });

    root.querySelector('[data-ayuda]').addEventListener('click', function () {
      U.abrirSheet('Cómo instalar la app',
        '<div class="stack small">' +
        '<p><b>iPhone (Safari):</b> abre esta página en Safari, toca el botón <b>Compartir</b> (el cuadrado con la flecha hacia arriba), baja hasta <b>«Añadir a pantalla de inicio»</b> y confirma con <b>Añadir</b>. Te quedará el icono junto a las demás apps.</p>' +
        '<p><b>Android (Chrome):</b> menú ⋮ → «Añadir a pantalla de inicio» o «Instalar aplicación».</p>' +
        '<p class="muted">Una vez instalada se abre a pantalla completa, sin la barra del navegador, y funciona aunque no tengas cobertura. Los datos se guardan en el propio móvil: si la quitas de la pantalla de inicio, exporta antes una copia.</p>' +
        '</div>');
    });

    root.querySelector('[data-ejemplo]').addEventListener('click', function () {
      S.datosDeEjemplo().then(function () { U.toast('Datos de ejemplo añadidos'); global.App.render(); });
    });

    root.querySelector('[data-borrartodo]').addEventListener('click', function () {
      U.confirmar('Borrar todos los datos', 'Se eliminarán todos los avisos, técnicos y fotos de este dispositivo. Exporta una copia antes si no quieres perderlos.',
        { peligro: true, aceptar: 'Borrar todo' }).then(function (ok) {
          if (!ok) return;
          DB.clearAll().then(function () {
            S.state.avisos = []; S.state.tecnicos = [];
            S.state.ajustes.contadorRef = 0;
            return S.guardarAjustes({});
          }).then(function () { U.toast('Datos borrados'); location.hash = '#/agenda'; global.App.render(); });
        });
    });
  }

  function csv() {
    var cab = ['Ref', 'Titulo', 'Estado', 'Prioridad', 'Tipo', 'Sistema', 'Fecha', 'Hora',
      'Tecnico', 'Cliente', 'Direccion', 'Telefono', 'Contacto', 'Horas', 'Material', 'Descripcion', 'Creado'];
    function q(v) { return '"' + String(v == null ? '' : v).replace(/"/g, '""').replace(/\r?\n/g, ' ') + '"'; }
    var filas = S.ordenar(S.state.avisos, 'fecha').map(function (a) {
      var c = a.cliente || {}, t = S.tecnico(a.asignadoA);
      return [a.ref, a.titulo, S.catalogo(S.ESTADOS, a.estado).label, S.catalogo(S.PRIORIDADES, a.prioridad).label,
        S.catalogo(S.TIPOS, a.tipo).label, S.catalogo(S.SISTEMAS, a.sistema).label, a.fecha, a.hora,
        t ? t.nombre : '', c.nombre, c.direccion, c.telefono, c.contacto,
        S.totalHoras(a), (a.materiales || []).map(function (m) { return m.desc + ' x' + m.cantidad; }).join(' | '),
        a.descripcion, a.creado].map(q).join(';');
    });
    return [cab.map(q).join(';')].concat(filas).join('\r\n');
  }

  global.Views = {
    agenda: agenda, verLista: aplicarVista, detalle: detalle, formulario: formulario,
    historico: historico, equipo: equipo, ajustes: ajustes,
    menuAviso: menuAviso, liberarURLs: liberarURLs, pegarAviso: pegarAviso
  };
})(window);
