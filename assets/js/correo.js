/* correo.js — convierte en avisos los correos que descarga la parte nativa.
   Un navegador no puede hablar IMAP ni POP3, así que la conexión la hace la
   app de Android y aquí solo se recogen los mensajes ya descargados. */
(function (global) {
  'use strict';

  var S = global.Store;
  var TROZO = 192 * 1024;

  function puente() { return global.AvisosNativo; }

  function disponible() {
    var p = puente();
    return !!(p && p.correoPendientes);
  }

  function leerJSON(texto, porDefecto) {
    try { return JSON.parse(texto); } catch (e) { return porDefecto; }
  }

  /* ---------- estado y configuración ---------- */

  function estado() {
    if (!disponible()) return { soportado: false };
    var e = leerJSON(puente().correoEstado(), {}) || {};
    e.soportado = true;
    return e;
  }

  function guardarCuenta(cuenta) {
    if (!disponible()) return false;
    puente().correoGuardarCuenta(JSON.stringify(cuenta));
    return true;
  }

  function borrarCuenta() {
    if (disponible()) puente().correoBorrarCuenta();
  }

  function sincronizarAhora() {
    if (disponible()) puente().correoSincronizarAhora();
  }

  /* La prueba de conexión tarda: la parte nativa contesta llamando a
     Correo.alProbar cuando termina. */
  var esperandoPrueba = null;

  function probar(cuenta) {
    return new Promise(function (resolver) {
      if (!disponible()) { resolver({ ok: false, mensaje: 'Solo disponible en la app de Android' }); return; }
      esperandoPrueba = resolver;
      puente().correoProbar(JSON.stringify(cuenta));
      setTimeout(function () {
        if (esperandoPrueba === resolver) {
          esperandoPrueba = null;
          resolver({ ok: false, mensaje: 'La conexión ha tardado demasiado' });
        }
      }, 45000);
    });
  }

  function alProbar(json) {
    var r = leerJSON(json, { ok: false, mensaje: 'Respuesta no válida' });
    if (esperandoPrueba) { var f = esperandoPrueba; esperandoPrueba = null; f(r); }
  }

  /* ---------- texto del correo ---------- */

  var ENTIDADES = {
    amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ',
    aacute: 'á', eacute: 'é', iacute: 'í', oacute: 'ó', uacute: 'ú',
    ntilde: 'ñ', Ntilde: 'Ñ', uuml: 'ü', iexcl: '¡', iquest: '¿',
    Aacute: 'Á', Eacute: 'É', Iacute: 'Í', Oacute: 'Ó', Uacute: 'Ú',
    euro: '€', hellip: '…', mdash: '—', ndash: '–', laquo: '«', raquo: '»'
  };

  function decodificar(texto) {
    return String(texto).replace(/&(#x?[0-9a-fA-F]+|[a-zA-Z]+);/g, function (todo, cuerpo) {
      if (cuerpo.charAt(0) === '#') {
        var n = cuerpo.charAt(1) === 'x' || cuerpo.charAt(1) === 'X'
          ? parseInt(cuerpo.slice(2), 16)
          : parseInt(cuerpo.slice(1), 10);
        return isFinite(n) ? String.fromCodePoint(n) : todo;
      }
      return ENTIDADES[cuerpo] !== undefined ? ENTIDADES[cuerpo] : todo;
    });
  }

  function htmlATexto(html) {
    var t = String(html || '');
    t = t.replace(/<(script|style)[\s\S]*?<\/\1>/gi, ' ');
    t = t.replace(/<!--[\s\S]*?-->/g, ' ');
    t = t.replace(/<br\s*\/?>/gi, '\n');
    t = t.replace(/<\/(p|div|tr|li|h[1-6]|table|blockquote)>/gi, '\n');
    t = t.replace(/<li[^>]*>/gi, '· ');
    t = t.replace(/<[^>]+>/g, '');
    t = decodificar(t);
    t = t.replace(/\r\n?/g, '\n');
    t = t.replace(/[ \t ]+/g, ' ');
    t = t.replace(/ *\n */g, '\n');
    t = t.replace(/\n{3,}/g, '\n\n');
    return t.trim();
  }

  /* Corta la cadena de respuestas anteriores: al aviso solo le interesa
     lo último que han escrito. */
  function quitarCitas(texto) {
    var cortes = [
      /^-{2,}\s*Mensaje original\s*-{2,}/im,
      /^-{2,}\s*Original Message\s*-{2,}/im,
      /^El .{0,60}escribi[óo]:\s*$/im,
      /^On .{0,80}wrote:\s*$/im,
      /^De:\s.+\nEnviado:/im,
      /^From:\s.+\nSent:/im
    ];
    var fin = texto.length;
    cortes.forEach(function (re) {
      var m = re.exec(texto);
      if (m && m.index < fin) fin = m.index;
    });
    var recortado = texto.slice(0, fin).trim();
    return recortado || texto.trim();
  }

  function normalizarTexto(t) {
    return String(t || '')
      .replace(/\r\n?/g, '\n')
      .replace(/[ \t\u00a0]+/g, ' ')
      .replace(/ *\n */g, '\n')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  /* Si el correo trae texto plano se usa tal cual: pasarlo por el limpiador
     de HTML se comería cosas como «temperatura <5 grados». */
  function cuerpoLegible(mensaje) {
    var plano = (mensaje.texto || '').trim();
    if (plano) return quitarCitas(normalizarTexto(plano));
    return quitarCitas(htmlATexto(mensaje.html || ''));
  }

  /* ---------- deducciones ---------- */

  function sinAcentos(t) {
    var texto = String(t || '').toLowerCase();
    return texto.normalize ? texto.normalize('NFD').replace(/[̀-ͯ]/g, '') : texto;
  }

  var REGLAS_TIPO = [
    { tipo: 'averia', palabras: ['averia', 'no funciona', 'no va', 'fallo', 'falla', 'error', 'roto', 'incidencia', 'salta sola'] },
    { tipo: 'mantenimiento', palabras: ['mantenimiento', 'revision anual', 'preventivo'] },
    { tipo: 'presupuesto', palabras: ['presupuesto', 'oferta', 'precio', 'cotizacion'] },
    { tipo: 'instalacion', palabras: ['instalacion', 'instalar', 'montaje', 'ampliacion', 'nueva'] },
    { tipo: 'revision', palabras: ['revision', 'revisar', 'comprobar', 'verificar'] }
  ];

  var REGLAS_SISTEMA = [
    { sistema: 'cctv', palabras: ['camara', 'camaras', 'cctv', 'videovigilancia', 'grabador', 'dvr', 'nvr'] },
    { sistema: 'incendios', palabras: ['incendio', 'incendios', 'detector de humo', 'bie', 'extintor', 'pci'] },
    { sistema: 'accesos', palabras: ['control de acceso', 'lector', 'tarjeta', 'torno', 'huella'] },
    { sistema: 'interfono', palabras: ['portero', 'videoportero', 'interfono', 'telefonillo'] },
    { sistema: 'alarma', palabras: ['alarma', 'central', 'sirena', 'volumetrico', 'cra', 'intrusion'] }
  ];

  function deducir(reglas, texto, campo, porDefecto) {
    var t = sinAcentos(texto);
    for (var i = 0; i < reglas.length; i++) {
      var r = reglas[i];
      for (var j = 0; j < r.palabras.length; j++) {
        if (t.indexOf(r.palabras[j]) !== -1) return r[campo];
      }
    }
    return porDefecto;
  }

  function deducirPrioridad(texto) {
    var t = sinAcentos(texto);
    if (/\burgent|\burgencia|inmediat|cuanto antes|sin alarma|sin cobertura/.test(t)) return 'urgente';
    if (/\bprioritario|importante|hoy mismo/.test(t)) return 'alta';
    return 'normal';
  }

  /* Teléfonos españoles: 9 cifras que empiezan por 6, 7, 8 o 9. */
  function buscarTelefono(texto) {
    var m = /(?:\+34[\s.-]?)?([6789]\d{2})[\s.-]?(\d{2})[\s.-]?(\d{2})[\s.-]?(\d{2})/.exec(String(texto || ''));
    return m ? m[1] + m[2] + m[3] + m[4] : '';
  }

  function nombreDe(mensaje) {
    if (mensaje.deNombre && mensaje.deNombre.trim()) return mensaje.deNombre.trim();
    var correo = String(mensaje.de || '');
    var arroba = correo.indexOf('@');
    return arroba > 0 ? correo.slice(0, arroba) : (correo || 'Remitente desconocido');
  }

  function diaDe(iso) {
    var d = iso ? new Date(iso) : new Date();
    if (isNaN(d.getTime())) d = new Date();
    return S.hoyISO(d);
  }

  /* ---------- conversión ---------- */

  function aAviso(mensaje) {
    var aviso = S.nuevoAvisoVacio();
    var cuerpo = cuerpoLegible(mensaje);
    var conjunto = (mensaje.asunto || '') + '\n' + cuerpo;

    aviso.titulo = (mensaje.asunto || '').trim() || '(correo sin asunto)';
    aviso.descripcion = cuerpo;
    aviso.tipo = deducir(REGLAS_TIPO, conjunto, 'tipo', 'averia');
    aviso.sistema = deducir(REGLAS_SISTEMA, conjunto, 'sistema', 'otros');
    aviso.prioridad = deducirPrioridad(conjunto);
    aviso.estado = 'pendiente';
    aviso.fecha = diaDe(mensaje.fecha);
    aviso.hora = '';
    aviso.asignadoA = '';
    aviso.origen = 'correo';
    aviso.cliente = {
      nombre: nombreDe(mensaje),
      direccion: '',
      telefono: buscarTelefono(cuerpo),
      contacto: mensaje.de || ''
    };
    aviso.correo = {
      messageId: mensaje.messageId || '',
      uid: mensaje.uid || '',
      de: mensaje.de || '',
      deNombre: mensaje.deNombre || '',
      asunto: mensaje.asunto || '',
      fecha: mensaje.fecha || '',
      cuerpo: cuerpo
    };
    return aviso;
  }

  function ignorado(mensaje) {
    var lista = S.state.ajustes.correoIgnorados || [];
    var de = String(mensaje.de || '').toLowerCase();
    if (!de) return false;
    return lista.some(function (patron) {
      var p = String(patron || '').toLowerCase().trim();
      if (!p) return false;
      if (p.charAt(0) !== '@') return de === p;
      return de.length > p.length && de.slice(-p.length) === p;   // dominio entero
    });
  }

  function yaConvertido(mensaje) {
    var id = mensaje.messageId;
    return S.state.avisos.some(function (a) {
      if (!a.correo) return false;
      if (id && a.correo.messageId === id) return true;
      return !!mensaje.uid && a.correo.uid === mensaje.uid;
    });
  }

  /* ---------- adjuntos ---------- */

  function leerAdjunto(id, tamano) {
    var p = puente();
    var trozos = [];
    var pos = 0;
    try {
      while (pos < tamano) {
        var b64 = p.correoAdjuntoTrozo(id, pos, TROZO);
        if (!b64) break;
        var datos = deBase64(b64);
        if (!datos.length) break;
        trozos.push(datos);
        pos += datos.length;      // lo recibido, no lo pedido
      }
    } catch (e) {
      return null;
    }
    return trozos.length ? new Blob(trozos) : null;
  }

  function deBase64(b64) {
    var bin = atob(b64);
    var arr = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    return arr;
  }

  function guardarAdjuntos(avisoId, adjuntos) {
    return (adjuntos || []).reduce(function (cadena, adj) {
      return cadena.then(function () {
        var blob = leerAdjunto(adj.id, adj.tam || 0);
        if (!blob) return null;
        return S.addAdjunto(avisoId, {
          blob: blob,
          mime: adj.mime || 'application/octet-stream',
          nombre: adj.nombre || 'adjunto',
          origen: 'correo'
        });
      });
    }, Promise.resolve());
  }

  /* ---------- ciclo principal ---------- */

  function procesarPendientes() {
    if (!disponible()) return Promise.resolve({ creados: 0, ignorados: 0 });

    var mensajes = leerJSON(puente().correoPendientes(), []) || [];
    if (!mensajes.length) return Promise.resolve({ creados: 0, ignorados: 0 });

    var creados = 0, saltados = 0;
    var tratados = [];

    return mensajes.reduce(function (cadena, mensaje) {
      return cadena.then(function () {
        if (ignorado(mensaje) || yaConvertido(mensaje)) {
          saltados++;
          tratados.push(mensaje.uid);
          return null;
        }

        var aviso = aAviso(mensaje);
        return S.guardarAviso(aviso).then(function (guardado) {
          return guardarAdjuntos(guardado.id, mensaje.adjuntos).then(function () {
            creados++;
            tratados.push(mensaje.uid);   // solo se da por hecho lo que se guardó
          });
        });
      }).catch(function (e) {
        /* Sin marcarlo: en la próxima sincronización se vuelve a intentar.
           El correo sigue en el servidor, nunca se borra. */
        if (global.console) console.error('Correo no convertido', e);
      });
    }, Promise.resolve()).then(function () {
      try { puente().correoMarcarProcesados(JSON.stringify(tratados)); } catch (e) {}
      return { creados: creados, ignorados: saltados };
    });
  }

  /* La parte nativa llama aquí cuando entra correo con la app abierta. */
  function alLlegar() {
    procesarPendientes().then(function (r) {
      if (r.creados && global.App) {
        global.UI.toast(r.creados === 1 ? 'Nuevo aviso desde el correo' : r.creados + ' avisos nuevos desde el correo');
        global.App.render();
      }
    });
  }

  global.Correo = {
    disponible: disponible,
    estado: estado,
    guardarCuenta: guardarCuenta,
    borrarCuenta: borrarCuenta,
    sincronizarAhora: sincronizarAhora,
    probar: probar,
    alProbar: alProbar,
    alLlegar: alLlegar,
    procesarPendientes: procesarPendientes,
    aAviso: aAviso,
    htmlATexto: htmlATexto,
    quitarCitas: quitarCitas,
    buscarTelefono: buscarTelefono
  };
})(window);
