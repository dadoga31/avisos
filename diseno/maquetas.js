/* Maquetas v2: Material 3 Expressive, pensadas para un técnico en la calle.
   Solo para aprobar el diseño; no es código de la app. node maquetas2.js <salida> */
'use strict';
var fs = require('fs'), path = require('path');
var chromium = require('playwright').chromium;
var DIR = __dirname, OUT = path.resolve(process.argv[2] || path.join(DIR, 'png'));
fs.mkdirSync(OUT, { recursive: true });

var CSS = fs.readFileSync(path.join(DIR, 'tokens2.css'), 'utf8') + `
@font-face{font-family:Roboto;font-weight:400;src:url(node_modules/@fontsource/roboto/files/roboto-latin-400-normal.woff2)}
@font-face{font-family:Roboto;font-weight:500;src:url(node_modules/@fontsource/roboto/files/roboto-latin-500-normal.woff2)}
@font-face{font-family:Roboto;font-weight:700;src:url(node_modules/@fontsource/roboto/files/roboto-latin-700-normal.woff2)}
@font-face{font-family:MS;src:url(node_modules/material-symbols/material-symbols-rounded.woff2)}
:root{--page:var(--surface-container);--item:var(--surface-container-lowest);--sh:0 1px 3px rgba(0,0,0,.18),0 4px 10px 2px rgba(0,0,0,.10)}
.dark{--page:var(--surface);--item:var(--surface-container-high);--sh:0 1px 3px rgba(0,0,0,.5),0 4px 10px 2px rgba(0,0,0,.35)}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:412px;height:915px;overflow:hidden}
body{font-family:Roboto,sans-serif;background:var(--page);color:var(--on-surface);position:relative;-webkit-font-smoothing:antialiased}
.i{font-family:MS;font-weight:normal;font-style:normal;font-size:24px;line-height:1;display:inline-block;white-space:nowrap;
  font-feature-settings:'liga';font-variation-settings:'FILL' 0,'wght' 400,'GRAD' 0,'opsz' 24;user-select:none}
.f{font-variation-settings:'FILL' 1,'wght' 400,'GRAD' 0,'opsz' 24}
.sb{position:absolute;top:0;left:0;right:0;height:32px;display:flex;align-items:center;justify-content:space-between;padding:0 18px 0 26px;font-size:14px;font-weight:500;z-index:20}
.sb .i{font-size:17px;margin-left:2px}
.gn{position:absolute;bottom:0;left:0;right:0;height:24px;display:flex;align-items:center;justify-content:center;z-index:20}
.gn:after{content:'';width:108px;height:4px;border-radius:2px;background:var(--on-surface);opacity:.75}
.pg{position:absolute;inset:0;padding-top:32px;overflow:hidden}
.bar{height:64px;display:flex;align-items:center;padding:0 4px}
.bar h1{flex:1;font-size:22px;line-height:28px;font-weight:400;padding-left:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ib{width:48px;height:48px;border-radius:24px;display:grid;place-items:center;color:var(--on-surface-variant);flex:none}
.ib.on{color:var(--on-surface)}
.ib.ton{width:40px;height:40px;background:var(--secondary-container);color:var(--on-secondary-container)}
.big{padding:4px 24px 16px}
.big h1{font-size:32px;line-height:40px;font-weight:500;letter-spacing:-.2px}
.big p{font-size:14px;line-height:20px;color:var(--on-surface-variant);margin-top:2px}
.search{margin:8px 16px 0;height:56px;border-radius:28px;background:var(--surface-container-highest);display:flex;align-items:center;padding:0 4px 0 16px;gap:12px;color:var(--on-surface-variant);font-size:16px}
.dark .search{background:var(--surface-container-high)}
.search span.t{flex:1}
.chips{display:flex;gap:8px;padding:12px 16px;overflow:hidden;white-space:nowrap}
.chip{height:32px;border-radius:8px;border:1px solid var(--outline-variant);display:inline-flex;align-items:center;gap:8px;padding:0 16px;font-size:14px;font-weight:500;color:var(--on-surface-variant);flex:none;background:transparent}
.chip .i{font-size:18px}
.chip.ci{padding-left:8px}
.chip.av{padding-left:4px}
.chip.av .ava{width:24px;height:24px;font-size:10px}
.chip.sel{background:var(--secondary-container);color:var(--on-secondary-container);border-color:transparent}
.chip b{font-weight:500;opacity:.75}
.sec{display:flex;justify-content:space-between;align-items:baseline;padding:20px 32px 8px;font-size:14px;line-height:20px;font-weight:500;color:var(--on-surface-variant)}
.sec small{font-size:12px;font-weight:500;color:var(--on-surface-variant)}
.sec.err{color:var(--error)}
.grp{margin:0 16px;display:flex;flex-direction:column;gap:2px}
.it{background:var(--item);border-radius:4px;display:flex;align-items:center;gap:16px;padding:12px 16px;min-height:56px;position:relative}
.grp>*:first-child,.grp>*:first-child>.it{border-top-left-radius:20px;border-top-right-radius:20px}
.grp>*:last-child,.grp>*:last-child>.it{border-bottom-left-radius:20px;border-bottom-right-radius:20px}
.it .tx{flex:1;min-width:0}
.it .hl{font-size:16px;line-height:24px}
.it .sp{font-size:14px;line-height:20px;color:var(--on-surface-variant)}
.it>.i{color:var(--on-surface-variant)}
.av .hl{font-weight:500;line-height:22px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.it.av{align-items:flex-start;padding:14px 16px}
.lead{width:40px;height:40px;border-radius:20px;display:grid;place-items:center;flex:none}
.lead .i{font-size:22px}
.meta{display:flex;align-items:center;gap:6px;margin-top:6px;font-size:12px;line-height:16px;font-weight:500;color:var(--on-surface-variant)}
.tag{height:20px;border-radius:6px;padding:0 6px;display:inline-flex;align-items:center;font-size:11px;font-weight:500}
.tr{display:flex;flex-direction:column;align-items:flex-end;justify-content:space-between;align-self:stretch;gap:8px;flex:none}
.tr .h{font-size:12px;line-height:16px;font-weight:500;color:var(--on-surface-variant);margin-top:3px}
.tr .h.err{color:var(--error)}
.ava{width:28px;height:28px;border-radius:14px;display:grid;place-items:center;font-size:11px;font-weight:500;flex:none}
.ava.l{width:40px;height:40px;border-radius:20px;font-size:15px}
.nav{position:absolute;left:0;right:0;bottom:0;height:104px;padding-bottom:24px;background:var(--surface-container-low);display:flex;z-index:5}
.dark .nav{background:var(--surface-container)}
.nav a{flex:1;display:flex;flex-direction:column;align-items:center;padding-top:12px;gap:4px;font-size:12px;line-height:16px;font-weight:500;color:var(--on-surface-variant)}
.nav .ind{width:64px;height:32px;border-radius:16px;display:grid;place-items:center;position:relative}
.nav a.on{color:var(--on-surface);font-weight:700}
.nav a.on .ind{background:var(--secondary-container);color:var(--on-secondary-container)}
.badge{position:absolute;top:-1px;left:34px;min-width:16px;height:16px;border-radius:8px;background:var(--error);color:var(--on-error);font-size:11px;font-weight:500;display:grid;place-items:center;padding:0 4px;font-style:normal}
.fab{position:absolute;right:16px;bottom:120px;height:56px;border-radius:16px;padding:0 20px 0 16px;display:flex;align-items:center;gap:12px;background:var(--primary);color:var(--on-primary);font-size:16px;font-weight:500;z-index:6;box-shadow:var(--sh)}
.btn{height:40px;border-radius:20px;display:inline-flex;align-items:center;justify-content:center;gap:8px;padding:0 20px 0 16px;font-size:14px;font-weight:500;flex:none}
.btn .i{font-size:20px}
.btn.fill{background:var(--primary);color:var(--on-primary)}
.btn.ton{background:var(--secondary-container);color:var(--on-secondary-container)}
.btn.out{border:1px solid var(--outline-variant);color:var(--on-surface-variant)}
.btn.txt{color:var(--primary);padding:0 12px}
.sup{font-size:12px;line-height:16px;color:var(--on-surface-variant);padding:4px 16px 0;display:flex;gap:6px;align-items:center}
.sup .i{font-size:16px;color:var(--primary)}
.tf{position:relative;height:56px;border:1px solid var(--outline);border-radius:4px;display:flex;align-items:center;padding:0 12px 0 16px;gap:12px;font-size:16px;color:var(--on-surface);background:transparent}
.tf .lb{position:absolute;top:-9px;left:12px;padding:0 4px;background:var(--page);font-size:12px;line-height:16px;color:var(--on-surface-variant)}
.tf .v{flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tf>.i{color:var(--on-surface-variant)}
.form{padding:8px 16px 0;display:flex;flex-direction:column;gap:18px}
.lbl{font-size:14px;line-height:20px;font-weight:500;color:var(--on-surface-variant);margin-bottom:8px;padding-left:4px}
.wrap{display:flex;flex-wrap:wrap;gap:8px}
.seg{display:flex;height:40px}
.seg>span{flex:1;border:1px solid var(--outline);margin-left:-1px;display:flex;align-items:center;justify-content:center;gap:6px;font-size:14px;font-weight:500;color:var(--on-surface)}
.seg>span:first-child{border-radius:20px 0 0 20px;margin-left:0}
.seg>span:last-child{border-radius:0 20px 20px 0}
.seg>span.sel{background:var(--secondary-container);color:var(--on-secondary-container)}
.seg .i{font-size:18px}
/* Siguiente aviso */
.nxt{margin:6px 16px 0;background:var(--item);border-radius:28px;padding:18px 18px 16px 20px}
.nxt .top{display:flex;justify-content:space-between;align-items:center;font-size:12px;line-height:16px;font-weight:500;color:var(--on-surface-variant)}
.nxt .top b{color:var(--primary);font-weight:700;letter-spacing:.4px}
.nxt h3{font-size:22px;line-height:28px;font-weight:500;margin-top:8px}
.nxt p{font-size:14px;line-height:20px;color:var(--on-surface-variant);margin-top:2px}
.nxt .acts{display:flex;gap:8px;margin-top:14px}
.nxt .acts .btn{height:48px;border-radius:24px;font-size:15px}
.nxt .acts .btn.fill{flex:1}
.prog{display:flex;align-items:center;gap:10px;margin:14px 0 0;font-size:12px;font-weight:500;color:var(--on-surface-variant)}
.prog .b{flex:1;height:6px;border-radius:3px;background:var(--surface-container-highest);display:flex;gap:3px;overflow:hidden}
.prog .b i{display:block;height:6px;border-radius:3px;background:var(--e-resuelto)}
/* Deslizar */
.swp{position:relative;background:var(--e-resuelto);color:var(--e-resuelto-on);border-radius:4px}
.swp .ac{position:absolute;right:24px;top:0;bottom:0;display:flex;align-items:center;gap:8px;font-size:14px;font-weight:500}
.swp .ac .i{font-size:26px}
.swp>.it{transform:translateX(-132px);box-shadow:var(--sh);color:var(--on-surface)}
.snack{position:absolute;left:16px;right:16px;bottom:118px;min-height:52px;border-radius:12px;background:var(--inverse-surface);color:var(--inverse-on-surface);display:flex;align-items:center;padding:0 4px 0 16px;font-size:14px;line-height:20px;z-index:7;box-shadow:var(--sh)}
.snack span{flex:1}
.snack b{color:var(--inverse-primary);font-weight:500;padding:0 12px;height:40px;display:flex;align-items:center}
/* Ficha */
.pill{display:inline-flex;align-items:center;gap:8px;height:40px;border-radius:20px;padding:0 10px 0 12px;font-size:15px;font-weight:500}
.pill .i{font-size:20px}
.ttl{padding:4px 24px 0}
.ttl h2{font-size:26px;line-height:34px;font-weight:400;margin-top:14px}
.ttl p{font-size:16px;line-height:24px;color:var(--on-surface-variant);margin-top:4px;display:flex;align-items:center;gap:6px}
.ttl p .i{font-size:18px}
.ttl .chips{padding:12px 0 4px}
.stp{display:flex;align-items:center;gap:6px}
.stp .ib{width:36px;height:36px;background:var(--secondary-container);color:var(--on-secondary-container)}
.stp .ib .i{font-size:20px}
.stp b{min-width:52px;text-align:center;font-size:16px;font-weight:500}
.ftb{position:absolute;left:16px;right:16px;bottom:36px;height:72px;border-radius:36px;background:var(--surface-container-high);display:flex;align-items:center;padding:0 8px 0 12px;gap:4px;box-shadow:var(--sh);z-index:6}
.dark .ftb{background:var(--surface-container-highest)}
.ftb .ib{color:var(--on-surface)}
.ftb .go{margin-left:auto;height:56px;border-radius:28px;padding:0 22px 0 18px;background:var(--primary);color:var(--on-primary);display:flex;align-items:center;gap:8px;font-size:16px;font-weight:500}
/* Hoja */
.scrim{position:absolute;inset:0;background:rgba(0,0,0,.36);z-index:8}
.sheet{position:absolute;left:0;right:0;bottom:0;z-index:9;background:var(--surface-container-low);border-radius:28px 28px 0 0;padding:0 0 32px}
.dark .sheet{background:var(--surface-container)}
.hd{width:32px;height:4px;border-radius:2px;background:var(--on-surface-variant);opacity:.4;margin:16px auto 12px}
.sheet h4{font-size:22px;line-height:28px;font-weight:400;padding:0 24px 4px}
.sheet .s{font-size:14px;line-height:20px;color:var(--on-surface-variant);padding:0 24px 12px}
.opt{display:flex;align-items:center;gap:16px;padding:10px 24px;min-height:64px}
.opt .tx{flex:1}
.opt .hl{font-size:16px;line-height:24px}
.opt .sp{font-size:14px;line-height:20px;color:var(--on-surface-variant)}
.opt.sel{background:var(--secondary-container);margin:0 12px;border-radius:32px;padding:10px 12px}
/* Pantalla de bloqueo */
.lock{background:linear-gradient(160deg,#3b2a58 0%,#7a3b4f 45%,#c9653a 100%)}
.lock .sb{color:#fff}
.reloj{color:#fff;text-align:left;padding:28px 28px 22px}
.reloj b{display:block;font-size:96px;line-height:100px;font-weight:400;letter-spacing:-2px}
.reloj span{font-size:16px;opacity:.9}
.ntf{margin:0 12px 6px;background:var(--surface-container-lowest);border-radius:28px;padding:16px 16px 10px;color:var(--on-surface)}
.dark .ntf{background:var(--surface-container-high)}
.ntf .ap{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--on-surface-variant)}
.ntf .ap .ic{width:22px;height:22px;border-radius:11px;background:var(--primary);color:var(--on-primary);display:grid;place-items:center}
.ntf .ap .ic .i{font-size:14px}
.ntf h5{font-size:15px;line-height:20px;font-weight:500;margin-top:10px}
.ntf p{font-size:14px;line-height:20px;color:var(--on-surface-variant);margin-top:2px}
.ntf .acts{display:flex;gap:4px;margin:8px -4px 0}
.ntf .acts span{height:36px;border-radius:18px;padding:0 12px;display:flex;align-items:center;font-size:14px;font-weight:500;color:var(--primary);background:var(--surface-container)}
.dark .ntf .acts span{background:var(--surface-container-highest)}
.stats{margin:0 16px;background:var(--item);border-radius:20px;display:flex;padding:16px 4px}
.stats div{flex:1;padding:0 12px;border-left:1px solid var(--outline-variant)}
.stats div:first-child{border-left:0}
.stats strong{display:block;font-size:28px;line-height:36px;font-weight:400}
.stats span{font-size:12px;line-height:16px;font-weight:500;color:var(--on-surface-variant)}
.swt{width:52px;height:32px;border-radius:16px;flex:none;position:relative;background:var(--primary)}
.swt:after{content:'check';font-family:MS;font-size:16px;color:var(--primary);position:absolute;top:4px;right:4px;width:24px;height:24px;border-radius:12px;background:var(--on-primary);display:grid;place-items:center;font-feature-settings:'liga'}
.swt.off{background:var(--surface-container-highest);border:2px solid var(--outline)}
.swt.off:after{content:'';width:16px;height:16px;border-radius:8px;background:var(--outline);top:6px;left:6px;right:auto}
.txtbtn{color:var(--primary);font-size:14px;font-weight:500;padding:0 12px;height:40px;display:flex;align-items:center}
`;

