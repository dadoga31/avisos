/* buzon.js — recoge los avisos que deja un atajo del iPhone.

   Un atajo no puede escribir dentro de la app: si abre un enlace, el iPhone lo
   manda a Safari, que guarda sus datos aparte de la app de la pantalla de
   inicio, y el aviso se quedaría donde nadie lo ve. Así que el atajo lo deja
   en /api/buzon y la app lo recoge cada vez que se abre o se vuelve a ella.

   El token es un secreto que genera la propia app: quien lo tenga puede dejar
   y recoger avisos de ese buzón, así que no se enseña entero por pantalla más
   que cuando hace falta copiarlo. */
(function (global) {
  'use strict';

  var S = global.Store;
  var RUTA = 'api/buzon';
  var ESPERA = 12000;

  function ajustes() { return S.state.ajustes || {}; }
  function token() { return ajustes().buzonToken || ''; }
  function activo() { return !!token(); }

  function direccion() {
    return location.origin + location.pathname.replace(/index\.html$/, '') + RUTA;
  }

  function nuevoToken() {
    var bytes = new Uint8Array(24);
    if (global.crypto && crypto.getRandomValues) crypto.getRandomValues(bytes);
    else for (var i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256);
    return Array.prototype.map.call(bytes, function (b) {
      return b.toString(16).padStart(2, '0');
    }).join('');
  }

  /* Una llamada al buzón, con su plazo: si el móvil está sin cobertura no
     tiene sentido dejar la promesa colgada para siempre. */
  function llamar(opciones) {
    opciones = opciones || {};
    var ctl = global.AbortController ? new AbortController() : null;
    var reloj = setTimeout(function () { if (ctl) ctl.abort(); }, ESPERA);

    return fetch(direccion() + (opciones.consulta || ''), {
      method: opciones.metodo || 'GET',
      headers: { 'X-Token': opciones.token || token() },
      body: opciones.cuerpo,
      cache: 'no-store',
      signal: ctl ? ctl.signal : undefined
    }).then(function (r) {
      return r.text().then(function (t) {
        var j = null;
        try { j = JSON.parse(t); } catch (e) {}
        if (!r.ok) {
          var e = new Error((j && j.mensaje) || ('El buzón respondió ' + r.status));
          e.codigo = (j && j.error) || String(r.status);
          throw e;
        }
        return j || {};
      });
    }).catch(function (e) {
      if (e && e.name === 'AbortError') {
        var t = new Error('El buzón no contesta');
        t.codigo = 'tiempo';
        throw t;
      }
      /* «Failed to fetch» no le dice nada a nadie. */
      if (e instanceof TypeError) {
        var r = new Error('No se ha podido llegar al buzón: mira la cobertura');
        r.codigo = 'red';
        throw r;
      }
      throw e;
    }).then(function (r) { clearTimeout(reloj); return r; },
            function (e) { clearTimeout(reloj); throw e; });
  }

  /* Comprueba que el buzón está montado antes de dar por buena la
     configuración: sin el almacén conectado en Vercel no sirve de nada. */
  function probar(tk) {
    return llamar({ token: tk || token(), consulta: '?ojear=1' });
  }

  function activar() {
    var tk = nuevoToken();
    return probar(tk).then(function () {
      return S.guardarAjustes({ buzonToken: tk, buzonUltima: '', buzonError: '' });
    });
  }

  function desactivar() {
    return S.guardarAjustes({ buzonToken: '', buzonUltima: '', buzonError: '' });
  }

  /* Recoge lo que haya y lo convierte en avisos. El buzón entrega y vacía en
     la misma llamada, así que lo que llega hay que guardarlo sí o sí: si
     fallara a medias se perdería, y por eso se guarda uno a uno y se sigue
     aunque alguno dé error. */
  function recoger() {
    if (!activo()) return Promise.resolve({ creados: 0 });

    return llamar({}).then(function (r) {
      var lista = (r && r.avisos) || [];
      if (!lista.length) {
        return S.guardarAjustes({ buzonUltima: new Date().toISOString(), buzonError: '' })
          .then(function () { return { creados: 0, quedan: 0 }; });
      }

      var creados = 0;
      var cadena = Promise.resolve();
      lista.forEach(function (sobre) {
        cadena = cadena.then(function () {
          var datos = global.Atajos.deTexto(sobre.texto);
          if (!datos || !datos.titulo) return;
          var a = global.Atajos.aAviso(datos);
          a.origen = 'atajo';
          if (sobre.recibido) a.creado = sobre.recibido;
          return S.guardarAviso(a).then(function () { creados++; });
        }).catch(function (e) { console.warn('Aviso del buzón no guardado:', e); });
      });

      return cadena
        .then(function () { return S.guardarAjustes({ buzonUltima: new Date().toISOString(), buzonError: '' }); })
        .then(function () { return { creados: creados, quedan: (r && r.quedan) || 0 }; });
    }).catch(function (e) {
      return S.guardarAjustes({ buzonError: String(e && e.message || e) })
        .then(function () { throw e; });
    });
  }

  function estado() {
    return {
      activo: activo(),
      direccion: direccion(),
      token: token(),
      ultima: ajustes().buzonUltima || '',
      error: ajustes().buzonError || ''
    };
  }

  global.Buzon = {
    activo: activo, estado: estado, direccion: direccion,
    activar: activar, desactivar: desactivar, probar: probar, recoger: recoger
  };
})(window);
