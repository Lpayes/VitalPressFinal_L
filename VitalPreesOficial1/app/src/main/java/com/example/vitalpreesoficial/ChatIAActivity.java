package com.example.vitalpreesoficial;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ChatIAActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ia);

        // Botón Modo Aprendizaje
        Button btnModoAprendizaje = findViewById(R.id.btn_modo_aprendizaje_chat);
        btnModoAprendizaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatIAActivity.this, ModoAprendizajeActivity.class);
                startActivity(intent);
            }
        });

        // Botón Modo Enseñanza
        Button btnModoEnsenanza = findViewById(R.id.btn_modo_ensenanza_chat);
        btnModoEnsenanza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatIAActivity.this, ModoEnsenanzaActivity.class);
                startActivity(intent);
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_chat_ia);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
