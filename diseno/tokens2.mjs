import * as M from '@material/material-color-utilities';
const seed = M.argbFromHex('#F27427');
const hct = M.Hct.fromInt(seed);
const hex = M.hexFromArgb;
const P = M.TonalPalette.fromInt(seed);
const S = M.TonalPalette.fromHueAndChroma(hct.hue, 16);
const T = M.TonalPalette.fromHueAndChroma((hct.hue + 60) % 360, 24);
const N = M.TonalPalette.fromHueAndChroma(hct.hue, 0);
const NV = M.TonalPalette.fromHueAndChroma(hct.hue, 1);
const Er = M.TonalPalette.fromHueAndChroma(25, 84);
const light = { primary:[P,50], 'on-primary':[P,100], 'primary-container':[P,90], 'on-primary-container':[P,30],
  secondary:[S,40], 'on-secondary':[S,100], 'secondary-container':[S,90], 'on-secondary-container':[S,30],
  tertiary:[T,40], 'on-tertiary':[T,100], 'tertiary-container':[T,90], 'on-tertiary-container':[T,30],
  error:[Er,40], 'on-error':[Er,100], 'error-container':[Er,90], 'on-error-container':[Er,30],
  surface:[N,98], 'on-surface':[N,10], 'on-surface-variant':[NV,30], 'surface-container-lowest':[N,100], 'surface-container-low':[N,96],
  'surface-container':[N,94], 'surface-container-high':[N,92], 'surface-container-highest':[N,90], outline:[NV,50], 'outline-variant':[NV,80],
  'inverse-surface':[N,20], 'inverse-on-surface':[N,95], 'inverse-primary':[P,80] };
const dark = { primary:[P,80], 'on-primary':[P,20], 'primary-container':[P,30], 'on-primary-container':[P,90],
  secondary:[S,80], 'on-secondary':[S,20], 'secondary-container':[S,30], 'on-secondary-container':[S,90],
  tertiary:[T,80], 'on-tertiary':[T,20], 'tertiary-container':[T,30], 'on-tertiary-container':[T,90],
  error:[Er,80], 'on-error':[Er,20], 'error-container':[Er,30], 'on-error-container':[Er,90],
  surface:[N,6], 'on-surface':[N,90], 'on-surface-variant':[NV,80], 'surface-container-lowest':[N,4], 'surface-container-low':[N,10],
  'surface-container':[N,12], 'surface-container-high':[N,17], 'surface-container-highest':[N,22], outline:[NV,60], 'outline-variant':[NV,30],
  'inverse-surface':[N,90], 'inverse-on-surface':[N,20], 'inverse-primary':[P,40] };
const estados = { pendiente:['#7A7F87',6], programado:['#00897B'], curso:['#1E6FE0'], espera:['#8E5CD9'], resuelto:['#2E9D52'], cancelado:['#8A8A8A',4] };
let css = '';
for (const [sel, map, d] of [[':root', light, false], ['.dark', dark, true]]) {
  css += sel + '{\n';
  for (const [k, [p, t]] of Object.entries(map)) css += `  --${k}:${hex(p.tone(t))};\n`;
  for (const [k, [v, c]] of Object.entries(estados)) {
    const h = M.Hct.fromInt(M.argbFromHex(v));
    const p = c ? M.TonalPalette.fromHueAndChroma(h.hue, c) : M.TonalPalette.fromHueAndChroma(h.hue, 40);
    css += `  --e-${k}:${hex(p.tone(d ? 80 : 40))};--e-${k}-on:${hex(p.tone(d ? 20 : 100))};--e-${k}-c:${hex(p.tone(d ? 30 : 90))};--e-${k}-onc:${hex(p.tone(d ? 90 : 10))};\n`;
  }
  css += '}\n';
}
console.log(css);
