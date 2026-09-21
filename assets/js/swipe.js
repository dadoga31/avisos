/* swipe.js — gestos sobre las filas de aviso.
   Deslizar a la izquierda lo marca como hecho; a la derecha despliega
   «En curso» y «Cancelar». Se apoya en Pointer Events, así que funciona
   igual con el dedo y con el ratón. */
(function (global) {
  'use strict';

  var UMBRAL_MIN = 96;        // px mínimos para dar por hecho el aviso
  var UMBRAL_PROP = 0.38;     // o esta parte del ancho de la fila
  var cfg = {};
  var abierta = null;         // fila con las acciones desplegadas
  var global_ = false;

  function umbralDe(cont) {
    return Math.max(UMBRAL_MIN, cont.offsetWidth * UMBRAL_PROP);
  }

  /* Lo que se abre a la derecha es justo lo que ocupan sus botones:
     dos acciones en un aviso abierto, una sola («Reabrir») si está cerrado. */
  function anchoAcciones(cont) {
    /* El contenedor ocupa toda la fila (inset:0): lo que manda es lo que
       suman los botones. */
    var total = 0;
    Array.prototype.forEach.call(cont.querySelectorAll('.swipe__acc'), function (b) {
      total += b.offsetWidth;
    });
    return total;
  }

  function permiteHecho(cont) {
    return cont.dataset.hecho !== '0';
  }

  function vibrar(ms) {
    if (navigator.vibrate) { try { navigator.vibrate(ms); } catch (e) {} }
  }

  function cerrar(cont) {
    if (!cont) return;
    var front = cont.querySelector('.swipe__front');
    if (front) { front.style.transition = ''; front.style.transform = ''; }
    cont.classList.remove('swipe--izq', 'swipe--der', 'swipe--abierta', 'swipe--armado');
    cont.__armado = false;
    if (abierta === cont) abierta = null;
  }

  function cerrarAbierta() { if (abierta) cerrar(abierta); }

  function abrir(cont, front) {
    front.style.transition = '';
    front.style.transform = 'translateX(' + anchoAcciones(cont) + 'px)';
    cont.classList.remove('swipe--izq', 'swipe--armado');
    cont.classList.add('swipe--der', 'swipe--abierta');
    abierta = cont;
  }

  /* Sale deslizándose y encogiendo; al terminar avisa a la vista. */
  function completar(cont, front) {
    var id = cont.dataset.swipe;
    cont.style.height = cont.offsetHeight + 'px';
    cont.classList.add('swipe--saliendo');
    front.style.transition = '';
    front.style.transform = 'translateX(-100%)';
    vibrar(18);
    requestAnimationFrame(function () {
      requestAnimationFrame(function () {
        cont.style.height = '0px';
        cont.style.opacity = '0';
      });
    });
    setTimeout(function () {
      if (cfg.onEstado) cfg.onEstado(id, 'resuelto');
    }, 250);
  }

  function montar(cont) {
    var front = cont.querySelector('.swipe__front');
    if (!front) return;

    var x0 = 0, y0 = 0, eje = null, base = 0, d = 0, arrastrado = false, activo = false;

    cont.addEventListener('pointerdown', function (e) {
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      if (e.target.closest('.swipe__acciones')) return;
      if (abierta && abierta !== cont) cerrar(abierta);
      x0 = e.clientX; y0 = e.clientY;
      eje = null; d = 0; arrastrado = false; activo = true;
      base = (abierta === cont) ? anchoAcciones(cont) : 0;
      front.style.transition = 'none';
    });

    cont.addEventListener('pointermove', function (e) {
      if (!activo) return;
      var ddx = e.clientX - x0, ddy = e.clientY - y0;

      if (!eje) {
        if (Math.abs(ddx) > 10 && Math.abs(ddx) > Math.abs(ddy) * 1.3) {
          eje = 'h';
          try { cont.setPointerCapture(e.pointerId); } catch (err) {}
        } else if (Math.abs(ddy) > 10) {
          eje = 'v'; activo = false; front.style.transition = ''; return;
        } else {
          return;
        }
      }
      if (eje !== 'h') return;

      arrastrado = true;
      var tope = anchoAcciones(cont);
      d = base + ddx;
      if (d > tope) d = tope + (d - tope) * 0.25;                 // resistencia
      if (d < 0 && !permiteHecho(cont)) d = d * 0.12;             // cerrado: no hay «hecho»
      if (d < -cont.offsetWidth) d = -cont.offsetWidth;

      front.style.transform = 'translateX(' + d + 'px)';
      cont.classList.toggle('swipe--izq', d < 0);
      cont.classList.toggle('swipe--der', d > 0);

      var armado = permiteHecho(cont) && d <= -umbralDe(cont);
      if (armado !== !!cont.__armado) {
        cont.__armado = armado;
        cont.classList.toggle('swipe--armado', armado);
        if (armado) vibrar(12);
      }
    });

    function soltar() {
      if (!activo) return;
      activo = false;
      front.style.transition = '';
      if (eje !== 'h') return;

      if (permiteHecho(cont) && d <= -umbralDe(cont)) completar(cont, front);
      else if (d > 0 && d >= anchoAcciones(cont) * 0.5) abrir(cont, front);
      else cerrar(cont);
    }

    cont.addEventListener('pointerup', soltar);
    cont.addEventListener('pointercancel', function () {
      activo = false; front.style.transition = ''; cerrar(cont);
    });

    /* Un arrastre no debe abrir el aviso, y con las acciones desplegadas
       el primer toque sobre la fila solo las cierra. */
    cont.addEventListener('click', function (e) {
      /* Los botones de acción se pulsan justo después del arrastre que los
         descubre: nunca se les suprime el clic. */
      if (e.target.closest('.swipe__acciones')) { arrastrado = false; return; }

      if (arrastrado) {
        arrastrado = false;
        e.preventDefault(); e.stopPropagation();
        return;
      }
      if (abierta === cont) {
        e.preventDefault(); e.stopPropagation();
        cerrar(cont);
      }
    }, true);

    Array.prototype.forEach.call(cont.querySelectorAll('.swipe__acc'), function (b) {
      b.addEventListener('click', function (e) {
        e.stopPropagation();
        var estado = b.dataset.estado;
        cerrar(cont);
        if (cfg.onEstado) cfg.onEstado(cont.dataset.swipe, estado);
      });
    });
  }

  function conectar(root, opts) {
    cfg = opts || {};
    abierta = null;
    Array.prototype.forEach.call(root.querySelectorAll('[data-swipe]'), montar);

    if (!global_) {
      global_ = true;
      document.addEventListener('pointerdown', function (e) {
        if (abierta && !abierta.contains(e.target)) cerrar(abierta);
      }, true);
      global.addEventListener('scroll', cerrarAbierta, { passive: true });
    }
  }

  global.Swipe = { conectar: conectar, cerrarAbierta: cerrarAbierta };
})(window);
