package com.example.vitalpreesoficial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ModosIAActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modos_ia);

        // Botón Modo Aprendizaje
        Button btnModoAprendizaje = findViewById(R.id.btn_modo_aprendizaje);
        btnModoAprendizaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí iría la lógica del Modo Aprendizaje
                // Por ahora solo muestra un mensaje o regresa
            }
        });

        // Botón Modo Enseñanza
        Button btnModoEnseñanza = findViewById(R.id.btn_modo_ensenanza);
        btnModoEnseñanza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Iniciar Modo Enseñanza IA
                Intent intent = new Intent(ModosIAActivity.this, ModoEnsenanzaActivity.class);
                startActivity(intent);
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_modos_ia);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Regresar al menú principal
            }
        });
    }
}
