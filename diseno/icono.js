/* Icono nuevo: propuestas, vista en contexto y recursos de Android.
   node icono.js <salida> */
'use strict';
var fs = require('fs'), path = require('path');
var chromium = require('playwright').chromium;
var DIR = __dirname, OUT = path.resolve(process.argv[2] || path.join(DIR, 'icono'));
fs.mkdirSync(OUT, { recursive: true });

/* Lienzo de icono adaptable: 108 x 108, zona segura = círculo de 66 en el centro. */
var ESCUDO = 'M54,28.5 C59.5,32.5 66.5,34.8 73.5,35.3 Q76,35.5 76,38 V53 C76,66.5 67,76.5 55.2,80.6 Q54,81 52.8,80.6 ' +
  'C41,76.5 32,66.5 32,53 V38 Q32,35.5 34.5,35.3 C41.5,34.8 48.5,32.5 54,28.5 Z';
var CAMPANA = 'M54,40.8 C48.2,40.8 44.2,45.3 44.2,51.2 V57.6 C44.2,58.6 43.9,59.5 43.3,60.3 L41.9,62.1 C41.2,63 41.8,64.2 42.9,64.2 ' +
  'H65.1 C66.2,64.2 66.8,63 66.1,62.1 L64.7,60.3 C64.1,59.5 63.8,58.6 63.8,57.6 V51.2 C63.8,45.3 59.8,40.8 54,40.8 Z ' +
  'M50.6,66.6 H57.4 C57.4,68.5 55.9,70 54,70 C52.1,70 50.6,68.5 50.6,66.6 Z';
var GLIFO = ESCUDO + ' ' + CAMPANA;          // con evenOdd, la campana queda hueca
/* Contorno del ✓ (trazo de 6,5 con puntas redondas) dibujado como figura: con evenOdd queda hueco. */
var CHECK = 'M42.202,57.298 L48.702,63.798 A3.25,3.25 0 0 0 53.298,63.798 L66.298,50.798 A3.25,3.25 0 0 0 61.702,46.202 L51,56.904 L46.798,52.702 A3.25,3.25 0 0 0 42.202,57.298 Z';
var GLIFO_B = ESCUDO + ' ' + CHECK;
var ELEGIDA = 'B';                            // la que va en la app
var GLIFO_ELEGIDO = ELEGIDA === 'B' ? GLIFO_B : GLIFO;
var ESCALA = 0.9;                             // peso visual dentro de la zona segura
var C1 = '#FF5A1F', C2 = '#F23D00';          // Red Orange #FF4200, con un degradado mínimo

function defs() {
  return '<defs><linearGradient id="g" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="108" y2="108"><stop offset="0" stop-color="' + C1 + '"/><stop offset="1" stop-color="' + C2 + '"/></linearGradient>' +
    '<radialGradient id="h" gradientUnits="userSpaceOnUse" cx="30" cy="20" r="70"><stop offset="0" stop-color="#fff" stop-opacity=".22"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient></defs>';
}
function grupo(contenido) { return '<g transform="translate(54 54) scale(' + ESCALA + ') translate(-54 -54)">' + contenido + '</g>'; }
var PROPUESTAS = {
  A: function () {   // naranja con escudo-campana blanco
    return defs() + '<rect width="108" height="108" fill="url(#g)"/><rect width="108" height="108" fill="url(#h)"/>' +
      grupo('<path fill="#fff" fill-rule="evenodd" d="' + GLIFO + '"/>');
  },
  B: function () {   // naranja con escudo y check
    return defs() + '<rect width="108" height="108" fill="url(#g)"/><rect width="108" height="108" fill="url(#h)"/>' +
      grupo('<path fill="#F9F9F9" fill-rule="evenodd" d="' + GLIFO_B + '"/>');
  },
  C: function () {   // oscuro con escudo-campana naranja
    return defs() + '<rect width="108" height="108" fill="#16181D"/>' +
      grupo('<path fill="url(#g)" fill-rule="evenodd" d="' + GLIFO + '"/>');
  }
};
function svg(k, forma, tam) {
  /* Lo que enseña el lanzador: los 72 dp centrales, recortados con la forma. */
  return '<svg width="' + tam + '" height="' + tam + '" viewBox="18 18 72 72" style="display:block;border-radius:' + (forma === 'circulo' ? '50%' : '28%') + '">' + PROPUESTAS[k]() + '</svg>';
}
function silueta(tam, color) {
  return '<svg width="' + tam + '" height="' + tam + '" viewBox="22.5 23.25 63 63"><path fill="' + color + '" fill-rule="evenodd" d="' + GLIFO_ELEGIDO + '"/></svg>';
}
function tematico(tam) {
  return '<svg width="' + tam + '" height="' + tam + '" viewBox="18 18 72 72" style="display:block;border-radius:50%"><rect x="0" y="0" width="108" height="108" fill="#D3E3FD"/>' +
    grupo('<path fill="#0B2A5C" fill-rule="evenodd" d="' + GLIFO_ELEGIDO + '"/>') + '</svg>';
}

