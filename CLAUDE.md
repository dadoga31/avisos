# Contexto para trabajar en esta app

Lo que una sesión nueva necesita saber antes de tocar nada. El funcionamiento
para el usuario está en el `README.md`; esto es cómo se trabaja en el código.

Habla en español. La app la usa un instalador de sistemas de seguridad en su
móvil **Android**, instalada con la APK de este repo.

## Rumbo: app nativa de Android (decidido en noviembre de 2026)

- El iPhone se ha dejado. No hay que mantener la PWA de Safari ni nada de iOS.
- La app tiene que ser **nativa**: Kotlin + Jetpack Compose + Material 3, no
  una web dentro de un WebView. Se reescribe la interfaz y los datos; el
  servicio de correo, el widget y las notificaciones (Java en `android/`) se
  aprovechan.
- Los avisos de la APK web actual no se migran: el usuario acepta perderlos.
- El diseño aprobado está en `diseno/`: maquetas Material 3 Expressive con
  menú flotante, paleta, icono (escudo con ✓) y los criterios de uso que tiene
  que cumplir. Léelo antes de escribir una pantalla.
- Se compila aquí: el entorno «avisos» instala el Android SDK al arrancar
  (`/root/android-sdk`, y escribe `android/local.properties`). Luego
  `cd android && ./gradlew assembleRelease`. El workflow `apk.yml` además
  publica la release `apk`: no lo lances sin que el usuario lo pida.
- Mientras dure la reescritura, lo que se cuenta abajo sobre la web y el diseño
  de iPhone describe el código que hay, no el que se quiere. No añadas nada
  nuevo de iOS (Liquid Glass, Atajos, barra de estado de Safari, etc.).

## App nativa (`android/`)

- Kotlin + Jetpack Compose + Material 3 (BOM 2024.12.01, M3 1.3.1), Room 2.6.1
  con KSP, AGP 8.5.2, compileSdk 34, minSdk 26. Las versiones van atadas a esa
  cadena de herramientas: no las subas sin necesidad.
- `datos/`: `Modelo.kt` (mismos campos y valores que la web, para poder
  importar sus copias), `BaseDatos.kt` (Room), `Reglas.kt` (reglas puras:
  grupos, línea de tiempo y bandejas de la Agenda, panel de Hechos, siguiente
  aviso, búsqueda, referencias, textos),
  `Repositorio.kt` (todo cambio pasa por aquí: referencia, cierre, inicio,
  widget) y `Ajustes.kt`. `ui/`: `Tema.kt` (paleta de `diseno/tokens2.css` y
  colores de estado), `Piezas.kt` (rejilla de puntos, marca, botones
  redondos, cifras, anillo, interruptor, segmentado, relojes), `Armazon.kt`
  (dock flotante), `Agenda.kt`, `Ficha.kt`, `Formulario.kt`, `Secciones.kt`
  (Hechos, Equipo, Ajustes), `Navegacion.kt`. `correo/` y `WidgetAvisos.java`
  vienen de antes.
- Las pantallas no tienen estado propio de datos: reciben avisos y acciones.
  Así se prueban y se capturan sin base de datos.
- Pruebas: `cd android && ./gradlew testDebugUnitTest`. `ReglasTest` (JVM),
  `RepositorioTest` (Robolectric + Room en memoria), `ActualizadorTest`, `GestosTest` (Compose:
  deslizar para cerrar, la línea de tiempo, la semana y el dock) y
  `CapturasTest` (Roborazzi, con `-Proborazzi.test.record=true`): deja en
  `diseno/capturas-app/` cada pantalla en claro y oscuro a tamaño Pixel 7.
  Tras un cambio visual, mira esas capturas. Sin «ñ» ni tildes en los nombres
  de las pruebas: el compilador no sabe escribir esas clases en disco.
- Al publicar una APK nueva sube `versionCode` y `versionName` en
  `app/build.gradle`. Se firma con `keystore/depuracion.keystore`, la de
  siempre: con otra firma Android no deja actualizar la app instalada.
- Correo → aviso (3.1.0): `ServicioCorreo` (Java) descarga y guarda en
  `AlmacenCorreo`; `ProcesadorCorreo` los convierte en el momento, uno a uno
  y con cerrojo (no volver a dejar dos pasadas a la vez: así entraban
  duplicados), y `Notificaciones` saca una por aviso con «Me lo quedo»,
  «Llamar» y «Ver». `ConversorCorreo` es `assets/js/correo.js` traducido:
  `ConversorCorreoTest` comprueba que da lo mismo que la web; si cambias una
  regla, cámbiala en los dos o borra la web. Los adjuntos se copian a
  `filesDir/adjuntos/<aviso>/` y se abren con FileProvider.
