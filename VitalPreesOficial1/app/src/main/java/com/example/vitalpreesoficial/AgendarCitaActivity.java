package com.example.vitalpreesoficial;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.vitalpreesoficial.db.DBVitalPress;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AgendarCitaActivity extends AppCompatActivity {
    private TextView tvNombre, tvCorreo, tvFechaHora;
    private EditText etSistolica, etDiastolica, etPulso, etComentario;
    private Button btnSeleccionarFechaHora, btnCrearCita, btnVerHistorial;
    private Calendar calendar = Calendar.getInstance();
    private String fechaIso = "";
    private String nombreUsuario = "";
    private String correoUsuario = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendar_cita);

        tvNombre = findViewById(R.id.tvNombre);
        tvCorreo = findViewById(R.id.tvCorreo);
        tvFechaHora = findViewById(R.id.tvFechaHora);
        etSistolica = findViewById(R.id.etSistolica);
        etDiastolica = findViewById(R.id.etDiastolica);
        etPulso = findViewById(R.id.etPulso);
        etComentario = findViewById(R.id.etComentario);
        btnSeleccionarFechaHora = findViewById(R.id.btnSeleccionarFechaHora);
        btnCrearCita = findViewById(R.id.btnCrearCita);
        btnVerHistorial = findViewById(R.id.btnVerHistorial);

        // Obtener correo del usuario desde SharedPreferences de sesión
        SharedPreferences prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        correoUsuario = prefs.getString("correo", "");
        // Consultar el nombre en la base de datos usando el correo
        DBVitalPress db = new DBVitalPress(this);
        String nombreBD = "";
        android.database.Cursor cursor = db.buscarUsuarioPorCorreo(correoUsuario);
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex("nombre");
            if (idx != -1) nombreBD = cursor.getString(idx);
            cursor.close();
        }
        nombreUsuario = nombreBD;
        tvNombre.setText("Nombre: " + nombreUsuario);
        tvCorreo.setText("Correo: " + correoUsuario);

        // Recuperar datos clínicos del historial local
        int sistolica = prefs.getInt("sistolica", -1);
        int diastolica = prefs.getInt("diastolica", -1);
        int pulso = prefs.getInt("pulso", -1);
        if (sistolica != -1) etSistolica.setText(String.valueOf(sistolica));
        if (diastolica != -1) etDiastolica.setText(String.valueOf(diastolica));
        if (pulso != -1) etPulso.setText(String.valueOf(pulso));

        // Selección de fecha y hora
        btnSeleccionarFechaHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDateTimePicker();
            }
        });

        // Crear cita
        btnCrearCita.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                crearCita(nombreUsuario, correoUsuario);
            }
        });

        // Ver historial
        btnVerHistorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(AgendarCitaActivity.this, HistorialCitasActivity.class));
            }
        });
    }

    private void mostrarDateTimePicker() {
        final Calendar actual = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog timePicker = new TimePickerDialog(this, (tpView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                fechaIso = sdf.format(calendar.getTime()) + "-06:00";
                tvFechaHora.setText("Fecha: " + fechaIso);
            }, actual.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.MINUTE), true);
            timePicker.show();
        }, actual.get(Calendar.YEAR), actual.get(Calendar.MONTH), actual.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void crearCita(String nombre, String correo) {
        String sistolicaStr = etSistolica.getText().toString();
        String diastolicaStr = etDiastolica.getText().toString();
        String pulsoStr = etPulso.getText().toString();
        String comentarioStr = etComentario.getText().toString();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(sistolicaStr) ||
                TextUtils.isEmpty(diastolicaStr) || TextUtils.isEmpty(pulsoStr) || TextUtils.isEmpty(fechaIso) || TextUtils.isEmpty(comentarioStr)) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int sistolica = Integer.parseInt(sistolicaStr);
        int diastolica = Integer.parseInt(diastolicaStr);
        int pulso = Integer.parseInt(pulsoStr);

        JSONObject citaJson = new JSONObject();
        try {
            citaJson.put("Nombre", nombre);
            citaJson.put("Correo", correo);
            citaJson.put("Sistolica", sistolica);
            citaJson.put("Diastolica", diastolica);
            citaJson.put("Pulso", pulso);
            citaJson.put("Fecha", fechaIso);
            citaJson.put("Comentario", comentarioStr);
        } catch (JSONException e) {
            Toast.makeText(this, "Error al crear JSON", Toast.LENGTH_SHORT).show();
            return;
        }

        enviarCitaAlWebhook(citaJson, sistolica, diastolica, pulso);
    }

    private void enviarCitaAlWebhook(JSONObject citaJson, int sistolica, int diastolica, int pulso) {
        String url = "https://primary-production-f943.up.railway.app/webhook/1ea7089c-40c7-43a4-a9c4-1da3bdd6c88f";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, citaJson,
                response -> {
                    guardarCitaLocal(fechaIso, sistolica, diastolica, pulso);
                    Toast.makeText(this, "Cita agendada correctamente", Toast.LENGTH_LONG).show();
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 200) {
                        guardarCitaLocal(fechaIso, sistolica, diastolica, pulso);
                        Toast.makeText(this, "Cita agendada correctamente", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No se pudo agendar la cita. Intenta nuevamente", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        queue.add(request);
    }

    private void guardarCitaLocal(String fecha, int sistolica, int diastolica, int pulso) {
        SharedPreferences prefs = getSharedPreferences("historial_citas", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String cita = fecha + "," + sistolica + "," + diastolica + "," + pulso;
        int total = prefs.getInt("total", 0);
        editor.putString("cita_" + total, cita);
        editor.putInt("total", total + 1);
        editor.apply();
    }
}
