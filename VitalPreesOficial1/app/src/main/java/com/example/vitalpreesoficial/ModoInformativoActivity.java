package com.example.vitalpreesoficial;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ModoInformativoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modo_informativo);

        // Aquí se implementará la lógica para mostrar información educativa sobre presión arterial
        TextView infoTextView = findViewById(R.id.info_text_view);
        infoTextView.setText("Información educativa sobre presión arterial.");
    }
}
