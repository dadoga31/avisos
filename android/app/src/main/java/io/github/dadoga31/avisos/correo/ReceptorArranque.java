package io.github.dadoga31.avisos.correo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Al reiniciar el móvil, el buzón vuelve a quedar vigilado. */
public class ReceptorArranque extends BroadcastReceiver {
    @Override
    public void onReceive(Context contexto, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Cuenta cuenta = Cuenta.cargar(contexto);
        if (cuenta != null && cuenta.valida()) ServicioCorreo.arrancar(contexto);
    }
}