var FUENTES = '@font-face{font-family:Roboto;font-weight:400;src:url(file://' + DIR + '/node_modules/@fontsource/roboto/files/roboto-latin-400-normal.woff2)}' +
  '@font-face{font-family:Roboto;font-weight:500;src:url(file://' + DIR + '/node_modules/@fontsource/roboto/files/roboto-latin-500-normal.woff2)}' +
  '@font-face{font-family:MS;src:url(file://' + DIR + '/node_modules/material-symbols/material-symbols-rounded.woff2)}' +
  '.i{font-family:MS;font-size:30px;line-height:1;font-feature-settings:"liga";font-variation-settings:"FILL" 1}';

function hoja() {
  var otras = [['call', '#1E8E3E', 'Teléfono'], ['chat', '#1A73E8', 'Mensajes'], ['photo_camera', '#5F6368', 'Cámara'], ['map', '#188038', 'Mapas'],
    ['mail', '#C5221F', 'Correo'], ['calendar_month', '#1967D2', 'Calendario'], ['folder', '#E37400', 'Archivos']];
  function app(ic, color, nombre, tematica) {
    if (tematica) return '<div class="app"><div class="ico" style="background:#D3E3FD;color:#0B2A5C"><span class="i">' + ic + '</span></div><span>' + nombre + '</span></div>';
    return '<div class="app"><div class="ico" style="background:#fff;color:' + color + '"><span class="i">' + ic + '</span></div><span>' + nombre + '</span></div>';
  }
  function inicio(tematica) {
    var celdas = otras.slice(0, 3).map(function (o) { return app(o[0], o[1], o[2], tematica); }).join('') +
      '<div class="app">' + (tematica ? tematico(60) : svg(ELEGIDA, 'circulo', 60)) + '<span>Avisos</span></div>' +
      otras.slice(3).map(function (o) { return app(o[0], o[1], o[2], tematica); }).join('');
    return '<div class="home"><div class="sbar"><span>9:41</span></div><div class="grid">' + celdas + '</div></div>';
  }
  return '<!doctype html><html><head><meta charset="utf-8"><style>' + FUENTES +
    'body{margin:0;padding:32px;background:#ececf0;font-family:Roboto,sans-serif;color:#111;width:1180px}' +
    'h1{font-size:28px;margin:0 0 6px}h2{font-size:20px;margin:34px 0 14px;font-weight:500}p.n{margin:0;color:#555;font-size:15px}' +
    '.props{display:flex;gap:40px;margin-top:22px}.prop{display:flex;flex-direction:column;align-items:center;gap:12px;background:#fff;border-radius:28px;padding:24px 30px}' +
    '.prop .par{display:flex;gap:18px;align-items:center}.prop b{font-size:17px;font-weight:500}.prop small{color:#666;font-size:13px;text-align:center;max-width:250px}' +
    '.prop.rec{outline:3px solid #E5530A}' +
    '.fila{display:flex;gap:28px;align-items:flex-start}' +
    '.home{width:300px;height:250px;border-radius:28px;background:linear-gradient(160deg,#2b3d6b,#6d4a7d 55%,#c27b5a);padding:12px 16px;box-sizing:border-box;color:#fff}' +
    '.sbar{font-size:13px;font-weight:500;padding:0 6px 14px}.grid{display:grid;grid-template-columns:repeat(4,1fr);row-gap:16px;justify-items:center}' +
    '.app{display:flex;flex-direction:column;align-items:center;gap:6px;font-size:12px}.ico{width:60px;height:60px;border-radius:50%;display:grid;place-items:center}' +
    '.cap{font-size:13px;color:#555;margin-top:8px;text-align:center}' +
    '.panel{width:430px;border-radius:28px;background:#f4f4f6;padding:12px 12px 14px;box-sizing:border-box}' +
    '.st{display:flex;align-items:center;gap:8px;font-size:14px;font-weight:500;padding:4px 10px 12px;color:#1b1b1f}' +
    '.ntf{background:#fff;border-radius:24px;padding:14px 14px 10px;display:grid;grid-template-columns:40px 1fr;column-gap:12px}' +
    '.ntf .c{width:40px;height:40px;border-radius:50%;background:#F27427;display:grid;place-items:center}' +
    '.ntf .ap{font-size:12px;color:#5f5f66}.ntf h5{margin:2px 0 0;font-size:15px;font-weight:500}.ntf p{margin:2px 0 0;font-size:14px;color:#46464c}' +
    '.ntf .ac{grid-column:2;display:flex;gap:6px;margin-top:10px}.ntf .ac span{height:34px;border-radius:17px;background:#f0f0f3;color:#C65500;font-size:14px;font-weight:500;padding:0 12px;display:flex;align-items:center}' +
    '.dk{background:#1c1c1f}.dk .st{color:#e6e6ea}.dk .ntf{background:#2a2a2e;color:#eee}.dk .ntf p{color:#c8c8cc}.dk .ntf .ap{color:#a9a9b0}.dk .ntf .ac span{background:#38383d;color:#FFB691}' +
    '</style></head><body>' +
    '<h1>Icono nuevo de Avisos</h1><p class="n">Lienzo adaptable de Android (se recorta en círculo, gota o cuadrado según el móvil). Elegida la B. La notificación usa la misma silueta que el icono.</p>' +
    '<div class="props">' + ['A', 'B', 'C'].map(function (k) {
      var txt = { A: 'Escudo con campana, en naranja. Seguridad + aviso. Se reconoce también en la silueta de la notificación.',
        B: 'Escudo con ✓, en naranja. Seguridad y trabajo hecho; el más cercano al icono de antes.',
        C: 'La A sobre fondo oscuro, como el icono actual. Menos llamativo en un fondo de pantalla oscuro.' }[k];
      return '<div class="prop' + (k === ELEGIDA ? ' rec' : '') + '"><div class="par">' + svg(k, 'circulo', 120) + svg(k, 'cuadrado', 96) + '</div><b>' + k + (k === ELEGIDA ? ' · elegida' : '') + '</b><small>' + txt + '</small></div>';
    }).join('') + '</div>' +
    '<h2>La ' + ELEGIDA + ' en el móvil</h2><div class="fila">' +
    '<div>' + inicio(false) + '<div class="cap">Pantalla de inicio</div></div>' +
    '<div>' + inicio(true) + '<div class="cap">Iconos temáticos (Android 13+): toma el color del fondo</div></div>' +
    '<div><div class="panel"><div class="st">9:41 ' + silueta(18, '#1b1b1f') + '</div>' +
    '<div class="ntf"><span class="c">' + silueta(28, '#fff') + '</span><div><div class="ap">Avisos · ahora</div><h5>Nuevo aviso urgente · Farmacia Centro</h5><p>Central de alarma en fallo de comunicación.</p></div>' +
    '<div class="ac"><span>Me lo quedo</span><span>Llamar</span><span>Ver</span></div></div></div>' +
    '<div class="panel dk" style="margin-top:12px"><div class="st">9:41 ' + silueta(18, '#e6e6ea') + '</div>' +
    '<div class="ntf"><span class="c">' + silueta(28, '#fff') + '</span><div><div class="ap">Avisos · en 20 min</div><h5>Naves Logísticas Sur · 11:00</h5><p>Sustitución de 2 cámaras exteriores.</p></div>' +
    '<div class="ac"><span>Ir</span><span>Empezar</span><span>Retrasar 30 min</span></div></div></div>' +
    '<div class="cap">Barra de estado y notificaciones (claro y oscuro)</div></div>' +
    '</div></body></html>';
}

