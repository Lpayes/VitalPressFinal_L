package com.example.vitalpreesoficial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;
    private static final int RC_RETRY_AUTH = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Botón Iniciar Sesión
        Button btnIniciarSesion = findViewById(R.id.btn_iniciar_sesion);
        btnIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Iniciar Google Sign-In para obtener token OAuth
                Intent signInIntent = OAuthHelper.getSignInIntent(MainActivity.this, DocAiConfig.CLIENT_ID);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        // Texto "Registrarse" clickeable
        TextView txtRegistrarse = findViewById(R.id.txt_registrarse);
        txtRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ir a pantalla de registro (usar Registro2Activity)
                Intent intent = new Intent(MainActivity.this, Registro2Activity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            OAuthHelper.handleSignInResult(requestCode, RC_SIGN_IN, data, this, new OAuthHelper.TokenCallback() {
                @Override
                public void onToken(String accessToken) {
                    // Guardar token y continuar a la app
                    SharedPreferences prefs = getSharedPreferences("oauth", MODE_PRIVATE);
                    prefs.edit().putString("access_token", accessToken).apply();

                    // Ir a InicioActivity
                    Intent intent = new Intent(MainActivity.this, InicioActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, "Error OAuth: " + error, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onRecoverable(Intent recoverIntent) {
                    // Lanzar intent recuperable para pedir permisos al usuario
                    startActivityForResult(recoverIntent, RC_RETRY_AUTH);
                }
            });
        } else if (requestCode == RC_RETRY_AUTH) {
            // Reintentar iniciar sign-in después de acción recuperable
            Intent signInIntent = OAuthHelper.getSignInIntent(this, DocAiConfig.CLIENT_ID);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_alerta) {
            // Verificar si hay contacto de emergencia registrado
            SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
            String contactoEmergencia = prefs.getString("contacto_emergencia", null);
            if (contactoEmergencia != null && !contactoEmergencia.isEmpty()) {
                Toast.makeText(this, "Llamando a: " + contactoEmergencia, Toast.LENGTH_SHORT).show();
                // Aquí se podría implementar la lógica para realizar la llamada
            } else {
                Toast.makeText(this, "No tiene un contacto de emergencia registrado.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}