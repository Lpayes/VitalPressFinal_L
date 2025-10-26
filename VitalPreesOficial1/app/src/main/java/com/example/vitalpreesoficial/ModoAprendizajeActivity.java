package com.example.vitalpreesoficial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ModoAprendizajeActivity extends AppCompatActivity {

    private TextView txtRespuestaIA;
    private EditText editPregunta;
    private ScrollView scrollRespuestas;

    private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DOMAIN_GUARD = "Responde únicamente sobre salud relacionada con la presión arterial (PA). Si la pregunta está fuera de ese ámbito, indícale amablemente al usuario que el Modo Aprendizaje solo cubre PA y su autocuidado, y sugiere reformular la pregunta para PA.";

    private String getOpenAiApiKey() {
        // Usar la clave expuesta por BuildConfig (configurada en app/build.gradle.kts desde secrets.properties o placeholder)
        return BuildConfig.OPENAI_API_KEY;
    }

    private final List<String> poolDudas = new ArrayList<>();
    private int dudasOffset = 0; // índice base del bloque actual (0, 10, 20...)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_aprendizaje);

        // Referencias
        txtRespuestaIA = findViewById(R.id.txt_respuesta_ia);
        editPregunta = findViewById(R.id.edit_pregunta);
        scrollRespuestas = findViewById(R.id.scroll_respuestas);

        // Construir dudas por defecto
        construirPoolDudasPersonalizadas();

        // Mensaje de bienvenida + bloques guía
        dudasOffset = 0;
        txtRespuestaIA.setText(mensajeBienvenidaDinamico());
        mostrarBloquesGuia(true);

        // Botón Enviar Pregunta
        Button btnEnviar = findViewById(R.id.btn_enviar_pregunta);
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pregunta = editPregunta.getText().toString().trim();
                if (pregunta.isEmpty()) return;
                // Paginación simple: si el usuario escribe "más"
                if (pregunta.equalsIgnoreCase("más") || pregunta.equalsIgnoreCase("mas")) {
                    if (dudasOffset + 10 < poolDudas.size()) {
                        dudasOffset += 10;
                    } else {
                        dudasOffset = 0; // reinicia
                    }
                    mostrarBloquesGuia(false);
                    editPregunta.setText("");
                    return;
                }
                procesarPregunta(pregunta);
                editPregunta.setText("");
            }
        });

        // Botón Limpiar Chat
        Button btnLimpiar = findViewById(R.id.btn_limpiar_chat);
        btnLimpiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dudasOffset = 0;
                txtRespuestaIA.setText(mensajeBienvenidaDinamico());
                mostrarBloquesGuia(true);
                scrollRespuestas.scrollTo(0, 0);
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_aprendizaje);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Procesar prompt inicial si llega desde Recomendaciones
        try {
            Intent intent = getIntent();
            if (intent != null) {
                String promptInicial = intent.getStringExtra("prompt_inicial");
                if (promptInicial != null && !promptInicial.trim().isEmpty()) {
                    txtRespuestaIA.setText("Generando recomendaciones personalizadas...");
                    procesarPregunta(promptInicial);
                }
            }
        } catch (Exception ignore) {}
    }

    private String mensajeBienvenidaDinamico() {
        String correoUsuario = obtenerCorreoActivo();
        String nombre = ""; int edad = 0; double est = 0; double peso = 0; double imc = 0;
        try {
            DBVitalPress db = new DBVitalPress(this);
            android.database.Cursor cursor = db.buscarUsuarioPorCorreo(correoUsuario);
            if (cursor != null && cursor.moveToFirst()) {
                nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                edad = cursor.getInt(cursor.getColumnIndexOrThrow("edad"));
                est = cursor.getDouble(cursor.getColumnIndexOrThrow("estatura"));
                peso = cursor.getDouble(cursor.getColumnIndexOrThrow("peso"));
            }
            if (cursor != null) cursor.close();
        } catch (Exception ignore) {}
        if (est > 0) imc = peso / (est * est);
        String sEdad = edad > 0 ? (" ("+edad+" años)") : "";
        String sImc = imc > 0 ? String.format(Locale.getDefault(), "%.2f", imc) : "--";
        return "Bienvenido al Modo Aprendizaje de VitalPress IA.\n\n" +
                "Puedes preguntar, por ejemplo:\n" +
                "• ¿Mi presión es normal para mi edad" + sEdad + "?\n" +
                "• ¿Mi IMC (" + sImc + ") está en un rango saludable?\n" +
                "• ¿Qué hábitos me recomiendas para mejorar mi presión arterial?";
    }

    private void mostrarBloquesGuia(boolean append) {
        String bloqueCheckIn = generarCheckInHoy();
        String bloqueDudas = generarBloqueDudasPaginado();
        String texto = bloqueCheckIn + "\n\n" + bloqueDudas;
        if (append) {
            txtRespuestaIA.append("\n\n" + texto);
        } else {
            txtRespuestaIA.setText(mensajeBienvenidaDinamico() + "\n\n" + texto);
        }
        scrollRespuestas.post(() -> scrollRespuestas.fullScroll(View.FOCUS_DOWN));
    }

    private String generarBloqueDudasPaginado() {
        if (poolDudas.isEmpty()) return "";
        int start = Math.max(0, dudasOffset);
        int end = Math.min(poolDudas.size(), start + 10);
        StringBuilder sb = new StringBuilder();
        sb.append("Sugerencias (" + (start + 1) + "-" + end + "/" + poolDudas.size() + "):\n");
        for (int i = start; i < end; i++) {
            int numero = i + 1;
            sb.append(numero).append(") ").append(poolDudas.get(i)).append("\n");
        }
        if (end < poolDudas.size()) {
            sb.append("Escribe 'más' para ver más.");
        } else if (poolDudas.size() > 10) {
            sb.append("Escribe 'más' para volver al inicio.");
        }
        return sb.toString().trim();
    }

    private String generarCheckInHoy() {
        return "Check-in diario: ¿Cómo te sientes hoy? (descanso, estrés, ejercicio, síntomas)";
    }

    private void construirPoolDudasPersonalizadas() {
        poolDudas.clear();
        // Perfil de usuario
        String correo = obtenerCorreoActivo();
        int edad = 0; double est = 0, peso = 0, imc = 0; String ultimaPa = null;
        List<String> resDocs = new ArrayList<>();
        try {
            DBVitalPress db = new DBVitalPress(this);
            android.database.Cursor c = db.buscarUsuarioPorCorreo(correo);
            if (c != null && c.moveToFirst()) {
                edad = c.getInt(c.getColumnIndexOrThrow("edad"));
                est = c.getDouble(c.getColumnIndexOrThrow("estatura"));
                peso = c.getDouble(c.getColumnIndexOrThrow("peso"));
            }
            if (c != null) c.close();
            if (est > 0) imc = peso / (est * est);
            android.database.Cursor cu = db.getUltimaPresionDetallada(correo);
            if (cu != null && cu.moveToFirst()) {
                int sis = cu.getInt(0), dia = cu.getInt(1), pul = cu.getInt(2);
                String f = cu.getString(3);
                ultimaPa = String.format(java.util.Locale.getDefault(), "%d/%d mmHg, pulso %d (%s)", sis, dia, pul, f);
            }
            if (cu != null) cu.close();
            android.database.Cursor cd = db.getDocumentosPorCorreo(correo);
            while (cd != null && cd.moveToNext()) {
                String resumen = cd.getString(0);
                if (resumen != null && !resumen.trim().isEmpty()) resDocs.add(resumen);
            }
            if (cd != null) cd.close();
        } catch (Exception ignore) {}

        // Detectar posibles condiciones en documentos
        boolean tDiabetes = false, tHta = false, tColesterol = false, tRenal = false, tEmbarazo = false;
        for (String r : resDocs) {
            String s = r.toLowerCase(java.util.Locale.ROOT);
            if (s.contains("diabet")) tDiabetes = true;
            if (s.contains("hipertens") || s.contains("hta")) tHta = true;
            if (s.contains("colesterol") || s.contains("dislipi")) tColesterol = true;
            if (s.contains("renal") || s.contains("riñon")) tRenal = true;
            if (s.contains("embaraz")) tEmbarazo = true;
        }

        // Sugerencias base enfocadas en PA
        String sEdad = edad > 0 ? (" ("+edad+" años)") : "";
        String sImc = imc > 0 ? String.format(java.util.Locale.getDefault(), "%.2f", imc) : "--";
        poolDudas.add("¿Mi presión es normal para mi edad" + sEdad + "?");
        poolDudas.add("¿Mi IMC (" + sImc + ") está en rango saludable respecto a mi PA?");
        poolDudas.add("¿Qué hábitos diarios me recomiendas para bajar mi presión arterial?");
        poolDudas.add("¿Con qué frecuencia debería medirme la presión y en qué condiciones?");
        if (ultimaPa != null) poolDudas.add("Mi última medición fue " + ultimaPa + ". ¿Cómo interpretarla?");

        // Sugerencias condicionadas por antecedentes
        if (tHta) poolDudas.add("Tengo hipertensión diagnosticada. ¿Qué metas de PA debería tener?");
        if (tDiabetes) poolDudas.add("Tengo diabetes. ¿Cómo afecta a mis metas de presión arterial?");
        if (tColesterol) poolDudas.add("Tengo colesterol alto. ¿Qué relación tiene con la PA y el riesgo cardiovascular?");
        if (tRenal) poolDudas.add("Tengo enfermedad renal. ¿Qué precauciones debo tener con mi presión?");
        if (tEmbarazo) poolDudas.add("Estoy/estuve embarazada. ¿Qué debo saber sobre PA en el embarazo?");

        // Completar hasta 10 con extras de PA
        String[] extras = new String[]{
                "¿El consumo de sal y ultraprocesados afecta mi presión?",
                "¿Qué tipo de ejercicio es más adecuado para controlar la PA?",
                "¿Cómo reducir el estrés para ayudar a mi presión arterial?",
                "¿A qué valores debo acudir a urgencias por PA?"
        };
        int i = 0;
        while (poolDudas.size() < 10 && i < extras.length) {
            poolDudas.add(extras[i++]);
        }
        // Si sobran, recortar a 10
        if (poolDudas.size() > 10) {
            poolDudas.subList(10, poolDudas.size()).clear();
        }
    }

    private boolean esNumero(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    private String obtenerCorreoActivo() {
        String c = getSharedPreferences("usuario_sesion", MODE_PRIVATE).getString("correo", null);
        if (c == null) c = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
        return c;
    }

    private void procesarPregunta(String pregunta) {
        txtRespuestaIA.setText("Consultando a la IA...");
        new AsyncTask<String, Void, String>() {
            String correoUsuario;
            @Override
            protected String doInBackground(String... params) {
                try {
                    correoUsuario = obtenerCorreoActivo();
                    URL url = new URL(OPENAI_ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "Bearer " + getOpenAiApiKey());
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Obtener datos del usuario y contexto clínico desde BD
                    String nombreUsuario = "";
                    String apellidoUsuario = "";
                    int edadUsuario = 0;
                    double estaturaUsuario = 0.0;
                    double pesoUsuario = 0.0;
                    String sexoUsuario = "";
                    double imcUsuario = 0.0;
                    String ultimaPa = null;
                    List<String> resDocs = new ArrayList<>();
                    try {
                        DBVitalPress db = new DBVitalPress(ModoAprendizajeActivity.this);
                        android.database.Cursor cursor = db.buscarUsuarioPorCorreo(correoUsuario);
                        if (cursor != null && cursor.moveToFirst()) {
                            nombreUsuario = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                            apellidoUsuario = cursor.getString(cursor.getColumnIndexOrThrow("apellido"));
                            edadUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("edad"));
                            estaturaUsuario = cursor.getDouble(cursor.getColumnIndexOrThrow("estatura"));
                            pesoUsuario = cursor.getDouble(cursor.getColumnIndexOrThrow("peso"));
                            sexoUsuario = cursor.getString(cursor.getColumnIndexOrThrow("sexo"));
                        }
                        if (cursor != null) cursor.close();
                        if (estaturaUsuario > 0) imcUsuario = pesoUsuario / (estaturaUsuario * estaturaUsuario);
                        // Última PA
                        android.database.Cursor cu = db.getUltimaPresionDetallada(correoUsuario);
                        if (cu != null && cu.moveToFirst()) {
                            int sis = cu.getInt(0), dia = cu.getInt(1), pul = cu.getInt(2);
                            String f = cu.getString(3);
                            ultimaPa = String.format(Locale.getDefault(), "%d/%d, pulso: %d (%s)", sis, dia, pul, f);
                        }
                        if (cu != null) cu.close();
                        // Últimos documentos (hasta 3)
                        android.database.Cursor cd = db.getDocumentosPorCorreo(correoUsuario);
                        int count = 0;
                        while (cd != null && cd.moveToNext() && count < 3) {
                            String resumen = cd.getString(0);
                            String fecha = cd.getString(1);
                            resDocs.add((fecha != null ? (fecha + ": ") : "") + (resumen != null ? resumen : ""));
                            count++;
                        }
                        if (cd != null) cd.close();
                    } catch (Exception ignore) {}

                    // Fallback antiguo: resumen en SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("datos_documento", MODE_PRIVATE);
                    String resumenSP = prefs.getString("resumen_documento", null);
                    if ((resDocs == null || resDocs.isEmpty()) && resumenSP != null) {
                        resDocs = new ArrayList<>();
                        resDocs.add(resumenSP);
                    }

                    // Construir mensaje system personalizado con guardia de dominio
                    String contextoUsuario = "Datos del usuario: Nombre: " + nombreUsuario + " " + apellidoUsuario +
                            ", Edad: " + edadUsuario + ", Sexo: " + sexoUsuario + ", IMC: " + String.format(Locale.getDefault(), "%.2f", imcUsuario) + ".";
                    String contextoClinico = "Contexto clínico: " +
                            (ultimaPa != null ? (" Última presión: " + ultimaPa + ".") : "") +
                            (!resDocs.isEmpty() ? (" Resúmenes recientes: " + android.text.TextUtils.join(" | ", resDocs)) : "");

                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("model", "gpt-4-1106-preview");
                    org.json.JSONArray messages = new org.json.JSONArray();
                    messages.put(new JSONObject().put("role", "system").put("content", DOMAIN_GUARD));
                    messages.put(new JSONObject().put("role", "system").put("content", contextoUsuario));
                    if (!contextoClinico.trim().isEmpty()) {
                        messages.put(new JSONObject().put("role", "system").put("content", contextoClinico));
                    }
                    messages.put(new JSONObject().put("role", "user").put("content", pregunta));
                    jsonBody.put("messages", messages);

                    String body = jsonBody.toString();
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    Scanner scanner;
                    if (responseCode == 200) {
                        scanner = new Scanner(conn.getInputStream());
                    } else {
                        scanner = new Scanner(conn.getErrorStream());
                    }
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();

                    if (responseCode == 200) {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String respuesta = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        return respuesta.trim();
                    } else {
                        return "Error al consultar la IA: " + response.toString();
                    }
                } catch (Exception e) {
                    return "Error de conexión: " + e.getMessage();
                }
            }
            @Override
            protected void onPostExecute(String result) {
                txtRespuestaIA.setText(result);
                scrollRespuestas.post(() -> scrollRespuestas.fullScroll(View.FOCUS_DOWN));
                long now = System.currentTimeMillis();
                try {
                    // Guardar interacción con timestamp en SharedPreferences (histórico visible en DOCX)
                    org.json.JSONArray arr;
                    String raw = getSharedPreferences("ia_aprendizaje_log", MODE_PRIVATE).getString("entries", null);
                    if (raw != null && !raw.isEmpty()) {
                        arr = new org.json.JSONArray(raw);
                    } else {
                        arr = new org.json.JSONArray();
                    }
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("ts", now);
                    obj.put("q", pregunta);
                    obj.put("a", result);
                    arr.put(obj);
                    getSharedPreferences("ia_aprendizaje_log", MODE_PRIVATE).edit().putString("entries", arr.toString()).apply();
                } catch (Exception ignore) {}
                String correoUsuario = obtenerCorreoActivo();
                try {
                    // Guardar interacción en BD
                    DBVitalPress db = new DBVitalPress(ModoAprendizajeActivity.this);
                    String fecha = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(now));
                    db.insertIaAprendizaje(correoUsuario, pregunta, result, fecha);
                } catch (Exception ignore) {}
                // Regenerar DOCX
                try {
                    if (correoUsuario != null) {
                        java.io.File f = DocxExporter.generateUserDocx(ModoAprendizajeActivity.this, correoUsuario);
                        getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
                    }
                } catch (Exception ignore) {}
            }
        }.execute(pregunta);
    }
}