/* Recursos de Android para la propuesta A. */
function xmlFondo() {
  return '<?xml version="1.0" encoding="utf-8"?>\n<!-- Fondo del icono adaptable: degradado naranja con un brillo arriba a la izquierda. -->\n' +
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:aapt="http://schemas.android.com/aapt"\n' +
    '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n' +
    '    <path android:pathData="M0,0h108v108h-108z">\n        <aapt:attr name="android:fillColor">\n' +
    '            <gradient\n                android:type="linear"\n                android:startX="0"\n                android:startY="0"\n                android:endX="108"\n                android:endY="108">\n' +
    '                <item android:offset="0" android:color="#FF' + C1.slice(1) + '" />\n                <item android:offset="1" android:color="#FF' + C2.slice(1) + '" />\n' +
    '            </gradient>\n        </aapt:attr>\n    </path>\n' +
    '    <path android:pathData="M0,0h108v108h-108z">\n        <aapt:attr name="android:fillColor">\n' +
    '            <gradient\n                android:type="radial"\n                android:centerX="30"\n                android:centerY="20"\n                android:gradientRadius="70">\n' +
    '                <item android:offset="0" android:color="#38FFFFFF" />\n                <item android:offset="1" android:color="#00FFFFFF" />\n' +
    '            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n';
}
function xmlGlifo(comentario, color) {
  return '<?xml version="1.0" encoding="utf-8"?>\n<!-- ' + comentario + ' -->\n' +
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n' +
    '    <group\n        android:pivotX="54"\n        android:pivotY="54"\n        android:scaleX="' + ESCALA + '"\n        android:scaleY="' + ESCALA + '">\n' +
    '        <path\n            android:fillColor="' + color + '"\n            android:fillType="evenOdd"\n            android:pathData="' + GLIFO_ELEGIDO + '" />\n    </group>\n</vector>\n';
}
function xmlPequeno(comentario, color, extra) {
  return '<?xml version="1.0" encoding="utf-8"?>\n<!-- ' + comentario + ' -->\n' +
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="24dp"\n    android:height="24dp"\n    android:viewportWidth="63"\n    android:viewportHeight="63"' + (extra || '') + '>\n' +
    '    <group\n        android:translateX="-22.5"\n        android:translateY="-23.25">\n' +
    '        <path\n            android:fillColor="' + color + '"\n            android:fillType="evenOdd"\n            android:pathData="' + GLIFO_ELEGIDO + '" />\n    </group>\n</vector>\n';
}

