package io.github.dadoga31.avisos.correo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Lee el buzón por IMAP o POP3. Nada se borra nunca del servidor: la app
 * solo lee, para no dejar sin correo a Outlook ni a ningún otro cliente.
 */
public class ClienteCorreo {

    private static final String TAG = "AvisosCorreo";
    private static final int MAX_POR_VUELTA = 30;              // para no avalanchar la agenda
    private static final long REFRESCO_IDLE = 8 * 60 * 1000L;  // se renueva la escucha cada 8 min
    private static final long MAX_ADJUNTO = 12L * 1024 * 1024; // 12 MB por adjunto
    private static final String PREFS = "correo_estado";

    private final Context contexto;
    private final AlmacenCorreo almacen;

    public ClienteCorreo(Context contexto, AlmacenCorreo almacen) {
        this.contexto = contexto.getApplicationContext();
        this.almacen = almacen;
    }

    // ---------- conexión ----------

    private Properties propiedades(Cuenta c) {
        String proto = c.esImap() ? (c.esSSL() ? "imaps" : "imap") : (c.esSSL() ? "pop3s" : "pop3");
        Properties p = new Properties();
        p.setProperty("mail.store.protocol", proto);
        p.setProperty("mail." + proto + ".host", c.servidor);
        p.setProperty("mail." + proto + ".port", String.valueOf(c.puerto));
        p.setProperty("mail." + proto + ".connectiontimeout", "20000");
        p.setProperty("mail." + proto + ".timeout", "40000");
        p.setProperty("mail." + proto + ".writetimeout", "20000");

        if (c.esSSL()) {
            p.setProperty("mail." + proto + ".ssl.enable", "true");
        } else {
            p.setProperty("mail." + proto + ".starttls.enable", "true");
            p.setProperty("mail." + proto + ".starttls.required", "true");
        }
        /* Sin bajar la guardia con los certificados: si el servidor tiene uno
           mal puesto, es mejor que falle y se vea. */
        p.setProperty("mail." + proto + ".ssl.protocols", "TLSv1.2 TLSv1.3");
        if (c.esImap()) p.setProperty("mail." + proto + ".peek", "true");   // no marcar como leído
        return p;
    }

    private Store conectar(Cuenta c) throws Exception {
        Session sesion = Session.getInstance(propiedades(c));
        Store store = sesion.getStore();
        store.connect(c.servidor, c.puerto, c.usuario, c.clave);
        return store;
    }

    /** Devuelve null si todo fue bien, o un mensaje de error para enseñar. */
    public String probar(Cuenta c) {
        Store store = null;
        try {
            store = conectar(c);
            Folder bandeja = store.getFolder("INBOX");
            bandeja.open(Folder.READ_ONLY);
            int n = bandeja.getMessageCount();
            bandeja.close(false);
            Log.i(TAG, "Conexión correcta, " + n + " mensajes en la bandeja");
            return null;
        } catch (javax.mail.AuthenticationFailedException e) {
            return "Usuario o contraseña incorrectos";
        } catch (Exception e) {
            return explicar(e);
        } finally {
            cerrar(store);
        }
    }

    /**
     * Con IMAP IDLE la conexión se cae sola cada dos por tres: el operador
     * móvil o el propio servidor cortan lo que lleva un rato en silencio.
     * Eso NO es una avería, es el día a día: toca reconectar y seguir.
     */
    public static boolean esCaidaDeRed(Exception e) {
        if (e instanceof javax.mail.AuthenticationFailedException) return false;

        Throwable t = e;
        while (t != null) {
            if (t instanceof java.io.IOException) return true;    // socket, EOF, reset…
            t = t.getCause();
        }

        String m = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return m.contains("bye")
                || m.contains("abort")
                || m.contains("reset")
                || m.contains("broken pipe")
                || m.contains("connection closed")
                || m.contains("connection dropped")
                || m.contains("timed out")
                || m.contains("unexpected end");
    }

