package com.example.vitalpreesoficial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

public class InicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        // Referencias a los campos de correo y contraseña
        EditText editCorreo = findViewById(R.id.edit_correo_inicio);
        EditText editContrasena = findViewById(R.id.edit_contrasena_inicio);

        // Botón Iniciar Sesión
        Button btnIniciarSesion = findViewById(R.id.btn_iniciar_sesion_inicio);
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correo = editCorreo.getText().toString().trim();
                String contrasena = editContrasena.getText().toString().trim();
                if (correo.isEmpty() || contrasena.isEmpty()) {
                    android.widget.Toast.makeText(InicioActivity.this, "Completa todos los campos", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!esCorreoValido(correo)) {
                    android.widget.Toast.makeText(InicioActivity.this, "Correo no válido", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                DBVitalPress db = new DBVitalPress(InicioActivity.this);
                if (db.validarUsuario(correo, contrasena)) {
                    // Guardar correo en SharedPreferences para la sesión
                    android.content.SharedPreferences prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
                    prefs.edit().putString("correo", correo).apply();
                    // Acceso permitido
                    Intent intent = new Intent(InicioActivity.this, MenuPrincipalActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // No auto-registrar: solicitar registro
                    android.widget.Toast.makeText(InicioActivity.this, "Usuario no registrado o contraseña incorrecta. Regístrate.", android.widget.Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(InicioActivity.this, Registro2Activity.class);
                    intent.putExtra("correo_prefill", correo);
                    startActivity(intent);
                }
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_inicio);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Regresar al login
                finish();
            }
        });

        // Texto "Regístrate"
        TextView txtRegistrate = findViewById(R.id.txt_mensaje_registrate);
        txtRegistrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InicioActivity.this, Registro2Activity.class);
                // Prefill si el usuario ya escribió algo
                String correo = editCorreo.getText().toString().trim();
                if (!correo.isEmpty()) intent.putExtra("correo_prefill", correo);
                startActivity(intent);
            }
        });
    }

    private boolean esCorreoValido(String correo) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches();
    }
}
