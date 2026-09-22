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

    static String explicar(Exception e) {
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

        if (ultimo == 0) {
            /* Primera vez: tomamos nota del final del buzón y ya está. */
            Message[] todos = bandeja.getMessages();
            long tope = todos.length > 0 ? bandeja.getUID(todos[todos.length - 1]) : 0;
            prefs().edit().putLong("ultimoUid", tope).apply();
            Log.i(TAG, "Primera sincronización: a partir del UID " + tope);
            return salida;
        }

        Message[] mensajes = bandeja.getMessagesByUID(ultimo + 1, UIDFolder.LASTUID);
        long mayor = ultimo;
        int contados = 0;

        for (Message m : mensajes) {
            long uid = bandeja.getUID(m);
            if (uid <= ultimo) continue;                 // getMessagesByUID incluye el último
            mayor = Math.max(mayor, uid);
            if (contados++ >= MAX_POR_VUELTA) continue;  // el resto vendrá en la siguiente vuelta
            Mensaje leido = leer((MimeMessage) m, validez + "-" + uid);
            if (leido != null) salida.add(leido);
        }

        if (contados <= MAX_POR_VUELTA) {
            prefs().edit().putLong("ultimoUid", mayor).apply();
        } else {
            /* Quedan más: se avanza solo hasta donde se ha leído de verdad. */
            long hasta = ultimo;
            for (Mensaje m : salida) {
                String[] partes = m.uid.split("-");
                hasta = Math.max(hasta, Long.parseLong(partes[partes.length - 1]));
            }
            prefs().edit().putLong("ultimoUid", hasta).apply();
        }
        return salida;
    }

    private List<Mensaje> nuevosPop3(POP3Folder bandeja) throws Exception {
        List<Mensaje> salida = new ArrayList<>();
        Set<String> vistos = almacen.vistos();
        boolean primera = !prefs().getBoolean("pop3Iniciado", false);

        Message[] mensajes = bandeja.getMessages();
        Set<String> nuevosVistos = new HashSet<>();
        int contados = 0;

        /* Del final hacia atrás: lo reciente es lo que importa. */
        for (int i = mensajes.length - 1; i >= 0; i--) {
            String uid = bandeja.getUID(mensajes[i]);
            if (uid == null || vistos.contains(uid)) continue;
            nuevosVistos.add(uid);
            if (primera) continue;                       // el pasado no se convierte
            if (contados++ >= MAX_POR_VUELTA) break;
            Mensaje leido = leer((MimeMessage) mensajes[i], uid);
            if (leido != null) salida.add(0, leido);
        }

        almacen.anadirVistos(nuevosVistos);
        if (primera) {
            prefs().edit().putBoolean("pop3Iniciado", true).apply();
            Log.i(TAG, "Primera sincronización POP3: " + nuevosVistos.size() + " mensajes marcados como antiguos");
        }
        return salida;
    }

    // ---------- lectura de un mensaje ----------

    private Mensaje leer(MimeMessage original, String uid) {
        try {
            Mensaje m = new Mensaje();
            m.uid = uid;

            String[] ids = original.getHeader("Message-ID");
            m.messageId = (ids != null && ids.length > 0) ? ids[0] : "";

            Address[] de = original.getFrom();
            if (de != null && de.length > 0 && de[0] instanceof InternetAddress) {
                InternetAddress dir = (InternetAddress) de[0];
                m.de = dir.getAddress() == null ? "" : dir.getAddress();
                m.deNombre = dir.getPersonal() == null ? "" : dir.getPersonal();
            }

            m.asunto = original.getSubject() == null ? "" : original.getSubject();

            Date fecha = original.getSentDate();
            if (fecha == null) fecha = original.getReceivedDate();
            if (fecha == null) fecha = new Date();
            m.fecha = fecha.toInstant().toString();

            recorrer(original, m, new int[]{0});
            return m;
        } catch (Exception e) {
            Log.e(TAG, "Mensaje ilegible", e);
            return null;
        }
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
        try {
            store = conectar(c);
            bandeja = (IMAPFolder) store.getFolder("INBOX");
            bandeja.open(Folder.READ_ONLY);
            almacen.anotarActivo(true);

            while (activo.get()) {
                bandeja.idle();                     // bloquea hasta que el servidor diga algo
                if (!activo.get()) break;
                int cuantos = sincronizarEnFolder(bandeja, c);
                if (cuantos > 0 && callback != null) callback.aviso(cuantos);
            }
        } finally {
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
