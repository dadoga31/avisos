# Diseño de la app nativa de Android

Referencia para reescribir la app en Kotlin + Jetpack Compose + Material 3.
Esto **no** es código de la app: son maquetas en HTML que imitan Material 3,
y el icono. Las capturas están en `capturas/`.

## Decisiones

- **Estilo**: Material 3 Expressive (Android 16). Roboto e iconos Material
  Symbols Rounded. Listas agrupadas con esquinas grandes fuera y pequeñas
  dentro, como Ajustes de Android.
- **Color**: fondos neutros (gris) y naranja de la marca (`#F27427`) solo en
  acciones: botón +, «Marcar resuelto», «Ir», interruptores. Paleta en
  `tokens2.css` (sale de `tokens2.mjs`). Cada estado tiene el suyo:
  pendiente gris, programado verde azulado, en curso azul, en espera morado,
  resuelto verde, cancelado gris. Vencido y urgente, rojo de error.
- **Menú**: flotante. Cápsula con Agenda · Hechos · Equipo · Ajustes y, a su
  lado, el botón de la pantalla (+ en Agenda, añadir técnico en Equipo). Sin
  botón, la cápsula va centrada. El contenido pasa por debajo.
- **Criterios de uso** (un técnico en la calle, con prisa y una mano):
  1. Del icono a ir al siguiente cliente, un toque: tarjeta «Siguiente» con
     Ir, Llamar y Empezar arriba de la Agenda.
  2. Cerrar un aviso, un gesto y sin confirmar: deslizar a la izquierda =
     hecho, con «Deshacer» abajo. A la derecha = en curso.
  3. Crear un aviso en menos de 20 s: qué pasa (con micrófono), cliente que
     rellena dirección y teléfono del aviso anterior, cuándo/quién/tipo con
     fichas y prioridad segmentada. El resto en «Más detalles».
  4. Acciones al pulgar: en la ficha, barra flotante abajo con llamar,
     WhatsApp, ir y «Marcar resuelto». El estado se cambia tocando la
     pastilla de color, en una hoja.
  5. Lo urgente te encuentra: notificaciones con acciones (Me lo quedo,
     Llamar, Ver; Ir, Empezar, Retrasar).
- **Icono**: escudo blanco con ✓ hueco sobre degradado naranja (propuesta B
  de `capturas/icono.png`, elegida por el usuario). El ✓ es un hueco de verdad
  (`evenOdd`), no un trazo del color del fondo: el lanzador mueve las capas por
  separado y se notaría. La notificación usa la misma silueta: Android la
  pinta de un color y el naranja lo pone `setColor`. Ya está en
  `android/app/src/main/res` (adaptable, temático, notificación, widget y PNG
  para Android 7). Para cambiar de propuesta: `ELEGIDA` en `icono.js`.

## Volver a generarlas

Necesita Playwright con Chromium (en las sesiones en la nube ya está).

```
cd diseno && npm install
npm run tokens      # paleta → tokens2.css
npm run maquetas    # capturas en png/
npm run icono       # propuestas en icono/ y recursos de Android en icono/res
```
