/* nativo.js — puente con la aplicación Android que envuelve la web.
   En un WebView las descargas por blob: no funcionan, así que los archivos
   (copias, CSV, calendario) se pasan al lado nativo por trozos y allí se
   guardan y se abren o comparten. En el navegador normal no hace nada. */
(function (global) {
  'use strict';

  var TROZO = 192 * 1024;   // bytes por envío, para no cruzar cadenas enormes

  function puente() { return global.AvisosNativo; }
  function disponible() { return !!puente(); }

  function base64De(blob) {
    return new Promise(function (res, rej) {
      var fr = new FileReader();
      fr.onload = function () { res(String(fr.result).split(',')[1] || ''); };
      fr.onerror = function () { rej(fr.error); };
      fr.readAsDataURL(blob);
    });
  }

  /* modo: 'abrir' (el sistema ofrece con qué abrirlo, p. ej. Calendario)
           'compartir' (hoja de compartir: correo, nube, archivos…) */
  function entregarArchivo(nombre, mime, contenido, modo) {
    if (!disponible()) return Promise.resolve(false);
    var blob = (contenido instanceof Blob) ? contenido : new Blob([contenido], { type: mime });
    var p = puente();

    try { p.iniciarArchivo(nombre, mime); }
    catch (e) { return Promise.reject(e); }

    var pos = 0;
    function siguiente() {
      if (pos >= blob.size) {
        p.finalizarArchivo(modo || 'compartir');
        return true;
      }
      var corte = blob.slice(pos, pos + TROZO);
      pos += TROZO;
      return base64De(corte).then(function (b64) {
        p.anexarTrozo(b64);
        return siguiente();
      });
    }

    return Promise.resolve().then(siguiente).catch(function (e) {
      try { p.cancelarArchivo(); } catch (err) {}
      throw e;
    });
  }

  /* Los contadores que enseña el widget de la pantalla de inicio. */
  function publicarResumen(resumen) {
    var p = puente();
    if (!p || !p.guardarResumen) return;
    try { p.guardarResumen(JSON.stringify(resumen)); } catch (e) {}
  }

  global.Nativo = {
    disponible: disponible,
    entregarArchivo: entregarArchivo,
    publicarResumen: publicarResumen
  };
})(window);
