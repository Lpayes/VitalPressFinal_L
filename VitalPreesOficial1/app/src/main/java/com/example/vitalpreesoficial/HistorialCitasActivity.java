package com.example.vitalpreesoficial;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistorialCitasActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView tvVacio;
    private CitaAdapter adapter;
    private List<Cita> listaCitas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial_citas);
        recyclerView = findViewById(R.id.recyclerViewCitas);
        tvVacio = findViewById(R.id.tvVacio);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cargarHistorial();
        adapter = new CitaAdapter(listaCitas);
        recyclerView.setAdapter(adapter);
        tvVacio.setVisibility(listaCitas.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }

    private void cargarHistorial() {
        SharedPreferences prefs = getSharedPreferences("historial_citas", MODE_PRIVATE);
        int total = prefs.getInt("total", 0);
        for (int i = 0; i < total; i++) {
            String citaStr = prefs.getString("cita_" + i, null);
            if (citaStr != null) {
                String[] partes = citaStr.split(",");
                if (partes.length == 4) {
                    listaCitas.add(new Cita(partes[0], Integer.parseInt(partes[1]), Integer.parseInt(partes[2]), Integer.parseInt(partes[3])));
                }
            }
        }
    }
}