    public static String explicar(Exception e) {
        if (esCaidaDeRed(e)) return "Se cortó la conexión con el buzón; reintentando";
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String bajo = m.toLowerCase();
        if (bajo.contains("unknownhost") || bajo.contains("unable to resolve"))
            return "No se encuentra el servidor. Revisa el nombre.";
        if (bajo.contains("timed out") || bajo.contains("timeout"))
            return "El servidor no responde. Revisa el puerto o la seguridad.";
        if (bajo.contains("certificate") || bajo.contains("ssl") || bajo.contains("handshake"))
            return "Problema con el certificado del servidor. Prueba con la otra opción de seguridad.";
        if (bajo.contains("connection refused"))
            return "El servidor rechaza la conexión en ese puerto.";
        return m;
    }

    private void cerrar(Store store) {
        if (store == null) return;
        try { store.close(); } catch (Exception ignorada) { }
    }

    private SharedPreferences prefs() {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- descarga ----------

    /**
     * Trae lo que haya llegado desde la última vez y lo deja en el almacén.
     * La primera vez no se descarga nada del pasado: solo se toma nota de
     * dónde está el buzón, para no convertir en avisos años de correo.
     */
    public int sincronizar(Cuenta c) throws Exception {
        Store store = null;
        Folder bandeja = null;
        try {
            store = conectar(c);
            bandeja = store.getFolder("INBOX");
            bandeja.open(Folder.READ_ONLY);

            List<Mensaje> nuevos = c.esImap()
                    ? nuevosImap((IMAPFolder) bandeja)
                    : nuevosPop3((POP3Folder) bandeja);

            almacen.anadirPendientes(nuevos);
            almacen.anotarSync(null);
            return nuevos.size();
        } finally {
            if (bandeja != null && bandeja.isOpen()) {
                try { bandeja.close(false); } catch (Exception ignorada) { }
            }
            cerrar(store);
        }
    }

    private List<Mensaje> nuevosImap(IMAPFolder bandeja) throws Exception {
        List<Mensaje> salida = new ArrayList<>();
        long validez = bandeja.getUIDValidity();
        long ultimo = prefs().getLong("ultimoUid", 0);
        long validezGuardada = prefs().getLong("uidValidity", 0);

        /* Si el servidor reinicia la numeración, se empieza de cero. */
        if (validezGuardada != validez) {
            ultimo = 0;
            prefs().edit().putLong("uidValidity", validez).apply();
        }

        if (ultimo == 0 && almacen.relecturaPedida() == 0) {
            /* Primera vez: tomamos nota del final del buzón y ya está. */
            Message[] todos = bandeja.getMessages();
            long tope = todos.length > 0 ? bandeja.getUID(todos[todos.length - 1]) : 0;
            prefs().edit().putLong("ultimoUid", tope).apply();
            Log.i(TAG, "Primera sincronización: a partir del UID " + tope);
            return salida;
        }

        /* Relectura pedida a mano: se retrocede el puntero para volver a
           mirar los últimos correos. Repetirlos no molesta, porque los que
           ya tienen aviso se descartan al convertir. */
        int relectura = almacen.relecturaPedida();
        if (relectura > 0) {
            ultimo = Math.max(0, ultimo - relectura);
            almacen.relecturaHecha();
            Log.i(TAG, "Relectura: se vuelve desde el UID " + (ultimo + 1));
        }

        Message[] mensajes = bandeja.getMessagesByUID(ultimo + 1, UIDFolder.LASTUID);

        /* El puntero avanza SOLO hasta el último correo que se ha entregado
           de verdad. Si se moviera con los que se saltan o fallan, esos
           correos no volverían a mirarse nunca y se perderían en silencio. */
        long entregadoHasta = ultimo;
        int entregados = 0;

        for (Message m : mensajes) {
            long uid = bandeja.getUID(m);
            if (uid <= ultimo) continue;                    // el rango incluye el último
            if (entregados >= MAX_POR_VUELTA) break;        // el resto, en la siguiente vuelta
            salida.add(leer((MimeMessage) m, validez + "-" + uid));
            entregadoHasta = uid;
            entregados++;
        }

        if (entregadoHasta > ultimo) {
            prefs().edit().putLong("ultimoUid", entregadoHasta).apply();
        }
        return salida;
    }

    private List<Mensaje> nuevosPop3(POP3Folder bandeja) throws Exception {
        List<Mensaje> salida = new ArrayList<>();
        Set<String> vistos = almacen.vistos();
        boolean primera = !prefs().getBoolean("pop3Iniciado", false);

        Message[] mensajes = bandeja.getMessages();

        /* Relectura pedida a mano: los últimos N dejan de contar como
           vistos para que vuelvan a mirarse. */
        int relectura = almacen.relecturaPedida();
        if (relectura > 0) {
            primera = false;
            for (int i = mensajes.length - 1; i >= 0 && i >= mensajes.length - relectura; i--) {
                String u = bandeja.getUID(mensajes[i]);
                if (u != null) vistos.remove(u);
            }
            almacen.relecturaHecha();
            Log.i(TAG, "Relectura POP3 de los últimos " + relectura);
        }
        Set<String> nuevosVistos = new HashSet<>();
        int entregados = 0;

        /* Del final hacia atrás: lo reciente es lo que importa. */
        for (int i = mensajes.length - 1; i >= 0; i--) {
            String uid = bandeja.getUID(mensajes[i]);
            if (uid == null || vistos.contains(uid)) continue;

            if (primera) {
                /* Al conectar la cuenta, el pasado solo se anota. */
                nuevosVistos.add(uid);
                continue;
            }

            if (entregados >= MAX_POR_VUELTA) break;      // el resto, en la siguiente vuelta

            /* Solo se da por visto lo que se entrega: si no, un correo que
               falle al leerse desaparecería para siempre. */
            salida.add(0, leer((MimeMessage) mensajes[i], uid));
            nuevosVistos.add(uid);
            entregados++;
        }

        almacen.anadirVistos(nuevosVistos);
        if (primera) {
            prefs().edit().putBoolean("pop3Iniciado", true).apply();
            Log.i(TAG, "Primera sincronización POP3: " + nuevosVistos.size() + " mensajes marcados como antiguos");
        }
        return salida;
    }

    // ---------- lectura de un mensaje ----------

    /**
     * Nunca devuelve null: si algo del correo no se puede leer, se entrega
     * con lo que sí se haya podido sacar. Un aviso con el asunto y el
     * remitente es infinitamente mejor que un correo perdido en silencio.
     */
    private Mensaje leer(MimeMessage original, String uid) {
        Mensaje m = new Mensaje();
        m.uid = uid;

        try {
            String[] ids = original.getHeader("Message-ID");
            m.messageId = (ids != null && ids.length > 0) ? ids[0] : "";
        } catch (Exception e) {
            Log.w(TAG, "Sin identificador de mensaje", e);
        }

        try {
            Address[] de = original.getFrom();
            if (de != null && de.length > 0 && de[0] instanceof InternetAddress) {
                InternetAddress dir = (InternetAddress) de[0];
                m.de = dir.getAddress() == null ? "" : dir.getAddress();
                m.deNombre = dir.getPersonal() == null ? "" : dir.getPersonal();
            }
        } catch (Exception e) {
            Log.w(TAG, "Remitente ilegible", e);
        }

        try {
            m.asunto = original.getSubject() == null ? "" : original.getSubject();
        } catch (Exception e) {
            Log.w(TAG, "Asunto ilegible", e);
        }
        if (m.asunto.isEmpty()) m.asunto = "(correo sin asunto)";

        try {
            Date fecha = original.getSentDate();
            if (fecha == null) fecha = original.getReceivedDate();
            if (fecha == null) fecha = new Date();
            m.fecha = Fechas.iso(fecha);
        } catch (Exception e) {
            m.fecha = Fechas.iso(new Date());
        }

        try {
            recorrer(original, m, new int[]{0});
        } catch (Exception e) {
            Log.e(TAG, "Cuerpo ilegible, se entrega el aviso igualmente", e);
        }

        if (m.texto.isEmpty() && m.html.isEmpty()) {
            m.texto = "(no se pudo leer el texto del correo; ábrelo en tu gestor de correo)";
        }
        return m;
    }

    private void recorrer(Part parte, Mensaje m, int[] contador) throws Exception {
        String disposicion = parte.getDisposition();
        String nombre = parte.getFileName();
        boolean esAdjunto = Part.ATTACHMENT.equalsIgnoreCase(disposicion)
                || (!TextUtils.isEmpty(nombre) && !parte.isMimeType("multipart/*"));

        if (parte.isMimeType("multipart/*") && !esAdjunto) {
            Multipart partes = (Multipart) parte.getContent();
            for (int i = 0; i < partes.getCount(); i++) recorrer(partes.getBodyPart(i), m, contador);
            return;
        }

        if (esAdjunto) {
            guardarAdjunto(parte, m, contador);
            return;
        }

        if (parte.isMimeType("text/plain")) {
            Object contenido = parte.getContent();
            if (contenido != null) m.texto = unir(m.texto, contenido.toString());
        } else if (parte.isMimeType("text/html")) {
            Object contenido = parte.getContent();
            if (contenido != null) m.html = unir(m.html, contenido.toString());
        }
    }

    private static String unir(String previo, String nuevo) {
        if (previo == null || previo.isEmpty()) return nuevo;
        return previo + "\n" + nuevo;
    }

    private void guardarAdjunto(Part parte, Mensaje m, int[] contador) {
        InputStream entrada = null;
        try {
            int tamano = parte.getSize();
            if (tamano > MAX_ADJUNTO) {
                Log.w(TAG, "Adjunto demasiado grande, se omite: " + parte.getFileName());
                return;
            }
            String id = m.uid.replaceAll("[^A-Za-z0-9]", "") + "_" + (contador[0]++);
            entrada = parte.getInputStream();
            long escritos = almacen.guardarAdjunto(id, entrada);

            Mensaje.Adjunto adj = new Mensaje.Adjunto();
            adj.id = id;
            adj.nombre = parte.getFileName() == null ? ("adjunto" + contador[0]) : parte.getFileName();
            adj.mime = tipoDe(parte);
            adj.tam = escritos;
            m.adjuntos.add(adj);
        } catch (Exception e) {
            Log.e(TAG, "Adjunto no guardado", e);
        } finally {
            if (entrada != null) {
                try { entrada.close(); } catch (Exception ignorada) { }
            }
        }
    }

    private static String tipoDe(Part parte) {
        try {
            String tipo = parte.getContentType();
            if (tipo == null) return "application/octet-stream";
            int puntoYcoma = tipo.indexOf(';');
            return (puntoYcoma > 0 ? tipo.substring(0, puntoYcoma) : tipo).trim().toLowerCase();
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    // ---------- escucha en tiempo real ----------

    public interface AlLlegar {
        void aviso(int cuantos);
    }

    /**
     * Mantiene abierta la conexión con IDLE: el servidor avisa en cuanto
     * entra un correo. Solo IMAP; POP3 no sabe hacer esto.
     */
    public void escuchar(Cuenta c, AlLlegar callback, java.util.concurrent.atomic.AtomicBoolean activo)
            throws Exception {
        Store store = null;
        IMAPFolder bandeja = null;
        java.util.Timer refresco = null;
        try {
            store = conectar(c);
            bandeja = (IMAPFolder) store.getFolder("INBOX");
            bandeja.open(Folder.READ_ONLY);
            almacen.anotarActivo(true);

            /* Se corta la escucha cada pocos minutos y se vuelve a pedir: así
               la conexión nunca lleva tanto rato callada como para que la
               corten por su cuenta, y se detecta antes si se ha caído. */
            final IMAPFolder vigilada = bandeja;
            refresco = new java.util.Timer("idle-refresco", true);
            refresco.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    try { vigilada.idleAbort(); } catch (Exception ignorada) { }
                }
            }, REFRESCO_IDLE, REFRESCO_IDLE);

            while (activo.get()) {
                bandeja.idle();                     // bloquea hasta que el servidor diga algo
                if (!activo.get()) break;
                int cuantos = sincronizarEnFolder(bandeja, c);
                if (cuantos > 0 && callback != null) callback.aviso(cuantos);
            }
        } finally {
            if (refresco != null) refresco.cancel();
            almacen.anotarActivo(false);
            if (bandeja != null && bandeja.isOpen()) {
                try { bandeja.close(false); } catch (Exception ignorada) { }
            }
            cerrar(store);
        }
    }

    private int sincronizarEnFolder(IMAPFolder bandeja, Cuenta c) throws Exception {
        List<Mensaje> nuevos = nuevosImap(bandeja);
        almacen.anadirPendientes(nuevos);
        almacen.anotarSync(null);
        return nuevos.size();
    }
}