- POP3 se consulta cada 2 minutos (`ESPERA_POP3`, lo pidió el usuario); con
  IMAP es al instante (IDLE). No bajar de 1 minuto: hay servidores de empresa
  que bloquean la cuenta si inicia sesión tan a menudo.
- Actualizar desde la app (3.6.0): el workflow publica en la release «apk»,
  junto a `avisos.apk`, un `version.json` con el `versionCode` y el
  `versionName` de `app/build.gradle`. `Actualizador` lo mira al abrir y al
  volver a la app (como mucho cada 30 min) y dos veces al día con una alarma
  inexacta (`ReceptorActualizacion`); si hay una más nueva saca una
  notificación por versión y una franja en la Agenda y en Ajustes. «Actualizar»
  baja la APK a `cache/actualizacion/`, comprueba que es de esta app y más
  nueva, y abre el instalador del sistema (FileProvider). La primera vez
  Android pide permitir que Avisos instale apps (`REQUEST_INSTALL_PACKAGES`):
  al volver de ese ajuste sigue solo. Android siempre pide el toque en
  «Instalar»; eso no se puede quitar. Si cambias la release o su nombre, cambia
  también las URL de `Actualizador`: las versiones instaladas las tienen fijas.
- La base de datos va por la versión 2. Cada cambio de esquema necesita su
  migración (aquí `AutoMigration`) y su prueba: quien actualiza no puede
  perder sus avisos.
- Recordatorios (3.2.0): `Recordatorios.reprogramar` pone una alarma exacta
  por aviso mío o sin asignar, abierto, sin empezar y con fecha y hora
  (`Reglas.momentoRecordatorio`). Se llama tras cada cambio de datos (desde
  `AvisosApp`), al cambiar la antelación y desde `ReceptorHora` (arranque,
  cambio de hora o zona, actualización). El receptor comprueba que la alarma
  sigue valiendo antes de avisar. «Avisar en 15 min» usa otra alarma (otro
  Uri): si compartiera la de siempre, el siguiente reprogramar la borraría.
  Exacta gracias a `USE_EXACT_ALARM`, que vale porque la app no está en Play.
- Copia de seguridad (3.3.0): `CopiaSeguridad` escribe y lee el formato de
  la web («avisos-backup», ficheros en base64), versión 2 la nativa y 1 la
  web; `Repositorio.importar` añade lo que falte. `CopiaTest` recupera una
  copia real generada con `store.js` (`src/test/resources/copia-web.json`):
  horas en lista, correo de origen, materiales y el «Yo» de la web.
- Fotos (3.3.0): `Fotos` reduce a 1600 px, endereza por EXIF y guarda JPEG en
  `adjuntos/<aviso>/`. La cámara va por `TakePicture` con un fichero en
  `cache/camara/` (FileProvider): sin permiso de cámara. Si alguna librería
  metiera `CAMERA` en el manifiesto, habría que pedirlo antes de abrirla.
- Estilo (3.4.0), pedido por el usuario: minimalista y «tech futurista» con
  su paleta Off-White #F9F9F9, Matte Black #28282B y Red Orange #FF4200. La
  interfaz es neutra; el naranja va en acciones y en lo que está en curso.
  #FF4200 no llega al contraste mínimo para texto pequeño (3,3:1 sobre el
  Off-White): los botones naranjas llevan letra casi negra y el texto naranja
  pequeño usa `Superficies.acentoTexto`. Space Grotesk (títulos, cifras,
  botones) y JetBrains Mono (referencias, horas, etiquetas en mayúsculas), en
  `res/font` con su licencia OFL en `android/licencias-fuentes/`. Líneas finas
  en vez de sombras (`Modifier.tarjeta`). Menú y barra de la ficha de cristal
  con Haze 1.0.2 (desenfoque en Android 12+, translúcido antes).