CSS += `.nav{left:16px;right:auto;bottom:34px;height:68px;padding:0 6px;border-radius:34px;background:var(--surface-container-lowest);box-shadow:0 2px 6px rgba(0,0,0,.14),0 8px 24px rgba(0,0,0,.14)}.dark .nav{background:var(--surface-container-highest);box-shadow:0 2px 6px rgba(0,0,0,.5),0 8px 24px rgba(0,0,0,.45)}.nav.centro{left:50%;transform:translateX(-50%)}.nav a{flex:none;width:72px;padding-top:0;justify-content:center;gap:3px}.nav .ind{width:56px;height:30px;border-radius:15px}.nav .badge{left:30px}.fabr{position:absolute;right:16px;bottom:36px;width:64px;height:64px;border-radius:22px;background:var(--primary);color:var(--on-primary);display:grid;place-items:center;z-index:6;box-shadow:0 2px 6px rgba(0,0,0,.18),0 8px 20px rgba(0,0,0,.18)}.fabr .i{font-size:30px}.snack{bottom:116px}.ntf{position:relative;padding-left:68px}.ntf .ap .ic{position:absolute;left:14px;top:16px;width:40px;height:40px;border-radius:20px;background:#F27427;color:#fff}`;
var GLIFO = 'M54,28.5 C59.5,32.5 66.5,34.8 73.5,35.3 Q76,35.5 76,38 V53 C76,66.5 67,76.5 55.2,80.6 Q54,81 52.8,80.6 C41,76.5 32,66.5 32,53 V38 Q32,35.5 34.5,35.3 C41.5,34.8 48.5,32.5 54,28.5 Z M42.202,57.298 L48.702,63.798 A3.25,3.25 0 0 0 53.298,63.798 L66.298,50.798 A3.25,3.25 0 0 0 61.702,46.202 L51,56.904 L46.798,52.702 A3.25,3.25 0 0 0 42.202,57.298 Z';
function glifo(t) { return '<svg width="' + t + '" height="' + t + '" viewBox="22.5 23.25 63 63"><path fill="#fff" fill-rule="evenodd" d="' + GLIFO + '"/></svg>'; }
var E = {
  pendiente: ['radio_button_unchecked', 'Pendiente', 'Sin fecha o sin técnico'],
  programado: ['event', 'Programado', 'Tiene fecha y técnico'],
  curso: ['construction', 'En curso', 'Estás en ello ahora'],
  espera: ['pause_circle', 'En espera', 'Falta material o respuesta'],
  resuelto: ['check_circle', 'Resuelto', 'Hecho: pasa a Hechos'],
  cancelado: ['cancel', 'Cancelado', 'No se hace']
};
var TEC = { Y: ['Y', 'primary'], MR: ['MR', 'e-programado'], LP: ['LP', 'e-espera'] };
function i(n, f, st) { return '<span class="i' + (f ? ' f' : '') + '"' + (st ? ' style="' + st + '"' : '') + '>' + n + '</span>'; }
function ava(t, grande) {
  if (!t) return '<span class="ava' + (grande ? ' l' : '') + '" style="border:1.5px dashed var(--outline);color:var(--outline)">' + i('person', 0, 'font-size:' + (grande ? 22 : 16) + 'px') + '</span>';
  var c = TEC[t][1];
  var bg = c === 'primary' ? 'var(--primary-container)' : 'var(--' + c + '-c)';
  var fg = c === 'primary' ? 'var(--on-primary-container)' : 'var(--' + c + '-onc)';
  return '<span class="ava' + (grande ? ' l' : '') + '" style="background:' + bg + ';color:' + fg + '">' + TEC[t][0] + '</span>';
}
function prio(p) {
  if (p === 'urgente') return '<span class="tag" style="background:var(--error-container);color:var(--on-error-container)">Urgente</span>';
  if (p === 'alta') return '<span class="tag" style="background:var(--tertiary-container);color:var(--on-tertiary-container)">Alta</span>';
  return '';
}
function lead(e) { return '<span class="lead" style="background:var(--e-' + e + '-c);color:var(--e-' + e + '-onc)">' + i(E[e][0], 1) + '</span>'; }
function aviso(a) {
  return '<div class="it av">' + lead(a.e) +
    '<div class="tx"><div class="hl">' + a.t + '</div><div class="sp">' + a.c + '</div>' +
    '<div class="meta"><span style="color:var(--e-' + a.e + ')">' + (a.el || E[a.e][1]) + '</span>·<span>' + a.ref + '</span>' + prio(a.p) + '</div></div>' +
    '<div class="tr"><span class="h' + (a.venc ? ' err' : '') + '">' + a.h + '</span>' + ava(a.tec) + '</div></div>';
}
function nav(on, sinBadge, fab) {
  var T = [['agenda', 'calendar_month', 'Agenda', 1], ['hechos', 'task_alt', 'Hechos'], ['equipo', 'group', 'Equipo'], ['ajustes', 'settings', 'Ajustes']];
  return '<nav class="nav' + (fab ? '' : ' centro') + '">' + T.map(function (t) {
    return '<a class="' + (t[0] === on ? 'on' : '') + '"><span class="ind">' + i(t[1], t[0] === on) +
      (t[3] && !sinBadge ? '<em class="badge">' + t[3] + '</em>' : '') + '</span>' + t[2] + '</a>';
  }).join('') + '</nav>' + (fab ? '<span class="fabr">' + i(fab) + '</span>' : '');
}
function it(ic, hl, sp, tr) {
  return '<div class="it">' + (ic ? i(ic) : '') + '<div class="tx"><div class="hl">' + hl + '</div>' + (sp ? '<div class="sp">' + sp + '</div>' : '') + '</div>' + (tr || '') + '</div>';
}
var A1 = { e: 'pendiente', t: 'Central de alarma en fallo de comunicación', c: 'Farmacia Centro · Avería · Alarma', ref: 'AV-0001', p: 'urgente', h: 'Ayer 09:30', venc: 1, tec: 'Y' };
var A2 = { e: 'programado', t: 'Sustitución de 2 cámaras exteriores', c: 'Naves Logísticas Sur · Instalación · CCTV', ref: 'AV-0002', p: 'alta', h: '11:00', tec: 'MR' };
var A3 = { e: 'curso', t: 'Mantenimiento anual del sistema de incendios', c: 'Colegio San Pablo · Mantenimiento', ref: 'AV-0003', h: '16:00', tec: 'Y' };
var A4 = { e: 'programado', t: 'Lector de acceso no lee tarjetas', c: 'Gimnasio Atlas · Avería · Control de accesos', ref: 'AV-0004', p: 'alta', h: '08:00', tec: 'LP' };
var A5 = { e: 'pendiente', t: 'Presupuesto de videoportero para 12 viviendas', c: 'Comunidad C/ Olmo 8 · Presupuesto', ref: 'AV-0005', h: 'Lun 16', tec: null };

