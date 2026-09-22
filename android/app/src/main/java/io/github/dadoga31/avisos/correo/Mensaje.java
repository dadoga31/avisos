package io.github.dadoga31.avisos.correo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Un correo ya descargado, listo para que la web lo convierta en aviso. */
public class Mensaje {

    public String uid = "";
    public String messageId = "";
    public String de = "";
    public String deNombre = "";
    public String asunto = "";
    public String fecha = "";        // ISO 8601
    public String texto = "";
    public String html = "";
    public final List<Adjunto> adjuntos = new ArrayList<>();

    public static class Adjunto {
        public String id = "";
        public String nombre = "";
        public String mime = "";
        public long tam = 0;

        JSONObject aJSON() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("nombre", nombre);
                o.put("mime", mime);
                o.put("tam", tam);
            } catch (Exception ignorada) { }
            return o;
        }
    }

    public JSONObject aJSON() {
        JSONObject o = new JSONObject();
        try {
            o.put("uid", uid);
            o.put("messageId", messageId);
            o.put("de", de);
            o.put("deNombre", deNombre);
            o.put("asunto", asunto);
            o.put("fecha", fecha);
            o.put("texto", texto);
            o.put("html", html);
            JSONArray lista = new JSONArray();
            for (Adjunto a : adjuntos) lista.put(a.aJSON());
            o.put("adjuntos", lista);
        } catch (Exception ignorada) { }
        return o;
    }
}
