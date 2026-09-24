package io.github.dadoga31.avisos.correo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import io.github.dadoga31.avisos.MainActivity;
import io.github.dadoga31.avisos.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mantiene el buzón vigilado. Con IMAP se queda a la escucha (IDLE) y el
 * servidor avisa al instante; con POP3, que no sabe avisar, se consulta
 * cada pocos minutos.
 */
public class ServicioCorreo extends Service {

    private static final String TAG = "AvisosServicio";
    private static final String CANAL_ESTADO = "correo_estado";
    private static final String CANAL_AVISOS = "correo_nuevos";
    private static final int ID_NOTIFICACION = 1;
    private static final long ESPERA_POP3 = 5 * 60 * 1000L;
    private static final long ESPERA_MIN = 15 * 1000L;
    private static final long ESPERA_MAX = 10 * 60 * 1000L;
    private static final int CAIDAS_PARA_AVISAR = 6;

    public static final String ACCION_INICIAR = "io.github.dadoga31.avisos.CORREO_INICIAR";
    public static final String ACCION_PARAR = "io.github.dadoga31.avisos.CORREO_PARAR";
    public static final String ACCION_SINCRONIZAR = "io.github.dadoga31.avisos.CORREO_SINCRONIZAR";

    /** La actividad se apunta aquí para enterarse con la app abierta. */
    public interface Oyente { void nuevoCorreo(int cuantos); }
    private static volatile Oyente oyente;

    public static void escuchar(Oyente o) { oyente = o; }
    public static void dejarDeEscuchar(Oyente o) { if (oyente == o) oyente = null; }

    private final AtomicBoolean activo = new AtomicBoolean(false);
    private Thread hilo;
    private AlmacenCorreo almacen;

    public static void arrancar(Context contexto) {
        lanzar(contexto, ACCION_INICIAR);
    }

    public static void parar(Context contexto) {
        lanzar(contexto, ACCION_PARAR);
    }

    public static void sincronizar(Context contexto) {
        lanzar(contexto, ACCION_SINCRONIZAR);
    }

    private static void lanzar(Context contexto, String accion) {
        Intent i = new Intent(contexto, ServicioCorreo.class).setAction(accion);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) contexto.startForegroundService(i);
            else contexto.startService(i);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo lanzar el servicio", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        almacen = new AlmacenCorreo(this);
        crearCanales();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String accion = intent == null ? ACCION_INICIAR : intent.getAction();
        arrancarEnPrimerPlano(getString(R.string.correo_conectando));

        if (ACCION_PARAR.equals(accion)) {
            detener();
            return START_NOT_STICKY;
        }

        Cuenta cuenta = Cuenta.cargar(this);
        if (cuenta == null || !cuenta.valida()) {
            Log.i(TAG, "Sin cuenta configurada");
            detener();
            return START_NOT_STICKY;
        }

        if (ACCION_SINCRONIZAR.equals(accion) && activo.get()) {
            /* Ya hay un hilo trabajando: con IDLE la consulta es continua. */
            return START_STICKY;
        }