var PANT = {}, NOMBRES = {};

NOMBRES.agenda = '1 · Agenda con menú flotante';
PANT.agenda = function () {
  return '<div class="pg">' +
    '<div class="search">' + i('search') + '<span class="t">Buscar cliente, dirección, ref.</span><span class="ib">' + i('tune') + '</span></div>' +
    '<div class="chips"><span class="chip ci">' + i('warning', 1, 'color:var(--error)') + 'Vencidos <b>1</b></span>' +
    '<span class="chip">Urgentes <b>1</b></span><span class="chip">Sin asignar <b>1</b></span><span class="chip">Esta semana <b>4</b></span></div>' +
    '<div class="nxt"><div class="top"><b>SIGUIENTE · 11:00</b><span>en 1 h 19 min</span></div>' +
    '<h3>Sustitución de 2 cámaras exteriores</h3><p>Naves Logísticas Sur · Pol. Sur, nave 7</p>' +
    '<div class="acts"><span class="btn fill">' + i('navigation', 1) + 'Ir</span><span class="btn ton">' + i('call') + 'Llamar</span><span class="btn ton">' + i('play_arrow', 1) + 'Empezar</span></div>' +
    '<div class="prog"><span>Hoy</span><span class="b"><i style="width:33%"></i></span><span>1 de 3 hechos</span></div></div>' +
    '<div class="sec err">Vencidos<small>1</small></div><div class="grp">' + aviso(A1) +
    '</div><div class="sec">Hoy<small>2</small></div><div class="grp">' + aviso(A2) + aviso(A3) + '</div></div>' +
    nav('agenda', 0, 'add');
};

