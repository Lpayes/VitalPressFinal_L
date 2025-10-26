package com.example.vitalpreesoficial;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.app.Activity;
import android.database.Cursor;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import android.graphics.pdf.PdfDocument;
import com.example.vitalpreesoficial.db.DBVitalPress;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.SharedPreferences;
import android.content.ClipData;

// Imports para ML Kit OCR
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import com.google.mlkit.vision.text.Text;

public class SeccionDatosActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_IMAGE_PICK = 1003;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CREATE_DOCX_REQUEST = 2001;

    private String currentPhotoPath;
    private String pendingDocxPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seccion_datos);

        Button btnExportarPDF = findViewById(R.id.btn_exportar_pdf);
        btnExportarPDF.setOnClickListener(v -> {
            String correoUsuario = null;
            SharedPreferences p1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
            correoUsuario = p1.getString("correo", null);
            if (correoUsuario == null) correoUsuario = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
            exportarDocxAUbicacion(correoUsuario);
        });

        Button btnImportarArchivo = findViewById(R.id.btn_importar_archivo);
        btnImportarArchivo.setOnClickListener(v -> seleccionarArchivo());

        Button btnSubirGaleria = findViewById(R.id.btn_subir_imagen_galeria);
        if (btnSubirGaleria != null) {
            btnSubirGaleria.setOnClickListener(v -> seleccionarImagenGaleria());
        }

        Button btnEscanearDispositivo = findViewById(R.id.btn_escanear_dispositivo);
        btnEscanearDispositivo.setOnClickListener(v -> abrirCamara());

        Button btnRegresar = findViewById(R.id.btn_regresar_datos);
        btnRegresar.setOnClickListener(v -> finish());

        Button btnVerArchivo = findViewById(R.id.btn_ver_archivo_importado);
        btnVerArchivo.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("datos_documento", MODE_PRIVATE);
            String uriString = prefs.getString("archivo_importado_uri", null);
            if (uriString != null) {
                try {
                    Uri uri = Uri.parse(uriString);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(SeccionDatosActivity.this, "No se pudo abrir el archivo.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SeccionDatosActivity.this, "No hay archivo importado.", Toast.LENGTH_SHORT).show();
            }
        });

        final android.widget.EditText editSis = findViewById(R.id.edit_sistolica);
        final android.widget.EditText editDia = findViewById(R.id.edit_diastolica);
        final android.widget.EditText editPul = findViewById(R.id.edit_pulso);

        // Limpiar campos al entrar (sin números predeterminados)
        if (editSis != null) editSis.setText("");
        if (editDia != null) editDia.setText("");
        if (editPul != null) editPul.setText("");

        SharedPreferences prefs1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        String correoTemp = prefs1.getString("correo", null);
        if (correoTemp == null) {
            SharedPreferences prefs2 = getSharedPreferences("usuario_activo", MODE_PRIVATE);
            correoTemp = prefs2.getString("correo", null);
        }
        final String correoUsuario = correoTemp;
        final DBVitalPress db = new DBVitalPress(SeccionDatosActivity.this);

        // Nota: se eliminó el prellenado automático con el último registro para evitar números por defecto

        Button btnGuardarDetallada = findViewById(R.id.btn_guardar_presion_detallada);
        btnGuardarDetallada.setOnClickListener(v -> {
            try {
                String sSis = editSis.getText().toString().trim();
                String sDia = editDia.getText().toString().trim();
                String sPul = editPul.getText().toString().trim();
                if (sSis.isEmpty() || sDia.isEmpty() || sPul.isEmpty()) {
                    Toast.makeText(SeccionDatosActivity.this, "Ingresa los 3 valores", Toast.LENGTH_SHORT).show();
                    return;
                }
                int sis = Integer.parseInt(sSis);
                int dia = Integer.parseInt(sDia);
                int pul = Integer.parseInt(sPul);
                if (sis < 60 || sis > 250 || dia < 40 || dia > 150 || pul < 30 || pul > 220) {
                    Toast.makeText(SeccionDatosActivity.this, "Valores fuera de rango", Toast.LENGTH_LONG).show();
                    return;
                }
                if (correoUsuario == null) {
                    Toast.makeText(SeccionDatosActivity.this, "Inicia sesión primero", Toast.LENGTH_LONG).show();
                    return;
                }
                String fechaNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                db.insertPresionDetallada(correoUsuario, sis, dia, pul, fechaNow);
                db.updatePresionPorCorreo(correoUsuario, sis + "/" + dia);
                db.insertPresionHistorial(correoUsuario, sis + "/" + dia, fechaNow);
                Toast.makeText(SeccionDatosActivity.this, "Presi��n guardada", Toast.LENGTH_SHORT).show();
                try {
                    File f = DocxExporter.generateUserDocx(SeccionDatosActivity.this, correoUsuario);
                    getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
                } catch (Exception ignore) {}
            } catch (NumberFormatException e) {
                Toast.makeText(SeccionDatosActivity.this, "Valores inválidos", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnHistorial = findViewById(R.id.btn_historial_presion);
        btnHistorial.setOnClickListener(v -> {
            if (correoUsuario == null) {
                Toast.makeText(SeccionDatosActivity.this, "Inicia sesión para ver tu historial", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(SeccionDatosActivity.this, HistorialPresionActivity.class);
            intent.putExtra("correo", correoUsuario);
            startActivity(intent);
        });

        Button btnEnviarCorreo = findViewById(R.id.btn_enviar_correo);
        if (btnEnviarCorreo != null) {
            btnEnviarCorreo.setOnClickListener(v -> enviarExpedientePorCorreo());
        }

        // Ver y exportar documento (regenera y abre)
        Button btnVerExportar = findViewById(R.id.btn_pendiente_datos);
        if (btnVerExportar != null) {
            btnVerExportar.setOnClickListener(v -> abrirDocxActualizado(correoUsuario, false));
        }

        // NUEVO: botón ¿Cómo estás?
        Button btnComoEstas = findViewById(R.id.btn_como_estas);
        if (btnComoEstas != null) {
            btnComoEstas.setOnClickListener(v -> {
                if (correoUsuario == null) {
                    Toast.makeText(SeccionDatosActivity.this, "Inicia sesión para ver tu resumen", Toast.LENGTH_LONG).show();
                    return;
                }
                String resumen = computeComoEstasSummary(correoUsuario);
                androidx.appcompat.app.AlertDialog.Builder b = new androidx.appcompat.app.AlertDialog.Builder(SeccionDatosActivity.this);
                b.setTitle("¿Cómo estás?");
                b.setMessage(resumen);
                b.setPositiveButton("Agregar al documento", (dialog, which) -> {
                    try {
                        // insertar resumen como documento y regenerar docx
                        DBVitalPress db2 = new DBVitalPress(SeccionDatosActivity.this);
                        String fechaNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                        File f = DocxExporter.generateUserDocx(SeccionDatosActivity.this, correoUsuario);
                        // guardar ruta y registro
                        getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
                        db2.insertDocumento(correoUsuario, f.getAbsolutePath(), resumen, fechaNow);
                        Toast.makeText(SeccionDatosActivity.this, "Resumen agregado al documento", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(SeccionDatosActivity.this, "No se pudo agregar al documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                b.setNegativeButton("Cerrar", null);
                b.show();
            });
        }

        // Crear nuevo documento (archivo con timestamp)
        Button btnCrearNuevo = findViewById(R.id.btn_crear_nuevo_doc);
        if (btnCrearNuevo != null) {
            btnCrearNuevo.setOnClickListener(v -> abrirDocxActualizado(correoUsuario, true));
        }

        // Actualizar info del documento (sobre-escribe el base)
        Button btnActualizarDoc = findViewById(R.id.btn_actualizar_doc);
        if (btnActualizarDoc != null) {
            btnActualizarDoc.setOnClickListener(v -> abrirDocxActualizado(correoUsuario, false));
        }
    }

    private void exportarDocxAUbicacion(String correoUsuario) {
        try {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                Toast.makeText(this, "Inicia sesión para exportar tu documento", Toast.LENGTH_LONG).show();
                return;
            }
            // Generar/actualizar el DOCX personalizado
            File f = DocxExporter.generateUserDocx(this, correoUsuario);
            pendingDocxPath = f.getAbsolutePath();
            // Abrir selector para guardar
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            String nombre = "VitalPress_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".docx";
            intent.putExtra(Intent.EXTRA_TITLE, nombre);
            startActivityForResult(intent, CREATE_DOCX_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo preparar el documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.vitalpreesoficial.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportarDatosConDocumentAI() {
        DBVitalPress dbVitalPress = new DBVitalPress(this);
        List<String> usuarios = dbVitalPress.exportarUsuarios();
        if (usuarios.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        int y = 25;
        for (String usuario : usuarios) {
            page.getCanvas().drawText(usuario, 10, y, new android.graphics.Paint());
            y += 25;
        }
        pdfDocument.finishPage(page);
        File pdfFile = new File(getFilesDir(), "usuarios_exportados.pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            procesarArchivoConDocumentAI(Uri.fromFile(pdfFile));
        } catch (IOException e) {
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void seleccionarArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Selecciona un archivo PDF"), PICK_FILE_REQUEST_CODE);
    }

    private void seleccionarImagenGaleria() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarExpedientePorCorreo() {
        SharedPreferences oauthPrefs = getSharedPreferences("oauth", MODE_PRIVATE);
        String accessToken = oauthPrefs.getString("access_token", null);
        String correo = null;
        SharedPreferences p1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        correo = p1.getString("correo", null);
        if (correo == null) correo = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
        if (correo == null) {
            Toast.makeText(this, "Inicia sesión con tu usuario antes de enviar", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            File f = DocxExporter.generateUserDocx(this, correo);
            Uri fileUri = FileProvider.getUriForFile(this, "com.example.vitalpreesoficial.fileprovider", f);
            String asunto = "Expediente VitalPress";
            String cuerpo = "Adjuntamos tu expediente médico personal con datos, última presión, historial y resúmenes de documentos.";
            if (accessToken != null && !accessToken.isEmpty()) {
                GmailSender.sendEmailWithAttachment(this, accessToken, correo, asunto, cuerpo, fileUri, new GmailSender.GmailCallback() {
                    @Override public void onSuccess() {
                        runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Correo enviado", Toast.LENGTH_LONG).show());
                    }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> compartirDocumentoPorIntent(fileUri));
                    }
                });
            } else {
                compartirDocumentoPorIntent(fileUri);
            }
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo generar/enviar el documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void compartirDocumentoPorIntent(Uri uri) {
        try {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Expediente VitalPress");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Adjunto tu expediente médico personal.");
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.setClipData(ClipData.newRawUri("Expediente", uri));
            startActivity(Intent.createChooser(sendIntent, "Enviar expediente"));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el cliente de correo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void procesarArchivoConDocumentAI(Uri uri) {
        Toast.makeText(this, "Procesando archivo con Document AI...", Toast.LENGTH_SHORT).show();
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) throw new IOException("No se pudo abrir el archivo");
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) baos.write(buffer, 0, len);
            is.close();
            byte[] fileBytes = baos.toByteArray();

            String mimeType = getMimeTypeFromUri(uri);

            SharedPreferences oauthPrefs = getSharedPreferences("oauth", MODE_PRIVATE);
            String accessToken = oauthPrefs.getString("access_token", null);
            if ((accessToken == null || accessToken.isEmpty()) && mimeType != null && mimeType.startsWith("image/")) {
                procesarImagenConOcr(uri);
                return;
            }
            if (accessToken == null || accessToken.isEmpty()) {
                Toast.makeText(this, "Necesitas iniciar sesión con Google para usar Document AI.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return;
            }

            final String correoUsuario;
            {
                SharedPreferences p1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
                String c = p1.getString("correo", null);
                if (c == null) {
                    SharedPreferences p2 = getSharedPreferences("usuario_activo", MODE_PRIVATE);
                    c = p2.getString("correo", null);
                }
                correoUsuario = c;
            }

            DocumentAiRestClient.processDocument(this, fileBytes, mimeType,
                    DocAiConfig.PROJECT_ID, DocAiConfig.LOCATION, DocAiConfig.PROCESSOR_ID,
                    accessToken, new DocumentAiRestClient.DocumentCallback() {
                        @Override
                        public void onSuccess(String jsonResponse) {
                            SharedPreferences prefs = getSharedPreferences("datos_documento", MODE_PRIVATE);
                            prefs.edit().putString("resultado_documento", jsonResponse).apply();
                            String resumen = generarResumenSimpleDesdeJson(jsonResponse);
                            prefs.edit().putString("resumen_documento", resumen).apply();
                            String plainText = extraerPlainTextDesdeJson(jsonResponse);
                            try {
                                String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                if (correoUsuario != null) {
                                    DBVitalPress db = new DBVitalPress(SeccionDatosActivity.this);
                                    db.insertDocumento(correoUsuario, uri.toString(), resumen, fecha);
                                    String fuente = (plainText != null && !plainText.trim().isEmpty()) ? plainText : resumen;
                                    int[] trio = extraerTrioPorSaltosDeLinea(fuente);
                                    if (trio == null) trio = extraerTrioSimpleDesdeLineas(fuente);
                                    if (trio != null) {
                                        rellenarCampos(trio[0], trio[1], trio[2]);
                                        agregarHistorialAuto(trio[0], trio[1], trio[2]);
                                        final int a = trio[0], b = trio[1], c = trio[2];
                                        runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Tomado del texto extraído: " + a + " " + b + " " + c + ".", Toast.LENGTH_LONG).show());
                                    } else {
                                        if (mimeType != null && mimeType.startsWith("image/")) {
                                            procesarImagenConOcr(uri);
                                            return;
                                        }
                                        runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "No se detectaron 3 líneas numéricas claras.", Toast.LENGTH_LONG).show());
                                    }
                                    File f = DocxExporter.generateUserDocx(SeccionDatosActivity.this, correoUsuario);
                                    prefs.edit().putString("docx_path", f.getAbsolutePath()).apply();
                                }
                            } catch (Exception ignore) {}
                            runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Procesado correctamente", Toast.LENGTH_LONG).show());
                        }
                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Error al procesar: " + errorMessage, Toast.LENGTH_LONG).show());
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // OCR local (implementación con ML Kit)
    private void procesarImagenConOcr(Uri uri) {
        try {
            Toast.makeText(this, "Extrayendo texto en el dispositivo...", Toast.LENGTH_SHORT).show();
            // Preprocesar imagen para mejorar OCR
            Bitmap bmp = cargarBitmapReducido(uri, 1200);
            if (bmp == null) {
                // fallback a InputImage desde ruta
                InputImage image = InputImage.fromFilePath(this, uri);
                com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(image)
                        .addOnSuccessListener(result -> manejarResultadoOcr(result, uri))
                        .addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "No se pudo extraer texto: " + e.getMessage(), Toast.LENGTH_LONG).show()));
                return;
            }

            // mejorar contraste / binarizar ligeramente
            Bitmap proc = enhanceContrastAndGrayscale(bmp);
            InputImage image = InputImage.fromBitmap(proc, 0);
            com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        // usar extracción robusta basada en posiciones y limpieza de OCR
                        int[] trio = extraerTrioRobustoFromResult(result);
                        if (trio == null) {
                            // fallback a texto bruto
                            String rawText = result != null ? result.getText() : null;
                            if (rawText == null) rawText = "";
                            trio = extraerTrioPorSaltosDeLinea(rawText);
                            if (trio == null) trio = extraerTrioSimpleDesdeLineas(rawText);
                        }
                        if (trio != null) {
                            rellenarCampos(trio[0], trio[1], trio[2]);
                            agregarHistorialAuto(trio[0], trio[1], trio[2]);
                            final int a = trio[0], b = trio[1], c = trio[2];
                            runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Texto extraído: " + a + " " + b + " " + c, Toast.LENGTH_LONG).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "No se detectaron 3 números claros (sis/dia/pul).", Toast.LENGTH_LONG).show());
                        }
                    })
                    .addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "No se pudo extraer texto: " + e.getMessage(), Toast.LENGTH_LONG).show()));
        } catch (Exception e) {
            Toast.makeText(this, "Error preparando imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Manejar resultado cuando se usa fallback InputImage.fromFilePath
    private void manejarResultadoOcr(Text result, Uri uri) {
        try {
            int[] trio = extraerTrioRobustoFromResult(result);
            if (trio == null) {
                String rawText = result != null ? result.getText() : "";
                trio = extraerTrioPorSaltosDeLinea(rawText);
                if (trio == null) trio = extraerTrioSimpleDesdeLineas(rawText);
            }
            if (trio != null) {
                rellenarCampos(trio[0], trio[1], trio[2]);
                agregarHistorialAuto(trio[0], trio[1], trio[2]);
                final int a = trio[0], b = trio[1], c = trio[2];
                runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Texto extraído: " + a + " " + b + " " + c, Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "No se detectaron 3 números claros (sis/dia/pul).", Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(SeccionDatosActivity.this, "Error procesando OCR: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // Cargar bitmap escalado desde Uri (evita OOM)
    private Bitmap cargarBitmapReducido(Uri uri, int maxDim) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int width = opts.outWidth;
            int height = opts.outHeight;
            int scale = 1;
            int max = Math.max(width, height);
            while (max / (scale * 2) >= maxDim) scale *= 2;

            opts = new BitmapFactory.Options();
            opts.inSampleSize = scale;
            is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    // Mejora de contraste y conversión a escala de grises sencilla
    private Bitmap enhanceContrastAndGrayscale(Bitmap src) {
        try {
            Bitmap bmp = src.copy(Bitmap.Config.ARGB_8888, true);
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int[] pixels = new int[w * h];
            bmp.getPixels(pixels, 0, w, 0, 0, w, h);
            // Ajustes simples: convertir a grayscale y aumentar contraste
            double contrast = 1.3; // 1.0 = sin cambio, subir si se necesita
            double translate = -20; // brillo
            for (int i = 0; i < pixels.length; i++) {
                int c = pixels[i];
                int r = Color.red(c);
                int g = Color.green(c);
                int b = Color.blue(c);
                int gray = (r + g + b) / 3;
                int value = (int) (contrast * (gray) + translate);
                if (value < 0) value = 0; if (value > 255) value = 255;
                int col = Color.rgb(value, value, value);
                pixels[i] = (0xFF << 24) | (col & 0x00FFFFFF);
            }
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            return bmp;
        } catch (Exception e) {
            return src;
        }
    }

    // Extracción robusta de tres números usando estructura y posición vertical de líneas
    private int[] extraerTrioRobustoFromResult(Text result) {
        if (result == null) return null;
        try {
            List<LineItem> lines = new java.util.ArrayList<>();
            for (Text.TextBlock block : result.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String t = line.getText();
                    android.graphics.Rect bb = line.getBoundingBox();
                    int top = (bb != null) ? bb.top : (block.getBoundingBox() != null ? block.getBoundingBox().top : 0);
                    lines.add(new LineItem(t, top));
                }
            }
            if (lines.isEmpty()) {
                // fallback: use raw text split
                String raw = result.getText();
                return extraerTrioPorSaltosDeLinea(raw);
            }
            // ordenar por posición vertical (de arriba hacia abajo)
            java.util.Collections.sort(lines, (a,b) -> Integer.compare(a.top, b.top));

            // intentar combinar líneas consecutivas buscando números
            java.util.regex.Pattern numPat = java.util.regex.Pattern.compile("(\\d{1,3})");
            for (int i = 0; i < lines.size(); i++) {
                List<Integer> found = new java.util.ArrayList<>();
                // mirar en ventana de hasta 3 líneas
                for (int j = i; j < Math.min(lines.size(), i+3); j++) {
                    String cleaned = cleanOcrLine(lines.get(j).text);
                    java.util.regex.Matcher m = numPat.matcher(cleaned);
                    while (m.find()) {
                        try { found.add(Integer.parseInt(m.group(1))); } catch (Exception ignore) {}
                        if (found.size() == 3) break;
                    }
                    if (found.size() == 3) break;
                }
                if (found.size() == 3) {
                    int a = found.get(0), b = found.get(1), c = found.get(2);
                    if (validTriple(a,b,c)) return new int[]{a,b,c};
                }
            }

            // Si no se encontró en ventanas, extraer todos los números ordenados por verticalidad y probar combinaciones
            List<Integer> allNums = new java.util.ArrayList<>();
            for (LineItem li : lines) {
                String cleaned = cleanOcrLine(li.text);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,3})").matcher(cleaned);
                while (m.find()) {
                    try { allNums.add(Integer.parseInt(m.group(1))); } catch (Exception ignore) {}
                }
            }
            // probar combinaciones consecutivas
            for (int i = 0; i + 2 < allNums.size(); i++) {
                int a = allNums.get(i), b = allNums.get(i+1), c = allNums.get(i+2);
                if (validTriple(a,b,c)) return new int[]{a,b,c};
            }
            // último recurso: escoger los 3 primeros válidos por rango y sis>dia
            List<Integer> valid = new java.util.ArrayList<>();
            for (int v : allNums) if (v>=60 && v<=250) valid.add(v);
            for (int i = 0; i + 2 < valid.size(); i++) {
                int a = valid.get(i), b = valid.get(i+1), c = valid.get(i+2);
                if (validTriple(a,b,c)) return new int[]{a,b,c};
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static class LineItem { String text; int top; LineItem(String t, int top){ this.text=t; this.top=top; } }

    // Limpieza de una línea para corregir errores comunes de OCR
    private String cleanOcrLine(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[.,]"," ");
        // reemplazos comunes: O->0, o->0, l/I->1, S->5 si está entre dígitos
        t = t.replace('O','0').replace('o','0');
        t = t.replace('I','1').replace('l','1');
        // eliminar caracteres no numéricos salvo separadores
        t = t.replaceAll("[^0-9/\\- ]"," ");
        // normalizar múltiples espacios
        t = t.replaceAll("\\s+"," ").trim();
        return t;
    }

    private String getMimeTypeFromUri(Uri uri) {
        String mime = null;
        try { mime = getContentResolver().getType(uri); } catch (Exception ignore) {}
        if (mime != null) return mime;
        String path = uri.getPath();
        if (path != null) {
            String ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            if (ext == null || ext.isEmpty()) {
                int dot = path.lastIndexOf('.');
                if (dot != -1) ext = path.substring(dot + 1);
            }
            if (ext != null) {
                String guess = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                if (guess != null) return guess;
                if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) return "image/jpeg";
                if (ext.equalsIgnoreCase("png")) return "image/png";
                if (ext.equalsIgnoreCase("pdf")) return "application/pdf";
            }
        }
        return "application/pdf";
    }

    private void rellenarCampos(final int sis, final int dia, final int pul) {
        runOnUiThread(() -> {
            try {
                android.widget.EditText eSis = findViewById(R.id.edit_sistolica);
                android.widget.EditText eDia = findViewById(R.id.edit_diastolica);
                android.widget.EditText ePul = findViewById(R.id.edit_pulso);
                if (eSis != null) eSis.setText(String.valueOf(sis));
                if (eDia != null) eDia.setText(String.valueOf(dia));
                if (ePul != null) ePul.setText(String.valueOf(pul));
            } catch (Exception ignore) {}
        });
    }

    private void agregarHistorialAuto(int sis, int dia, int pul) {
        try {
            SharedPreferences p1 = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
            String correo = p1.getString("correo", null);
            if (correo == null) correo = getSharedPreferences("usuario_activo", MODE_PRIVATE).getString("correo", null);
            if (correo == null) return;
            DBVitalPress db = new DBVitalPress(this);
            String fechaNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            db.insertPresionDetallada(correo, sis, dia, pul, fechaNow);
            db.updatePresionPorCorreo(correo, sis + "/" + dia);
            db.insertPresionHistorial(correo, sis + "/" + dia, fechaNow);
        } catch (Exception ignore) {}
    }

    // NUEVOS MÉTODOS: helpers para texto y resumen desde JSON de Document AI
    private String extraerPlainTextDesdeJson(String resultadoJson) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(resultadoJson);
            String plainText = null;
            if (json.has("text")) {
                plainText = json.optString("text", null);
            }
            if ((plainText == null || plainText.isEmpty()) && json.has("document") && json.opt("document") instanceof org.json.JSONObject) {
                org.json.JSONObject doc = json.getJSONObject("document");
                if (doc.has("text")) plainText = doc.optString("text", null);
            }
            if (plainText == null) {
                // Fallback simple: quitar saltos y compactar espacios
                plainText = resultadoJson.replaceAll("[\n\r]+", " ").replaceAll("\\s+", " ").trim();
            }
            return plainText;
        } catch (Exception e) {
            return null;
        }
    }

    private String generarResumenSimpleDesdeJson(String resultadoJson) {
        try {
            String plainText = extraerPlainTextDesdeJson(resultadoJson);
            if (plainText == null) return "";
            // Separar por oraciones simples o saltos de línea
            String[] parts = plainText.split("(?<=\\n|[.!?])\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (parts[i] != null && !parts[i].trim().isEmpty()) {
                    sb.append(parts[i].trim());
                    if (i == 0) sb.append(" ");
                }
            }
            String resumen = sb.toString().trim();
            if (resumen.isEmpty()) {
                resumen = plainText.length() > 300 ? plainText.substring(0, 300) + "..." : plainText;
            }
            return resumen;
        } catch (Exception e) {
            return "";
        }
    }

    // Genera y abre el DOCX; unique=true crea archivo con timestamp
    private void abrirDocxActualizado(String correoUsuario, boolean unique) {
        try {
            if (correoUsuario == null || correoUsuario.isEmpty()) {
                Toast.makeText(this, "Inicia sesión para generar el documento", Toast.LENGTH_LONG).show();
                return;
            }
            File f = unique ? DocxExporter.generateUserDocxUnique(this, correoUsuario)
                            : DocxExporter.generateUserDocx(this, correoUsuario);
            getSharedPreferences("datos_documento", MODE_PRIVATE).edit().putString("docx_path", f.getAbsolutePath()).apply();
            Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, unique?"Abrir nuevo documento":"Abrir documento"));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            try {
                if (currentPhotoPath != null) {
                    Uri uri = Uri.fromFile(new File(currentPhotoPath));
                    procesarImagenConOcr(uri);
                } else {
                    Toast.makeText(this, "No se obtuvo la ruta de la foto", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error al procesar foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_IMAGE_PICK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                procesarImagenConOcr(uri);
            }
        } else if (requestCode == PICK_FILE_REQUEST_CODE) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                // Guardar para botón "Ver archivo importado"
                getSharedPreferences("datos_documento", MODE_PRIVATE)
                        .edit().putString("archivo_importado_uri", uri.toString()).apply();
                procesarArchivoConDocumentAI(uri);
            }
        } else if (requestCode == CREATE_DOCX_REQUEST) {
            try {
                if (data != null && data.getData() != null && pendingDocxPath != null) {
                    Uri dest = data.getData();
                    File src = new File(pendingDocxPath);
                    try (java.io.InputStream in = new java.io.FileInputStream(src);
                         java.io.OutputStream out = getContentResolver().openOutputStream(dest)) {
                        if (out == null) throw new IOException("No se pudo abrir el destino");
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        out.flush();
                    }
                    Toast.makeText(this, "Documento exportado en la ubicación elegida", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No se pudo exportar el documento", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Extrae exactamente 3 números en tres líneas separadas (en orden)
    private int[] extraerTrioPorSaltosDeLinea(String raw) {
        if (raw == null) return null;
        try {
            String[] lines = raw.replace('\r','\n').split("\\R");
            // patrón para línea con solo un número
            java.util.regex.Pattern onlyNum = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\b");
            // patrón para línea con dos números separados por / or - or espacios
            java.util.regex.Pattern twoNums = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\s*[/\\-\\s]+\\s*(\\d{1,3})\\b");

            // Caso 1: tres líneas con un número cada una
            int[] trio = new int[3]; int idx = 0;
            for (String line : lines) {
                if (line == null) continue;
                java.util.regex.Matcher m = onlyNum.matcher(line);
                if (m.find() && line.trim().matches("^.*\\d.*$")) {
                    trio[idx++] = Integer.parseInt(m.group(1));
                    if (idx == 3) return trio;
                }
            }

            // Caso 2: línea con dos números (ej "90/80") seguida de línea con uno ("70")
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i];
                if (l == null) continue;
                java.util.regex.Matcher m2 = twoNums.matcher(l);
                if (m2.find()) {
                    int a = Integer.parseInt(m2.group(1));
                    int b = Integer.parseInt(m2.group(2));
                    // buscar siguiente línea con número simple
                    if (i+1 < lines.length) {
                        java.util.regex.Matcher mnext = onlyNum.matcher(lines[i+1]);
                        if (mnext.find()) {
                            int c = Integer.parseInt(mnext.group(1));
                            if (validTriple(a,b,c)) return new int[]{a,b,c};
                        }
                    }
                    // también aceptar si la misma línea tiene 3 números (90/80/70)
                    java.util.regex.Pattern threeInLine = java.util.regex.Pattern.compile("\\b(\\d{1,3})\\s*[/\\-\\s]+\\s*(\\d{1,3})\\s*[/\\-\\s]+\\s*(\\d{1,3})\\b");
                    java.util.regex.Matcher m3 = threeInLine.matcher(l);
                    if (m3.find()) {
                        int a3 = Integer.parseInt(m3.group(1));
                        int b3 = Integer.parseInt(m3.group(2));
                        int c3 = Integer.parseInt(m3.group(3));
                        if (validTriple(a3,b3,c3)) return new int[]{a3,b3,c3};
                    }
                }
            }

            // Caso 3: si llegamos aquí, intentar extraer cualquier secuencia de 3 números en el texto en orden vertical
            java.util.regex.Matcher all = java.util.regex.Pattern.compile("(\\d{1,3})").matcher(raw);
            java.util.ArrayList<Integer> nums = new java.util.ArrayList<>();
            while (all.find()) {
                try { nums.add(Integer.parseInt(all.group(1))); } catch (Exception ignore) {}
            }
            for (int i = 0; i + 2 < nums.size(); i++) {
                int a = nums.get(i), b = nums.get(i+1), c = nums.get(i+2);
                if (validTriple(a,b,c)) return new int[]{a,b,c};
            }

        } catch (Exception ignore) {}
        return null;
    }

    // Extrae 3 números en una misma línea o patrón similar
    private int[] extraerTrioSimpleDesdeLineas(String plainText) {
        if (plainText == null) return null;
        try {
            // Normalizar texto para facilitar regex
            String text = plainText.replace('\r','\n').replaceAll("[Oo]", "0").replaceAll("[lI]", "1");
            String[] lines = text.split("\\R");

            // patrones robustos que cubren: "90 80 70", "90/80 70", "90/80/70", "90-80-70"
            java.util.regex.Pattern[] patterns = new java.util.regex.Pattern[] {
                java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,3})\\s+(\\d{1,3})\\s+(\\d{1,3})(?!\\d)"),
                java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,3})\\s*[/\\-]\\s*(\\d{1,3})\\s*[/\\-]\\s*(\\d{1,3})(?!\\d)"),
                java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,3})\\s*[/\\-]\\s*(\\d{1,3})\\s+(\\d{1,3})(?!\\d)"),
                java.util.regex.Pattern.compile("(?<!\\d)(\\d{1,3})\\s+(\\d{1,3})\\s*[/\\-]\\s*(\\d{1,3})(?!\\d)")
            };

            for (String line : lines) {
                if (line == null) continue;
                String cleaned = cleanOcrLine(line);
                for (java.util.regex.Pattern p : patterns) {
                    java.util.regex.Matcher m = p.matcher(cleaned);
                    if (m.find()) {
                        int a = Integer.parseInt(m.group(1));
                        int b = Integer.parseInt(m.group(2));
                        int c = Integer.parseInt(m.group(3));
                        if (validTriple(a,b,c)) return new int[]{a,b,c};
                    }
                }
            }

            // Intentar en todo el texto combinado (por si OCR separó en líneas inadecuadamente)
            String combined = String.join(" ", lines);
            for (java.util.regex.Pattern p : patterns) {
                java.util.regex.Matcher m = p.matcher(combined);
                if (m.find()) {
                    int a = Integer.parseInt(m.group(1));
                    int b = Integer.parseInt(m.group(2));
                    int c = Integer.parseInt(m.group(3));
                    if (validTriple(a,b,c)) return new int[]{a,b,c};
                }
            }

        } catch (Exception ignore) {}
        return null;
    }

    private boolean validTriple(Integer sis, Integer dia, Integer pul) {
        if (sis==null||dia==null||pul==null) return false;
        if (!(sis>=60 && sis<=250)) return false;
        if (!(dia>=40 && dia<=150)) return false;
        if (!(pul>=30 && pul<=220)) return false;
        return sis > dia;
    }

    // Nuevo: genera un resumen legible "¿Cómo estás?" según historial y última lectura del usuario
    private String computeComoEstasSummary(String correo) {
        if (correo == null || correo.isEmpty()) return "No hay usuario activo.";
        DBVitalPress db = new DBVitalPress(this);

        // Obtener última lectura
        Cursor last = db.getUltimaPresionDetallada(correo);
        Integer lastS = null, lastD = null, lastP = null;
        String lastFecha = null;
        if (last != null && last.moveToFirst()) {
            try {
                lastS = last.getInt(last.getColumnIndexOrThrow("sistolica"));
                lastD = last.getInt(last.getColumnIndexOrThrow("diastolica"));
                lastP = last.getInt(last.getColumnIndexOrThrow("pulso"));
                lastFecha = last.getString(last.getColumnIndexOrThrow("fecha"));
            } catch (Exception ignore) {}
            last.close();
        }

        // Obtener historial (excluyendo la última entrada ya tomada)
        Cursor hist = db.getHistorialDetallado(correo);
        List<int[]> entries = new java.util.ArrayList<>();
        if (hist != null) {
            while (hist.moveToNext()) {
                try {
                    int s = hist.getInt(hist.getColumnIndexOrThrow("sistolica"));
                    int d = hist.getInt(hist.getColumnIndexOrThrow("diastolica"));
                    int p = hist.getInt(hist.getColumnIndexOrThrow("pulso"));
                    String f = hist.getString(hist.getColumnIndexOrThrow("fecha"));
                    // saltar la misma fecha que la última si coincide
                    if (lastFecha != null && lastFecha.equals(f)) continue;
                    entries.add(new int[]{s,d,p});
                } catch (Exception ignore) {}
            }
            hist.close();
        }

        if (lastS == null || lastD == null) return "No hay lecturas registradas.";

        // Calcular promedio de anteriores (hasta 10)
        int limit = Math.min(entries.size(), 10);
        int sumS = 0, sumD = 0, sumP = 0;
        for (int i = 0; i < limit; i++) {
            sumS += entries.get(i)[0];
            sumD += entries.get(i)[1];
            sumP += entries.get(i)[2];
        }
        String estadoUlt = categorize(lastS, lastD);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "Última toma: %d/%d mmHg", lastS, lastD));
        if (lastP != null) sb.append(String.format(Locale.getDefault(), ", pulso: %d", lastP));
        sb.append(" - " + estadoUlt + ".\n");

        if (limit > 0) {
            double avgS = sumS / (double) limit;
            double avgD = sumD / (double) limit;
            String estadoPrev = categorize((int)Math.round(avgS), (int)Math.round(avgD));
            sb.append(String.format(Locale.getDefault(), "Promedio de últimas %d: %.0f/%.0f mmHg (%s).\n", limit, avgS, avgD, estadoPrev));

            // determinar si hubo mejoría o empeoramiento
            if (!estadoPrev.equals(estadoUlt)) {
                if (isWorse(estadoPrev, estadoUlt)) {
                    sb.append("Hay mejoría según el historial: antes estuvo " + estadoPrev.toLowerCase() + ", ahora " + estadoUlt.toLowerCase() + ".");
                } else {
                    sb.append("Hay empeoramiento: antes estaba " + estadoPrev.toLowerCase() + ", ahora " + estadoUlt.toLowerCase() + ".");
                }
            } else {
                sb.append("Estado similar al historial: " + estadoUlt.toLowerCase() + ".");
            }
        } else {
            sb.append("No hay historial suficiente para comparar.");
        }

        return sb.toString();
    }

    // Categoriza el estado según sistólica/diastólica
    private String categorize(int s, int d) {
        if (s < 120 && d < 80) return "Normal";
        if (s < 130 && d < 80) return "Elevada";
        if (s < 140 || d < 90) return "Hipertensión grado 1";
        if (s < 180 || d < 120) return "Hipertensión grado 2";
        return "Crisis hipertensiva";
    }

    // Determina si prevEstado es peor que ultEstado
    private boolean isWorse(String prev, String ult) {
        java.util.List<String> order = java.util.Arrays.asList("Normal", "Elevada", "Hipertensión grado 1", "Hipertensión grado 2", "Crisis hipertensiva");
        int ip = order.indexOf(prev);
        int iu = order.indexOf(ult);
        if (ip == -1 || iu == -1) return false;
        return ip > iu; // prev peor que ult (higher index = worse)
    }
}
