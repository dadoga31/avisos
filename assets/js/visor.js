/* visor.js — pantalla completa para ver los adjuntos de un aviso.
   Las imágenes se ven aquí dentro, con zoom de dos dedos; los demás
   archivos se entregan al móvil para que los abra su aplicación. */
(function (global) {
  'use strict';

  var UI = global.UI;

  var ESCALA_MIN = 1, ESCALA_MAX = 6, ESCALA_DOBLE = 2.6;

  var nodo = null, img = null, lienzo = null;
  var lista = [], indice = 0, url = null;
  var escala = 1, x = 0, y = 0;
  var punteros = {}, base = null, ultimoToque = 0;
  var abiertoConHistoria = false;

  function esImagen(a) { return /^image\//.test(a && a.mime || ''); }

  function construir() {
    if (nodo) return;
    nodo = UI.el('' +
      '<div class="visor" hidden>' +
        '<div class="visor__barra">' +
          '<span class="visor__nombre"></span>' +
          '<button class="iconbtn visor__btn" data-compartir type="button" aria-label="Abrir con otra aplicación">' +
            '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12M8 7l4-4 4 4"/><path d="M5 14v5h14v-5"/></svg>' +
          '</button>' +
          '<button class="iconbtn visor__btn" data-cerrar type="button" aria-label="Cerrar">' +
            '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>' +
          '</button>' +
        '</div>' +
        '<div class="visor__lienzo"><img class="visor__img" alt=""></div>' +
        '<div class="visor__pie">' +
          '<button class="iconbtn visor__btn" data-ant type="button" aria-label="Anterior">' +
            '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5l-7 7 7 7"/></svg></button>' +
          '<span class="visor__pos"></span>' +
          '<button class="iconbtn visor__btn" data-sig type="button" aria-label="Siguiente">' +
            '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 5l7 7-7 7"/></svg></button>' +
        '</div>' +
      '</div>');
    document.body.appendChild(nodo);

    img = nodo.querySelector('.visor__img');
    lienzo = nodo.querySelector('.visor__lienzo');

    nodo.querySelector('[data-cerrar]').addEventListener('click', cerrar);
    nodo.querySelector('[data-compartir]').addEventListener('click', compartirActual);
    nodo.querySelector('[data-ant]').addEventListener('click', function () { mover(-1); });
    nodo.querySelector('[data-sig]').addEventListener('click', function () { mover(1); });

    lienzo.addEventListener('pointerdown', alBajar);
    lienzo.addEventListener('pointermove', alMover);
    lienzo.addEventListener('pointerup', alSoltar);
    lienzo.addEventListener('pointercancel', alSoltar);
    lienzo.addEventListener('dblclick', function (e) { alternarZoom(e.clientX, e.clientY); });
  }

  /* ---------- apertura ---------- */

  function abrir(adjuntos, desde) {
    var imagenes = (adjuntos || []).filter(esImagen);
    var elegido = adjuntos[desde];

    if (!esImagen(elegido)) { entregarAlSistema(elegido); return; }

    construir();
    lista = imagenes;
    indice = Math.max(0, imagenes.indexOf(elegido));
    pintar();

    nodo.hidden = false;
    document.body.style.overflow = 'hidden';

    /* El botón atrás del móvil debe cerrar el visor, no salir del aviso. */
    try {
      history.pushState({ visor: true }, '');
      abiertoConHistoria = true;
      global.addEventListener('popstate', alVolver);
    } catch (e) { abiertoConHistoria = false; }

    document.addEventListener('keydown', alTeclado);
  }

  function alVolver() { cerrar(true); }

  function alTeclado(e) {
    if (e.key === 'Escape') cerrar();
    else if (e.key === 'ArrowLeft') mover(-1);
    else if (e.key === 'ArrowRight') mover(1);
  }

  function cerrar(desdeHistoria) {
    if (!nodo || nodo.hidden) return;
    nodo.hidden = true;
    document.body.style.overflow = '';
    document.removeEventListener('keydown', alTeclado);
    global.removeEventListener('popstate', alVolver);
    soltarURL();
    punteros = {}; base = null;

    if (abiertoConHistoria && !desdeHistoria) {
      abiertoConHistoria = false;
      try { history.back(); } catch (e) {}
    }
    abiertoConHistoria = false;
  }

  function soltarURL() {
    if (url) { URL.revokeObjectURL(url); url = null; }
  }

  function pintar() {
    var a = lista[indice];
    if (!a) return;
    soltarURL();
    url = URL.createObjectURL(a.blob);
    img.src = url;
    img.alt = a.nombre || 'Adjunto';
    nodo.querySelector('.visor__nombre').textContent = a.nombre || 'Adjunto';
    nodo.querySelector('.visor__pos').textContent = lista.length > 1
      ? (indice + 1) + ' de ' + lista.length : '';
    nodo.querySelector('[data-ant]').hidden = lista.length < 2;
    nodo.querySelector('[data-sig]').hidden = lista.length < 2;
    reiniciarZoom();
  }

  function mover(paso) {
    if (lista.length < 2) return;
    indice = (indice + paso + lista.length) % lista.length;
    pintar();
  }

  function compartirActual() {
    entregarAlSistema(lista[indice]);
  }

  /* Un PDF o cualquier otro archivo no se puede pintar aquí dentro: se le
     pasa al móvil, que lo abre con la aplicación que tenga para ello. */
  function entregarAlSistema(a) {
    if (!a) return;
    UI.descargar(a.nombre || 'adjunto', a.blob, a.mime || 'application/octet-stream', 'abrir');
  }

  /* ---------- zoom y arrastre ---------- */

  function aplicar() {
    img.style.transform = 'translate(' + x + 'px,' + y + 'px) scale(' + escala + ')';
  }

  function reiniciarZoom() {
    escala = 1; x = 0; y = 0;
    img.style.transition = '';
    aplicar();
  }

  function limitar() {
    var caja = lienzo.getBoundingClientRect();
    var maxX = Math.max(0, (caja.width * escala - caja.width) / 2);
    var maxY = Math.max(0, (caja.height * escala - caja.height) / 2);
    x = Math.min(maxX, Math.max(-maxX, x));
    y = Math.min(maxY, Math.max(-maxY, y));
  }

  function listaPunteros() {
    return Object.keys(punteros).map(function (k) { return punteros[k]; });
  }

  function alBajar(e) {
    lienzo.setPointerCapture(e.pointerId);
    punteros[e.pointerId] = { x: e.clientX, y: e.clientY };
    base = null;
    img.style.transition = '';

    /* Doble toque: acerca o aleja. */
    var ahora = Date.now();
    if (e.pointerType !== 'mouse' && ahora - ultimoToque < 300 && listaPunteros().length === 1) {
      alternarZoom(e.clientX, e.clientY);
    }
    ultimoToque = ahora;
  }

  function alMover(e) {
    if (!punteros[e.pointerId]) return;
    var previos = listaPunteros();
    punteros[e.pointerId] = { x: e.clientX, y: e.clientY };
    var actuales = listaPunteros();

    if (actuales.length >= 2) {
      var a = actuales[0], b = actuales[1];
      var dist = Math.hypot(b.x - a.x, b.y - a.y);
      var medio = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
      if (!base) { base = { dist: dist, escala: escala, medio: medio, x: x, y: y }; return; }
      var factor = dist / (base.dist || 1);
      escala = Math.min(ESCALA_MAX, Math.max(ESCALA_MIN, base.escala * factor));
      x = base.x + (medio.x - base.medio.x);
      y = base.y + (medio.y - base.medio.y);
      limitar();
      aplicar();
      return;
    }

    if (escala > 1 && previos.length === 1) {
      x += e.clientX - previos[0].x;
      y += e.clientY - previos[0].y;
      limitar();
      aplicar();
    }
  }

  function alSoltar(e) {
    delete punteros[e.pointerId];
    if (listaPunteros().length < 2) base = null;
    if (escala <= 1.02) { escala = 1; x = 0; y = 0; suave(); }
  }

  var ultimoZoom = 0;

  function alternarZoom(cx, cy) {
    /* El navegador manda su propio dblclick además de nuestra detección de
       doble toque: sin esta guarda el zoom se pondría y se quitaría en el
       mismo gesto. */
    var ahora = Date.now();
    if (ahora - ultimoZoom < 400) return;
    ultimoZoom = ahora;

    var caja = lienzo.getBoundingClientRect();
    if (escala > 1) { escala = 1; x = 0; y = 0; }
    else {
      escala = ESCALA_DOBLE;
      x = (caja.left + caja.width / 2 - cx) * (escala - 1);
      y = (caja.top + caja.height / 2 - cy) * (escala - 1);
      limitar();
    }
    suave();
  }

  function suave() {
    img.style.transition = 'transform .2s cubic-bezier(.22,.8,.3,1)';
    aplicar();
  }

  global.Visor = { abrir: abrir, cerrar: cerrar, esImagen: esImagen };
})(window);