NOMBRES.deslizar = '2 · Deslizar para cerrar, con Deshacer';
PANT.deslizar = function () {
  return '<div class="pg">' +
    '<div class="search">' + i('search') + '<span class="t">Buscar cliente, dirección, ref.</span><span class="ib">' + i('tune') + '</span></div>' +
    '<div class="sec" style="padding-top:16px">Hoy<small>2</small></div><div class="grp">' +
    '<div class="swp"><span class="ac">' + i('check_circle', 1) + 'Hecho</span>' + aviso(A2) + '</div>' + aviso(A3) +
    '</div><div class="sec">Mañana<small>1</small></div><div class="grp">' + aviso(A4) +
    '</div><div class="sec">Más adelante<small>1</small></div><div class="grp">' + aviso(A5) + '</div></div>' +
    '<div class="snack"><span>AV-0001 marcado como hecho</span><b>Deshacer</b></div>' +
    nav('agenda', 1, 'add');
};

function ficha() {
  return '<div class="pg">' +
    '<div class="bar"><span class="ib on">' + i('arrow_back') + '</span><h1>AV-0003</h1><span class="ib">' + i('edit') + '</span><span class="ib">' + i('more_vert') + '</span></div>' +
    '<div class="ttl"><span class="pill" style="background:var(--e-curso-c);color:var(--e-curso-onc)">' + i('construction', 1) + 'En curso' + i('arrow_drop_down') + '</span>' +
    '<h2>Mantenimiento anual del sistema de incendios</h2><p>' + i('apartment') + 'Colegio San Pablo</p>' +
    '<div class="chips" style="padding-left:0"><span class="chip ci">' + i('build') + 'Mantenimiento</span><span class="chip ci">' + i('local_fire_department') + 'Incendios</span><span class="chip">Normal</span></div></div>' +
    '<div class="sec">Cuándo y quién</div><div class="grp">' +
    it('event', 'Hoy · 16:00', 'Empezado a las 15:52 · 2 h previstas', i('chevron_right')) +
    it('person', 'Yo', 'Asignado', i('chevron_right')) +
    it('timer', 'Horas', null, '<span class="stp"><span class="ib">' + i('remove') + '</span><b>1,5 h</b><span class="ib">' + i('add') + '</span></span>') +
    '</div><div class="sec">Cliente</div><div class="grp">' +
    it('location_on', 'Av. de la Paz 3, Madrid', 'Conserjería · 913 33 44 55') +
    '</div><div class="sec">Notas y fotos</div><div class="grp">' +
    it('sticky_note_2', 'Extintores de la planta 2 caducan en diciembre', 'Hoy 15:58') +
    it('add_a_photo', 'Añadir nota o foto') +
    '</div></div>' +
    '<div class="ftb"><span class="ib">' + i('call') + '</span><span class="ib">' + i('chat') + '</span><span class="ib">' + i('navigation') + '</span>' +
    '<span class="go">' + i('check', 0) + 'Marcar resuelto</span></div>';
}
NOMBRES.ficha = '3 · Ficha: acciones abajo, al pulgar';
PANT.ficha = ficha;

