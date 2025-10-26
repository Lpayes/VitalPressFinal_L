package com.example.vitalpreesoficial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

public class RegistroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        EditText editNombre = findViewById(R.id.edit_nombre);
        EditText editApellido = findViewById(R.id.edit_apellido); // Si existe en el layout
        EditText editCorreo = findViewById(R.id.edit_correo);
        EditText editContrasena = findViewById(R.id.edit_contrasena);
        EditText editContactoEmergencia = findViewById(R.id.edit_contacto_emergencia);
        EditText editEdad = findViewById(R.id.edit_edad); // Si existe en el layout
        EditText editEstatura = findViewById(R.id.edit_estatura); // Si existe en el layout
        EditText editPeso = findViewById(R.id.edit_peso); // Si existe en el layout
        EditText editSexo = findViewById(R.id.edit_sexo); // Si existe en el layout
        Button btnRegistrarse = findViewById(R.id.btn_registrarse);

        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = editNombre.getText().toString().trim();
                String apellido = editApellido != null ? editApellido.getText().toString().trim() : "";
                String correo = editCorreo.getText().toString().trim();
                String contrasena = editContrasena.getText().toString().trim();
                int edad = editEdad != null && !editEdad.getText().toString().isEmpty() ? Integer.parseInt(editEdad.getText().toString()) : 0;
                double estatura = editEstatura != null && !editEstatura.getText().toString().isEmpty() ? Double.parseDouble(editEstatura.getText().toString()) : 0.0;
                double peso = editPeso != null && !editPeso.getText().toString().isEmpty() ? Double.parseDouble(editPeso.getText().toString()) : 0.0;
                String sexo = editSexo != null ? editSexo.getText().toString().trim() : "";

                if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                    Toast.makeText(RegistroActivity.this, "Por favor, complete los campos obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!correo.matches("^[A-Za-z0-9+_.-]+@gmail\\.com$")) {
                    Toast.makeText(RegistroActivity.this, "Por favor ingresa una cuenta de correo válida (@gmail.com)", Toast.LENGTH_LONG).show();
                    return;
                }
                DBVitalPress db = new DBVitalPress(RegistroActivity.this);
                boolean registrado = db.registrarUsuarioSiNoExiste(nombre, apellido, correo, edad, estatura, peso, contrasena, sexo);
                if (!registrado) {
                    Toast.makeText(RegistroActivity.this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(RegistroActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();

                // Redirigir a la pantalla principal
                Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
