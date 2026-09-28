/* sw.js — caché de la aplicación para que funcione sin conexión.
   Sirve lo guardado al instante y refresca por detrás; cuando hay una
   versión nueva lista, se avisa a la página para que lo diga. */
var VERSION = 'avisos-v1.8.1';
var SHELL = [
  './',
  './index.html',
  './manifest.webmanifest',
  './assets/css/app.css',
  './assets/js/fallos.js',
  './assets/js/db.js',
  './assets/js/nativo.js',
  './assets/js/store.js',
  './assets/js/ics.js',
  './assets/js/correo.js',
  './assets/js/ui.js',
  './assets/js/swipe.js',
  './assets/js/visor.js',
  './assets/js/atajos.js',
  './assets/js/buzon.js',
  './assets/js/views.js',
  './assets/js/app.js',
  './assets/icons/favicon.svg',
  './assets/icons/apple-touch-icon.png',
  './assets/icons/icon-192.png',
  './assets/icons/icon-512.png',
  './assets/icons/icon-maskable-512.png'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(VERSION).then(function (c) { return c.addAll(SHELL); })
    /* Sin skipWaiting: la página avisa y el usuario decide cuándo pasar a
       la versión nueva, para no cambiarle el suelo mientras trabaja. */
  );
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.map(function (k) {
        return k === VERSION ? null : caches.delete(k);
      }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('message', function (e) {
  if (e.data && e.data.tipo === 'saltar') self.skipWaiting();
});

function guardar(peticion, respuesta) {
  if (!respuesta || respuesta.status !== 200 || respuesta.type !== 'basic') return respuesta;
  var copia = respuesta.clone();
  caches.open(VERSION).then(function (c) { c.put(peticion, copia); });
  return respuesta;
}

/* ¿Es una de las piezas con las que se monta la app? */
function esDelArmazon(url) {
  var base = self.location.pathname.replace(/sw\.js$/, '');
  var rel = './' + url.pathname.slice(base.length);
  return SHELL.indexOf(rel) !== -1 || rel === './';
}

self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  /* El buzón de los atajos habla con el servidor: guardarlo en caché
     devolvería siempre la misma respuesta y no entraría ningún aviso. */
  if (url.pathname.indexOf('/api/') === 0) return;

  /* La página y sus piezas salen SIEMPRE de la misma versión guardada.
     Mezclar una página recién bajada con los .js viejos de la caché deja la
     app a medias —secciones que no aparecen, botones que no hacen nada—, y
     eso es peor que ir una versión por detrás. La versión entera se cambia de
     golpe cuando el usuario acepta actualizar. */
  if (req.mode === 'navigate') {
    e.respondWith(
      caches.open(VERSION)
        .then(function (c) { return c.match('./index.html'); })
        .then(function (r) {
          return r || fetch(req).then(function (res) { return guardar('./index.html', res); });
        })
        .catch(function () { return fetch(req); })
    );
    return;
  }

  if (esDelArmazon(url)) {
    e.respondWith(
      caches.open(VERSION)
        .then(function (c) { return c.match(req); })
        .then(function (r) { return r || fetch(req).then(function (res) { return guardar(req, res); }); })
    );
    return;
  }

  /* Lo de fuera del armazón (iconos sueltos, lo que se añada): lo guardado al
     momento y, por detrás, se refresca. */
  e.respondWith(
    caches.match(req).then(function (cacheado) {
      var red = fetch(req).then(function (res) {
        return guardar(req, res);
      }).catch(function () { return cacheado; });
      /* Si respondemos de caché, el refresco hay que sostenerlo aparte o el
         navegador puede cortarlo al dar la respuesta por terminada. */
      if (cacheado) { e.waitUntil(red); return cacheado; }
      return red;
    })
  );
});
