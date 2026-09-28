/* api/buzon.js — buzón de avisos para los Atajos del iPhone.

   En el iPhone un atajo no puede meter nada dentro de la app: si abre un
   enlace, cae en Safari, que guarda sus datos aparte de la app de la pantalla
   de inicio. Así que el atajo deja aquí el aviso y la app lo recoge sola la
   próxima vez que se abre.

     POST /api/buzon      (cabecera X-Token)  deja un aviso
     GET  /api/buzon      (cabecera X-Token)  recoge y vacía lo que haya
     GET  /api/buzon?ojear=1                  mira sin vaciar

   Detrás hay un Redis de Upstash, que es lo que ofrece Vercel en su
   Marketplace: da las variables KV_REST_API_URL y KV_REST_API_TOKEN. Nada más
   que configurar. El token del buzón no se guarda en ningún sitio: la clave
   de la lista se saca de su huella, así que el servidor no sabe cuáles son
   los buzones que existen ni puede enseñarlos.

   Sin las variables puestas, el buzón responde 503 explicando qué falta, que
   es más útil que fallar en silencio. */

var MAX_CUERPO = 16 * 1024;     // un aviso escrito no ocupa más
var MAX_COLA = 100;             // si nadie abre la app, no se acumula sin fin
var VIDA = 60 * 60 * 24 * 30;   // lo que no se recoja en un mes, se borra
var MAX_ENTREGA = 50;

function almacen() {
  var url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  var token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  return url && token ? { url: url.replace(/\/+$/, ''), token: token } : null;
}

function redis(kv, orden) {
  return fetch(kv.url, {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + kv.token, 'Content-Type': 'application/json' },
    body: JSON.stringify(orden)
  }).then(function (r) {
    return r.text().then(function (t) {
      var j = null;
      try { j = JSON.parse(t); } catch (e) {}
      if (!r.ok || (j && j.error)) {
        throw new Error('Redis ' + r.status + ': ' + ((j && j.error) || t).slice(0, 200));
      }
      return j ? j.result : null;
    });
  });
}

/* La clave sale del token, nunca al revés. */
function claveDe(token) {
  var datos = new TextEncoder().encode('avisos-buzon-v1:' + token);
  return crypto.subtle.digest('SHA-256', datos).then(function (buf) {
    var hex = Array.prototype.map.call(new Uint8Array(buf), function (b) {
      return b.toString(16).padStart(2, '0');
    }).join('');
    return 'buzon:' + hex;
  });
}

function tokenDe(req) {
  var t = req.headers['x-token'] || '';
  if (!t) {
    var auth = req.headers.authorization || '';
    if (/^Bearer\s+/i.test(auth)) t = auth.replace(/^Bearer\s+/i, '');
  }
  return String(t).trim();
}

function leerCuerpo(req) {
  /* Vercel ya deja el cuerpo interpretado en req.body casi siempre; si llega
     en crudo (el atajo manda texto plano), se junta a mano. */
  if (typeof req.body === 'string') return Promise.resolve(req.body);
  if (req.body && typeof req.body === 'object') return Promise.resolve(JSON.stringify(req.body));
  return new Promise(function (res, rej) {
    var trozos = [], total = 0;
    req.on('data', function (c) {
      total += c.length;
      if (total > MAX_CUERPO) { rej(new Error('demasiado-largo')); return; }
      trozos.push(c);
    });
    req.on('end', function () { res(Buffer.concat(trozos).toString('utf8')); });
    req.on('error', rej);
  });
}

/* El atajo puede mandar el aviso como texto pelado o como JSON
   {"texto": "…"}. Las dos formas acaban en lo mismo. */
function textoDelAviso(cuerpo) {
  var t = String(cuerpo || '').trim();
  if (!t) return '';
  if (t.charAt(0) === '{') {
    try {
      var j = JSON.parse(t);
      var v = j.texto || j.aviso || j.text || j.titulo || '';
      if (typeof v === 'string' && v.trim()) return v.trim();
    } catch (e) { /* no era JSON: se toma tal cual */ }
  }
  return t;
}

function responder(res, codigo, cuerpo) {
  res.statusCode = codigo;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(JSON.stringify(cuerpo));
}

module.exports = async function (req, res) {
  if (req.method === 'OPTIONS') { res.statusCode = 204; res.end(); return; }
  if (req.method !== 'POST' && req.method !== 'GET') {
    return responder(res, 405, { error: 'metodo', mensaje: 'Solo POST (dejar) y GET (recoger).' });
  }

  var kv = almacen();
  if (!kv) {
    return responder(res, 503, {
      error: 'sin-almacen',
      mensaje: 'Falta conectar un Redis en Vercel (Storage → Upstash). Se necesitan KV_REST_API_URL y KV_REST_API_TOKEN.'
    });
  }

  var token = tokenDe(req);
  if (token.length < 16) {
    return responder(res, 401, { error: 'token', mensaje: 'Falta la cabecera X-Token del buzón.' });
  }

  try {
    var clave = await claveDe(token);

    if (req.method === 'POST') {
      var cuerpo;
      try { cuerpo = await leerCuerpo(req); }
      catch (e) { return responder(res, 413, { error: 'largo', mensaje: 'El aviso es demasiado largo.' }); }

      var texto = textoDelAviso(cuerpo);
      if (!texto) return responder(res, 400, { error: 'vacio', mensaje: 'No venía ningún aviso.' });
      if (texto.length > MAX_CUERPO) texto = texto.slice(0, MAX_CUERPO);

      var sobre = JSON.stringify({ texto: texto, recibido: new Date().toISOString() });
      var largo = await redis(kv, ['RPUSH', clave, sobre]);
      /* Se recorta por la cola y se le pone fecha de caducidad a la lista. */
      if (largo > MAX_COLA) await redis(kv, ['LTRIM', clave, -MAX_COLA, '-1']);
      await redis(kv, ['EXPIRE', clave, String(VIDA)]);
      return responder(res, 200, { ok: true, esperando: Math.min(largo, MAX_COLA) });
    }

    /* GET: recoger. Se saca de la lista en una sola orden para que dos
       recogidas a la vez no se lleven el mismo aviso dos veces. */
    var url = new URL(req.url, 'http://x');
    if (url.searchParams.get('ojear')) {
      var cuantos = await redis(kv, ['LLEN', clave]);
      return responder(res, 200, { ok: true, esperando: Number(cuantos) || 0 });
    }

    var crudos = await redis(kv, ['LPOP', clave, String(MAX_ENTREGA)]) || [];
    var avisos = crudos.map(function (s) {
      try { return JSON.parse(s); } catch (e) { return { texto: String(s), recibido: '' }; }
    }).filter(function (a) { return a && a.texto; });

    var quedan = avisos.length === MAX_ENTREGA ? Number(await redis(kv, ['LLEN', clave])) || 0 : 0;
    return responder(res, 200, { ok: true, avisos: avisos, quedan: quedan });
  } catch (e) {
    return responder(res, 502, { error: 'almacen', mensaje: String(e && e.message || e).slice(0, 200) });
  }
};
