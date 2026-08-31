/* make-icons.js — genera los iconos PNG de la app sin dependencias externas.
   Uso: node tools/make-icons.js */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const OUT = path.join(__dirname, '..', 'assets', 'icons');
const BG = [16, 18, 21, 255];      // #101215
const FG = [242, 116, 39, 255];    // naranja de señal
const SS = 3;                       // supermuestreo para suavizar bordes

function shieldAlpha(x, y, N, inset) {
  const cx = N / 2;
  const top = N * inset;
  const bottom = N * (1 - inset);
  const h = bottom - top;
  const hw = h * 0.36;
  const shoulder = top + h * 0.46;
  if (y < top || y > bottom) return false;
  let half;
  if (y <= shoulder) {
    const r = hw * 0.34;
    if (y < top + r) {
      const dy = (top + r) - y;
      half = hw - r + Math.sqrt(Math.max(0, r * r - dy * dy));
    } else {
      half = hw;
    }
  } else {
    half = hw * Math.pow(1 - (y - shoulder) / (bottom - shoulder), 0.55);
  }
  return Math.abs(x - cx) <= half;
}

function distSeg(px, py, ax, ay, bx, by) {
  const vx = bx - ax, vy = by - ay;
  const wx = px - ax, wy = py - ay;
  const len = vx * vx + vy * vy;
  let t = len ? (wx * vx + wy * vy) / len : 0;
  t = Math.max(0, Math.min(1, t));
  const dx = px - (ax + t * vx), dy = py - (ay + t * vy);
  return Math.sqrt(dx * dx + dy * dy);
}

function checkAlpha(x, y, N, inset) {
  const top = N * inset;
  const bottom = N * (1 - inset);
  const h = bottom - top;
  const cx = N / 2;
  const grosor = h * 0.085;
  const a = [cx - h * 0.155, top + h * 0.44];
  const b = [cx - h * 0.045, top + h * 0.56];
  const c = [cx + h * 0.17, top + h * 0.30];
  return distSeg(x, y, a[0], a[1], b[0], b[1]) <= grosor ||
         distSeg(x, y, b[0], b[1], c[0], c[1]) <= grosor;
}

function render(N, inset, fondoCompleto) {
  const px = Buffer.alloc(N * N * 4);
  const radio = fondoCompleto ? 0 : N * 0.16;
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      let acc = [0, 0, 0, 0];
      for (let sy = 0; sy < SS; sy++) {
        for (let sx = 0; sx < SS; sx++) {
          const fx = x + (sx + 0.5) / SS;
          const fy = y + (sy + 0.5) / SS;
          let col;
          if (!fondoCompleto && !enRedondeado(fx, fy, N, radio)) col = [0, 0, 0, 0];
          else if (shieldAlpha(fx, fy, N, inset) && !checkAlpha(fx, fy, N, inset)) col = FG;
          else col = BG;
          acc[0] += col[0] * col[3] / 255;
          acc[1] += col[1] * col[3] / 255;
          acc[2] += col[2] * col[3] / 255;
          acc[3] += col[3];
        }
      }
      const n = SS * SS;
      const alfa = acc[3] / n;
      const o = (y * N + x) * 4;
      px[o]     = alfa ? Math.round(acc[0] / n / (alfa / 255)) : 0;
      px[o + 1] = alfa ? Math.round(acc[1] / n / (alfa / 255)) : 0;
      px[o + 2] = alfa ? Math.round(acc[2] / n / (alfa / 255)) : 0;
      px[o + 3] = Math.round(alfa);
    }
  }
  return png(N, N, px);
}

function enRedondeado(x, y, N, r) {
  if (x >= r && x <= N - r) return y >= 0 && y <= N;
  if (y >= r && y <= N - r) return x >= 0 && x <= N;
  const cx = x < r ? r : N - r;
  const cy = y < r ? r : N - r;
  return (x - cx) ** 2 + (y - cy) ** 2 <= r * r;
}

function png(w, h, rgba) {
  const filas = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    filas[y * (w * 4 + 1)] = 0;
    rgba.copy(filas, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  }
  const idat = zlib.deflateSync(filas, { level: 9 });
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    trozo('IHDR', ihdr), trozo('IDAT', idat), trozo('IEND', Buffer.alloc(0))
  ]);
}

function trozo(tipo, datos) {
  const len = Buffer.alloc(4); len.writeUInt32BE(datos.length, 0);
  const cuerpo = Buffer.concat([Buffer.from(tipo, 'ascii'), datos]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(cuerpo) >>> 0, 0);
  return Buffer.concat([len, cuerpo, crc]);
}

let TABLA = null;
function crc32(buf) {
  if (!TABLA) {
    TABLA = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
      TABLA[n] = c;
    }
  }
  let c = -1;
  for (let i = 0; i < buf.length; i++) c = TABLA[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return c ^ -1;
}

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, 'icon-192.png'), render(192, 0.17, false));
fs.writeFileSync(path.join(OUT, 'icon-512.png'), render(512, 0.17, false));
fs.writeFileSync(path.join(OUT, 'icon-maskable-512.png'), render(512, 0.27, true));
console.log('Iconos generados en', OUT);
