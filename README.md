# Avisos

App de móvil para llevar los avisos de trabajo de un instalador de sistemas de
seguridad: qué hay que hacer, cuándo, quién lo tiene asignado y cómo va.

Es una **PWA**: se instala en la pantalla de inicio, se abre como una app normal
y **funciona sin cobertura**. No hay servidor ni cuentas: todos los datos se
guardan en el propio móvil.

---

## Cómo ponerla en el móvil

### Opción A — GitHub Pages (recomendada)

1. En GitHub, entra en este repositorio → **Settings** → **Pages**.
2. En *Build and deployment* elige **Deploy from a branch**.
3. Branch: `claude/app-gestion-avisos-408zhn` (o `main` si ya has fusionado los
   cambios), carpeta `/ (root)`. Guarda.
4. Espera un minuto y abre en el móvil la dirección que te da GitHub
   (`https://dadoga31.github.io/avisos/`).
5. Instálala:
   - **Android / Chrome:** menú ⋮ → *Añadir a pantalla de inicio*.
   - **iPhone / Safari:** botón Compartir → *Añadir a pantalla de inicio*.

A partir de ahí se abre a pantalla completa y funciona aunque no tengas datos.

### Opción B — probarla en el ordenador

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
- Compartir un resumen del aviso por WhatsApp, correo, etc.
- Duplicar y eliminar.

**Equipo**
- Alta de técnicos con teléfono y color; se ve cuántos avisos abiertos y
  vencidos lleva cada uno. Al borrar un técnico sus avisos quedan sin asignar.

**Ajustes**
- Tema automático / claro / oscuro.
- Prefijo de la numeración de referencias.
- **Copias de seguridad**: exportar copia completa (con fotos), solo datos, o
  un CSV para abrir en Excel. Importar añadiendo a lo que ya hay o reemplazando
  todo. Importar dos veces la misma copia no duplica nada.
- Datos de ejemplo para probar, y borrado total.

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
assets/js/db.js          Capa sobre IndexedDB
assets/js/store.js       Modelo de datos y reglas de negocio
assets/js/ui.js          Formato, componentes, hoja inferior, avisos flotantes
assets/js/views.js       Pantallas: agenda, lista, ficha, formulario, equipo, ajustes
assets/js/app.js         Arranque y enrutado por hash
tools/make-icons.js      Genera los iconos PNG (node tools/make-icons.js)
```

No hay dependencias ni proceso de compilación: son ficheros estáticos.

### Personalizarla

- **Tipos de trabajo y sistemas**: listas `TIPOS` y `SISTEMAS` al principio de
  `assets/js/store.js`.
- **Estados**: lista `ESTADOS` en el mismo fichero (`abierto: true/false` marca
  si el aviso sigue vivo). Si añades uno nuevo, dale color en `app.css`
  (`.pill--<id>`).
- **Colores de la app**: variables `--accent`, `--ink`, etc., al principio de
  `assets/css/app.css`.

Después de tocar los ficheros, sube la versión en `sw.js` (`VERSION`) para que
los móviles ya instalados se actualicen.
