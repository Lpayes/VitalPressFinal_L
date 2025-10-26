package com.example.vitalpreesoficial;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.io.File;

public class ActualizarDatosActivity extends AppCompatActivity {

    // [MODIFICACIÓN] Conexión a la base de datos VitalPress
    private DBVitalPress dbVitalPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actualizar_datos);

        // [MODIFICACIÓN] Inicialización de la base de datos
        dbVitalPress = new DBVitalPress(this);

        // Spinner Sexo
        Spinner spinnerSexo = findViewById(R.id.spinner_sexo_actualizar);
        ArrayAdapter<CharSequence> adapterSexo = ArrayAdapter.createFromResource(this, R.array.sexo_array, android.R.layout.simple_spinner_item);
        adapterSexo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSexo.setAdapter(adapterSexo);
        spinnerSexo.setEnabled(false);

        // Cargar datos del usuario activo
        SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
        String correo = prefs.getString("correo", null);
        TextView txtResumen = findViewById(R.id.txt_resumen_actualizar); // TextView agregado en layout
        if (correo != null) {
            Cursor cursor = dbVitalPress.buscarUsuarioPorCorreo(correo);
            if (cursor.moveToFirst()) {
                ((EditText)findViewById(R.id.edit_nombre_actualizar)).setText(cursor.getString(cursor.getColumnIndexOrThrow("nombre")));
                ((EditText)findViewById(R.id.edit_apellido_actualizar)).setText(cursor.getString(cursor.getColumnIndexOrThrow("apellido")));
                ((EditText)findViewById(R.id.edit_correo_actualizar)).setText(cursor.getString(cursor.getColumnIndexOrThrow("correo")));
                ((EditText)findViewById(R.id.edit_edad_actualizar)).setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("edad"))));
                ((EditText)findViewById(R.id.edit_estatura_actualizar)).setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("estatura"))));
                ((EditText)findViewById(R.id.edit_peso_actualizar)).setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("peso"))));
                ((EditText)findViewById(R.id.edit_contrasena_actualizar)).setText(cursor.getString(cursor.getColumnIndexOrThrow("contrasena")));
                // Sexo
                String sexo = cursor.getString(cursor.getColumnIndexOrThrow("sexo"));
                int pos = adapterSexo.getPosition(sexo);
                spinnerSexo.setSelection(pos);
            }
            cursor.close();

            // Mostrar resumen personalizado del usuario (solo sus datos)
            if (txtResumen != null) {
                String resumen = computeResumen(correo);
                txtResumen.setText(resumen);
            }
        }

        // Botón Actualizar
        Button btnActualizar = findViewById(R.id.btn_actualizar);
        btnActualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editNombre = findViewById(R.id.edit_nombre_actualizar);
                EditText editApellido = findViewById(R.id.edit_apellido_actualizar);
                EditText editCorreo = findViewById(R.id.edit_correo_actualizar);
                EditText editEdad = findViewById(R.id.edit_edad_actualizar);
                EditText editEstatura = findViewById(R.id.edit_estatura_actualizar);
                EditText editPeso = findViewById(R.id.edit_peso_actualizar);
                EditText editContrasena = findViewById(R.id.edit_contrasena_actualizar);
                String nombre = editNombre.getText().toString();
                String apellido = editApellido.getText().toString();
                String correo = editCorreo.getText().toString();
                int edad = editEdad.getText().toString().isEmpty() ? 0 : Integer.parseInt(editEdad.getText().toString());
                double estatura = editEstatura.getText().toString().isEmpty() ? 0 : Double.parseDouble(editEstatura.getText().toString());
                double peso = editPeso.getText().toString().isEmpty() ? 0 : Double.parseDouble(editPeso.getText().toString());
                String contrasena = editContrasena.getText().toString();
                String sexo = spinnerSexo.getSelectedItem().toString();
                // Actualizar todos los datos por correo
                dbVitalPress.updateUsuarioNombreCompleto(correo, nombre, apellido);
                Cursor cursor = dbVitalPress.buscarUsuarioPorCorreo(correo);
                if (cursor.moveToFirst()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    dbVitalPress.updateUsuario(id, nombre, correo, edad, estatura, peso, contrasena, sexo);
                }
                cursor.close();
                try {
                    File f = DocxExporter.generateUserDocx(ActualizarDatosActivity.this, correo);
                    // Guardar ruta por usuario (clave por correo) para evitar mezclar con otros usuarios
                    getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path_" + correo, f.getAbsolutePath()).apply();
                } catch (Exception ignore) {}

                // Actualizar resumen en pantalla después de cambios
                if (correo != null && txtResumen != null) {
                    String nuevoResumen = computeResumen(correo);
                    txtResumen.setText(nuevoResumen);
                }
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_actualizar);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Cálculo automático del IMC al actualizar datos
        EditText editPeso = findViewById(R.id.edit_peso_actualizar);
        EditText editEstatura = findViewById(R.id.edit_estatura_actualizar);
        TextView txtIMC = findViewById(R.id.txt_imc_actualizar);

        View.OnFocusChangeListener imcCalculator = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    calcularIMC(editPeso, editEstatura, txtIMC);
                }
            }
        };

        editPeso.setOnFocusChangeListener(imcCalculator);
        editEstatura.setOnFocusChangeListener(imcCalculator);
    }

    private void calcularIMC(EditText editPeso, EditText editEstatura, TextView txtIMC) {
        try {
            String pesoStr = editPeso.getText().toString();
            String estaturaStr = editEstatura.getText().toString();

            if (!pesoStr.isEmpty() && !estaturaStr.isEmpty()) {
                double peso = Double.parseDouble(pesoStr);
                double estatura = Double.parseDouble(estaturaStr);

                if (estatura > 0) {
                    double imc = peso / (estatura * estatura);
                    txtIMC.setText(String.format("%.2f", imc));
                }
            }
        } catch (NumberFormatException e) {
            // Error en el formato de números
        }
    }

    // Genera un resumen simple basado en la última lectura y últimas hasta 5 lecturas
    private String computeResumen(String correo) {
        if (correo == null || correo.isEmpty()) return "No hay usuario activo.";
        Cursor latest = dbVitalPress.getUltimaPresionDetallada(correo);
        try {
            if (latest != null && latest.moveToFirst()) {
                int sistolica = latest.getInt(latest.getColumnIndexOrThrow("sistolica"));
                int diastolica = latest.getInt(latest.getColumnIndexOrThrow("diastolica"));
                String estado;
                if (sistolica < 120 && diastolica < 80) estado = "Normal";
                else if (sistolica < 130 && diastolica < 80) estado = "Elevada";
                else if (sistolica < 140 || diastolica < 90) estado = "Hipertensión grado 1";
                else if (sistolica < 180 || diastolica < 120) estado = "Hipertensión grado 2";
                else estado = "Crisis hipertensiva";

                // promedio de últimas hasta 5 lecturas
                Cursor hist = dbVitalPress.getHistorialDetallado(correo);
                int count = 0; int sumS = 0, sumD = 0;
                if (hist != null) {
                    while (hist.moveToNext() && count < 5) {
                        sumS += hist.getInt(hist.getColumnIndexOrThrow("sistolica"));
                        sumD += hist.getInt(hist.getColumnIndexOrThrow("diastolica"));
                        count++;
                    }
                    hist.close();
                }

                if (count > 0) {
                    double avgS = sumS / (double) count;
                    double avgD = sumD / (double) count;
                    String trend;
                    if (avgS < sistolica && avgD < diastolica) trend = "Tendencia al alza";
                    else if (avgS > sistolica && avgD > diastolica) trend = "Tendencia a la baja";
                    else trend = "Estable";
                    return String.format("Última: %d/%d mmHg — %s. Promedio últimas %d: %.0f/%.0f mmHg (%s).",
                            sistolica, diastolica, estado, count, avgS, avgD, trend);
                } else {
                    return String.format("Última: %d/%d mmHg — %s. No hay historial suficiente.", sistolica, diastolica, estado);
                }
            } else {
                return "No hay lecturas registradas.";
            }
        } finally {
            if (latest != null) latest.close();
        }
    }
}
