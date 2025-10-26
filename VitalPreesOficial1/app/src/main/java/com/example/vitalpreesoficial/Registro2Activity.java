package com.example.vitalpreesoficial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

public class Registro2Activity extends AppCompatActivity {

    // [MODIFICACIÓN] Conexión a la base de datos VitalPress
    private DBVitalPress dbVitalPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro2);

        // [MODIFICACIÓN] Inicialización de la base de datos
        dbVitalPress = new DBVitalPress(this);

        // Spinner para seleccionar el sexo
        Spinner spinnerSexo = findViewById(R.id.spinner_sexo);
        ArrayAdapter<CharSequence> adapterSexo = ArrayAdapter.createFromResource(this, R.array.sexo_array, android.R.layout.simple_spinner_item);
        adapterSexo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSexo.setAdapter(adapterSexo);

        // Referencias a los campos
        EditText editNombreApellido = findViewById(R.id.edit_nombre_apellido);
        EditText editCorreo = findViewById(R.id.edit_correo);
        EditText editEdad = findViewById(R.id.edit_edad);
        EditText editEstatura = findViewById(R.id.edit_estatura);
        EditText editPeso = findViewById(R.id.edit_peso);
        EditText editContrasena = findViewById(R.id.edit_contrasena);
        EditText editContacto = findViewById(R.id.edit_contacto_emergencia);

        // Prefill correo si viene del login
        String pre = getIntent() != null ? getIntent().getStringExtra("correo_prefill") : null;
        if (pre != null && !pre.isEmpty()) editCorreo.setText(pre);

        // Botón Registrarse
        Button btnRegistrarse = findViewById(R.id.btn_registrarse);
        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombreApellido = editNombreApellido.getText().toString().trim();
                String correo = editCorreo.getText().toString().trim();
                String edadStr = editEdad.getText().toString().trim();
                String estaturaStr = editEstatura.getText().toString().trim();
                String pesoStr = editPeso.getText().toString().trim();
                String contrasena = editContrasena.getText().toString().trim();
                String sexo = spinnerSexo.getSelectedItem().toString();
                String contactoEmergencia = editContacto.getText().toString().trim();

                if (nombreApellido.isEmpty() || correo.isEmpty() || edadStr.isEmpty() || estaturaStr.isEmpty() || pesoStr.isEmpty() || contrasena.isEmpty()) {
                    android.widget.Toast.makeText(Registro2Activity.this, "Completa todos los campos", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                    android.widget.Toast.makeText(Registro2Activity.this, "Correo no válido", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                // Evitar duplicados
                android.database.Cursor c = dbVitalPress.buscarUsuarioPorCorreo(correo);
                boolean existe = c != null && c.moveToFirst();
                if (c != null) c.close();
                if (existe) {
                    android.widget.Toast.makeText(Registro2Activity.this, "Ese correo ya está registrado. Inicia sesión.", android.widget.Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Registro2Activity.this, InicioActivity.class);
                    intent.putExtra("correo_prefill", correo);
                    startActivity(intent);
                    return;
                }

                int edad;
                double estatura; double peso;
                try {
                    edad = Integer.parseInt(edadStr);
                    estatura = Double.parseDouble(estaturaStr);
                    peso = Double.parseDouble(pesoStr);
                } catch (NumberFormatException e) {
                    android.widget.Toast.makeText(Registro2Activity.this, "Valores numéricos inválidos", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // Separar nombre y apellido si el usuario ingresó ambos
                String nombre = nombreApellido;
                String apellido = "";
                int firstSpace = nombreApellido.indexOf(' ');
                if (firstSpace > 0) {
                    nombre = nombreApellido.substring(0, firstSpace).trim();
                    apellido = nombreApellido.substring(firstSpace + 1).trim();
                }

                dbVitalPress.insertUsuario(nombre, apellido, correo, edad, estatura, peso, contrasena, sexo);

                // Guardar usuario activo en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("correo", correo);
                // Guardar contacto de emergencia (opcional)
                if (!contactoEmergencia.isEmpty()) {
                    editor.putString("contacto_emergencia", contactoEmergencia);
                }
                // Guardar fecha de registro
                String fechaRegistro = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                editor.putString("fecha_registro", fechaRegistro);
                editor.apply();

                // Generar DOCX inicial del usuario
                try {
                    File f = DocxExporter.generateUserDocx(Registro2Activity.this, correo);
                    getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
                } catch (Exception ignore) {}

                android.widget.Toast.makeText(Registro2Activity.this, "Registro exitoso. Ahora inicia sesión.", android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Registro2Activity.this, InicioActivity.class);
                intent.putExtra("correo_prefill", correo);
                startActivity(intent);
                finish();
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Regresar a la pantalla de login
                finish();
            }
        });

        // Cálculo automático del IMC
        EditText editPeso2 = findViewById(R.id.edit_peso);
        EditText editEstatura2 = findViewById(R.id.edit_estatura);
        TextView txtIMC = findViewById(R.id.txt_imc);

        // Listeners para calcular IMC automáticamente
        View.OnFocusChangeListener imcCalculator = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    calcularIMC(editPeso2, editEstatura2, txtIMC);
                }
            }
        };

        editPeso2.setOnFocusChangeListener(imcCalculator);
        editEstatura2.setOnFocusChangeListener(imcCalculator);
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
                    txtIMC.setText(String.format(Locale.getDefault(), "%.2f", imc));
                }
            }
        } catch (NumberFormatException e) {
            // Error en el formato de números
        }
    }
}
