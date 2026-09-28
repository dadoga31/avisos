# Avisos

App de móvil para llevar los avisos de trabajo de un instalador de sistemas de
seguridad: qué hay que hacer, cuándo, quién lo tiene asignado y cómo va.

Es una **PWA**: se instala en la pantalla de inicio, se abre como una app normal
y **funciona sin cobertura**. No hay servidor ni cuentas: todos los datos se
guardan en el propio móvil.

---

## Cómo ponerla en el móvil

### Vercel (así está montada)

El repositorio está conectado a Vercel: **cada push a la rama por defecto
despliega la versión nueva**, sin compilar nada. La configuración vive en dos
ficheros:

- `vercel.json` — cabeceras de caché. `sw.js`, `index.html` y el manifiesto se
  piden siempre al servidor (si se quedaran en caché, el móvil no vería nunca
  una versión nueva); el resto de `assets/` se guarda una hora.
- `.vercelignore` — deja fuera del despliegue `android/`, `tools/`, `.github/`
  y el README, que no forman parte de la web.

Cuando publicas una versión nueva, la app la detecta al volver a abrirla y
ofrece **Actualizar** en un aviso flotante. Si no lo tocas, se aplica la próxima
vez que la cierres del todo.

### Instalarla en el iPhone

1. Abre la dirección de Vercel **en Safari** (no en Chrome ni desde un enlace de
   WhatsApp: tiene que ser Safari para que se pueda instalar).
2. Toca el botón **Compartir** — el cuadrado con la flecha hacia arriba, abajo
   en el centro.
3. Baja en la lista hasta **«Añadir a pantalla de inicio»**.
4. Confirma con **Añadir**.

Queda un icono junto a las demás apps y se abre a pantalla completa, sin la
barra de Safari. Funciona sin cobertura.

En **Android / Chrome** es menú ⋮ → *Añadir a pantalla de inicio*, o el botón
que sale en Ajustes → *Instalación*.

### Probarla en el ordenador

```bash
python3 -m http.server 8000
# abre http://localhost:8000
```

Necesita servirse por HTTP (no vale abrir `index.html` con doble clic) para que
funcionen el modo sin conexión y la instalación.

---

## Qué hace

**Agenda (pantalla de inicio)**
- Contadores de vencidos, hoy, próximos 7 días y sin asignar; cada uno abre la
  lista ya filtrada.
- Los avisos agrupados en *Vencidos · Hoy · Mañana · Próximos 7 días · Sin fecha*.
- **Gestos sobre cada aviso** (también en la lista de Avisos):
  - Deslizar a la **izquierda** lo marca como hecho: aparece el panel verde, al
    pasar el umbral vibra y la fila se va deslizándose y encogiendo.
  - Deslizar a la **derecha** despliega *En curso* y *Cancelar*.
  - Todo cambio hecho con un gesto sale con un **Deshacer** en el aviso flotante.
  - La **banda de color** del borde izquierdo y la píldora indican el estado:
    gris pendiente, azul programado, ámbar en curso, violeta en espera, verde
    resuelto, gris claro cancelado. La prioridad alta o urgente se marca aparte
    con su etiqueta.
  - Los avisos ya cerrados no se deslizan, y el desplazamiento vertical de la
    lista sigue funcionando con normalidad.

**Avisos**
- Buscador por cliente, dirección, teléfono, referencia, título o técnico.
- Filtros rápidos (solo abiertos, vencidos, urgentes, sin asignar, hoy) y
  filtros completos por estado, prioridad, tipo, sistema, técnico y rango de fechas.
- Orden por fecha, por prioridad o por los más recientes.

**Ficha del aviso**
- Referencia automática (`AV-0001`, `AV-0002`…), título, tipo de trabajo
  (avería, instalación, mantenimiento, revisión, presupuesto) y sistema
  (alarma, CCTV, control de accesos, incendios, portero, otros).
- Estado en un toque: pendiente → programado → en curso → en espera → resuelto
  / cancelado.
- Prioridad (baja, normal, alta, urgente); las altas y urgentes se marcan con
  una banda de color en la lista.
- Cliente con **llamar**, **WhatsApp** y **cómo llegar** (abre el mapa).
- Fecha y hora, duración prevista y reprogramación rápida (hoy / mañana / +1 semana).
- Técnico asignado, con la carga de trabajo de cada uno a la vista.
- Seguimiento: notas con fecha y hora.
- Material usado y horas trabajadas (acepta coma decimal: `2,5`).
- Fotos desde la cámara, comprimidas y guardadas en el móvil.
- **Ver los adjuntos**: al tocar una foto se abre a pantalla completa, con
  zoom de dos dedos o doble toque, y se pasa de una a otra si hay varias. Un
  PDF o cualquier otro archivo se abre con la aplicación que tengas para ello.
