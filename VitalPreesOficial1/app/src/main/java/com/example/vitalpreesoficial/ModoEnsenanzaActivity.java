package com.example.vitalpreesoficial;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ModoEnsenanzaActivity extends AppCompatActivity {
    private TextView txtRespuesta;
    private EditText editRespuesta;
    private ScrollView scroll;
    private Button btnEnviar;
    private Button btnLimpiar;

    private List<String> preguntas;
    private int indice = 0;

    private DBVitalPress dbVitalPress;
    private String correo;
    private String nombre = "";
    private int edad = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_ensenanza);

        txtRespuesta = findViewById(R.id.txt_respuesta_ia);
        editRespuesta = findViewById(R.id.edit_pregunta);
        scroll = findViewById(R.id.scroll_respuestas);
        btnEnviar = findViewById(R.id.btn_enviar_pregunta);
        btnLimpiar = findViewById(R.id.btn_limpiar_chat);

        dbVitalPress = new DBVitalPress(this);

        SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
        correo = prefs.getString("correo", "");

        Cursor cursor = dbVitalPress.buscarUsuarioPorCorreo(correo);
        if (cursor != null && cursor.moveToFirst()) {
            int idxNombre = cursor.getColumnIndex("nombre");
            int idxEdad = cursor.getColumnIndex("edad");
            nombre = idxNombre != -1 ? cursor.getString(idxNombre) : "";
            edad = idxEdad != -1 ? cursor.getInt(idxEdad) : 0;
            cursor.close();
        }

        prepararPreguntas();
        mostrarPreguntaActual();

        btnEnviar.setOnClickListener(v -> manejarRespuesta());
        btnLimpiar.setOnClickListener(v -> limpiarChat());

        Button btnRegresar = findViewById(R.id.btn_regresar_aprendizaje);
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void prepararPreguntas() {
        preguntas = new ArrayList<>();
        // Datos auxiliares para personalizar
        String ultimaPa = null; int sis = -1, dia = -1, pul = -1; String fechaUlt = null;
        try {
            Cursor cu = dbVitalPress.getUltimaPresionDetallada(correo);
            if (cu != null && cu.moveToFirst()) {
                sis = cu.getInt(0); dia = cu.getInt(1); pul = cu.getInt(2); fechaUlt = cu.getString(3);
                ultimaPa = String.format(Locale.getDefault(), "%d/%d mmHg, pulso %d (%s)", sis, dia, pul, fechaUlt);
            }
            if (cu != null) cu.close();
        } catch (Exception ignore) {}

        double est = 0, peso = 0, imc = 0;
        try {
            Cursor c = dbVitalPress.buscarUsuarioPorCorreo(correo);
            if (c != null && c.moveToFirst()) {
                est = c.getDouble(c.getColumnIndexOrThrow("estatura"));
                peso = c.getDouble(c.getColumnIndexOrThrow("peso"));
            }
            if (c != null) c.close();
        } catch (Exception ignore) {}
        if (est > 0) imc = peso / (est * est);

        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String saludo = hora < 12 ? "Buenos días" : (hora < 19 ? "Buenas tardes" : "Buenas noches");

        // Base: preguntas cortas y claras, orientadas a síntomas relacionados con PA
        preguntas.add(saludo + (nombre.isEmpty()?"":" "+nombre) + ". ¿Cómo te sientes hoy en general?");
        preguntas.add("¿Has tenido dolor de cabeza, mareos o zumbidos en los oídos hoy?");
        preguntas.add("¿Has notado palpitaciones, falta de aire o presión en el pecho?");
        preguntas.add("En la última hora, ¿estuviste en reposo, con estrés o hiciste ejercicio?");
        preguntas.add("¿Dormiste bien anoche y cuántas horas aproximadamente?");

        // Personalización por edad
        if (edad >= 60) {
            preguntas.add("Al ponerte de pie, ¿sientes mareo u oscurecimiento de la vista?");
            preguntas.add("¿Has tomado tus medicamentos de presión como te indicó tu médico?");
        } else {
            preguntas.add("¿Has consumido alimentos muy salados hoy (embutidos, sopas instantáneas, snacks)?");
        }

        // Personalización por IMC (solo informativa)
        if (imc > 0 && imc >= 25) {
            preguntas.add("¿Realizaste alguna actividad física ligera (caminar) al menos 20-30 minutos?");
        }

        // Si hay última PA conocida, preguntar seguimiento
        if (ultimaPa != null) {
            preguntas.add("Tu última medición registrada fue " + ultimaPa + ". ¿Has notado síntomas desde entonces?");
        } else {
            preguntas.add("Aún no veo una medición reciente. ¿Tienes oportunidad de medir tu presión hoy?");
        }

        // Cierre base
        preguntas.add("Si deseas agregar algún otro síntoma o comentario, escríbelo aquí.");

        // Garantizar exactamente 10 preguntas
        String[] extras = new String[]{
                "¿Has tenido visión borrosa o náuseas hoy?",
                "¿Has sentido hormigueo en manos o pies?",
                "¿Has notado hinchazón en tobillos o piernas?",
                "¿Tuviste estrés notable hoy (trabajo, familia)?"
        };
        int e = 0;
        while (preguntas.size() < 10 && e < extras.length) {
            preguntas.add(extras[e++]);
        }
        if (preguntas.size() > 10) {
            preguntas.subList(10, preguntas.size()).clear();
        }
    }

    private void mostrarPreguntaActual() {
        String historial = getSharedPreferences("ia_ensenanza", MODE_PRIVATE).getString("historial", "");
        String prompt = "Modo Enseñanza (Chequeo de síntomas relacionados con PA)\n\n" + (historial.isEmpty() ? "" : historial + "\n\n") + "Pregunta: " + preguntas.get(indice);
        txtRespuesta.setText(prompt);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void manejarRespuesta() {
        String resp = editRespuesta.getText().toString().trim();
        if (resp.isEmpty()) {
            Toast.makeText(this, "Escribe una respuesta", Toast.LENGTH_SHORT).show();
            return;
        }
        // Guardar última respuesta por categoría simple
        SharedPreferences sp = getSharedPreferences("ia_ensenanza", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        if (indice == 0) ed.putString("mood_last", resp);
        if (indice == 1) ed.putString("symptoms_last", resp);
        if (indice == 2) ed.putString("palpit_last", resp);
        // Añadir a historial
        String h = sp.getString("historial", "");
        h = (h.isEmpty() ? "" : h + "\n") + "P: " + preguntas.get(indice) + "\nR: " + resp;
        ed.putString("historial", h);
        ed.apply();

        // Log con timestamp para agrupación por fecha en DOCX
        try {
            long now = System.currentTimeMillis();
            org.json.JSONArray arr;
            String raw = getSharedPreferences("ia_ensenanza_log", MODE_PRIVATE).getString("entries", null);
            if (raw != null && !raw.isEmpty()) arr = new org.json.JSONArray(raw); else arr = new org.json.JSONArray();
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("ts", now);
            obj.put("q", preguntas.get(indice));
            obj.put("a", resp);
            arr.put(obj);
            getSharedPreferences("ia_ensenanza_log", MODE_PRIVATE).edit().putString("entries", arr.toString()).apply();
        } catch (Exception ignore) {}

        // Regenerar DOCX con resumen IA actualizado
        try {
            File f = DocxExporter.generateUserDocx(this, correo);
            getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
        } catch (Exception ignore) {}

        // Siguiente: si era la última, regenerar set y reiniciar; si no, avanzar
        boolean eraUltima = (indice == preguntas.size() - 1);
        if (eraUltima) {
            prepararPreguntas();
            indice = 0;
        } else {
            indice = indice + 1;
        }
        editRespuesta.setText("");
        mostrarPreguntaActual();
    }

    private void limpiarChat() {
        getSharedPreferences("ia_ensenanza", MODE_PRIVATE).edit().remove("historial").apply();
        txtRespuesta.setText("Modo Enseñanza\n\nPreguntas cortas para conocerte y cuidar mejor tu salud de presión arterial.");
        scroll.post(() -> scroll.fullScroll(View.FOCUS_UP));
    }
}
