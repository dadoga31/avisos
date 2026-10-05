/* ios.js — lo que hace que la web parezca una app del iPhone: esquinas
   concéntricas con la pantalla, vibración, luz al tocar, bloques que aparecen
   al llegar a ellos, cifras que cuentan, color de la barra de estado, lente de
   la pestaña activa, segmentados con muelle y hoja que se cierra arrastrando.

   Lo que una web NO puede imitar de una app nativa, para no prometerlo:
   la refracción real del Liquid Glass (se aproxima con desenfoque, brillo y
   un borde especular), el radio de las esquinas de la pantalla (se deduce
   del modelo), la vibración (se usa un truco que solo va desde iOS 18) y el
   gesto de volver deslizando desde el borde. */
(function (global) {
  'use strict';

  var doc = document.documentElement;

  function esIOS() {
    return /iPad|iPhone|iPod/.test(navigator.userAgent) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  }

  function instalada() {
    if (navigator.standalone === true) return true;
    return !!(global.matchMedia && global.matchMedia('(display-mode: standalone)').matches);
  }

  function sinMovimiento() {
    return !!(global.matchMedia && global.matchMedia('(prefers-reduced-motion: reduce)').matches);
  }

  /* ---------- esquinas de la pantalla ---------- */

  /* Radio de las esquinas de cada iPhone, en puntos. iOS no lo da a la web,
     así que se deduce del tamaño. 0: modelos con botón de inicio. */
  var RADIOS = {
    '320x568': 0, '375x667': 0, '414x736': 0,
    '375x812': 44,
    '414x896': function (dpr) { return dpr >= 3 ? 39 : 41.5; },
    '390x844': 47.33,
    '428x926': 53.33,
    '393x852': 55, '430x932': 55,
    '402x874': 62, '440x956': 62
  };
  var RADIO_DESCONOCIDO = 55;

  function radioPantalla(ancho, alto, dpr, sinBoton) {
    var r = RADIOS[Math.min(ancho, alto) + 'x' + Math.max(ancho, alto)];
    if (typeof r === 'function') return r(dpr);
    if (typeof r === 'number') return r;
    return sinBoton ? RADIO_DESCONOCIDO : 0;
  }

  function margenInferior() {
    var p = document.createElement('div');
    p.style.cssText = 'position:fixed;visibility:hidden;padding-bottom:env(safe-area-inset-bottom,0px)';
    document.body.appendChild(p);
    var v = parseFloat(getComputedStyle(p).paddingBottom) || 0;
    p.remove();
    return v;
  }

  /* Solo con la app instalada: en Safari la barra del navegador ocupa el
     borde y no hay esquinas con las que casar. */
  function esquinas() {
    if (!esIOS() || !instalada()) return;
    var r = radioPantalla(screen.width, screen.height, global.devicePixelRatio || 1, margenInferior() > 0);
    if (r <= 0) return;
    doc.style.setProperty('--screen-radius', r + 'px');
    doc.setAttribute('data-screen-corners', '');
  }

  /* ---------- vibración ---------- */

  var gatillo = null;

  /* Safari no tiene API de vibración, pero desde iOS 18 un interruptor
     nativo (<input switch>) vibra al cambiar: se pulsa uno invisible. En
     Android, navigator.vibrate. */
  function vibrar() {
    try {
      if (!esIOS()) { if (navigator.vibrate) navigator.vibrate(8); return; }
      if (!gatillo) {
        var input = document.createElement('input');
        input.type = 'checkbox';
        input.id = 'vibrador';
        input.setAttribute('switch', '');
        gatillo = document.createElement('label');
        gatillo.htmlFor = 'vibrador';
        var caja = document.createElement('div');
        caja.setAttribute('aria-hidden', 'true');
        caja.style.cssText = 'position:fixed;left:-100px;top:0;width:1px;height:1px;overflow:hidden;opacity:0;pointer-events:none';
        caja.appendChild(input);
        caja.appendChild(gatillo);
        document.body.appendChild(caja);
      }
      gatillo.click();
    } catch (e) { /* sin vibración no pasa nada */ }
  }

  /* ---------- luz al tocar ---------- */

  var CON_LUZ = [
    '.tab', '.tab-mas', '.btn', '.chip', '.circulo', '.segmented label', '.statusgrid button',
    'button.fila', 'a.fila', 'label.fila', '.avrow', '.cierre', '.hero__stat', '.archivo'
  ].join(',');

  function anfitrion(el) {
    if (el.matches('.tab-mas')) return el.querySelector('.mas') || el;
    return el;
  }

  /* Al tocar nace una luz en el punto del dedo, crece con muelle y se apaga
     al soltar. */
  function luz() {
    if (sinMovimiento()) return;
    document.addEventListener('pointerdown', function (e) {
      var el = e.target && e.target.closest && e.target.closest(CON_LUZ);
      if (!el || el.disabled) return;
      var host = anfitrion(el);
      host.classList.add('glow-host');
      var r = host.getBoundingClientRect();
      var lado = Math.max(r.width, r.height) * 1.6;
      var g = document.createElement('span');
      g.className = 'touch-glow';
      g.setAttribute('aria-hidden', 'true');
      g.style.width = g.style.height = lado + 'px';
      g.style.left = (e.clientX - r.left - lado / 2) + 'px';
      g.style.top = (e.clientY - r.top - lado / 2) + 'px';
      host.appendChild(g);

      function soltar() {
        global.removeEventListener('pointerup', soltar);
        global.removeEventListener('pointercancel', soltar);
        g.classList.add('touch-glow--fuera');
        g.addEventListener('animationend', function () { g.remove(); }, { once: true });
        setTimeout(function () { g.remove(); }, 900);   // por si no llega a animarse
      }
      global.addEventListener('pointerup', soltar);
      global.addEventListener('pointercancel', soltar);
    }, { passive: true });
  }

  /* ---------- aparición al hacer scroll ---------- */

  var BLOQUES = '.view > .hero, .view > .pista, .view > .section, .view > .card, .view > .kpis, ' +
    '.view > .cierre, .view > .bloque, .view > .empty, .view > form > .section';
  var CASCADA_MS = 55;
  var observador = null;

  /* Cada bloque aparece la primera vez que entra en pantalla. Solo al entrar
     en una pantalla: si la misma se vuelve a pintar (un cambio de estado, un
     filtro), lo que ya se veía no vuelve a animarse. */
  function aparecer(root) {
    if (observador) { observador.disconnect(); observador = null; }
    if (sinMovimiento() || !('IntersectionObserver' in global)) return;

    observador = new IntersectionObserver(function (entradas) {
      entradas
        .filter(function (x) { return x.isIntersecting; })
        .sort(function (a, b) { return a.boundingClientRect.top - b.boundingClientRect.top; })
        .forEach(function (x, i) {
          x.target.style.setProperty('--reveal-delay', Math.min(i, 6) * CASCADA_MS + 'ms');
          x.target.setAttribute('data-reveal', 'in');
          observador.unobserve(x.target);
        });
    }, { rootMargin: '0px 0px -6% 0px', threshold: 0.04 });

    var bloques = Array.prototype.slice.call(root.parentNode.querySelectorAll(BLOQUES));
    bloques.forEach(function (el) {
      el.setAttribute('data-reveal', 'pending');
      observador.observe(el);
    });
    /* Red de seguridad: nada se queda escondido aunque el observador falle. */
    setTimeout(function () {
      bloques.forEach(function (el) {
        if (el.getAttribute('data-reveal') === 'pending' && el.getBoundingClientRect().top < global.innerHeight) {
          el.setAttribute('data-reveal', 'in');
        }
      });
    }, 1200);
  }

  /* ---------- cifras que cuentan ---------- */

  /* La cifra cuenta hasta su valor (ease-out exponencial) y el anillo se
     llena con muelle, como en las apps de Apple. */
  function contar(root, opts) {
    var quieto = sinMovimiento() || !!(opts && opts.sinAnimar);
    Array.prototype.forEach.call(root.querySelectorAll('[data-cuenta]'), function (el) {
      var fin = Number(el.getAttribute('data-cuenta')) || 0;
      if (quieto || !fin) { el.textContent = fin; return; }
      var t0 = performance.now(), dur = 750;
      el.textContent = '0';
      (function paso(ahora) {
        var t = Math.min(1, (ahora - t0) / dur);
        el.textContent = t === 1 ? fin : Math.round(fin * (1 - Math.pow(2, -10 * t)));
        if (t < 1) requestAnimationFrame(paso);
      })(t0);
    });
    Array.prototype.forEach.call(root.querySelectorAll('[data-anillo]'), function (c) {
      var largo = Number(c.getAttribute('stroke-dasharray')) || 0;
      var meta = largo * (1 - Math.max(0, Math.min(1, Number(c.getAttribute('data-anillo')) || 0)));
      if (quieto) { c.style.strokeDashoffset = meta; return; }
      c.style.strokeDashoffset = largo;
      requestAnimationFrame(function () {
        requestAnimationFrame(function () { c.style.strokeDashoffset = meta; });
      });
    });
  }

  /* ---------- barra de estado ---------- */

  /* Con «default», iOS pinta la barra de estado como una franja del color de
     theme-color. Tiene que coincidir con el borde de arriba de la página (o
     con el velo de la hoja) para que no se vea el corte. */
  var BARRA = { claro: '#f2f2f7', oscuro: '#000000', claroVelado: '#b6b6b9' };

  function temaOscuro() {
    var t = doc.getAttribute('data-theme');
    if (t) return t === 'dark';
    return !!(global.matchMedia && global.matchMedia('(prefers-color-scheme: dark)').matches);
  }

  function barraEstado() {
    var hoja = document.getElementById('sheet');
    var velada = !!(hoja && !hoja.hidden && !hoja.classList.contains('sheet--cerrando'));
    var color = temaOscuro() ? BARRA.oscuro : (velada ? BARRA.claroVelado : BARRA.claro);
    Array.prototype.forEach.call(document.querySelectorAll('meta[name="theme-color"]'), function (m) {
      m.setAttribute('content', color);
      m.removeAttribute('media');         // manda el tema elegido en la app
    });
  }

  /* ---------- título grande que se recoge ---------- */

  var RECOGER_EN = 38;

  function vigilarTitulo() {
    var barra = document.getElementById('navbar');
    var marco = 0;
    function mirar() {
      var y = global.scrollY;
      var apilada = doc.getAttribute('data-vista') === 'apilada';
      barra.classList.toggle('navbar--material', apilada ? y > 4 : y > RECOGER_EN);
      if (y > 4) doc.setAttribute('data-scrolled', ''); else doc.removeAttribute('data-scrolled');
    }
    global.addEventListener('scroll', function () {
      cancelAnimationFrame(marco);
      marco = requestAnimationFrame(mirar);
    }, { passive: true });
    return mirar;
  }

  /* ---------- lente de la pestaña activa ---------- */

  var tLente = null;

  /* Se mide en vez de calcularse: así vale aunque cambien las columnas. */
  function lente(animar) {
    var barra = document.getElementById('tabbar');
    var l = document.getElementById('lente');
    if (!barra || !l) return;
    var activa = barra.querySelector('.tab[aria-current="page"]');
    l.classList.toggle('tab-lente--fuera', !activa);
    if (!activa) return;
    var x = activa.offsetLeft + 'px', w = activa.offsetWidth + 'px';
    var mueve = animar && barra.style.getPropertyValue('--lens-x') && barra.style.getPropertyValue('--lens-x') !== x;
    if (!animar || sinMovimiento()) l.style.transition = 'none';
    barra.style.setProperty('--lens-x', x);
    barra.style.setProperty('--lens-w', w);
    if (!animar || sinMovimiento()) { void l.offsetWidth; l.style.transition = ''; return; }
    if (mueve) {
      l.classList.add('tab-lente--mueve');
      clearTimeout(tLente);
      tLente = setTimeout(function () { l.classList.remove('tab-lente--mueve'); }, 450);
    }
  }

  /* ---------- segmentados ---------- */

  /* Una sola pieza que se desliza con muelle entre opciones. */
  function segmentados(root) {
    Array.prototype.forEach.call(root.querySelectorAll('.segmented'), function (s) {
      var opciones = s.querySelectorAll('label');
      if (!opciones.length || s.querySelector('.seg-thumb')) return;
      var pieza = document.createElement('span');
      pieza.className = 'seg-thumb';
      pieza.setAttribute('aria-hidden', 'true');
      s.insertBefore(pieza, s.firstChild);
      s.classList.add('segmented--thumb');
      s.style.setProperty('--n', opciones.length);
      function colocar() {
        var i = -1;
        Array.prototype.forEach.call(opciones, function (o, k) {
          var r = o.querySelector('input');
          if (r && r.checked) i = k;
        });
        pieza.classList.toggle('seg-thumb--nada', i < 0);
        s.style.setProperty('--i', Math.max(0, i));
      }
      s.addEventListener('change', function () { colocar(); vibrar(); });
      colocar();
    });
  }

  /* ---------- hoja que se cierra arrastrando ---------- */

  var DISTANCIA_CIERRE = 120;

  function arrastrarHoja(hoja, onCerrar) {
    var cabeza = hoja.querySelector('.sheet__head');
    var panel = hoja.querySelector('.sheet__panel');
    var y0 = 0, dy = 0, activo = false;
    cabeza.addEventListener('touchstart', function (e) {
      if (e.target.closest('button')) return;
      activo = true; y0 = e.touches[0].clientY; dy = 0;
      hoja.classList.add('sheet--arrastre');
    }, { passive: true });
    cabeza.addEventListener('touchmove', function (e) {
      if (!activo) return;
      dy = Math.max(0, e.touches[0].clientY - y0);
      panel.style.transform = 'translateY(' + dy + 'px)';
    }, { passive: true });
    function fin() {
      if (!activo) return;
      activo = false;
      hoja.classList.remove('sheet--arrastre');
      if (dy > DISTANCIA_CIERRE) { panel.style.transform = ''; onCerrar(); return; }
      hoja.classList.add('sheet--suelta');
      panel.style.transform = '';
      setTimeout(function () { hoja.classList.remove('sheet--suelta'); }, 560);
    }
    cabeza.addEventListener('touchend', fin);
    cabeza.addEventListener('touchcancel', fin);
  }

  global.Ios = {
    esIOS: esIOS, instalada: instalada, sinMovimiento: sinMovimiento,
    esquinas: esquinas, vibrar: vibrar, luz: luz,
    aparecer: aparecer, contar: contar, barraEstado: barraEstado,
    vigilarTitulo: vigilarTitulo, lente: lente, segmentados: segmentados,
    arrastrarHoja: arrastrarHoja, radioPantalla: radioPantalla
  };
})(window);
