package com.example.vitalpreesoficial;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecordatorioActivity extends AppCompatActivity {

    private LinearLayout layoutFormulario;
    private LinearLayout containerRecordatorios;
    private TextView txtTituloRecordatorios;
    private EditText editFecha, editHora, editDescripcion;
    private List<Recordatorio> listaRecordatorios;
    private int recordatorioEditandoIndex = -1; // Para saber si estamos editando o agregando nuevo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordatorio);

        // Inicializar vistas
        initViews();

        // Inicializar lista de recordatorios
        listaRecordatorios = new ArrayList<>();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        layoutFormulario = findViewById(R.id.layout_formulario_recordatorio);
        containerRecordatorios = findViewById(R.id.container_recordatorios);
        txtTituloRecordatorios = findViewById(R.id.txt_titulo_recordatorios);
        editFecha = findViewById(R.id.edit_fecha);
        editHora = findViewById(R.id.edit_hora);
        editDescripcion = findViewById(R.id.edit_descripcion);
    }

    private void setupListeners() {
        // Clic en imagen para mostrar/ocultar formulario
        ImageView imagenRecordatorio = findViewById(R.id.imagen_recordatorio);
        imagenRecordatorio.setOnClickListener(v -> toggleFormulario());

        // Selectores de fecha y hora
        editFecha.setOnClickListener(v -> mostrarSelectorFecha());
        editHora.setOnClickListener(v -> mostrarSelectorHora());

        // Botones del formulario
        Button btnAceptar = findViewById(R.id.btn_aceptar_recordatorio);
        Button btnCancelar = findViewById(R.id.btn_cancelar_recordatorio);

        btnAceptar.setOnClickListener(v -> guardarRecordatorio());
        btnCancelar.setOnClickListener(v -> cancelarFormulario());

        // Botón regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_recordatorio);
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void toggleFormulario() {
        if (layoutFormulario.getVisibility() == View.GONE) {
            layoutFormulario.setVisibility(View.VISIBLE);
            limpiarFormulario();
            recordatorioEditandoIndex = -1; // Nuevo recordatorio
            // Calcular y mostrar sugerencia de frecuencia según edad del usuario
            calcularSugerenciaFrecuencia();
        } else {
            layoutFormulario.setVisibility(View.GONE);
        }
    }

    // Calcula sugerencia de frecuencia de toma de presión según la edad del usuario
    private void calcularSugerenciaFrecuencia() {
        TextView txtSugerencia = findViewById(R.id.txt_sugerencia_frecuencia);
        SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
        String correo = prefs.getString("correo", null);
        int edad = -1;
        if (correo != null) {
            DBVitalPress db = new DBVitalPress(this);
            Cursor cursor = db.buscarUsuarioPorCorreo(correo);
            if (cursor.moveToFirst()) {
                try {
                    edad = cursor.getInt(cursor.getColumnIndexOrThrow("edad"));
                } catch (Exception e) {
                    edad = -1;
                }
            }
            cursor.close();
        }
        String sugerencia;
        if (edad <= 0) {
            sugerencia = "No hay edad registrada. Completa tu perfil para sugerencias.";
        } else if (edad < 30) {
            sugerencia = "Sugerencia: medir la presión una vez a la semana.";
        } else if (edad < 50) {
            sugerencia = "Sugerencia: medir la presión 3 veces por semana.";
        } else if (edad < 65) {
            sugerencia = "Sugerencia: medir la presión cada 2 días.";
        } else {
            sugerencia = "Sugerencia: medir la presión diariamente.";
        }
        txtSugerencia.setText(sugerencia);
    }

    private void mostrarSelectorFecha() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                editFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void mostrarSelectorHora() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                editHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        timePickerDialog.show();
    }

    private void guardarRecordatorio() {
        String fecha = editFecha.getText().toString().trim();
        String hora = editHora.getText().toString().trim();
        String descripcion = editDescripcion.getText().toString().trim();

        if (fecha.isEmpty() || hora.isEmpty() || descripcion.isEmpty()) {
            // Mostrar mensaje de error
            new AlertDialog.Builder(this)
                .setTitle("Campos incompletos")
                .setMessage("Por favor, completa todos los campos")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        Recordatorio recordatorio = new Recordatorio(fecha, hora, descripcion);

        if (recordatorioEditandoIndex >= 0) {
            // Actualizando recordatorio existente
            listaRecordatorios.set(recordatorioEditandoIndex, recordatorio);
            // Reprogramar alarma
            scheduleAlarmForRecordatorio(recordatorio, recordatorioEditandoIndex);
            recordatorioEditandoIndex = -1;
        } else {
            // Agregando nuevo recordatorio
            listaRecordatorios.add(recordatorio);
            int newIndex = listaRecordatorios.size() - 1;
            scheduleAlarmForRecordatorio(recordatorio, newIndex);
        }

        actualizarListaRecordatorios();
        layoutFormulario.setVisibility(View.GONE);
        limpiarFormulario();
    }

    // Programa una alarma usando AlarmManager para el recordatorio
    private void scheduleAlarmForRecordatorio(Recordatorio recordatorio, int id) {
        try {
            String fecha = recordatorio.getFecha(); // formato dd/MM/yyyy
            String hora = recordatorio.getHora();   // formato HH:mm
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(fecha + " " + hora);
            if (date == null) return;
            long triggerAtMillis = date.getTime();
            if (triggerAtMillis < System.currentTimeMillis()) return; // No programar en el pasado

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("descripcion", recordatorio.getDescripcion());
            intent.putExtra("id", id);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelarFormulario() {
        layoutFormulario.setVisibility(View.GONE);
        limpiarFormulario();
        recordatorioEditandoIndex = -1;
    }

    private void limpiarFormulario() {
        editFecha.setText("");
        editHora.setText("");
        editDescripcion.setText("");
    }

    private void actualizarListaRecordatorios() {
        containerRecordatorios.removeAllViews();

        if (listaRecordatorios.isEmpty()) {
            txtTituloRecordatorios.setVisibility(View.GONE);
            return;
        }

        txtTituloRecordatorios.setVisibility(View.VISIBLE);

        for (int i = 0; i < listaRecordatorios.size(); i++) {
            final int index = i;
            Recordatorio recordatorio = listaRecordatorios.get(i);

            View itemView = LayoutInflater.from(this).inflate(R.layout.item_recordatorio, containerRecordatorios, false);

            TextView txtFecha = itemView.findViewById(R.id.txt_fecha_recordatorio);
            TextView txtHora = itemView.findViewById(R.id.txt_hora_recordatorio);
            TextView txtDescripcion = itemView.findViewById(R.id.txt_descripcion_recordatorio);
            ImageView btnMenu = itemView.findViewById(R.id.btn_menu_recordatorio);

            txtFecha.setText(recordatorio.getFecha());
            txtHora.setText(recordatorio.getHora());
            txtDescripcion.setText(recordatorio.getDescripcion());

            // Configurar menú de opciones (tres puntitos)
            btnMenu.setOnClickListener(v -> mostrarMenuOpciones(v, index));

            containerRecordatorios.addView(itemView);
        }
    }

    private void mostrarMenuOpciones(View view, int index) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_recordatorio, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_editar) {
                editarRecordatorio(index);
                return true;
            } else if (item.getItemId() == R.id.action_eliminar) {
                eliminarRecordatorio(index);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void editarRecordatorio(int index) {
        Recordatorio recordatorio = listaRecordatorios.get(index);

        editFecha.setText(recordatorio.getFecha());
        editHora.setText(recordatorio.getHora());
        editDescripcion.setText(recordatorio.getDescripcion());

        recordatorioEditandoIndex = index;
        layoutFormulario.setVisibility(View.VISIBLE);
    }

    private void eliminarRecordatorio(int index) {
        new AlertDialog.Builder(this)
            .setTitle("Eliminar recordatorio")
            .setMessage("¿Estás seguro de que quieres eliminar este recordatorio?")
            .setPositiveButton("Eliminar", (dialog, which) -> {
                listaRecordatorios.remove(index);
                actualizarListaRecordatorios();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // Clase interna para representar un recordatorio
    private static class Recordatorio {
        private String fecha;
        private String hora;
        private String descripcion;

        public Recordatorio(String fecha, String hora, String descripcion) {
            this.fecha = fecha;
            this.hora = hora;
            this.descripcion = descripcion;
        }

        public String getFecha() { return fecha; }
        public String getHora() { return hora; }
        public String getDescripcion() { return descripcion; }
    }
}
