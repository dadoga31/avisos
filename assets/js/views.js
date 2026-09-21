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
    calendario: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="1"/><path d="M3 10h18M8 3v4M16 3v4M12 14v4M10 16h4"/></svg>'
  };

  /* =========================================================
     AGENDA
     ========================================================= */

  function agenda() {
    var hoy = S.hoyISO();
    var r = S.resumen();
    var abiertos = S.state.avisos.filter(S.abierto);

    function grupo(pred) { return S.ordenar(abiertos.filter(pred), 'fecha'); }

    var vencidos = grupo(function (a) { return a.fecha && a.fecha < hoy; });
    var deHoy = grupo(function (a) { return a.fecha === hoy; });
    var manana = grupo(function (a) { return a.fecha === S.sumaDias(hoy, 1); });
    var proximos = grupo(function (a) { return a.fecha && a.fecha > S.sumaDias(hoy, 1) && a.fecha <= S.sumaDias(hoy, 7); });
    var sinFecha = grupo(function (a) { return !a.fecha; });

    var html = '';

    html += '<div class="kpis">' +
      kpi('vencidos', r.vencidos, 'Vencidos', r.vencidos ? 'kpi--alert' : '') +
      kpi('hoy', r.hoy, 'Hoy', '') +
      kpi('semana', r.semana, '7 días', '') +
      kpi('sinasignar', r.sinAsignar, 'Sin asignar', r.sinAsignar ? 'kpi--accent' : '') +
      '</div>';

    if (S.state.avisos.length && S.state.ajustes.pistaGestos !== false) {
      html += '<div class="pista">' +
        '<span>Desliza un aviso <b>hacia la izquierda</b> para darlo por hecho, o ' +
        '<b>hacia la derecha</b> para ponerlo en curso o cancelarlo. ' +
        'La banda de color de cada fila indica su estado.</span>' +
        '<button class="iconbtn" data-pista type="button" aria-label="Entendido">' + ICON.x + '</button>' +
        '</div>';
    }

    if (!S.state.avisos.length) {
      html += U.vacioHTML({
        titulo: 'Todavía no hay avisos',
        texto: 'Crea el primero con el botón + o carga unos datos de ejemplo para ver cómo funciona.',
        accion: 'ejemplo', accionLabel: 'Cargar datos de ejemplo'
      });
    } else {
      var desliza = { swipe: true };
      if (vencidos.length) {
        html += U.seccion('<span style="color:var(--pr-urgente)">Vencidos</span>',
          U.lista(vencidos, null, desliza), U.plural(vencidos.length, 'aviso'));
      }
      html += U.seccion('Hoy · <b>' + esc(U.fmtFechaLarga(hoy)) + '</b>',
        deHoy.length ? U.lista(deHoy, null, desliza) : '<div class="card card__pad small muted">Nada programado para hoy.</div>',
        deHoy.length ? U.plural(deHoy.length, 'aviso') : '');
      if (manana.length) html += U.seccion('Mañana', U.lista(manana, null, desliza), U.plural(manana.length, 'aviso'));
      if (proximos.length) html += U.seccion('Próximos 7 días', U.lista(proximos, null, desliza), U.plural(proximos.length, 'aviso'));
      if (sinFecha.length) html += U.seccion('Sin fecha', U.lista(sinFecha, null, desliza), U.plural(sinFecha.length, 'aviso'));
      if (!vencidos.length && !deHoy.length && !manana.length && !proximos.length && !sinFecha.length) {
        html += U.vacioHTML({ titulo: 'Todo al día', texto: 'No queda ningún aviso abierto. Buen trabajo.' });
      }

      var cerrados = S.cerrados();
      if (cerrados.length) {
        html += '<a class="cierre" href="#/historico">' +
          '<span>' + (r.hechosHoy
            ? '<b>' + U.plural(r.hechosHoy, 'aviso') + '</b> que has dado por hecho hoy'
            : '<b>' + U.plural(cerrados.length, 'aviso cerrado', 'avisos cerrados') + '</b> en el histórico') +
          '</span><span class="cierre__ir">Ver histórico ›</span></a>';
      }
    }

    return {
      titulo: 'Agenda',
      sub: U.plural(r.abiertos, 'aviso abierto', 'avisos abiertos') + ' · ' + r.total + ' en total',
      html: html,
      mount: function (root) {
        U.$$('.kpi', root).forEach(function (b) {
          b.addEventListener('click', function () { location.hash = '#/avisos?v=' + b.dataset.k; });
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
     LISTA DE AVISOS
     ========================================================= */

  var filtro = { texto: '', estado: '', prioridad: '', tecnico: '', tipo: '', sistema: '', soloAbiertos: true, vencidos: false, orden: 'fecha' };

  function aplicarVista(v) {
    filtro.vencidos = false; filtro.estado = ''; filtro.prioridad = ''; filtro.tecnico = '';
    filtro.desde = ''; filtro.hasta = ''; filtro.soloAbiertos = true;
    if (v === 'vencidos') filtro.vencidos = true;
    else if (v === 'hoy') { filtro.desde = S.hoyISO(); filtro.hasta = S.hoyISO(); }
    else if (v === 'semana') { filtro.desde = S.hoyISO(); filtro.hasta = S.sumaDias(S.hoyISO(), 7); }
    else if (v === 'sinasignar') filtro.tecnico = '__sin__';
  }

  function avisos(params) {
    if (params && params.v) { aplicarVista(params.v); location.replace('#/avisos'); }

    var res = S.ordenar(S.filtrar(filtro), filtro.orden);
    var activos = contarFiltros();

    var vacio = { titulo: 'Sin resultados', texto: 'Prueba a quitar filtros o a buscar otra cosa.' };
    if (!res.length) {
      var sinFiltros = S.filtrar({ texto: filtro.texto });
      if (sinFiltros.length) {
        vacio.texto = sinFiltros.length + (sinFiltros.length === 1 ? ' aviso coincide' : ' avisos coinciden') +
          ' con la búsqueda, pero los filtros activos los ocultan.';
        vacio.accion = 'todos';
        vacio.accionLabel = 'Buscar en todos los avisos';
      }
    }

    var chips = '<div class="chips">' +
      chip('abiertos', filtro.soloAbiertos, 'Solo abiertos') +
      chip('vencidos', filtro.vencidos, 'Vencidos') +
      chip('urgente', filtro.prioridad === 'urgente', 'Urgentes') +
      chip('sinasignar', filtro.tecnico === '__sin__', 'Sin asignar') +
      chip('hoy', filtro.desde === S.hoyISO() && filtro.hasta === S.hoyISO(), 'Hoy') +
      '<button class="chip" data-mas type="button">' + ICON.filtro + 'Más filtros' +
        (activos ? '<span class="chip__n">' + activos + '</span>' : '') + '</button>' +
      '</div>';

    var html =
      '<div class="searchbar">' +
        '<span class="searchbar__field">' + ICON.lupa +
          '<input id="q" type="search" inputmode="search" placeholder="Buscar cliente, dirección, ref…" value="' + esc(filtro.texto) + '" autocomplete="off">' +
        '</span>' +
        (filtro.texto ? '<button class="iconbtn searchbar__clear" data-clear type="button" aria-label="Limpiar búsqueda">' + ICON.x + '</button>' : '') +
      '</div>' + chips +
      '<div class="section__head"><h2 class="section__title">' +
        U.plural(res.length, 'resultado') +
      '</h2><button class="btn btn--ghost btn--sm" data-orden type="button">Orden: ' + esc(nombreOrden(filtro.orden)) + '</button></div>' +
      U.lista(res, vacio, { swipe: true });

    return {
      titulo: 'Avisos',
      sub: U.plural(S.state.avisos.length, 'aviso guardado', 'avisos guardados') + ' en este dispositivo',
      html: html,
      mount: function (root) {
        var q = root.querySelector('#q');
        var t = null;
        q.addEventListener('input', function () {
          clearTimeout(t);
          t = setTimeout(function () { filtro.texto = q.value; global.App.render({ mantenerFoco: '#q' }); }, 220);
        });
        var cl = root.querySelector('[data-clear]');
        if (cl) cl.addEventListener('click', function () { filtro.texto = ''; global.App.render(); });

        U.$$('.chip[data-chip]', root).forEach(function (c) {
          c.addEventListener('click', function () { toggleChip(c.dataset.chip); global.App.render(); });
        });
        root.querySelector('[data-mas]').addEventListener('click', sheetFiltros);

        var todos = root.querySelector('[data-accion="todos"]');
        if (todos) todos.addEventListener('click', function () {
          filtro.estado = filtro.prioridad = filtro.tipo = filtro.sistema = filtro.tecnico = '';
          filtro.desde = filtro.hasta = '';
          filtro.vencidos = false; filtro.soloAbiertos = false;
          global.App.render();
        });
        root.querySelector('[data-orden]').addEventListener('click', function () {
          var ordenes = ['fecha', 'prioridad', 'reciente'];
          filtro.orden = ordenes[(ordenes.indexOf(filtro.orden) + 1) % ordenes.length];
          global.App.render();
        });
        conectarGestos(root);
      }
    };
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
    if (!a) return { titulo: 'Aviso', html: U.vacioHTML({ titulo: 'Aviso no encontrado', texto: 'Puede que se haya eliminado.' }), atras: '#/avisos' };

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
      '<span class="tag"' + (a.prioridad === 'urgente' ? ' style="color:var(--pr-urgente);border-color:currentColor"' : (a.prioridad === 'alta' ? ' style="color:var(--pr-alta);border-color:currentColor"' : '')) + '>' +
        'Prioridad ' + esc(S.catalogo(S.PRIORIDADES, a.prioridad).label.toLowerCase()) + '</span>' +
      '</div>';

    /* estado rápido */
    html += U.seccion('Estado', '<div class="card card__pad"><div class="statusgrid">' +
      S.ESTADOS.map(function (e) {
        return '<button data-estado="' + e.id + '" type="button" aria-pressed="' + (e.id === a.estado ? 'true' : 'false') + '">' + esc(e.label) + '</button>';
      }).join('') + '</div></div>');

    /* cita y asignación */
    var tarde = S.vencido(a);
    html += U.seccion('Cuándo y quién', '<div class="card card__pad">' +
      '<dl class="kv">' +
        '<dt>Fecha</dt><dd' + (tarde ? ' style="color:var(--pr-urgente);font-weight:600"' : '') + '>' +
          esc(U.fmtFechaLarga(a.fecha)) + (tarde ? ' · vencido hace ' + U.plural(U.diasDe(a.fecha), 'día') : '') + '</dd>' +
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
    html += U.seccion('Fotos', '<div class="card card__pad">' +
      '<input type="file" id="fotoInput" accept="image/*" capture="environment" multiple hidden>' +
      '<button class="btn btn--sm btn--block" data-foto type="button">' + ICON.camara + 'Añadir foto</button>' +
      '<div id="fotos" class="photos" style="margin-top:10px"></div>' +
      '</div>');

    html += '<p class="small muted center">Creado ' + esc(U.fmtSello(a.creado)) +
      ' · modificado ' + esc(U.fmtSello(a.actualizado)) +
      (a.cerrado ? ' · cerrado ' + esc(U.fmtSello(a.cerrado)) : '') + '</p>';

    return {
      titulo: a.ref || 'Aviso',
      sub: (c.nombre || 'Sin cliente'),
      atras: '#/avisos',
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
      if (!cat.length) { U.toast('Añade técnicos en la pestaña Equipo'); return; }
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

  function pintarFotos(root, avisoId) {
    var cont = root.querySelector('#fotos');
    if (!cont) return;
    S.fotosDe(avisoId).then(function (fotos) {
      liberarURLs();
      if (!fotos.length) { cont.innerHTML = '<p class="small muted">Sin fotos.</p>'; return; }
      fotos.sort(function (x, y) { return String(x.ts).localeCompare(String(y.ts)); });
      cont.innerHTML = fotos.map(function (f) {
        var url = URL.createObjectURL(f.blob);
        urlsVivas.push(url);
        return '<figure class="photo"><a href="' + url + '" target="_blank" rel="noopener">' +
          '<img src="' + url + '" alt="Foto del aviso" loading="lazy"></a>' +
          '<button class="photo__del" data-delfoto="' + esc(f.id) + '" type="button" aria-label="Borrar foto">' + ICON.x + '</button></figure>';
      }).join('');
      U.$$('[data-delfoto]', cont).forEach(function (b) {
        b.addEventListener('click', function () {
          U.confirmar('Borrar foto', '¿Seguro que quieres borrar esta foto? No se puede deshacer.', { peligro: true, aceptar: 'Borrar' })
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
                  S.borrarAviso(a.id).then(function () { U.toast('Aviso eliminado'); location.hash = '#/avisos'; });
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

  function formulario(params) {
    var editando = !!(params && params.id);
    var a = editando ? JSON.parse(JSON.stringify(S.byId(S.state.avisos, params.id) || {})) : S.nuevoAvisoVacio();
    if (editando && !a.id) {
      return { titulo: 'Aviso', html: U.vacioHTML({ titulo: 'Aviso no encontrado', texto: '' }), atras: '#/avisos' };
    }
    var c = a.cliente || (a.cliente = {});
    var tecs = S.state.tecnicos.map(function (t) { return { id: t.id, label: t.nombre }; });

    var html =
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
        (tecs.length ? '' : '<p class="field__hint">Aún no hay técnicos. Puedes añadirlos en la pestaña Equipo.</p>') +
        '</div>') +

      '<div class="btnrow btnrow--split">' +
        '<button class="btn" type="button" data-cancelar>Cancelar</button>' +
        '<button class="btn btn--primary" type="submit">' + (editando ? 'Guardar cambios' : 'Crear aviso') + '</button>' +
      '</div>' +
      '</form>';

    return {
      titulo: editando ? 'Editar ' + (a.ref || 'aviso') : 'Nuevo aviso',
      sub: editando ? 'Modifica los datos y guarda' : 'Se guardará como ' + S.siguienteRef(),
      atras: editando ? '#/aviso/' + a.id : '#/agenda',
      html: html,
      mount: function (root) {
        var form = root.querySelector('#fAviso');
        root.querySelector('[data-cancelar]').addEventListener('click', function () { history.back(); });
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
        return '<button class="avrow" data-tec="' + esc(t.id) + '" type="button">' +
          '<span class="avrow__flag" style="background:' + esc(t.color) + '"></span>' +
          '<span class="avrow__main">' +
            '<span class="avrow__title">' + esc(t.nombre) + '</span>' +
            '<span class="avrow__sub">' + (t.telefono ? esc(t.telefono) + ' · ' : '') +
              U.plural(carga, 'abierto') + (venc ? ' · <span style="color:var(--pr-urgente)">' + venc + ' vencidos</span>' : '') + '</span>' +
          '</span>' +
          '<span class="avrow__side"><span class="avrow__when">' + carga + '</span></span>' +
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
      '<div class="field"><span class="field__label">Color</span><div class="segmented">' +
        S.COLORES.map(function (col) {
          return '<label style="background:' + col + '1a"><input type="radio" name="color" value="' + col + '"' + (col === t.color ? ' checked' : '') + '>' +
            '<span style="color:' + col + ';font-weight:600">' + (col === t.color ? '●' : '○') + '</span></label>';
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

    var html = '';

    html += U.seccion('Resumen', '<div class="card card__pad">' +
      '<dl class="kv">' +
        '<dt>Avisos</dt><dd>' + r.total + ' (' + r.abiertos + ' abiertos, ' + r.resueltos + ' resueltos)</dd>' +
        '<dt>Vencidos</dt><dd>' + r.vencidos + '</dd>' +
        '<dt>Urgentes</dt><dd>' + r.urgentes + '</dd>' +
        '<dt>Técnicos</dt><dd>' + S.state.tecnicos.length + '</dd>' +
        '<dt>Espacio</dt><dd id="espacio">—</dd>' +
      '</dl></div>');

    html += U.seccion('Apariencia', '<div class="card card__pad">' +
      '<div class="field" style="margin:0"><span class="field__label">Tema</span><div class="segmented">' +
        [['auto', 'Automático'], ['light', 'Claro'], ['dark', 'Oscuro']].map(function (o) {
          return '<label><input type="radio" name="tema" value="' + o[0] + '"' + (tema === o[0] ? ' checked' : '') + '><span>' + o[1] + '</span></label>';
        }).join('') + '</div></div></div>');

    html += U.seccion('Numeración', '<div class="card card__pad">' +
      '<div class="field" style="margin:0"><label class="field__label" for="prefijo">Prefijo de referencia</label>' +
      '<input class="input" id="prefijo" value="' + esc(S.state.ajustes.prefijoRef || 'AV') + '" maxlength="6">' +
      '<p class="field__hint">El próximo aviso será <b class="mono">' + esc(S.siguienteRef()) + '</b>.</p></div></div>');

    var conFecha = ICS.exportables(S.state.avisos);
    var abiertosCal = conFecha.filter(S.abierto);
    var limite30 = S.sumaDias(S.hoyISO(), 30);
    var proximos = abiertosCal.filter(function (a) { return a.fecha >= S.hoyISO() && a.fecha <= limite30; });

    html += U.seccion('Calendario del móvil', '<div class="card card__pad">' +
      '<p class="small muted" style="margin-bottom:12px">Genera un archivo <b>.ics</b> y ábrelo: el móvil te ofrecerá añadir los avisos a tu calendario, junto al resto de tu día. Solo entran los avisos que tengan fecha.</p>' +
      campoSelect('recordatorio', 'Aviso previo en el calendario', RECORDATORIOS, String(Number(S.state.ajustes.recordatorio) || 0)) +
      '<div class="stack">' +
        '<button class="btn btn--block" data-cal="proximos" type="button">Próximos 30 días (' + proximos.length + ')</button>' +
        '<button class="btn btn--block" data-cal="abiertos" type="button">Todos los abiertos (' + abiertosCal.length + ')</button>' +
        '<button class="btn btn--block" data-cal="todos" type="button">Todos con fecha (' + conFecha.length + ')</button>' +
      '</div>' +
      '<p class="field__hint">Al reimportar, los eventos ya añadidos se actualizan en vez de duplicarse.</p>' +
      '</div>');

    html += U.seccion('Copia de seguridad', '<div class="card card__pad">' +
      '<p class="small muted" style="margin-bottom:12px">Los datos viven solo en este móvil. Haz copias de vez en cuando y guárdalas donde quieras (correo, nube, PC).</p>' +
      '<div class="stack">' +
        '<button class="btn btn--block" data-exp="full" type="button">Exportar copia completa (con fotos)</button>' +
        '<button class="btn btn--block" data-exp="light" type="button">Exportar solo datos (sin fotos)</button>' +
        '<button class="btn btn--block" data-exp="csv" type="button">Exportar avisos a CSV (Excel)</button>' +
        '<input type="file" id="impInput" accept="application/json,.json" hidden>' +
        '<button class="btn btn--block" data-imp type="button">Importar copia…</button>' +
      '</div></div>');

    html += U.seccion('Instalación', '<div class="card card__pad">' +
      '<p class="small muted" style="margin-bottom:12px">Instálala en la pantalla de inicio para abrirla como una app y usarla sin cobertura.</p>' +
      '<div class="stack">' +
        '<button class="btn btn--block" id="btnInstalar" type="button" hidden>Añadir a la pantalla de inicio</button>' +
        '<button class="btn btn--block" data-ayuda type="button">Cómo instalarla</button>' +
      '</div></div>');

    html += U.seccion('Datos', '<div class="card card__pad"><div class="stack">' +
      '<button class="btn btn--block" data-ejemplo type="button">Cargar datos de ejemplo</button>' +
      '<button class="btn btn--block btn--danger" data-borrartodo type="button">Borrar todos los datos</button>' +
      '</div></div>');

    html += '<p class="small muted center">Avisos · versión ' + esc(global.APP_VERSION || '1.0.0') + '<br>Funciona sin conexión. Ningún dato sale del dispositivo.</p>';

    return {
      titulo: 'Ajustes',
      sub: 'Copias, apariencia y datos',
      html: html,
      mount: function (root) { montarAjustes(root); }
    };
  }

  function montarAjustes(root) {
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

    root.querySelector('[data-ayuda]').addEventListener('click', function () {
      U.abrirSheet('Cómo instalar la app',
        '<div class="stack small">' +
        '<p><b>Android (Chrome):</b> menú ⋮ → «Añadir a pantalla de inicio» o «Instalar aplicación».</p>' +
        '<p><b>iPhone (Safari):</b> botón Compartir → «Añadir a pantalla de inicio».</p>' +
        '<p class="muted">Una vez instalada se abre a pantalla completa y funciona aunque no tengas cobertura.</p>' +
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
    agenda: agenda, avisos: avisos, detalle: detalle, formulario: formulario,
    historico: historico, equipo: equipo, ajustes: ajustes,
    menuAviso: menuAviso, liberarURLs: liberarURLs
  };
})(window);
