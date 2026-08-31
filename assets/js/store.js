/* store.js — modelo de datos y reglas de negocio de los avisos. */
(function (global) {
  'use strict';

  var ESTADOS = [
    { id: 'pendiente',  label: 'Pendiente',  corto: 'Pend.',   abierto: true  },
    { id: 'programado', label: 'Programado', corto: 'Prog.',   abierto: true  },
    { id: 'en_curso',   label: 'En curso',   corto: 'Curso',   abierto: true  },
    { id: 'en_espera',  label: 'En espera',  corto: 'Espera',  abierto: true  },
    { id: 'resuelto',   label: 'Resuelto',   corto: 'Hecho',   abierto: false },
    { id: 'cancelado',  label: 'Cancelado',  corto: 'Canc.',   abierto: false }
  ];

  var PRIORIDADES = [
    { id: 'baja',    label: 'Baja',    peso: 0 },
    { id: 'normal',  label: 'Normal',  peso: 1 },
    { id: 'alta',    label: 'Alta',    peso: 2 },
    { id: 'urgente', label: 'Urgente', peso: 3 }
  ];

  var TIPOS = [
    { id: 'averia',        label: 'Avería' },
    { id: 'instalacion',   label: 'Instalación' },
    { id: 'mantenimiento', label: 'Mantenimiento' },
    { id: 'revision',      label: 'Revisión' },
    { id: 'presupuesto',   label: 'Presupuesto' }
  ];

  var SISTEMAS = [
    { id: 'alarma',    label: 'Alarma' },
    { id: 'cctv',      label: 'CCTV' },
    { id: 'accesos',   label: 'Control de accesos' },
    { id: 'incendios', label: 'Incendios' },
    { id: 'interfono', label: 'Portero / interfono' },
    { id: 'otros',     label: 'Otros' }
  ];

  var COLORES = ['#c2410c', '#0e7490', '#15803d', '#6d28d9', '#b45309', '#be123c', '#0f766e', '#4338ca'];

  var state = {
    avisos: [],
    tecnicos: [],
    ajustes: { tema: 'auto', prefijoRef: 'AV', contadorRef: 0, verCerrados: false, recordatorio: 30, pistaGestos: true }
  };

  /* ---------- utilidades ---------- */

  function uid(prefix) {
    var rnd = (global.crypto && crypto.getRandomValues)
      ? Array.from(crypto.getRandomValues(new Uint8Array(6))).map(function (b) {
          return b.toString(16).padStart(2, '0');
        }).join('')
      : Math.random().toString(16).slice(2, 14);
    return (prefix || 'id') + '_' + Date.now().toString(36) + rnd;
  }

  function hoyISO(d) {
    var t = d ? new Date(d) : new Date();
    return [t.getFullYear(), String(t.getMonth() + 1).padStart(2, '0'), String(t.getDate()).padStart(2, '0')].join('-');
  }

  function sumaDias(iso, n) {
    var p = String(iso).split('-');
    var d = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
    d.setDate(d.getDate() + n);
    return hoyISO(d);
  }

  function byId(list, id) {
    for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
    return null;
  }

  function catalogo(list, id) {
    return byId(list, id) || { id: id, label: id || '—' };
  }

  /* ---------- carga inicial ---------- */

  function load() {
    return Promise.all([
      DB.getAll('avisos'),
      DB.getAll('tecnicos'),
      DB.get('ajustes', 'app')
    ]).then(function (r) {
      state.avisos = r[0] || [];
      state.tecnicos = r[1] || [];
      if (r[2] && r[2].v) Object.assign(state.ajustes, r[2].v);
      state.ajustes.contadorRef = Math.max(state.ajustes.contadorRef || 0, maxRef());
      return state;
    });
  }

  function maxRef() {
    var max = 0;
    state.avisos.forEach(function (a) {
      var m = /(\d+)\s*$/.exec(a.ref || '');
      if (m) max = Math.max(max, Number(m[1]));
    });
    return max;
  }

  function guardarAjustes(patch) {
    Object.assign(state.ajustes, patch || {});
    return DB.put('ajustes', { k: 'app', v: state.ajustes }).then(function () { return state.ajustes; });
  }

  /* ---------- avisos ---------- */

  function nuevoAvisoVacio() {
    return {
      id: uid('av'),
      ref: '',
      titulo: '',
      tipo: 'averia',
      sistema: 'alarma',
      prioridad: 'normal',
      estado: 'pendiente',
      cliente: { nombre: '', direccion: '', telefono: '', contacto: '' },
      fecha: hoyISO(),
      hora: '',
      duracion: '',
      asignadoA: '',
      descripcion: '',
      notas: [],
      materiales: [],
      horas: [],
      creado: new Date().toISOString(),
      actualizado: new Date().toISOString(),
      cerrado: ''
    };
  }

  function siguienteRef() {
    var n = (state.ajustes.contadorRef || 0) + 1;
    return (state.ajustes.prefijoRef || 'AV') + '-' + String(n).padStart(4, '0');
  }

  function guardarAviso(aviso) {
    var esNuevo = !byId(state.avisos, aviso.id);
    aviso.actualizado = new Date().toISOString();
    if (!aviso.ref) {
      aviso.ref = siguienteRef();
      state.ajustes.contadorRef = (state.ajustes.contadorRef || 0) + 1;
    }
    var est = catalogo(ESTADOS, aviso.estado);
    if (est.abierto === false && !aviso.cerrado) aviso.cerrado = new Date().toISOString();
    if (est.abierto === true) aviso.cerrado = '';

    return DB.put('avisos', aviso).then(function () {
      return guardarAjustes({});
    }).then(function () {
      if (esNuevo) state.avisos.push(aviso);
      else state.avisos = state.avisos.map(function (a) { return a.id === aviso.id ? aviso : a; });
      return aviso;
    });
  }

  function borrarAviso(id) {
    return DB.del('avisos', id)
      .then(function () { return DB.delByIndex('fotos', 'avisoId', id); })
      .then(function () {
        state.avisos = state.avisos.filter(function (a) { return a.id !== id; });
      });
  }

  function duplicarAviso(id) {
    var src = byId(state.avisos, id);
    if (!src) return Promise.reject(new Error('Aviso no encontrado'));
    var copia = JSON.parse(JSON.stringify(src));
    copia.id = uid('av');
    copia.ref = '';
    copia.estado = 'pendiente';
    copia.notas = [];
    copia.materiales = [];
    copia.horas = [];
    copia.cerrado = '';
    copia.fecha = hoyISO();
    copia.creado = new Date().toISOString();
    copia.titulo = src.titulo + ' (copia)';
    return guardarAviso(copia);
  }

  function addNota(id, texto) {
    var a = byId(state.avisos, id);
    if (!a || !texto.trim()) return Promise.resolve(a);
    a.notas = a.notas || [];
    a.notas.unshift({ id: uid('n'), ts: new Date().toISOString(), texto: texto.trim() });
    return guardarAviso(a);
  }

  function delNota(id, notaId) {
    var a = byId(state.avisos, id);
    a.notas = (a.notas || []).filter(function (n) { return n.id !== notaId; });
    return guardarAviso(a);
  }

  function addMaterial(id, desc, cantidad) {
    var a = byId(state.avisos, id);
    if (!a || !desc.trim()) return Promise.resolve(a);
    a.materiales = a.materiales || [];
    a.materiales.push({ id: uid('m'), desc: desc.trim(), cantidad: Number(cantidad) || 1 });
    return guardarAviso(a);
  }

  function delMaterial(id, matId) {
    var a = byId(state.avisos, id);
    a.materiales = (a.materiales || []).filter(function (m) { return m.id !== matId; });
    return guardarAviso(a);
  }

  function addHoras(id, fecha, horas, tecnicoId) {
    var a = byId(state.avisos, id);
    var n = Number(String(horas).replace(',', '.'));
    if (!a || !isFinite(n) || n <= 0) return Promise.resolve(a);
    a.horas = a.horas || [];
    a.horas.push({ id: uid('h'), fecha: fecha || hoyISO(), horas: n, tecnicoId: tecnicoId || a.asignadoA || '' });
    return guardarAviso(a);
  }

  function delHoras(id, hId) {
    var a = byId(state.avisos, id);
    a.horas = (a.horas || []).filter(function (h) { return h.id !== hId; });
    return guardarAviso(a);
  }

  function totalHoras(a) {
    return (a.horas || []).reduce(function (s, h) { return s + (Number(h.horas) || 0); }, 0);
  }

  /* Al exportar al calendario se sube SEQUENCE de cada aviso: así, al volver
     a importar, el calendario actualiza el evento en vez de duplicarlo.
     No toca «actualizado»: exportar no es modificar el aviso. */
  function marcarExportados(lista) {
    var tocados = (lista || []).map(function (a) {
      a.icsSeq = (Number(a.icsSeq) || 0) + 1;
      return a;
    });
    if (!tocados.length) return Promise.resolve(0);
    return DB.putMany('avisos', tocados).then(function () { return tocados.length; });
  }

  /* ---------- fotos ---------- */

  function comprimirImagen(file, maxLado, calidad) {
    return new Promise(function (resolve, reject) {
      var url = URL.createObjectURL(file);
      var img = new Image();
      img.onload = function () {
        var escala = Math.min(1, maxLado / Math.max(img.width, img.height));
        var w = Math.round(img.width * escala);
        var h = Math.round(img.height * escala);
        var c = document.createElement('canvas');
        c.width = w; c.height = h;
        c.getContext('2d').drawImage(img, 0, 0, w, h);
        URL.revokeObjectURL(url);
        c.toBlob(function (blob) {
          resolve(blob || file);
        }, 'image/jpeg', calidad);
      };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('No se pudo leer la imagen')); };
      img.src = url;
    });
  }

  function addFoto(avisoId, file) {
    return comprimirImagen(file, 1400, 0.72).then(function (blob) {
      var reg = { id: uid('f'), avisoId: avisoId, blob: blob, ts: new Date().toISOString(), nombre: file.name || 'foto.jpg' };
      return DB.put('fotos', reg).then(function () { return reg; });
    });
  }

  function fotosDe(avisoId) { return DB.getByIndex('fotos', 'avisoId', avisoId); }
  function delFoto(fotoId) { return DB.del('fotos', fotoId); }

  /* ---------- técnicos ---------- */

  function guardarTecnico(t) {
    var esNuevo = !t.id;
    if (esNuevo) {
      t.id = uid('tc');
      t.color = t.color || COLORES[state.tecnicos.length % COLORES.length];
    }
    return DB.put('tecnicos', t).then(function () {
      if (esNuevo) state.tecnicos.push(t);
      else state.tecnicos = state.tecnicos.map(function (x) { return x.id === t.id ? t : x; });
      return t;
    });
  }

  function borrarTecnico(id) {
    return DB.del('tecnicos', id).then(function () {
      state.tecnicos = state.tecnicos.filter(function (t) { return t.id !== id; });
      var afectados = state.avisos.filter(function (a) { return a.asignadoA === id; });
      afectados.forEach(function (a) { a.asignadoA = ''; });
      return afectados.length ? DB.putMany('avisos', afectados) : null;
    });
  }

  function tecnico(id) { return byId(state.tecnicos, id); }

  function iniciales(nombre) {
    var p = String(nombre || '').trim().split(/\s+/).filter(Boolean);
    if (!p.length) return '?';
    return (p[0][0] + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase();
  }

  function cargaPorTecnico(id) {
    return state.avisos.filter(function (a) {
      return a.asignadoA === id && catalogo(ESTADOS, a.estado).abierto;
    }).length;
  }

  /* ---------- consultas ---------- */

  function abierto(a) { return catalogo(ESTADOS, a.estado).abierto === true; }

  function vencido(a) {
    return abierto(a) && !!a.fecha && a.fecha < hoyISO();
  }

  function ordenar(lista, modo) {
    var copia = lista.slice();
    if (modo === 'prioridad') {
      copia.sort(function (x, y) {
        var d = catalogo(PRIORIDADES, y.prioridad).peso - catalogo(PRIORIDADES, x.prioridad).peso;
        if (d) return d;
        return cmpFecha(x, y);
      });
    } else if (modo === 'reciente') {
      copia.sort(function (x, y) { return String(y.creado).localeCompare(String(x.creado)); });
    } else {
      copia.sort(cmpFecha);
    }
    return copia;
  }

  function cmpFecha(x, y) {
    var a = (x.fecha || '9999-12-31') + ' ' + (x.hora || '99:99');
    var b = (y.fecha || '9999-12-31') + ' ' + (y.hora || '99:99');
    if (a === b) return catalogo(PRIORIDADES, y.prioridad).peso - catalogo(PRIORIDADES, x.prioridad).peso;
    return a < b ? -1 : 1;
  }

  function filtrar(f) {
    f = f || {};
    var q = String(f.texto || '').trim().toLowerCase();
    return state.avisos.filter(function (a) {
      if (f.estado && a.estado !== f.estado) return false;
      if (f.soloAbiertos && !abierto(a)) return false;
      if (f.prioridad && a.prioridad !== f.prioridad) return false;
      if (f.tipo && a.tipo !== f.tipo) return false;
      if (f.sistema && a.sistema !== f.sistema) return false;
      if (f.tecnico === '__sin__' && a.asignadoA) return false;
      if (f.tecnico && f.tecnico !== '__sin__' && a.asignadoA !== f.tecnico) return false;
      if (f.desde && (!a.fecha || a.fecha < f.desde)) return false;
      if (f.hasta && (!a.fecha || a.fecha > f.hasta)) return false;
      if (f.vencidos && !vencido(a)) return false;
      if (q) {
        var t = tecnico(a.asignadoA);
        var heno = [a.ref, a.titulo, a.descripcion, a.cliente && a.cliente.nombre,
          a.cliente && a.cliente.direccion, a.cliente && a.cliente.telefono,
          a.cliente && a.cliente.contacto, t && t.nombre,
          catalogo(TIPOS, a.tipo).label, catalogo(SISTEMAS, a.sistema).label]
          .join(' ').toLowerCase();
        if (heno.indexOf(q) === -1) return false;
      }
      return true;
    });
  }

  function resumen() {
    var hoy = hoyISO();
    var semana = sumaDias(hoy, 7);
    var abiertos = state.avisos.filter(abierto);
    return {
      abiertos: abiertos.length,
      vencidos: abiertos.filter(function (a) { return a.fecha && a.fecha < hoy; }).length,
      hoy: abiertos.filter(function (a) { return a.fecha === hoy; }).length,
      semana: abiertos.filter(function (a) { return a.fecha && a.fecha > hoy && a.fecha <= semana; }).length,
      sinFecha: abiertos.filter(function (a) { return !a.fecha; }).length,
      sinAsignar: abiertos.filter(function (a) { return !a.asignadoA; }).length,
      urgentes: abiertos.filter(function (a) { return a.prioridad === 'urgente'; }).length,
      resueltos: state.avisos.filter(function (a) { return a.estado === 'resuelto'; }).length,
      total: state.avisos.length
    };
  }

  /* ---------- copia de seguridad ---------- */

  function blobADataURL(blob) {
    return new Promise(function (res, rej) {
      var fr = new FileReader();
      fr.onload = function () { res(fr.result); };
      fr.onerror = function () { rej(fr.error); };
      fr.readAsDataURL(blob);
    });
  }

  function dataURLABlob(dataURL) {
    var partes = String(dataURL).split(',');
    var mime = (/:(.*?);/.exec(partes[0]) || [])[1] || 'image/jpeg';
    var bin = atob(partes[1]);
    var arr = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    return new Blob([arr], { type: mime });
  }

  function exportar(conFotos) {
    var base = {
      formato: 'avisos-backup',
      version: 1,
      exportado: new Date().toISOString(),
      ajustes: state.ajustes,
      tecnicos: state.tecnicos,
      avisos: state.avisos,
      fotos: []
    };
    if (!conFotos) return Promise.resolve(base);
    return DB.getAll('fotos').then(function (fotos) {
      return Promise.all(fotos.map(function (f) {
        return blobADataURL(f.blob).then(function (d) {
          return { id: f.id, avisoId: f.avisoId, ts: f.ts, nombre: f.nombre, data: d };
        });
      }));
    }).then(function (fs) { base.fotos = fs; return base; });
  }

  function importar(datos, modo) {
    if (!datos || datos.formato !== 'avisos-backup') {
      return Promise.reject(new Error('El archivo no es una copia de seguridad válida'));
    }
    var paso = (modo === 'reemplazar') ? DB.clearAll() : Promise.resolve();
    return paso.then(function () {
      if (modo === 'reemplazar') { state.avisos = []; state.tecnicos = []; }
      var tecnicos = datos.tecnicos || [];
      var avisos = datos.avisos || [];
      if (modo !== 'reemplazar') {
        var idsT = state.tecnicos.map(function (t) { return t.id; });
        var idsA = state.avisos.map(function (a) { return a.id; });
        tecnicos = tecnicos.filter(function (t) { return idsT.indexOf(t.id) === -1; });
        avisos = avisos.filter(function (a) { return idsA.indexOf(a.id) === -1; });
      }
      var fotos = (datos.fotos || []).map(function (f) {
        return { id: f.id, avisoId: f.avisoId, ts: f.ts, nombre: f.nombre, blob: dataURLABlob(f.data) };
      });
      return Promise.all([
        tecnicos.length ? DB.putMany('tecnicos', tecnicos) : null,
        avisos.length ? DB.putMany('avisos', avisos) : null,
        fotos.length ? DB.putMany('fotos', fotos) : null
      ]).then(function () {
        state.tecnicos = state.tecnicos.concat(tecnicos);
        state.avisos = state.avisos.concat(avisos);
        state.ajustes.contadorRef = Math.max(state.ajustes.contadorRef || 0, maxRef());
        return guardarAjustes({});
      }).then(function () {
        return { avisos: avisos.length, tecnicos: tecnicos.length, fotos: fotos.length };
      });
    });
  }

  function datosDeEjemplo() {
    var equipo = [
      { id: uid('tc'), nombre: 'Yo', telefono: '', color: COLORES[0] },
      { id: uid('tc'), nombre: 'Marcos Rey', telefono: '600112233', color: COLORES[1] },
      { id: uid('tc'), nombre: 'Laura Pinto', telefono: '600445566', color: COLORES[2] }
    ];
    var hoy = hoyISO();
    var plantillas = [
      { titulo: 'Central de alarma en fallo de comunicación', tipo: 'averia', sistema: 'alarma', prioridad: 'urgente',
        cliente: { nombre: 'Farmacia Centro', direccion: 'C/ Mayor 14, Bajo', telefono: '911234567', contacto: 'Ana' },
        fecha: sumaDias(hoy, -1), hora: '09:30', estado: 'pendiente', asignadoA: equipo[0].id,
        descripcion: 'La central pierde conexión con la CRA por la noche. Revisar módulo GPRS y cableado.' },
      { titulo: 'Sustitución de 2 cámaras exteriores', tipo: 'instalacion', sistema: 'cctv', prioridad: 'alta',
        cliente: { nombre: 'Naves Logísticas Sur', direccion: 'Pol. Ind. El Prado, nave 7', telefono: '915556677', contacto: 'Jefe de planta' },
        fecha: hoy, hora: '11:00', estado: 'programado', asignadoA: equipo[1].id,
        descripcion: 'Cambio de dos domos por bullet 4MP. Llevar escalera y latiguillos.' },
      { titulo: 'Mantenimiento anual del sistema de incendios', tipo: 'mantenimiento', sistema: 'incendios', prioridad: 'normal',
        cliente: { nombre: 'Colegio San Pablo', direccion: 'Av. de la Paz 3', telefono: '913334455', contacto: 'Conserjería' },
        fecha: hoy, hora: '16:00', estado: 'en_curso', asignadoA: equipo[0].id,
        descripcion: 'Revisión de detectores y pulsadores. Firmar parte al terminar.' },
      { titulo: 'Lector de acceso no lee tarjetas', tipo: 'averia', sistema: 'accesos', prioridad: 'alta',
        cliente: { nombre: 'Gimnasio Atlas', direccion: 'C/ Deportes 22', telefono: '917778899', contacto: 'Recepción' },
        fecha: sumaDias(hoy, 1), hora: '08:00', estado: 'programado', asignadoA: equipo[2].id,
        descripcion: 'Lector de la puerta principal intermitente. Posible fuente o cableado Wiegand.' },
      { titulo: 'Presupuesto de videoportero para 12 viviendas', tipo: 'presupuesto', sistema: 'interfono', prioridad: 'baja',
        cliente: { nombre: 'Comunidad C/ Olmo 8', direccion: 'C/ Olmo 8', telefono: '600998877', contacto: 'Presidente' },
        fecha: sumaDias(hoy, 4), hora: '', estado: 'pendiente', asignadoA: '',
        descripcion: 'Tomar medidas del portal y pasar oferta con dos opciones.' },
      { titulo: 'Ampliación de 4 detectores volumétricos', tipo: 'instalacion', sistema: 'alarma', prioridad: 'normal',
        cliente: { nombre: 'Joyería Duarte', direccion: 'Plaza del Sol 1', telefono: '912223344', contacto: 'Sr. Duarte' },
        fecha: sumaDias(hoy, -6), hora: '10:00', estado: 'resuelto', asignadoA: equipo[1].id,
        descripcion: 'Ampliación realizada y probada con la CRA.' }
    ];

    var contador = 0;
    var avisos = plantillas.map(function (p) {
      var a = nuevoAvisoVacio();
      Object.assign(a, p);
      a.id = uid('av');
      a.ref = 'AV-' + String(++contador).padStart(4, '0');
      if (a.estado === 'resuelto') {
        a.cerrado = new Date().toISOString();
        a.horas = [{ id: uid('h'), fecha: a.fecha, horas: 3.5, tecnicoId: a.asignadoA }];
        a.materiales = [{ id: uid('m'), desc: 'Detector PIR grado 2', cantidad: 4 }];
        a.notas = [{ id: uid('n'), ts: new Date().toISOString(), texto: 'Trabajo terminado y firmado por el cliente.' }];
      }
      if (a.estado === 'en_curso') {
        a.notas = [{ id: uid('n'), ts: new Date().toISOString(), texto: 'Revisada la primera planta, faltan sótano y azotea.' }];
      }
      return a;
    });

    return DB.putMany('tecnicos', equipo)
      .then(function () { return DB.putMany('avisos', avisos); })
      .then(function () {
        state.tecnicos = state.tecnicos.concat(equipo);
        state.avisos = state.avisos.concat(avisos);
        return guardarAjustes({ contadorRef: Math.max(state.ajustes.contadorRef || 0, contador) });
      });
  }

  global.Store = {
    ESTADOS: ESTADOS, PRIORIDADES: PRIORIDADES, TIPOS: TIPOS, SISTEMAS: SISTEMAS, COLORES: COLORES,
    state: state,
    load: load, guardarAjustes: guardarAjustes,
    uid: uid, hoyISO: hoyISO, sumaDias: sumaDias, byId: byId, catalogo: catalogo,
    nuevoAvisoVacio: nuevoAvisoVacio, siguienteRef: siguienteRef,
    guardarAviso: guardarAviso, borrarAviso: borrarAviso, duplicarAviso: duplicarAviso,
    addNota: addNota, delNota: delNota,
    addMaterial: addMaterial, delMaterial: delMaterial,
    addHoras: addHoras, delHoras: delHoras, totalHoras: totalHoras,
    addFoto: addFoto, fotosDe: fotosDe, delFoto: delFoto,
    marcarExportados: marcarExportados,
    guardarTecnico: guardarTecnico, borrarTecnico: borrarTecnico, tecnico: tecnico,
    iniciales: iniciales, cargaPorTecnico: cargaPorTecnico,
    abierto: abierto, vencido: vencido, filtrar: filtrar, ordenar: ordenar, resumen: resumen,
    exportar: exportar, importar: importar, datosDeEjemplo: datosDeEjemplo
  };
})(window);