- UI «tech minimal» (3.5.0): el usuario pidió cambiar la interfaz entera, no
  solo los colores (la 3.4.0 seguía con las listas de siempre). Concepto
  aprobado antes de escribirla. Fondo con rejilla de puntos fija
  (`fondoPagina`), marca «■ SECCIÓN / extra» arriba, etiquetas en mono y
  mayúsculas (`Dato`), cifras en fila con líneas (`FilaKpi`) y tarjetas
  oscuras con halo para lo importante. Dock de solo iconos con el + naranja en
  el centro (siempre crea un aviso; añadir técnico está en Equipo) y un punto
  que se desliza bajo la sección.
  - Agenda = línea de tiempo de un día (`Reglas.linea`, 72 dp por hora): lo
    que está en curso llega hasta ahora, lo hecho ese día sale apagado, lo
    cancelado no sale y lo que se pisa va en columnas (`Reglas.carriles`).
    Debajo, «Sin hora» (`sinHora`), «Sin fecha» (`sinFecha`) y «Próximos»: así
    todo aviso abierto sigue saliendo en algún sitio. Vencidos en una barra
    arriba. El siguiente aviso lleva Llamar e Ir en su bloque (criterio 1).
    Tocar una cifra o la lupa la convierte en búsqueda; Atrás cierra la
    búsqueda y luego vuelve a hoy.
  - Ficha = hoja de misión: pasos del estado (tocar abre la hoja), cronómetro
    o cuenta atrás en tarjeta oscura (`Cronometro`, dígitos que ruedan),
    rejilla de datos, acciones redondas, registro (alta, inicio, notas,
    cierre) y abajo «Desliza para empezar / cerrar» (al 80 % vibra; con
    TalkBack es un botón).
  - Formulario en campos numerados 01–07, prioridad en barras de señal.
  - Relojes: `ahoraVivo` da la hora al segundo; `LocalRelojVivo = false` la
    congela en las capturas y en `GestosTest` (si no, nunca quedan quietas).
  - Los avisos de ejemplo están pensados para la línea: en curso a las 09:00,
    uno hecho a las 08:42, otro a las 11:30 y un vencido de ayer.
- Movimiento (`ui/Movimiento.kt`): muelles comunes, pulsar que encoge,
  entrada escalonada solo al abrir la pantalla, cifras que cuentan, el pulso
  de lo que está en curso, el indicador del menú que se desliza, vibración al
  pasar el umbral del deslizamiento y la ficha que crece desde su fila
  (`Modifier.compartido`, título con la misma clave en la fila y la ficha).
  Atrás predictivo activado. `LocalMovimiento` lo apaga todo: con «Quitar
  animaciones» del sistema y en las capturas (el pulso es infinito y si no la
  captura no termina).
- Widget (3.3.0): `WidgetAvisos.vistas` lo pinta con lo que deja
  `AvisosApp.publicarResumen` (hoy, vencidos, hechos, siguiente). Cada
  refresco de Android (30 min) llama a `AvisosApp.recalcular` para que no se
  quede con los datos de ayer. `WidgetTest` lo pinta en claro y oscuro.

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

- `cd tests && npm install && npm test` (o `node run.js gestos` para un solo
  fichero). Usan `playwright-core` con el Chromium preinstalado y un iPhone
  emulado: UA de iPhone, `navigator.standalone = true`, tamaño del modelo y
  movimiento reducido salvo donde se pide lo contrario (`movimiento: true`).
- Nada de push con una prueba en rojo. Las pruebas usan atributos estables
  (`data-aviso`, `data-swipe`, `data-estado`, ids del armazón): si cambias el
  marcado, conserva esos ganchos.
- Los gestos táctiles van por CDP (`Input.dispatchTouchEvent`). El puente de
  Android se simula con un `window.AvisosNativo` falso en `addInitScript`.
- Ante un fallo que cuenta el usuario: reprodúcelo con una prueba antes de
  arreglarlo y comprueba después que pasa.
- `node tests/capturas.js [carpeta] [modelos] [temas] [pantallas]` saca fotos
  de cada pantalla con la barra de estado simulada encima. Revísalas en claro y
  oscuro a 375, 393, 402 y 430 px antes de dar por bueno un cambio visual:
  que el + esté centrado, que la lente caiga bajo su pestaña, que nada se corte
  y que no haya errores de consola. Chromium sin pantalla no pinta bien
  `backdrop-filter` ni la tipografía SF: el resultado final se mira en el iPhone.

## Sistema de diseño: app nativa de iPhone (iOS 26 / Liquid Glass)

Viene del proyecto Investor del mismo usuario (Preact) y aquí está hecho sin
compilar: tokens y componentes en `assets/css/app.css`, comportamiento en
`assets/js/ios.js`. Mantenerlo al tocar cualquier pantalla.

- Tiene que parecer **una app de Apple**: materiales translúcidos, esquinas
  redondeadas y concéntricas, tipografía del sistema, colores del sistema y
  movimiento con muelles. Llamativa, pero con contención.
- La interfaz es neutra y **el color lo llevan los datos y las acciones**: el
  tinte es `systemBlue` (`--tint`) y cada estado del aviso tiene su color
  (`.e-<estado>` pone `--c` y `--ct`). La tarjeta de hoy cambia de degradado
  con los datos: rojizo con vencidos, violeta-naranja con urgentes, azul si no.
