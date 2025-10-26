package com.example.vitalpreesoficial;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MenuPrincipalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        // Botones arriba del corazón
        Button btnChatIA = findViewById(R.id.btn_chat_ia);
        Button btnSeccionDatos = findViewById(R.id.btn_seccion_datos);
        Button btnRecordatorio = findViewById(R.id.btn_recordatorio);

        // Botones debajo del corazón
        Button btnRecomendaciones = findViewById(R.id.btn_recomendaciones);
        Button btnActualizarDatos = findViewById(R.id.btn_actualizar_datos);
        Button btnVerInfo = findViewById(R.id.btn_ver_info);
        Button btnLlamarEmergencia = findViewById(R.id.btn_llamar_emergencia);
        Button btnAgendarCita = findViewById(R.id.btn_agendar_cita);

        // Icono de ajustes
        ImageView iconoAjustes = findViewById(R.id.icono_ajustes);

        // Configurar listeners
        btnChatIA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, ChatIAActivity.class);
                startActivity(intent);
            }
        });

        btnSeccionDatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, SeccionDatosActivity.class);
                startActivity(intent);
            }
        });

        btnRecordatorio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, RecordatorioActivity.class);
                startActivity(intent);
            }
        });

        btnRecomendaciones.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, RecomendacionesActivity.class);
                startActivity(intent);
            }
        });

        btnActualizarDatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, ActualizarDatosActivity.class);
                startActivity(intent);
            }
        });

        btnVerInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, VerInfoActivity.class);
                startActivity(intent);
            }
        });

        btnLlamarEmergencia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
                String contactoEmergencia = prefs.getString("contacto_emergencia", null);

                if (contactoEmergencia != null && !contactoEmergencia.isEmpty()) {
                    if (ContextCompat.checkSelfPermission(MenuPrincipalActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MenuPrincipalActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + contactoEmergencia));
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MenuPrincipalActivity.this, "No tiene un contacto de emergencia registrado.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Configurar listener para el botón Agendar cita
        btnAgendarCita.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuPrincipalActivity.this, AgendarCitaActivity.class);
                startActivity(intent);
            }
        });

        iconoAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Mostrar un mensaje para confirmar que se presionó el icono
                    Toast.makeText(MenuPrincipalActivity.this, "Abriendo Ajustes...", Toast.LENGTH_SHORT).show();

                    // Iniciar la actividad de Ajustes de forma segura
                    Intent intent = new Intent(MenuPrincipalActivity.this, AjustesActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    // Manejar cualquier error que pueda ocurrir
                    Toast.makeText(MenuPrincipalActivity.this, "Error al abrir ajustes", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted: intentar llamar al contacto de emergencia
                SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
                String contactoEmergencia = prefs.getString("contacto_emergencia", null);
                if (contactoEmergencia != null && !contactoEmergencia.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + contactoEmergencia));
                        startActivity(intent);
                    } catch (SecurityException e) {
                        Toast.makeText(this, "Permiso denegado para realizar llamadas.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No tiene un contacto de emergencia registrado.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Permiso de llamada no otorgado.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