NOMBRES.estado = '4 · Cambiar estado: un toque en la pastilla';
PANT.estado = function () {
  return ficha() + '<div class="scrim"></div><div class="sheet"><div class="hd"></div><h4>Estado del aviso</h4><div class="s">AV-0003 · Colegio San Pablo</div>' +
    ['pendiente', 'programado', 'curso', 'espera', 'resuelto', 'cancelado'].map(function (k) {
      return '<div class="opt' + (k === 'curso' ? ' sel' : '') + '">' + lead(k) + '<div class="tx"><div class="hl">' + E[k][1] + '</div><div class="sp">' + E[k][2] + '</div></div>' +
        (k === 'curso' ? i('check', 0, 'color:var(--on-secondary-container)') : '') + '</div>';
    }).join('') + '</div>';
};

NOMBRES.nuevo = '5 · Nuevo aviso en 20 segundos';
PANT.nuevo = function () {
  return '<div class="pg">' +
    '<div class="bar"><span class="ib on">' + i('close') + '</span><h1>Nuevo aviso</h1><span class="btn fill" style="margin-right:8px;padding:0 20px">Guardar</span></div>' +
    '<div class="form">' +
    '<div class="tf"><span class="lb">Qué pasa</span><span class="v">Central de alarma no comunica</span>' + i('mic') + '</div>' +
    '<div><div class="tf">' + i('apartment') + '<span class="lb" style="left:12px">Cliente</span><span class="v">Farmacia Centro</span>' + i('cancel') + '</div>' +
    '<div class="sup">' + i('auto_awesome', 1) + 'C/ Mayor 12 · 913 22 11 00 — del aviso AV-0001</div></div>' +
    '<div><div class="lbl">Cuándo</div><div class="wrap"><span class="chip sel ci">' + i('check') + 'Hoy</span><span class="chip">Mañana</span><span class="chip ci">' + i('calendar_month') + 'Otro día</span><span class="chip ci">' + i('schedule') + '17:00</span></div></div>' +
    '<div><div class="lbl">Quién</div><div class="wrap"><span class="chip sel av">' + ava('Y') + 'Yo</span><span class="chip av">' + ava('LP') + 'Laura</span><span class="chip av">' + ava('MR') + 'Marcos</span><span class="chip">Sin asignar</span></div></div>' +
    '<div><div class="lbl">Tipo</div><div class="wrap"><span class="chip sel ci">' + i('check') + 'Avería</span><span class="chip">Instalación</span><span class="chip">Mantenimiento</span><span class="chip">Presupuesto</span></div></div>' +
    '<div><div class="lbl">Prioridad</div><div class="seg"><span>Baja</span><span>Normal</span><span>Alta</span><span class="sel">' + i('check') + 'Urgente</span></div></div>' +
    '<div class="it" style="border-radius:20px">' + i('expand_more') + '<div class="tx"><div class="hl">Más detalles</div><div class="sp">Sistema, descripción, contacto, fotos</div></div></div>' +
    '</div></div>';
};

