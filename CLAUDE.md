# Contexto para trabajar en esta app

Lo que una sesión nueva necesita saber antes de tocar nada. El funcionamiento
para el usuario está en el `README.md`; esto es cómo se trabaja en el código.

Habla en español. La app la usa un instalador de sistemas de seguridad en su
iPhone, instalada desde Safari en la pantalla de inicio.

## Rama y despliegue

- Se trabaja **solo** en `claude/app-gestion-avisos-408zhn`. Es la rama por
  defecto de GitHub y la que Vercel publica en producción: cada push despliega.
- No se usa `main`. No cambies la rama por defecto en GitHub sin cambiar antes
  la de producción en Vercel: lo que se subiera iría a una rama que solo hace
  despliegues de prueba y el iPhone dejaría de recibir versiones.
- Lo que no es web va en `.vercelignore` para que no se publique.

## Versiones

- Si cambias cualquier fichero que sirve la web, sube `VERSION` en `sw.js` y
  `APP_VERSION` en `assets/js/app.js` **al mismo número**. Sin eso, los móviles
  instalados no se enteran del cambio.
- Si añades un `.js`, va en `SHELL` de `sw.js` y en `index.html`, en su orden.
- El service worker sirve la página y sus `.js` siempre de la misma versión, y
  el aviso de *Actualizar* no desaparece solo. Las dos cosas son a propósito
  (fallo de la 1.8.1: página nueva con `.js` viejos, secciones que no salían).

## Cómo está hecha

- Ficheros estáticos sin compilación ni dependencias. Scripts clásicos con
  espacios de nombres globales, no módulos ES.
- Los datos viven en IndexedDB del móvil (`avisos`, `tecnicos`, `fotos`,
  `ajustes`). No hay servidor salvo `api/buzon.js` (función de Vercel + Redis de
  Upstash) para los avisos que llegan desde Atajos.
- En el iPhone no hay correo → aviso, widget ni notificaciones en segundo
  plano. Eso solo existe en la APK de Android (`android/`), que se compila a
  mano con el workflow `apk.yml`.
- No montes un puente de correo en el servidor sin que el usuario lo pida
  expresamente: obliga a guardar la contraseña del correo de su empresa en un
  servidor.

## Pruebas

- Las pruebas se guardan en `tests/` y se suben con el código. Las de las
  primeras sesiones se quedaron fuera del repositorio y se perdieron.
- La web se sirve con `python3 -m http.server 8000` y se prueba con Playwright
  y el Chromium preinstalado. Los gestos táctiles, con `Input.dispatchTouchEvent`
  por CDP. El puente de Android se simula con un `window.AvisosNativo` falso
  inyectado con `addInitScript`.
- Ante un fallo que cuenta el usuario: reprodúcelo con una prueba antes de
  arreglarlo y comprueba después que pasa.

## Fallos ya resueltos (no volver a meterlos)

- Cada correo entraba dos veces: varios disparadores a la vez. `correo.js`
  serializa las pasadas (`enCurso` / `repetir`).
- La coma decimal: horas y duración son `type="text" inputmode="decimal"`, no
  `type="number"`.
- `[hidden]{display:none !important}` es global porque algunas clases con
  `display` lo anulaban.
- En el iPhone las descargas no llegan a ningún sitio con la app instalada:
  los archivos salen por `navigator.share({files})`.
- El estado de los workflows que devuelve la API de GitHub puede ir con
  retraso. Para saber si una APK está bien, bájala y mira su `APP_VERSION`.