        if (!activo.get()) iniciarBucle(cuenta);
        return START_STICKY;
    }

    private void iniciarBucle(final Cuenta cuenta) {
        activo.set(true);
        hilo = new Thread(new Runnable() {
            @Override
            public void run() {
                bucle(cuenta);
            }
        }, "correo");
        hilo.start();
    }

    private void bucle(Cuenta cuenta) {
        ClienteCorreo cliente = new ClienteCorreo(this, almacen);
        long espera = ESPERA_MIN;

        while (activo.get()) {
            try {
                if (cuenta.esImap()) {
                    int traidos = cliente.sincronizar(cuenta);   // lo pendiente desde la última vez
                    if (traidos > 0) anunciar(traidos);
                    notificarEstado(getString(R.string.correo_escuchando, cuenta.usuario));

                    cliente.escuchar(cuenta, new ClienteCorreo.AlLlegar() {
                        @Override
                        public void aviso(int cuantos) { anunciar(cuantos); }
                    }, activo);
                } else {
                    notificarEstado(getString(R.string.correo_vigilando, cuenta.usuario));
                    int traidos = cliente.sincronizar(cuenta);
                    if (traidos > 0) anunciar(traidos);
                    dormir(ESPERA_POP3);
                }
                espera = ESPERA_MIN;                             // fue bien: se reinicia la espera
            } catch (Exception e) {
                if (!activo.get()) break;

                /* Que se caiga la conexión es lo normal en un móvil: se
                   reconecta enseguida y sin marcar avería. Solo si pasa una
                   y otra vez se da por roto y se avisa. */
                if (ClienteCorreo.esCaidaDeRed(e)) {
                    int caidas = almacen.anotarCaida();
                    Log.i(TAG, "Conexión caída (" + caidas + "), reconectando", e);
                    if (caidas >= CAIDAS_PARA_AVISAR) {
                        String aviso = getString(R.string.correo_inestable);
                        almacen.anotarSync(aviso);
                        notificarEstado(aviso);
                    }
                    dormir(ESPERA_MIN);
                    continue;                                    // sin alargar la espera
                }

                String error = ClienteCorreo.explicar(e);
                Log.e(TAG, "Fallo con el buzón: " + error, e);
                almacen.anotarSync(error);
                notificarEstado(getString(R.string.correo_error, error));
                dormir(espera);
                espera = Math.min(espera * 2, ESPERA_MAX);       // se reintenta cada vez más despacio
            }
        }
    }

    private void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void anunciar(int cuantos) {
        Oyente o = oyente;
        if (o != null) {
            try { o.nuevoCorreo(cuantos); } catch (Exception ignorada) { }
        }
        avisarAlUsuario(cuantos);
    }

    private void detener() {
        activo.set(false);
        if (hilo != null) hilo.interrupt();
        almacen.anotarActivo(false);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        activo.set(false);
        if (hilo != null) hilo.interrupt();
        almacen.anotarActivo(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ---------- notificaciones ----------

    private void crearCanales() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel estado = new NotificationChannel(
                CANAL_ESTADO, getString(R.string.canal_estado), NotificationManager.IMPORTANCE_LOW);
        estado.setDescription(getString(R.string.canal_estado_desc));
        estado.setShowBadge(false);
        nm.createNotificationChannel(estado);

        NotificationChannel nuevos = new NotificationChannel(
                CANAL_AVISOS, getString(R.string.canal_nuevos), NotificationManager.IMPORTANCE_DEFAULT);
        nuevos.setDescription(getString(R.string.canal_nuevos_desc));
        nm.createNotificationChannel(nuevos);
    }

    private PendingIntent abrirApp(String ruta) {
        Intent i = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra("ruta", ruta)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(this, 10, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification construirEstado(String texto) {
        return new NotificationCompat.Builder(this, CANAL_ESTADO)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(texto)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(abrirApp("#/ajustes"))
                .build();
    }

    private void arrancarEnPrimerPlano(String texto) {
        try {
            startForeground(ID_NOTIFICACION, construirEstado(texto));
        } catch (Exception e) {
            Log.e(TAG, "No se pudo pasar a primer plano", e);
        }
    }

    private void notificarEstado(String texto) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(ID_NOTIFICACION, construirEstado(texto));
    }

    private void avisarAlUsuario(int cuantos) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        String texto = cuantos == 1
                ? getString(R.string.correo_uno)
                : getString(R.string.correo_varios, cuantos);
        Notification n = new NotificationCompat.Builder(this, CANAL_AVISOS)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(texto)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setAutoCancel(true)
                .setContentIntent(abrirApp("#/agenda"))
                .build();
        nm.notify((int) (System.currentTimeMillis() % 100000), n);
    }
}