NOMBRES.avisos = '6 · Lo urgente te encuentra a ti';
PANT.avisos = function () {
  return '<div class="pg"><div class="reloj"><b>9:41</b><span>jueves, 12 de noviembre</span></div>' +
    '<div class="ntf"><div class="ap"><span class="ic">' + glifo(26) + '</span>Avisos · correo de la empresa · ahora</div>' +
    '<h5>Nuevo aviso urgente · Farmacia Centro</h5><p>Central de alarma en fallo de comunicación. C/ Mayor 12.</p>' +
    '<div class="acts"><span>Me lo quedo</span><span>Llamar</span><span>Ver</span></div></div>' +
    '<div class="ntf"><div class="ap"><span class="ic">' + glifo(26) + '</span>Avisos · recordatorio · 10:41</div>' +
    '<h5>En 20 min · Naves Logísticas Sur</h5><p>Sustitución de 2 cámaras exteriores. Pol. Sur, nave 7.</p>' +
    '<div class="acts"><span>Ir</span><span>Empezar</span><span>Retrasar 30 min</span></div></div>' +
    '</div>';
};

NOMBRES.hechos = '7 · Hechos';
PANT.hechos = function () {
  return '<div class="pg">' +
    '<div class="bar"><span style="flex:1"></span><span class="ib">' + i('search') + '</span><span class="ib">' + i('share') + '</span></div>' +
    '<div class="big"><h1>Hechos</h1><p>3 resueltos · 1 cancelado esta semana</p></div>' +
    '<div class="stats"><div><strong>1</strong><span>Hoy</span></div><div><strong>3</strong><span>7 días</span></div><div><strong>7,5</strong><span>Horas 7 d</span></div><div><strong>4</strong><span>Este mes</span></div></div>' +
    '<div class="chips"><span class="chip sel ci">' + i('check') + 'Todos</span><span class="chip">Resueltos <b>3</b></span><span class="chip">Cancelados <b>1</b></span><span class="chip ci">' + i('calendar_month') + 'Noviembre</span></div>' +
    '<div class="sec">Hoy<small>3,5 h</small></div><div class="grp">' +
    aviso({ e: 'resuelto', t: 'Ampliación de 4 detectores volumétricos', c: 'Joyería Duarte · Instalación · Alarma', ref: 'AV-0006', h: '11:23', tec: 'MR', el: 'Resuelto · 3,5 h' }) +
    '</div><div class="sec">Ayer<small>4 h</small></div><div class="grp">' +
    aviso({ e: 'resuelto', t: 'Revisión de cámaras domo del parking', c: 'Hotel Mirador · Mantenimiento · CCTV', ref: 'AV-0098', h: '18:10', tec: 'LP', el: 'Resuelto · 2,5 h' }) +
    aviso({ e: 'cancelado', t: 'Cambio de batería de la central', c: 'Bar Los Arcos · Avería · Alarma', ref: 'AV-0097', h: '12:40', tec: 'Y', el: 'Cancelado' }) +
    '</div></div>' + nav('hechos');
};

