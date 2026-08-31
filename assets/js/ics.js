/* ics.js — genera archivos iCalendar (.ics) para importar los avisos
   al calendario nativo del móvil (Calendario de iOS, Google Calendar, etc.). */
(function (global) {
  'use strict';

  var S = global.Store;

  /* ---------- texto ---------- */

  function escapar(t) {
    return String(t == null ? '' : t)
      .replace(/\\/g, '\\\\')
      .replace(/;/g, '\\;')
      .replace(/,/g, '\\,')
      .replace(/\r?\n/g, '\\n');
  }

  /* El estándar limita cada línea a 75 octetos: se parte y se continúa
     con un espacio al principio, sin cortar caracteres multibyte. */
  function plegar(linea) {
    var cod = new TextEncoder();
    if (cod.encode(linea).length <= 75) return linea;
    var out = [];
    var actual = '';
    var bytes = 0;
    var limite = 75;
    for (var i = 0; i < linea.length; i++) {
      var ch = linea[i];
      var cp = linea.codePointAt(i);
      if (cp > 0xffff) { ch = linea.slice(i, i + 2); i++; }
      var n = cod.encode(ch).length;
      if (bytes + n > limite) {
        out.push(actual);
        actual = ' ' + ch;
        bytes = 1 + n;
        limite = 75;
      } else {
        actual += ch;
        bytes += n;
      }
    }
    out.push(actual);
    return out.join('\r\n');
  }

  /* ---------- fechas ---------- */

  function dosDig(n) { return String(n).padStart(2, '0'); }

  /* Hora local «flotante»: el calendario la muestra tal cual, sin husos. */
  function local(iso, hora) {
    var f = String(iso).replace(/-/g, '');
    var h = (hora || '00:00').replace(':', '') + '00';
    return f + 'T' + h;
  }

  function soloFecha(iso) { return String(iso).replace(/-/g, ''); }

  function utc(d) {
    return d.getUTCFullYear() + dosDig(d.getUTCMonth() + 1) + dosDig(d.getUTCDate()) + 'T' +
      dosDig(d.getUTCHours()) + dosDig(d.getUTCMinutes()) + dosDig(d.getUTCSeconds()) + 'Z';
  }

  function masMinutos(iso, hora, minutos) {
    var p = String(iso).split('-');
    var hm = (hora || '00:00').split(':');
    var d = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]), Number(hm[0]), Number(hm[1]));
    d.setMinutes(d.getMinutes() + minutos);
    return local(
      d.getFullYear() + '-' + dosDig(d.getMonth() + 1) + '-' + dosDig(d.getDate()),
      dosDig(d.getHours()) + ':' + dosDig(d.getMinutes())
    );
  }

  /* ---------- contenido del evento ---------- */

  function duracionMinutos(a) {
    var n = Number(String(a.duracion || '').replace(',', '.'));
    if (isFinite(n) && n > 0) return Math.round(n * 60);
    return 60;
  }

  function titulo(a) {
    return (a.ref ? a.ref + ' · ' : '') + (a.titulo || 'Aviso');
  }

  function descripcion(a) {
    var c = a.cliente || {};
    var t = S.tecnico(a.asignadoA);
    var lineas = [
      'Estado: ' + S.catalogo(S.ESTADOS, a.estado).label +
        ' · Prioridad: ' + S.catalogo(S.PRIORIDADES, a.prioridad).label,
      'Trabajo: ' + S.catalogo(S.TIPOS, a.tipo).label + ' / ' + S.catalogo(S.SISTEMAS, a.sistema).label,
      'Técnico: ' + (t ? t.nombre : 'sin asignar')
    ];
    if (c.nombre) lineas.push('Cliente: ' + c.nombre);
    if (c.contacto) lineas.push('Contacto: ' + c.contacto);
    if (c.telefono) lineas.push('Teléfono: ' + c.telefono);
    if (a.descripcion) lineas.push('', a.descripcion);
    return lineas.join('\n');
  }

  function ubicacion(a) {
    var c = a.cliente || {};
    return [c.nombre, c.direccion].filter(Boolean).join(', ');
  }

  function estadoICS(estado) {
    if (estado === 'cancelado') return 'CANCELLED';
    if (estado === 'programado' || estado === 'en_curso' || estado === 'resuelto') return 'CONFIRMED';
    return 'TENTATIVE';
  }

  /* 90 → PT1H30M, 1440 → P1D: formas más habituales y legibles */
  function duracionISO(min) {
    if (min % 1440 === 0) return 'P' + (min / 1440) + 'D';
    var h = Math.floor(min / 60), m = min % 60;
    return 'PT' + (h ? h + 'H' : '') + (m ? m + 'M' : '');
  }

  function evento(a, opts) {
    opts = opts || {};
    var ahora = utc(new Date());
    var L = [];
    L.push('BEGIN:VEVENT');
    L.push('UID:' + a.id + '@avisos.app');
    L.push('DTSTAMP:' + ahora);
    L.push('SEQUENCE:' + (Number(a.icsSeq) || 0));

    if (a.hora) {
      L.push('DTSTART:' + local(a.fecha, a.hora));
      L.push('DTEND:' + masMinutos(a.fecha, a.hora, duracionMinutos(a)));
    } else {
      /* sin hora: evento de día completo (DTEND es exclusivo) */
      L.push('DTSTART;VALUE=DATE:' + soloFecha(a.fecha));
      L.push('DTEND;VALUE=DATE:' + soloFecha(S.sumaDias(a.fecha, 1)));
    }

    L.push('SUMMARY:' + escapar(titulo(a)));
    var loc = ubicacion(a);
    if (loc) L.push('LOCATION:' + escapar(loc));
    L.push('DESCRIPTION:' + escapar(descripcion(a)));
    L.push('STATUS:' + estadoICS(a.estado));
    L.push('CATEGORIES:' + escapar(S.catalogo(S.TIPOS, a.tipo).label));
    if (a.prioridad === 'urgente') L.push('PRIORITY:1');
    else if (a.prioridad === 'alta') L.push('PRIORITY:3');
    else if (a.prioridad === 'baja') L.push('PRIORITY:7');
    else L.push('PRIORITY:5');

    var rec = Number(opts.recordatorio);
    if (isFinite(rec) && rec > 0) {
      L.push('BEGIN:VALARM');
      L.push('ACTION:DISPLAY');
      L.push('TRIGGER:-' + duracionISO(rec));
      L.push('DESCRIPTION:' + escapar(titulo(a)));
      L.push('END:VALARM');
    }

    L.push('END:VEVENT');
    return L;
  }

  function calendario(avisos, opts) {
    opts = opts || {};
    var L = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'PRODID:-//Avisos//Gestion de avisos//ES',
      'CALSCALE:GREGORIAN',
      'METHOD:PUBLISH',
      'X-WR-CALNAME:' + escapar(opts.nombre || 'Avisos')
    ];
    avisos.forEach(function (a) { L = L.concat(evento(a, opts)); });
    L.push('END:VCALENDAR');
    return L.map(plegar).join('\r\n') + '\r\n';
  }

  function exportables(lista) {
    return (lista || []).filter(function (a) { return !!a.fecha; });
  }

  function nombreArchivo(a) {
    var base = a ? (a.ref || 'aviso') : ('avisos-' + S.hoyISO());
    return String(base).replace(/[^\w.-]+/g, '-').toLowerCase() + '.ics';
  }

  /* Enlace directo de Google Calendar, útil en Android sin descargar nada. */
  function enlaceGoogle(a) {
    var fechas = a.hora
      ? local(a.fecha, a.hora) + '/' + masMinutos(a.fecha, a.hora, duracionMinutos(a))
      : soloFecha(a.fecha) + '/' + soloFecha(S.sumaDias(a.fecha, 1));
    var q = [
      'action=TEMPLATE',
      'text=' + encodeURIComponent(titulo(a)),
      'dates=' + fechas,
      'details=' + encodeURIComponent(descripcion(a)),
      'location=' + encodeURIComponent(ubicacion(a))
    ];
    return 'https://calendar.google.com/calendar/render?' + q.join('&');
  }

  global.ICS = {
    calendario: calendario,
    evento: evento,
    exportables: exportables,
    nombreArchivo: nombreArchivo,
    enlaceGoogle: enlaceGoogle
  };
})(window);