- Compartir un resumen del aviso por WhatsApp, correo, etc.
- **Llevar al calendario del móvil** (ver abajo).
- Duplicar y eliminar.

**Hechos (histórico)**
- Todo aviso resuelto o cancelado se conserva: nada se borra al deslizar.
- Agrupado por fecha de cierre (día a día la última semana, por meses lo
  anterior), con el número de avisos y las horas de cada grupo.
- Contadores de hechos hoy, de los últimos 7 días y horas de esa semana.
- Buscador y filtros de resueltos / cancelados.
- Deslizar a la derecha un aviso cerrado lo **reabre** como pendiente.

**Equipo**
- Alta de técnicos con teléfono y color; se ve cuántos avisos abiertos y
  vencidos lleva cada uno. Al borrar un técnico sus avisos quedan sin asignar.

**Ajustes**
- Tema automático / claro / oscuro.
- Prefijo de la numeración de referencias.
- Exportación al calendario del móvil, con recordatorio configurable.
- **Copias de seguridad**: exportar copia completa (con fotos), solo datos, o
  un CSV para abrir en Excel. Importar añadiendo a lo que ya hay o reemplazando
  todo. Importar dos veces la misma copia no duplica nada.
- Datos de ejemplo para probar, y borrado total.

---

## Avisos que llegan solos desde el correo

La app puede leer el buzón de la empresa y convertir en aviso cada correo
que entra. **Solo funciona en la APK de Android**, no en la PWA: para hablar
IMAP o POP3 hay que abrir un socket contra el servidor de correo, y eso ningún
navegador lo permite — ni Safari en el iPhone ni Chrome. La conexión la hace el
código nativo de Android; nada pasa por ningún servidor intermedio.

En la PWA, Ajustes → *Correo de la empresa* lo dice y los avisos se crean a
mano. Traerlo al iPhone exigiría un servicio intermedio (una función en Vercel
con un cron que lea el buzón y deje los avisos en una base de datos), y eso
significa **guardar la contraseña del correo de la empresa en un servidor**, no
solo en el móvil. Hoy no está hecho a propósito.

### Configurarlo

Ajustes → *Correo de la empresa* → **Conectar una cuenta**. Necesitas:

- La dirección y la contraseña del correo.
- El **servidor de entrada** (algo como `mail.tuempresa.es`). Lo tienes en
  Outlook, en los ajustes de la cuenta, como «servidor de correo entrante».
- El protocolo: **IMAP** si el servidor lo admite (avisos al instante), o
  **POP3** (consulta cada 5 minutos). Los puertos se rellenan solos.

El botón *Probar* comprueba la conexión antes de guardar nada.

### Qué hace con cada correo

| Del correo | Al aviso |
|---|---|
| Asunto | Título |
| Cuerpo, sin la cadena de respuestas | Descripción |
| Remitente | Cliente y contacto |
| Teléfono que aparezca en el texto | Teléfono del cliente |
| Adjuntos | Fotos y archivos del aviso |
| Palabras del asunto y el cuerpo | Tipo de trabajo, sistema y prioridad |

Un asunto con «urgente» sale con prioridad urgente; si habla de cámaras, el
sistema queda como CCTV; si dice «no funciona», el tipo es avería. Todo eso
lo puedes corregir a mano en la ficha, como cualquier otro aviso.

### Lo que conviene saber

- **El pasado no se convierte.** Al conectar la cuenta se toma nota de por
  dónde va el buzón y solo entran los correos a partir de ese momento. Si no,
  un buzón con años de correo generaría miles de avisos.
- **Nada se borra del servidor**, y con IMAP los mensajes ni se marcan como
  leídos: Outlook sigue viéndolo todo igual.
- **Entra todo.** Cada correo genera un aviso, también la publicidad. Para
  cortarlo, en la ficha del aviso tienes *No crear avisos de este remitente*,
  y en Ajustes la lista de ignorados (vale un correo suelto o un dominio
  entero, como `@publicidad.com`).
- **El aviso aparece al abrir la app.** El correo se descarga al instante y
  te salta la notificación, pero el aviso se crea cuando la app está delante,
  que es inmediato al abrirla.
- La app mantiene una **notificación fija** mientras vigila el buzón: es lo
  que Android exige para no cortar la conexión. Gasta algo más de batería que
  tenerla apagada.
- La contraseña se guarda en el **almacén cifrado de Android**, solo en tu
  móvil.