NOMBRES.equipo = '8 · Equipo';
PANT.equipo = function () {
  return '<div class="pg">' +
    '<div class="bar"><span style="flex:1"></span><span class="ib">' + i('search') + '</span></div>' +
    '<div class="big"><h1>Equipo</h1><p>3 técnicos · 4 avisos abiertos asignados</p></div>' +
    '<div class="grp">' +
    '<div class="it">' + ava('Y', 1) + '<div class="tx"><div class="hl">Yo</div><div class="sp">2 abiertos · <span style="color:var(--error)">1 vencido</span></div></div>' + i('chevron_right') + '</div>' +
    '<div class="it">' + ava('LP', 1) + '<div class="tx"><div class="hl">Laura Pinto</div><div class="sp">1 abierto · mañana 08:00</div></div><span class="ib ton">' + i('call') + '</span></div>' +
    '<div class="it">' + ava('MR', 1) + '<div class="tx"><div class="hl">Marcos Rey</div><div class="sp">1 abierto · hoy 11:00</div></div><span class="ib ton">' + i('call') + '</span></div>' +
    '</div><div class="sec">Sin asignar</div><div class="grp">' +
    '<div class="it">' + ava(null, 1) + '<div class="tx"><div class="hl">Presupuesto de videoportero</div><div class="sp">Comunidad C/ Olmo 8 · lun 16 nov</div></div><span class="txtbtn">Asignar</span></div>' +
    '</div></div>' +
    nav('equipo', 0, 'person_add');
};

