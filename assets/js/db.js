/* db.js — capa mínima sobre IndexedDB.
   Almacenes: avisos, tecnicos, fotos, ajustes. Todo vive en el móvil. */
(function (global) {
  'use strict';

  var NAME = 'avisos-db';
  var VERSION = 1;
  var dbp = null;

  function open() {
    if (dbp) return dbp;
    dbp = new Promise(function (resolve, reject) {
      var req = indexedDB.open(NAME, VERSION);
      req.onupgradeneeded = function (ev) {
        var db = ev.target.result;
        if (!db.objectStoreNames.contains('avisos')) {
          var av = db.createObjectStore('avisos', { keyPath: 'id' });
          av.createIndex('fecha', 'fecha');
          av.createIndex('estado', 'estado');
          av.createIndex('asignadoA', 'asignadoA');
        }
        if (!db.objectStoreNames.contains('tecnicos')) {
          db.createObjectStore('tecnicos', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('fotos')) {
          var fo = db.createObjectStore('fotos', { keyPath: 'id' });
          fo.createIndex('avisoId', 'avisoId');
        }
        if (!db.objectStoreNames.contains('ajustes')) {
          db.createObjectStore('ajustes', { keyPath: 'k' });
        }
      };
      req.onsuccess = function () { resolve(req.result); };
      req.onerror = function () { reject(req.error); };
      req.onblocked = function () { reject(new Error('Base de datos bloqueada por otra pestaña')); };
    });
    return dbp;
  }

  function tx(store, mode, fn) {
    return open().then(function (db) {
      return new Promise(function (resolve, reject) {
        var t = db.transaction(store, mode);
        var out;
        t.oncomplete = function () { resolve(out); };
        t.onerror = function () { reject(t.error); };
        t.onabort = function () { reject(t.error || new Error('Transacción cancelada')); };
        out = fn(t.objectStore(store), t);
        if (out && typeof out.then === 'function') {
          out.then(function (v) { out = v; }, reject);
        }
      });
    });
  }

  function req2promise(r) {
    return new Promise(function (res, rej) {
      r.onsuccess = function () { res(r.result); };
      r.onerror = function () { rej(r.error); };
    });
  }

  var DB = {
    getAll: function (store) {
      var acc;
      return tx(store, 'readonly', function (s) {
        return req2promise(s.getAll()).then(function (r) { acc = r; return r; });
      }).then(function () { return acc || []; });
    },
    get: function (store, key) {
      var acc;
      return tx(store, 'readonly', function (s) {
        return req2promise(s.get(key)).then(function (r) { acc = r; return r; });
      }).then(function () { return acc; });
    },
    getByIndex: function (store, index, value) {
      var acc;
      return tx(store, 'readonly', function (s) {
        return req2promise(s.index(index).getAll(value)).then(function (r) { acc = r; return r; });
      }).then(function () { return acc || []; });
    },
    put: function (store, value) {
      return tx(store, 'readwrite', function (s) { s.put(value); }).then(function () { return value; });
    },
    putMany: function (store, values) {
      return tx(store, 'readwrite', function (s) {
        values.forEach(function (v) { s.put(v); });
      }).then(function () { return values.length; });
    },
    del: function (store, key) {
      return tx(store, 'readwrite', function (s) { s.delete(key); });
    },
    delByIndex: function (store, index, value) {
      return tx(store, 'readwrite', function (s) {
        return req2promise(s.index(index).getAllKeys(value)).then(function (keys) {
          keys.forEach(function (k) { s.delete(k); });
        });
      });
    },
    clear: function (store) {
      return tx(store, 'readwrite', function (s) { s.clear(); });
    },
    clearAll: function () {
      return Promise.all(['avisos', 'tecnicos', 'fotos', 'ajustes'].map(function (s) {
        return DB.clear(s);
      }));
    },
    estimate: function () {
      if (navigator.storage && navigator.storage.estimate) return navigator.storage.estimate();
      return Promise.resolve(null);
    }
  };

  global.DB = DB;
})(window);