---

## Verlos en el calendario del móvil

Los avisos se pueden llevar al calendario nativo (Calendario de iPhone, Google
Calendar, Samsung Calendar…) en formato **.ics**, el estándar de calendarios.

- **Un aviso suelto:** ábrelo → *Al calendario*. El móvil te ofrece añadirlo, o
  puedes abrirlo directamente en Google Calendar.
- **Varios de golpe:** Ajustes → *Calendario del móvil* → próximos 30 días,
  todos los abiertos, o todos los que tengan fecha.

Qué se lleva cada evento: referencia y título, dirección del cliente como
ubicación (para poder tocar y navegar), y en la descripción el estado, la
prioridad, el tipo de trabajo, el sistema, el técnico, el contacto, el teléfono
y las notas de la descripción. Los avisos con hora ocupan la duración prevista
(1 h si no la has puesto); los que solo tienen fecha entran como evento de día
completo. En Ajustes eliges el recordatorio (de 15 minutos a 1 día antes, o
ninguno).

**Si cambias la fecha de un aviso, vuelve a exportarlo:** cada evento lleva un
identificador fijo, así que el calendario **actualiza el evento existente en vez
de duplicarlo**. Los avisos sin fecha no se exportan.

Es una exportación puntual, no una sincronización: el calendario no se entera de
los cambios por su cuenta. Una suscripción que se actualice sola (webcal)
necesitaría un servidor publicando el calendario, que hoy la app no tiene.

---

## Crear avisos desde Atajos (iPhone)

Dictas un aviso a Siri y aparece en la app. Es lo que sustituye al widget que
había en Android.

Hay dos pegas del iPhone que conviene entender, porque explican por qué esto
está montado como está:

- Una web no puede recibir nada desde fuera. Un atajo no puede escribir en la
  base de datos de la app.
