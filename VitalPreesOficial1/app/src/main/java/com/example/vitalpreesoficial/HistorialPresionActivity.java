package com.example.vitalpreesoficial;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.util.ArrayList;
import java.util.List;

public class HistorialPresionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_presion);

        ListView listView = findViewById(R.id.list_historial_presion);
        Button btnRegresar = findViewById(R.id.btn_regresar_historial);

        String correo = getIntent().getStringExtra("correo");
        if (correo == null || correo.isEmpty()) {
            Toast.makeText(this, "Usuario no especificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DBVitalPress db = new DBVitalPress(this);
        List<String> items = new ArrayList<>();

        // Intentar obtener historial detallado primero
        Cursor cd = db.getHistorialDetallado(correo);
        boolean tieneDetallado = false;
        if (cd != null) {
            while (cd.moveToNext()) {
                int sis = cd.getInt(0);
                int dia = cd.getInt(1);
                int pul = cd.getInt(2);
                String fecha = cd.getString(3);
                items.add(fecha + " - " + sis + "/" + dia + "  Pulso: " + pul);
                tieneDetallado = true;
            }
            cd.close();
        }

        if (!tieneDetallado) {
            // Fallback a historial legacy
            Cursor cursor = db.getHistorialPorCorreo(correo);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String valor = cursor.getString(0);
                    String fecha = cursor.getString(1);
                    items.add(fecha + " - " + valor);
                }
                cursor.close();
            }
        }

        if (items.isEmpty()) {
            items.add("No hay registros de presión arterial");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        btnRegresar.setOnClickListener(v -> finish());
    }
}