NOMBRES.ajustes = '9 · Ajustes';
PANT.ajustes = function () {
  return '<div class="pg">' +
    '<div class="bar"><span style="flex:1"></span><span class="ib">' + i('search') + '</span></div><div class="big"><h1>Ajustes</h1></div>' +
    '<div class="sec" style="padding-top:4px">Correo de la empresa</div><div class="grp">' +
    it('mail', 'avisos@empresa.es', 'Escuchando · comprobado hace 3 min', '<span class="swt"></span>') +
    it('sync', 'Comprobar ahora', 'Los correos nuevos entran como avisos') +
    '</div><div class="sec">Avisos</div><div class="grp">' +
    it('notifications', 'Notificaciones', 'Nuevos, urgentes y recordatorios', '<span class="swt"></span>') +
    it('alarm', 'Recordatorio antes de la cita', '20 minutos antes') +
    it('tag', 'Numeración', 'El próximo será AV-0007') +
    '</div><div class="sec">Apariencia</div><div class="grp">' +
    it('dark_mode', 'Tema', 'Automático (como el móvil)') +
    it('palette', 'Colores del fondo de pantalla', 'Material You', '<span class="swt off"></span>') +
    '</div><div class="sec">Datos</div><div class="grp">' +
    it('backup', 'Copia de seguridad', 'Guardar o recuperar todos los avisos') +
    '</div></div>' + nav('ajustes');
};

async function main() {
  var b = await chromium.launch();
  var hechas = {};
  for (var tema of ['light', 'dark']) {
    for (var k in PANT) {
      var lock = k === 'avisos';
      var html = '<!doctype html><html class="' + (tema === 'dark' ? 'dark' : '') + '"><head><meta charset="utf-8"><style>' + CSS + '</style></head><body' + (lock ? ' class="lock"' : '') + '>' +
        '<div class="sb"><span>9:41</span><span>' + i('signal_cellular_alt', 1) + i('wifi', 1) + i('battery_full', 1) + '</span></div>' +
        PANT[k]() + '<div class="gn"' + (lock ? ' style="--on-surface:#fff"' : '') + '></div></body></html>';
      var f = path.join(DIR, '_maqueta-' + k + '-' + tema + '.html');
      fs.writeFileSync(f, html);
      var p = await b.newPage({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2.625 });
      await p.goto('file://' + f);
      await p.evaluate(function () { return document.fonts.ready; });
      await p.waitForTimeout(150);
      var png = path.join(OUT, k + '-' + tema + '.png');
      await p.screenshot({ path: png });
      await p.close();
      (hechas[tema] = hechas[tema] || []).push([k, png]);
    }
  }
  for (var t in hechas) {
    var osc = t === 'dark';
    var hoja = '<body style="margin:0;padding:28px;background:' + (osc ? '#1c1c1e' : '#e9e9ee') + ';color:' + (osc ? '#fff' : '#111') + ';font-family:Roboto,Arial,sans-serif">' +
      '<h1 style="margin:0 0 20px;font-size:26px">Propuesta v3 · menú flotante e icono nuevo — ' + (osc ? 'tema oscuro' : 'tema claro') + '</h1>' +
      '<div style="display:grid;grid-template-columns:repeat(3,340px);gap:22px">' +
      hechas[t].map(function (x) {
        return '<figure style="margin:0"><img style="width:340px;display:block;border-radius:22px;box-shadow:0 2px 10px rgba(0,0,0,.25)" src="data:image/png;base64,' +
          fs.readFileSync(x[1]).toString('base64') + '"><figcaption style="text-align:center;font-size:17px;margin-top:8px;font-weight:500">' + NOMBRES[x[0]] + '</figcaption></figure>';
      }).join('') + '</div></body>';
    var p2 = await b.newPage({ viewport: { width: 3 * 340 + 2 * 22 + 56, height: 800 }, deviceScaleFactor: 1.5 });
    await p2.setContent(hoja);
    await p2.waitForTimeout(200);
    await p2.screenshot({ path: path.join(OUT, 'hoja-' + t + '.png'), fullPage: true });
    await p2.close();
  }
  await b.close();
  console.log('Maquetas en ' + OUT);
}
main().catch(function (e) { console.error(e); process.exit(1); });