- Abrir un enlace desde Atajos **lleva a Safari**, y en iOS la app de la
  pantalla de inicio [no comparte almacenamiento con
  Safari](https://bugs.webkit.org/show_bug.cgi?id=181849): el aviso se quedaría
  donde nunca lo verías.

Así que el atajo deja el aviso en un **buzón** en el propio despliegue de
Vercel, y la app lo recoge sola al abrirla. Sin copiar ni pegar.

### 1. Conectar el almacén (una vez)

El buzón necesita dónde guardar los avisos mientras la app está cerrada:

1. En **vercel.com** → tu proyecto → pestaña **Storage**.
2. *Create Database* → **Upstash** → **Redis**. El plan gratuito sobra.
3. Conéctalo al proyecto: Vercel pone solo `KV_REST_API_URL` y
   `KV_REST_API_TOKEN`.
4. **Redeploy** para que la función las vea.

Sin esas variables, `/api/buzon` responde `503` diciendo exactamente esto.

### 2. Encender el buzón

Ajustes → *Atajos del iPhone* → **Activar el buzón**. La app genera un token
(192 bits, solo vive en este móvil), comprueba que el buzón responde y te
enseña los dos datos que hay que meter en Atajos.

El token es la llave: quien lo tenga puede meter avisos en tu app. No viaja en
las copias de seguridad, para que no se escape al mandarlas por correo.

### 3. El atajo

En **Atajos** → **+**, dos acciones:

1. *Pedir entrada de texto* — pregunta: «¿Qué aviso?».
2. *Obtener contenido de URL*, con la dirección del buzón y, en *Mostrar más*:
   - **Método:** POST
   - **Cabeceras:** `X-Token` con el valor del token
   - **Cuerpo de solicitud:** Archivo → el *Texto proporcionado* del paso 1

Llámalo «Nuevo aviso» y lánzalo con Siri, con el botón de acción, desde el
widget de Atajos o **tocando dos veces la parte de atrás del móvil**.

El aviso entra en la app la próxima vez que la abres o vuelves a ella. Una web
no puede recogerlo con la app cerrada; eso sí necesitaría una app nativa.

### El buzón por dentro

`api/buzon.js`, una función sin dependencias:

| | |
|---|---|
| `POST /api/buzon` | deja un aviso (texto plano o `{"texto": "…"}`) |
| `GET /api/buzon` | entrega lo que haya **y lo vacía**, en una sola orden |
| `GET /api/buzon?ojear=1` | dice cuántos esperan, sin tocarlos |

Todo con la cabecera `X-Token`. La clave de Redis es el SHA-256 del token, así
que el servidor no guarda tokens ni puede listar los buzones que existen. La
cola se recorta a 100 avisos y caduca a los 30 días.

### Sin buzón: copiar y pegar

Si no quieres montar el almacén, el atajo puede *Copiar al portapapeles* y tú
pegas el aviso con el icono de la barra de la Agenda. Sirve igual para el texto
de un correo o un WhatsApp.

También funciona un enlace `#/nuevo?titulo=…` (con `crear=1` lo crea sin
preguntar). En Android y en el ordenador va bien; en el iPhone cae en Safari, y
cuando eso pasa la app **no crea el aviso a escondidas**: enseña el formulario
con una advertencia en amarillo.

### Qué se puede escribir

Una línea por dato, en cualquier orden, con `clave: valor`. Lo que no encaje se
queda como descripción; y si no pones ninguna clave, la primera línea es el
título y el resto la descripción (o sea: dictar y ya está).

| Clave | Valor |
|---|---|
| `titulo` (`asunto`, `t`) | lo que hay que hacer |
| `cliente` (`c`) | nombre o empresa |
| `dir` | dirección |
| `tel` | teléfono |
| `contacto` | persona de contacto |
| `desc` (`nota`) | descripción; se puede repetir |
| `fecha` | `hoy`, `mañana`, `lunes`, `+3`, `30/9`, `2026-09-30` |
| `hora` | `9`, `9:30`, `0930` |
| `duracion` | horas previstas, con coma: `1,5` |
| `prioridad` (`p`) | baja · normal · alta · urgente |
| `tipo` | avería · instalación · mantenimiento · revisión · presupuesto |
| `sistema` | alarma · cctv · accesos · incendios · portero |
| `tecnico` | nombre de alguien del Equipo |

Lo que no digas se deduce del texto, con las mismas reglas que los correos:
«cámara» es CCTV, «no funciona» es avería, «urgente» es urgente, y un teléfono
suelto en el texto se recoge como teléfono del cliente.

Ejemplo de lo que puede dictar el atajo:

```
Central en fallo de comunicación
cliente: Farmacia Centro
tel: 611223344
prioridad: urgente
fecha: mañana
hora: 9:30
```

---

## Qué no hay en el iPhone

La PWA lleva todo lo de arriba, con tres excepciones, todas por límites del
navegador y no de la app:

| | iPhone (PWA) | APK de Android |
|---|---|---|
| Avisos desde el correo | no | sí |
| Widget en la pantalla de inicio | no (hay atajo con Siri, ver arriba) | sí |
| Notificaciones en segundo plano | no | sí |

Un navegador no puede abrir sockets (de ahí el correo), iOS no tiene widgets
para páginas web, y una PWA no corre en segundo plano cuando está cerrada. Lo
demás —agenda, gestos, histórico, fotos, adjuntos a pantalla completa,
calendario, copias de seguridad— funciona igual en los dos sitios.

Guardar y compartir archivos sí cambia: en el iPhone, la copia de seguridad, el
CSV y el `.ics` salen por la **hoja de compartir** en vez de descargarse, porque
una descarga normal no llega a ninguna parte con la app instalada en la pantalla
de inicio. Desde ahí puedes guardarlos en Archivos, mandarlos por correo o
abrir el `.ics` con el Calendario.

---

## La app de Android (APK) y el widget

Además de la versión web, el repositorio trae una app nativa de Android en
`android/`. No es un acceso directo al navegador: la web va **dentro del APK**
y se sirve desde `appassets.androidplatform.net`, un origen seguro local, así
que funciona sin red y sin servidor ninguno. El único permiso de red que pide
es para leer el buzón de correo, y solo lo usa si configuras una cuenta.

Ya no se compila en cada push: la versión que se usa es la PWA, y el APK se
genera **solo cuando lo pides a mano**.

### Descargar e instalar

1. Ve a la pestaña **Actions** del repositorio → *APK de Android* → **Run
   workflow**.
2. Cuando termine, la APK queda publicada en
   **https://github.com/dadoga31/avisos/releases/tag/apk** como `avisos.apk`.
3. Abre ese enlace **desde el móvil**, descarga el archivo y ábrelo. Android
   pedirá permitir la instalación de apps de origen desconocido para el
   navegador; es lo normal al instalar fuera de Play Store.

Para actualizar, repite el proceso: la firma no cambia entre compilaciones, así
que se instala encima sin perder los datos.

### El widget de la pantalla de inicio

Mantén pulsada la pantalla de inicio → *Widgets* → **Avisos**. El widget tiene:

- Un botón grande **+ Nuevo aviso** que abre la app directamente en el
  formulario, sin pasar por la agenda.
- Los contadores del día (para hoy, vencidos, hechos), que la app va dejando
  cada vez que la usas. Si los contadores son de otro día, el widget lo dice en
  lugar de enseñar números viejos.
- Tocar la cabecera abre la agenda.

También hay accesos directos al **mantener pulsado el icono** de la app: *Nuevo
aviso* y *Agenda*.

### Diferencias con la versión web

- Los archivos (copia de seguridad, CSV, `.ics`) no se «descargan»: la app los
  entrega al sistema. El `.ics` se abre con el Calendario; las copias salen por
  la hoja de compartir, para mandarlas al correo o a la nube.
- **Los datos son independientes de los del navegador.** Si ya usabas la PWA y
  te pasas a la APK, exporta una copia desde la web e impórtala en la app.
- Al tener `allowBackup`, la copia automática de Google puede incluir los datos
  de la app; aun así conviene exportar copias a mano.

### Compilarla tú mismo

```bash
cd android
./gradlew assembleRelease     # necesita el SDK de Android y Java 17
```

El APK sale en `android/app/build/outputs/apk/release/`.

El almacén de claves de `android/keystore/` es el **de depuración**, con la
contraseña pública de siempre (`android`). No protege nada: está ahí para que
la firma no cambie entre compilaciones y puedas actualizar sin desinstalar. Si
algún día quieres publicarla, genera tu propia clave y cambia `signingConfigs`
en `android/app/build.gradle`.

---

## Dónde están los datos

En **IndexedDB del navegador del móvil**, nada sale del dispositivo. Eso implica:

- Si borras los datos del navegador o desinstalas la app, se van los avisos.
- No se sincroniza entre dispositivos.

Por eso conviene exportar una copia de vez en cuando (Ajustes → Copia de
seguridad) y guardarla en el correo o en la nube. Para pasar los datos a otro
móvil: exportas en uno e importas en el otro.

Si algún día hacen falta varios técnicos viendo y editando los mismos avisos a
la vez, el siguiente paso sería añadir un backend (por ejemplo Supabase) y
sincronizar contra estas mismas estructuras.

---

## Estructura del proyecto

```
index.html               Estructura de la interfaz
manifest.webmanifest     Datos de instalación de la PWA
sw.js                    Service worker (funcionamiento sin conexión)
assets/css/app.css       Estilos (tema claro y oscuro)
assets/js/fallos.js      Registro de errores (Ajustes → Si algo falla)
assets/js/db.js          Capa sobre IndexedDB
assets/js/store.js       Modelo de datos y reglas de negocio
assets/js/ics.js         Generación de archivos .ics para el calendario
assets/js/ui.js          Formato, componentes, hoja inferior, avisos flotantes
assets/js/swipe.js       Gestos de deslizamiento sobre las filas de aviso
assets/js/visor.js       Visor a pantalla completa de fotos y adjuntos
assets/js/views.js       Pantallas: agenda, lista, ficha, formulario, equipo, ajustes
assets/js/app.js         Arranque y enrutado por hash
assets/js/nativo.js      Puente con la app de Android (archivos y widget)
assets/js/correo.js      Conversión de correo entrante en avisos
assets/js/atajos.js      Avisos que llegan escritos de fuera (Atajos, portapapeles)
assets/js/buzon.js       Recogida de los avisos que deja un atajo
api/buzon.js             Buzón en el servidor (función de Vercel + Redis)
tools/make-icons.js      Genera los iconos PNG (node tools/make-icons.js)
android/                 Proyecto de la app Android (WebView + widget)
.github/workflows/       Compilación del APK (solo a mano, Run workflow)
vercel.json              Cabeceras de caché del despliegue
.vercelignore            Lo que no se publica en la web
```

No hay dependencias ni proceso de compilación: son ficheros estáticos.

### Personalizarla

- **Tipos de trabajo y sistemas**: listas `TIPOS` y `SISTEMAS` al principio de
  `assets/js/store.js`.
- **Estados**: lista `ESTADOS` en el mismo fichero (`abierto: true/false` marca
  si el aviso sigue vivo). Si añades uno nuevo, dale color en `app.css`
  (`.pill--<id>`).
- **Colores de la app**: variables `--accent`, `--ink`, etc., al principio de
  `assets/css/app.css`. Los colores de estado son las variables `--st-*`.
- **Sensibilidad de los gestos**: constantes `ANCHO_ACCIONES`, `UMBRAL_MIN` y
  `UMBRAL_PROP` al principio de `assets/js/swipe.js`.

Después de tocar los ficheros, sube la versión en `sw.js` (`VERSION`) para que
los móviles ya instalados se actualicen.