async function main() {
  var res = path.join(OUT, 'res');
  fs.mkdirSync(path.join(res, 'drawable'), { recursive: true });
  fs.mkdirSync(path.join(res, 'mipmap-xxhdpi'), { recursive: true });
  fs.writeFileSync(path.join(res, 'drawable', 'ic_launcher_background.xml'), xmlFondo());
  fs.writeFileSync(path.join(res, 'drawable', 'ic_launcher_foreground.xml'), xmlGlifo('Escudo con ✓: el ✓ queda hueco (evenOdd) y deja ver el fondo.', '#F9F9F9'));
  fs.writeFileSync(path.join(res, 'drawable', 'ic_launcher_monochrome.xml'), xmlGlifo('Capa para los iconos temáticos (Android 13+): el sistema la tiñe.', '#FFFFFF'));
  fs.writeFileSync(path.join(res, 'drawable', 'ic_notificacion.xml'), xmlPequeno('Icono de notificación: la misma silueta. Android la pinta de un color; el naranja lo pone setColor.', '#FFFFFF', '\n    android:tint="#FFFFFF"'));
  fs.writeFileSync(path.join(res, 'drawable', 'ic_escudo.xml'), xmlPequeno('Escudo con ✓ en naranja, para el widget.', '#FFFF4200'));

  var b = await chromium.launch();
  var p = await b.newPage({ viewport: { width: 1244, height: 900 }, deviceScaleFactor: 1.5 });
  fs.writeFileSync(path.join(DIR, '_icono.html'), hoja());
  await p.goto('file://' + path.join(DIR, '_icono.html'));
  await p.evaluate(function () { return document.fonts.ready; });
  await p.waitForTimeout(200);
  await p.screenshot({ path: path.join(OUT, 'icono-propuestas.png'), fullPage: true });
  await p.close();

  /* Iconos de siempre (Android 7, sin icono adaptable): 192 px como los de ahora. */
  for (var forma of [['ic_launcher', '22%'], ['ic_launcher_round', '50%']]) {
    var q = await b.newPage({ viewport: { width: 192, height: 192 }, deviceScaleFactor: 1 });
    await q.setContent('<body style="margin:0;background:transparent"><div style="width:176px;height:176px;margin:8px;border-radius:' + forma[1] + ';overflow:hidden;box-shadow:0 2px 4px rgba(0,0,0,.25)">' +
      '<svg width="176" height="176" viewBox="18 18 72 72" style="display:block">' + PROPUESTAS[ELEGIDA]() + '</svg></div></body>');
    await q.screenshot({ path: path.join(res, 'mipmap-xxhdpi', forma[0] + '.png'), omitBackground: true });
    await q.close();
  }
  await b.close();
  console.log('Hecho en ' + OUT);
}
main().catch(function (e) { console.error(e); process.exit(1); });
