/* atajos.js — entrada rápida de avisos desde fuera de la app.

   Un atajo del iPhone (o cualquier otra cosa: un correo, un WhatsApp) no puede
   escribir en la base de datos de la app, así que deja el aviso escrito en
   texto y la app lo entiende. Vale tanto un enlace

     https://…/#/nuevo?titulo=Alarma%20en%20fallo&cliente=Farmacia&p=urgente

   como texto suelto

     Alarma en fallo
     cliente: Farmacia Centro
     tel: 611223344
     prioridad: urgente

   y, si no trae ninguna clave, la primera línea es el título y el resto la
   descripción. Así el atajo puede ser solo «dicta y copia». */
(function (global) {
  'use strict';

  var S = global.Store;

  /* Las claves se comparan sin tildes ni mayúsculas: nadie va a escribir
     «descripción» con tilde dentro de un atajo. */
  var ALIAS = {
    titulo: 'titulo', t: 'titulo', asunto: 'titulo', aviso: 'titulo', trabajo: 'titulo',
    cliente: 'cliente', c: 'cliente', empresa: 'cliente',
    direccion: 'direccion', dir: 'direccion', lugar: 'direccion', calle: 'direccion',
    telefono: 'telefono', tel: 'telefono', movil: 'telefono', tfno: 'telefono',
    contacto: 'contacto', persona: 'contacto',
    descripcion: 'descripcion', desc: 'descripcion', d: 'descripcion',
    notas: 'descripcion', nota: 'descripcion', detalle: 'descripcion',
    fecha: 'fecha', dia: 'fecha', cuando: 'fecha',
    hora: 'hora', h: 'hora',
    duracion: 'duracion',
    prioridad: 'prioridad', p: 'prioridad',
    tipo: 'tipo',
    sistema: 'sistema',
    tecnico: 'tecnico', asignado: 'tecnico', asignadoa: 'tecnico',
    estado: 'estado',
    crear: 'crear'
  };

  var CAMPOS = ['titulo', 'cliente', 'direccion', 'telefono', 'contacto', 'descripcion',
    'fecha', 'hora', 'duracion', 'prioridad', 'tipo', 'sistema', 'tecnico', 'estado', 'crear'];

  function sinTildes(s) {
    return String(s == null ? '' : s)
      .normalize ? String(s).normalize('NFD').replace(/[̀-ͯ]/g, '') : String(s);
  }
  function llave(s) {
    return sinTildes(s).toLowerCase().replace(/[^a-z0-9]/g, '');
  }

  /* ---------- valores ---------- */

  var DIAS = ['domingo', 'lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado'];

  function aFecha(v) {
    var t = llave(v);
    if (!t) return '';
    if (t === 'hoy') return S.hoyISO();
    if (t === 'manana' || t === 'mnana') return S.sumaDias(S.hoyISO(), 1);
    if (t === 'pasadomanana' || t === 'pasado') return S.sumaDias(S.hoyISO(), 2);

    var mas = /^\+?(\d+)dias?$/.exec(t) || /^en(\d+)dias?$/.exec(t);
    if (mas) return S.sumaDias(S.hoyISO(), Number(mas[1]));
    if (/^\+\d+$/.test(String(v).trim())) return S.sumaDias(S.hoyISO(), Number(String(v).trim().slice(1)));

    /* «el lunes» / «lunes»: el próximo que venga, hoy no cuenta. */
    var dia = DIAS.indexOf(t.replace(/^el/, ''));
    if (dia !== -1) {
      var hoy = new Date(S.hoyISO() + 'T12:00:00');
      var salto = (dia - hoy.getDay() + 7) % 7 || 7;
      return S.sumaDias(S.hoyISO(), salto);
    }

    var s = String(v).trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;

    /* 30/9, 30-09-2026, 30.09.26 — en España el día va delante. */
    var m = /^(\d{1,2})[\/\-.](\d{1,2})(?:[\/\-.](\d{2,4}))?$/.exec(s);
    if (m) {
      var d = Number(m[1]), mes = Number(m[2]);
      var ano = m[3] ? Number(m[3]) : Number(S.hoyISO().slice(0, 4));
      if (ano < 100) ano += 2000;
      if (d < 1 || d > 31 || mes < 1 || mes > 12) return '';
      var iso = ano + '-' + ('0' + mes).slice(-2) + '-' + ('0' + d).slice(-2);
      /* Sin año escrito y ya pasada, se entiende que es del año que viene. */
      if (!m[3] && iso < S.hoyISO()) iso = (ano + 1) + iso.slice(4);
      return iso;
    }
    return '';
  }

  function aHora(v) {
    var s = String(v).trim().toLowerCase().replace(/\s/g, '');
    var m = /^(\d{1,2})(?:[:.h](\d{2}))?h?$/.exec(s) || /^(\d{2})(\d{2})$/.exec(s);
    if (!m) return '';
    var h = Number(m[1]), mi = Number(m[2] || 0);
    if (h > 23 || mi > 59) return '';
    return ('0' + h).slice(-2) + ':' + ('0' + mi).slice(-2);
  }

  /* Acepta el identificador («cctv») o la etiqueta visible («CCTV», «Avería»). */
  function deCatalogo(lista, v, extras) {
    var t = llave(v);
    if (!t) return '';
    var i, opt;
    for (i = 0; i < lista.length; i++) {
      opt = lista[i];
      if (llave(opt.id) === t || llave(opt.label) === t) return opt.id;
    }
    if (extras) {
      for (i = 0; i < extras.length; i++) {
        if (extras[i].voces.indexOf(t) !== -1) return extras[i].id;
      }
    }
    return '';
  }

  var VOCES_SISTEMA = [
    { id: 'cctv', voces: ['camara', 'camaras', 'video', 'videovigilancia', 'grabador', 'dvr', 'nvr'] },
    { id: 'accesos', voces: ['acceso', 'lector', 'tarjeta', 'tarjetas', 'torno'] },
    { id: 'incendios', voces: ['incendio', 'fuego', 'humo', 'extincion', 'pci'] },
    { id: 'interfono', voces: ['portero', 'videoportero', 'telefonillo', 'porteroautomatico'] },
    { id: 'alarma', voces: ['intrusion', 'central', 'sirena', 'cra'] }
  ];
  var VOCES_TIPO = [
    { id: 'averia', voces: ['avena', 'fallo', 'rotura', 'reparacion', 'nofunciona'] },
    { id: 'instalacion', voces: ['montaje', 'altanueva', 'nuevainstalacion'] },
    { id: 'mantenimiento', voces: ['manto', 'revisionanual'] },
    { id: 'revision', voces: ['repaso', 'comprobacion'] },
    { id: 'presupuesto', voces: ['oferta', 'valoracion', 'presu'] }
  ];

  function aTecnico(v) {
    var t = llave(v);
    if (!t) return '';
    var exacto = '', parcial = '';
    S.state.tecnicos.forEach(function (tc) {
      var n = llave(tc.nombre);
      if (n === t) exacto = tc.id;
      else if (!parcial && (n.indexOf(t) === 0 || t.indexOf(n) === 0)) parcial = tc.id;
    });
    return exacto || parcial;
  }

  function esSi(v) {
    var t = llave(v);
    return t === '' || t === '1' || t === 'si' || t === 'true' || t === 'ok';
  }

  /* ---------- lectura ---------- */

  function guardarCampo(datos, clave, valor) {
    var campo = ALIAS[llave(clave)];
    if (!campo) return false;
    valor = String(valor == null ? '' : valor).trim();
    if (campo !== 'crear' && !valor) return true;
    /* Una clave repetida (varias «nota:») se acumula en vez de pisarse. */
    if (campo === 'descripcion' && datos.descripcion) datos.descripcion += '\n' + valor;
    else datos[campo] = campo === 'crear' ? esSi(valor) : valor;
    return true;
  }

  function deObjeto(obj) {
    var datos = {};
    Object.keys(obj || {}).forEach(function (k) { guardarCampo(datos, k, obj[k]); });
    return datos;
  }

  function deConsulta(cadena, datos) {
    datos = datos || {};
    String(cadena || '').split('&').filter(Boolean).forEach(function (kv) {
      var i = kv.indexOf('=');
      var k = i === -1 ? kv : kv.slice(0, i);
      var v = i === -1 ? '' : kv.slice(i + 1);
      try { k = decodeURIComponent(k); } catch (e) {}
      try { v = decodeURIComponent(v.replace(/\+/g, ' ')); } catch (e) {}
      guardarCampo(datos, k, v);
    });
    return datos;
  }

  /* Un texto pegado: puede ser un enlace de la app, líneas «clave: valor», o
     texto normal del que se saca el título. */
  function deTexto(texto) {
    var t = String(texto || '').trim();
    if (!t) return null;

    /* Un enlace de la app: lo que interesa es lo que va tras la «?». Si no
       lleva datos no es un aviso, solo la dirección de la app. */
    if (/^https?:\/\//i.test(t) || t.charAt(0) === '#') {
      if (/\s/.test(t)) return null;
      var corte = t.indexOf('#');
      var tras = corte === -1 ? t : t.slice(corte + 1);
      var inte = tras.indexOf('?');
      return inte === -1 ? null : normalizar(deConsulta(tras.slice(inte + 1)));
    }

    var datos = {};
    var sueltas = [];
    var reconocidas = 0;
    t.split(/\r?\n/).forEach(function (linea) {
      var m = /^\s*([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]{1,14})\s*[:=]\s*(.*)$/.exec(linea);
      if (m && ALIAS[llave(m[1])]) {
        if (guardarCampo(datos, m[1], m[2])) { reconocidas++; return; }
      }
      if (linea.trim()) sueltas.push(linea.trim());
    });

    if (!reconocidas) {
      /* Texto corriente: primera línea de título, lo demás descripción. */
      datos.titulo = sueltas.shift() || '';
      if (sueltas.length) datos.descripcion = sueltas.join('\n');
    } else if (sueltas.length) {
      if (!datos.titulo) datos.titulo = sueltas.shift();
      if (sueltas.length) {
        datos.descripcion = (datos.descripcion ? datos.descripcion + '\n' : '') + sueltas.join('\n');
      }
    }

    if (!datos.titulo && !datos.descripcion) return null;
    return normalizar(datos);
  }

  /* Deja los valores como los quiere el modelo: fechas ISO, identificadores
     de catálogo, id de técnico. Lo que no se entiende se descarta en silencio,
     que es mejor que crear un aviso con basura dentro. */
  function normalizar(bruto) {
    var d = {};
    CAMPOS.forEach(function (k) { if (bruto[k] != null) d[k] = bruto[k]; });

    if (d.titulo) d.titulo = String(d.titulo).replace(/\s+/g, ' ').trim().slice(0, 200);
    if (d.fecha != null) d.fecha = aFecha(d.fecha);
    if (d.hora != null) d.hora = aHora(d.hora);
    if (d.prioridad != null) d.prioridad = deCatalogo(S.PRIORIDADES, d.prioridad);
    if (d.tipo != null) d.tipo = deCatalogo(S.TIPOS, d.tipo, VOCES_TIPO);
    if (d.sistema != null) d.sistema = deCatalogo(S.SISTEMAS, d.sistema, VOCES_SISTEMA);
    if (d.estado != null) d.estado = deCatalogo(S.ESTADOS, d.estado);
    if (d.tecnico != null) d.tecnico = aTecnico(d.tecnico);
    if (d.duracion != null) d.duracion = String(d.duracion).trim().replace('.', ',');
    if (d.telefono != null) d.telefono = String(d.telefono).replace(/[^\d+]/g, '');

    Object.keys(d).forEach(function (k) { if (d[k] === '' || d[k] == null) delete d[k]; });
    deducirLoQueFalte(d);
    return d;
  }

  /* Lo que no se ha dicho se saca del propio texto, con las mismas reglas que
     usan los correos: «cámara» es CCTV, «no funciona» es avería, «urgente» es
     urgente. Si no se reconoce nada se deja el valor por defecto del aviso. */
  function deducirLoQueFalte(d) {
    if (!global.Correo) return;
    var texto = [d.titulo, d.descripcion].filter(Boolean).join(' ');
    if (!texto) return;
    if (!d.tipo) { var t = Correo.deducirTipo(texto); if (t) d.tipo = t; }
    if (!d.sistema) { var s = Correo.deducirSistema(texto); if (s) d.sistema = s; }
    if (!d.prioridad) { var p = Correo.deducirPrioridad(texto); if (p) d.prioridad = p; }
    if (!d.telefono) { var f = Correo.buscarTelefono(texto); if (f) d.telefono = f; }
  }

  /* ---------- escritura ---------- */

  /* Vuelca los datos sobre un aviso vacío, listo para guardar o para pintar
     en el formulario. */
  function aAviso(datos) {
    var a = S.nuevoAvisoVacio();
    if (datos.titulo) a.titulo = datos.titulo;
    if (datos.descripcion) a.descripcion = datos.descripcion;
    if (datos.tipo) a.tipo = datos.tipo;
    if (datos.sistema) a.sistema = datos.sistema;
    if (datos.prioridad) a.prioridad = datos.prioridad;
    if (datos.estado) a.estado = datos.estado;
    if (datos.tecnico) a.asignadoA = datos.tecnico;
    if (datos.duracion) a.duracion = datos.duracion;
    if (datos.hora) a.hora = datos.hora;
    if (datos.fecha) a.fecha = datos.fecha;
    a.cliente = {
      nombre: datos.cliente || '',
      direccion: datos.direccion || '',
      telefono: datos.telefono || '',
      contacto: datos.contacto || ''
    };
    a.origen = 'atajo';
    return a;
  }

  /* El enlace que se pega dentro del atajo del iPhone. */
  function enlace(datos, base) {
    var raiz = base || (location.origin + location.pathname.replace(/index\.html$/, ''));
    var trozos = [];
    Object.keys(datos || {}).forEach(function (k) {
      if (datos[k] === '' || datos[k] == null) return;
      trozos.push(encodeURIComponent(k) + '=' + encodeURIComponent(datos[k]));
    });
    return raiz + '#/nuevo' + (trozos.length ? '?' + trozos.join('&') : '');
  }

  /* Lee el portapapeles. Safari exige un gesto del usuario y enseña su propio
     botón de «Pegar»; si lo deniegan o el navegador no lo trae, quien llama
     ofrece pegarlo a mano. */
  function delPortapapeles() {
    if (!global.navigator || !navigator.clipboard || !navigator.clipboard.readText) {
      return Promise.reject(new Error('sin-portapapeles'));
    }
    return navigator.clipboard.readText();
  }

  global.Atajos = {
    deTexto: deTexto,
    deConsulta: function (c) { return normalizar(deConsulta(c)); },
    deObjeto: function (o) { return normalizar(deObjeto(o)); },
    aAviso: aAviso,
    enlace: enlace,
    delPortapapeles: delPortapapeles,
    aFecha: aFecha, aHora: aHora
  };
})(window);
