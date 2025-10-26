package com.example.vitalpreesoficial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecomendacionesActivity extends AppCompatActivity {

    private TextView txtConsejos;
    private TextView txtVideos;
    private TextView txtInstrucciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recomendaciones);

        txtConsejos = findViewById(R.id.txt_consejos_generales);
        txtVideos = findViewById(R.id.txt_videos_sugeridos);
        txtInstrucciones = findViewById(R.id.txt_instrucciones_app);

        Button btnRegresar = findViewById(R.id.btn_regresar_recomendaciones);
        btnRegresar.setOnClickListener(v -> finish());

        // Inicial
        refrescarContenido();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refrescarContenido();
    }

    private void refrescarContenido() {
        String correo = obtenerCorreoActivo();
        int edad = 0; double est = 0; double peso = 0; String sexo = "";
        Integer sis = null, dia = null, pul = null;
        try {
            DBVitalPress db = new DBVitalPress(this);
            Cursor c = db.buscarUsuarioPorCorreo(correo);
            if (c != null && c.moveToFirst()) {
                edad = c.getInt(c.getColumnIndexOrThrow("edad"));
                est = c.getDouble(c.getColumnIndexOrThrow("estatura"));
                peso = c.getDouble(c.getColumnIndexOrThrow("peso"));
                sexo = c.getString(c.getColumnIndexOrThrow("sexo"));
            }
            if (c != null) c.close();
            Cursor cu = db.getUltimaPresionDetallada(correo);
            if (cu != null && cu.moveToFirst()) {
                sis = cu.getInt(0); dia = cu.getInt(1); pul = cu.getInt(2);
            }
            if (cu != null) cu.close();
        } catch (Exception ignore) {}
        double imc = (est > 0) ? (peso / (est * est)) : 0;

        String consejos = generarConsejos(edad, imc, sis, dia, pul, sexo);
        String videos = generarVideosSugeridos10(edad, imc, sis, dia, sexo);
        String instrucciones = generarInstruccionesApp();

        txtConsejos.setText(consejos);
        txtVideos.setText(videos);
        txtInstrucciones.setText(instrucciones);

        // Botón IA (si existe)
        int idIA = getResources().getIdentifier("btn_generar_con_ia", "id", getPackageName());
        if (idIA != 0) {
            Button btnIA = findViewById(idIA);
            final String promptIa = construirPromptIA(edad, imc, sis, dia, pul, sexo);
            btnIA.setOnClickListener(v -> {
                Intent intent = new Intent(RecomendacionesActivity.this, ModoAprendizajeActivity.class);
                intent.putExtra("prompt_inicial", promptIa);
                startActivity(intent);
            });
        }

        hacerLinksClickeables(txtVideos);
    }

    private String obtenerCorreoActivo() {
        SharedPreferences p1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        String correo = p1.getString("correo", null);
        if (correo == null) correo = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
        return correo;
    }

    private String generarConsejos(int edad, double imc, Integer sis, Integer dia, Integer pul, String sexo) {
        List<String> lines = new ArrayList<>();
        if (sexo != null && !sexo.trim().isEmpty()) {
            lines.add("Perfil: " + sexo + (edad > 0 ? ", " + edad + " años." : "."));
        } else if (edad > 0) {
            lines.add("Perfil: " + edad + " años.");
        }
        if (imc > 0) {
            lines.add(String.format(Locale.getDefault(), "IMC: %.2f.", imc));
            if (imc < 18.5) {
                lines.add("Aumenta calorías saludables (frutos secos, lácteos, legumbres) y proteínas; consulta nutrición.");
            } else if (imc < 25) {
                lines.add("Mantén dieta equilibrada (DASH/mediterránea), buena hidratación y actividad regular.");
            } else if (imc < 30) {
                lines.add("Déficit calórico moderado, 150-300 min/sem de cardio suave y fuerza 2x/sem.");
            } else {
                lines.add("Cambios graduales, control médico y actividad de bajo impacto (caminar, agua, bici estática).");
            }
        } else {
            lines.add("Completa estatura y peso para ajustar recomendaciones por IMC.");
        }
        if (edad >= 60) lines.add("Prioriza equilibrio, fuerza ligera y chequeos periódicos.");
        else if (edad >= 40) lines.add("Gestiona estrés, duerme 7-9 h y controla factores de riesgo.");
        else if (edad > 0) lines.add("Consolida hábitos saludables y chequeos preventivos.");
        if (sis != null && dia != null) {
            if (sis >= 140 || dia >= 90) {
                lines.add("Hipertensión: reduce sodio (<1500 mg/d), limita alcohol, aumenta potasio y fibra.");
                lines.add("Monitorea PA 3-4 veces/sem y sigue indicaciones médicas.");
            } else if (sis >= 130 || dia >= 85) {
                lines.add("PA elevada: ajusta dieta (menos ultraprocesados), +actividad y control del estrés.");
            } else if (sis < 90 || dia < 60) {
                lines.add("PA baja: hidrátate, levántate despacio y consulta si hay mareos/síncope.");
            } else {
                lines.add("PA en rango: mantén hábitos y controles periódicos.");
            }
        } else {
            lines.add("Registra tu PA (sistólica/diastólica) para recomendaciones más precisas.");
        }
        if (pul != null && (pul < 50 || pul > 100)) {
            lines.add("Pulso fuera de rango: comenta con tu médico si hay síntomas.");
        }
        return "• " + String.join("\n• ", lines);
    }

    private String generarVideosSugeridos10(int edad, double imc, Integer sis, Integer dia, String sexo) {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Qué es la dieta DASH (guía básica)", "https://www.paho.org/es/temas/hipertension");
        items.put("Cómo medir correctamente la presión arterial en casa", "https://www.youtube.com/results?search_query=como+medir+presion+arterial+en+casa");
        items.put("Técnicas de respiración para reducir el estrés", "https://www.youtube.com/results?search_query=respiracion+profunda+estres");
        items.put("Sueño y salud cardiometabólica: recomendaciones", "https://www.youtube.com/results?search_query=higiene+del+sue%C3%B1o+recomendaciones");
        if (imc >= 30) {
            items.put("Ejercicios de bajo impacto (obesidad): rutina segura", "https://www.youtube.com/results?search_query=ejercicios+bajo+impacto+obesidad");
        } else if (imc >= 25) {
            items.put("Cardio bajo impacto para principiantes", "https://www.youtube.com/results?search_query=cardio+bajo+impacto+principiantes");
        } else if (imc > 0 && imc < 18.5) {
            items.put("Alimentación para ganar peso saludablemente", "https://www.youtube.com/results?search_query=ganar+peso+saludable+nutricion");
        } else {
            items.put("Plan semanal de alimentación equilibrada", "https://www.youtube.com/results?search_query=menu+saludable+semanal");
        }
        if (sis != null && dia != null && (sis >= 130 || dia >= 85)) {
            items.put("Hábitos para bajar la presión arterial", "https://www.youtube.com/results?search_query=reducir+presion+arterial+habitos");
            items.put("Dieta baja en sodio: ideas prácticas", "https://www.youtube.com/results?search_query=dieta+baja+en+sodio+recetas");
        } else if (sis != null && dia != null && (sis < 90 || dia < 60)) {
            items.put("PA baja: hidratación y precauciones", "https://www.youtube.com/results?search_query=presion+arterial+baja+que+hacer");
        } else {
            items.put("Mantenimiento de PA saludable", "https://www.youtube.com/results?search_query=presion+arterial+saludable+mantenimiento");
        }
        if (edad >= 60) {
            items.put("Fuerza y equilibrio para mayores", "https://www.youtube.com/results?search_query=ejercicios+equilibrio+adultos+mayores");
        } else if (edad >= 40) {
            items.put("Salud a los 40+: chequeos y hábitos", "https://www.youtube.com/results?search_query=salud+40+chequeos+habitos");
        } else if (edad > 0) {
            items.put("Rutina saludable para adultos jóvenes", "https://www.youtube.com/results?search_query=habitos+saludables+adultos+jovenes");
        }
        String[][] relleno = new String[][]{
                {"Introducción a la dieta mediterránea", "https://www.youtube.com/results?search_query=dieta+mediterranea+explicacion"},
                {"Cómo leer etiquetas nutricionales", "https://www.youtube.com/results?search_query=como+leer+etiquetas+nutricionales"},
                {"Rutina de movilidad articular diaria", "https://www.youtube.com/results?search_query=rutina+movilidad+articular"},
                {"Mindfulness para principiantes", "https://www.youtube.com/results?search_query=mindfulness+principiantes"},
                {"Hidratación: cuánta agua necesitas", "https://www.youtube.com/results?search_query=cuanta+agua+beber+al+dia"}
        };
        int idx = 0;
        while (items.size() < 10 && idx < relleno.length) {
            items.put(relleno[idx][0], relleno[idx][1]);
            idx++;
        }
        List<Map.Entry<String, String>> list = new ArrayList<>(items.entrySet());
        if (list.size() > 10) list = list.subList(0, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Map.Entry<String, String> e = list.get(i);
            sb.append(i + 1).append(") ").append(e.getKey()).append(" – ").append(e.getValue());
            if (i < list.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private String generarInstruccionesApp() {
        return "1) Inicia sesión: correo y contraseña en la pantalla principal.\n" +
               "2) Datos: ingresa o escanea tu presión (sistólica/diastólica/pulso) y guarda.\n" +
               "3) Escanear/Foto: toma o elige imagen; verás ‘Extrayendo datos del dispositivo…’ y luego ‘Texto extraído: …’.\n" +
               "4) Importar archivo: selecciona un PDF; verás ‘Importando archivo…’.\n" +
               "5) Historial: revisa tus registros y evolución.\n" +
               "6) Recomendaciones: verás consejos y 10 videos sugeridos según tu perfil.\n" +
               "7) IA (Generar con IA): crea consejos y videos personalizados; si cambias datos, vuelve para actualizarlos.";
    }

    private String construirPromptIA(int edad, double imc, Integer sis, Integer dia, Integer pul, String sexo) {
        String sImc = imc > 0 ? String.format(Locale.getDefault(), "%.2f", imc) : "no disponible";
        String sPa = (sis != null && dia != null) ? (sis + "/" + dia + (pul != null ? (", pulso " + pul) : "")) : "no disponible";
        return "Eres un asistente de salud. Con la siguiente información del usuario: " +
                "edad=" + (edad > 0 ? edad : "no disponible") + ", sexo=" + (sexo != null ? sexo : "-") + ", IMC=" + sImc + ", última PA=" + sPa + ". " +
                "1) Genera consejos generales personalizados y prácticos en viñetas (dieta, actividad, sueño, estrés, señales de alerta). " +
                "2) Propón 10 enlaces de videos en español con una breve descripción de cada uno (formato: titulo – URL). " +
                "Sé claro y conciso, y adapta todo al perfil y valores actuales del usuario.";
    }

    private void hacerLinksClickeables(TextView tv) {
        try {
            tv.setAutoLinkMask(android.text.util.Linkify.WEB_URLS);
            tv.setLinksClickable(true);
            tv.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        } catch (Exception ignore) {}
    }
}