- Navegación: Agenda · Hechos · **+** · Equipo · Ajustes. No hay lista de
  avisos aparte: la Agenda lleva el buscador encima y, al buscar o filtrar,
  enseña los resultados (abiertos y cerrados); `#/avisos?v=…` es un alias que
  llega a ella ya filtrada. Todo aviso abierto tiene que salir en algún grupo
  de la Agenda (por eso existe «Más adelante»). Las pantallas de primer nivel llevan título grande (34 px) que se
  recoge en la barra al bajar; las que se abren desde ellas (`atras` en la
  vista) llevan el título pequeño en la barra y botón de volver
  (`data-vista="apilada"`). En los formularios no hay barra de pestañas.
- **Barra de estado**: `apple-mobile-web-app-status-bar-style` en `default`,
  nunca `black-translucent` (hora y batería en blanco, ilegibles en claro).
  `theme-color` lo pone `Ios.barraEstado()` según el tema de la app, sin
  `media`, y se oscurece a `#b6b6b9` con la hoja abierta en modo claro. El
  borde de arriba de la página es `--bg` liso y el fondo ambiental entra con
  una máscara en 150 px para que no se vea la unión.
- **Esquinas concéntricas** de la barra de pestañas: el radio de la pantalla
  se deduce del modelo (`RADIOS` en `ios.js`), solo con la app instalada.
  Margen igual por los lados y por abajo = max(12, R − alto/2); radio = R −
  margen; lo de dentro, radio − separación.
- `backdrop-filter` solo en capas flotantes (barras, hoja, aviso, botones
  redondos); las tarjetas son translúcidas sin desenfoque. Hay un `@supports`
  con fondos opacos. Las filas que se deslizan son opacas (`--card-solid`) para
  que no se transparente la acción de debajo.
- Componentes: listas agrupadas con separador que empieza en el texto
  (`.grupo`, `.fila`, helpers `fila()` y `bloque()` en `views.js`), filas de
  aviso en tres líneas (título y hora; cliente; estado, referencia y técnico),
  segmentados en cápsula con una pieza que se desliza, hoja con asa que se
  cierra arrastrando la cabecera más de 120 px, aviso flotante arriba en
  cápsula de cristal. Inputs de 16 px o más para que iOS no haga zoom.
- Movimiento: muelles con `linear()` (`--spring-smooth` sin rebote para hojas y
  títulos, `--spring` 4 %, `--spring-slide` 8 % para lente y segmentados,
  `--spring-bounce` 15 % al soltar). Pulsar es inmediato y soltar va con
  muelle. Luz al tocar, bloques que aparecen una vez al entrar en pantalla
  (atributo `data-reveal`, solo al cambiar de ruta: si se repinta la misma, no
  se anima) y cifras que cuentan. Todo se apaga con `prefers-reduced-motion`.
- Lo que una web **no** puede imitar, y no se promete: la refracción real del
  cristal, el radio exacto de la pantalla, la vibración antes de iOS 18 (se usa
  un `<input switch>` invisible) y volver deslizando desde el borde.

## Fallos ya resueltos (no volver a meterlos)

- Cada correo entraba dos veces: varios disparadores a la vez. `correo.js`
  serializa las pasadas (`enCurso` / `repetir`).
- La coma decimal: horas y duración son `type="text" inputmode="decimal"`, no
  `type="number"`.
- `[hidden]{display:none !important}` es global porque algunas clases con
  `display` lo anulaban.
- En el iPhone las descargas no llegan a ningún sitio con la app instalada:
  los archivos salen por `navigator.share({files})`.
- 2.1.0: página de una versión con `.js` de otra. El service worker guardaba lo
  que le daba la caché HTTP del navegador, y Vercel dejaba guardar `assets/`
  una hora: dos versiones seguidas se mezclaban. Ahora el service worker pide
  todo con `cache: 'reload'` y `vercel.json` pone `no-cache` en `assets/` (los
  dos hacen falta: lo prueba `tests/actualizar.test.js`). No subas esa caché.
- 2.1.2: barra de pestañas subida al abrir la app y que «se corregía sola».
  Es WebKit 301108 (iOS 26): con la app instalada, iOS a veces da a la web una
  ventana más corta que la pantalla; la franja de abajo queda fuera de la web
  y no se puede pintar. Lo dispara la barra de estado `black-translucent`, e
  iOS la lee solo al instalar: quien instaló antes de la 2.0 la conserva.
  `Ios.vigilarVentana()` lo detecta, quita y pone `viewport-fit=cover` para
  que WebKit vuelva a medir y, si sigue corta, pega la barra abajo
  (`.vp-corta`). Ajustes enseña la barra de estado y la ventana que ve el
  iPhone. Lo simula `tests/ventana.test.js` (márgenes seguros por CDP).
- El estado de los workflows que devuelve la API de GitHub puede ir con
  retraso. Para saber si una APK está bien, bájala y mira su `APP_VERSION`.
