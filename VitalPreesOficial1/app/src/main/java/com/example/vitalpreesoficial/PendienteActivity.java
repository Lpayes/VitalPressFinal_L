package com.example.vitalpreesoficial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PendienteActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private File lastGeneratedDocx;
    private ActivityResultLauncher<Intent> saveDocLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pendiente);

        imgPreview = findViewById(R.id.img_preview_scan);

        // Inicializa lanzador para guardar documento en ubicación elegida por el usuario
        saveDocLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri dest = result.getData().getData();
                if (dest != null && lastGeneratedDocx != null && lastGeneratedDocx.exists()) {
                    try {
                        try (InputStream in = new FileInputStream(lastGeneratedDocx);
                             OutputStream out = getContentResolver().openOutputStream(dest)) {
                            if (out == null) throw new IllegalStateException("No se pudo abrir destino");
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = in.read(buf)) != -1) {
                                out.write(buf, 0, len);
                            }
                            out.flush();
                        }
                        Toast.makeText(PendienteActivity.this, "Documento exportado correctamente", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(PendienteActivity.this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PendienteActivity.this, "No se encontró el archivo a exportar", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(PendienteActivity.this, "Exportación cancelada", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón Escanear dispositivo: delegar a Sección Datos (que ya maneja Document AI correctamente)
        Button btnEscanear = findViewById(R.id.btn_escanear_dispositivo);
        btnEscanear.setOnClickListener(v -> {
            Intent intent = new Intent(PendienteActivity.this, SeccionDatosActivity.class);
            startActivity(intent);
        });

        // Botón Ver y exportar DOCX
        Button btnExportarDocx = findViewById(R.id.btn_ver_exportar_docx);
        btnExportarDocx.setOnClickListener(v -> {
            try {
                String correo = obtenerCorreoActivo();
                if (correo == null || correo.isEmpty()) {
                    Toast.makeText(this, "Inicia sesión para generar el documento", Toast.LENGTH_LONG).show();
                    return;
                }
                // Generar DOCX con nombre único en almacenamiento interno de la app
                File f = DocxExporter.generateUserDocxUnique(PendienteActivity.this, correo);
                lastGeneratedDocx = f;
                getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();

                // Sugerir nombre de archivo y abrir selector de ubicación (ACTION_CREATE_DOCUMENT)
                String suggested = f.getName();
                Intent createDoc = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                createDoc.addCategory(Intent.CATEGORY_OPENABLE);
                createDoc.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                createDoc.putExtra(Intent.EXTRA_TITLE, suggested);
                saveDocLauncher.launch(createDoc);
            } catch (Exception e) {
                Toast.makeText(PendienteActivity.this, "No se pudo preparar la exportación: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // Botón Regresar
        Button btnRegresar = findViewById(R.id.btn_regresar_pendiente);
        btnRegresar.setOnClickListener(v -> finish());
    }

    private String obtenerCorreoActivo() {
        String c = getSharedPreferences("usuario_sesion", MODE_PRIVATE).getString("correo", null);
        if (c == null) c = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
        return c;
    }
}
