package com.example.vitalpreesoficial;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GmailSender {

    public interface GmailCallback {
        void onSuccess();
        void onError(String error);
    }

    private static final OkHttpClient client = new OkHttpClient();
    // DEBUG: si true, guarda el JSON raw en internal storage para inspección
    private static final boolean DEBUG_SAVE_RAW = true;

    public static void sendEmailWithAttachment(final Context context,
                                               final String accessToken,
                                               final String to,
                                               final String subject,
                                               final String bodyText,
                                               final Uri attachmentUri,
                                               final GmailCallback callback) {
        new Thread(() -> {
            try {
                // Construir MIME multipart manualmente
                String boundary = "----=_Part_" + System.currentTimeMillis();

                StringBuilder sb = new StringBuilder();
                sb.append("From: me\r\n");
                sb.append("To: ").append(to).append("\r\n");
                sb.append("Subject: ").append(subject).append("\r\n");
                sb.append("MIME-Version: 1.0\r\n");
                sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n");
                sb.append("\r\n");

                // Part: texto
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
                sb.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
                sb.append(bodyText).append("\r\n");

                // Part: attachment (si existe)
                if (attachmentUri != null) {
                    byte[] fileBytes = readBytesFromUri(context, attachmentUri);
                    if (fileBytes == null) throw new Exception("No se pudo leer el adjunto");

                    String filename = queryFileName(context, attachmentUri);
                    if (filename == null) filename = "adjunto";

                    String mime = context.getContentResolver().getType(attachmentUri);
                    if (mime == null) mime = "application/octet-stream";

                    String encodedAttachment = Base64.encodeToString(fileBytes, Base64.NO_WRAP);
                    // Insertar saltos de línea cada 76 caracteres para cumplir con MIME
                    StringBuilder chunked = new StringBuilder();
                    int index = 0;
                    while (index < encodedAttachment.length()) {
                        int end = Math.min(index + 76, encodedAttachment.length());
                        chunked.append(encodedAttachment, index, end).append("\r\n");
                        index = end;
                    }

                    sb.append("--").append(boundary).append("\r\n");
                    sb.append("Content-Type: ").append(mime).append("; name=\"").append(filename).append("\"\r\n");
                    sb.append("Content-Transfer-Encoding: base64\r\n");
                    sb.append("Content-Disposition: attachment; filename=\"").append(filename).append("\"\r\n\r\n");
                    sb.append(chunked.toString()).append("\r\n");
                }

                sb.append("--").append(boundary).append("--\r\n");

                byte[] mimeBytes = sb.toString().getBytes("UTF-8");
                String raw = Base64.encodeToString(mimeBytes, Base64.URL_SAFE | Base64.NO_WRAP);
                // Usar JSONObject para serializar correctamente
                org.json.JSONObject jsonObj = new org.json.JSONObject();
                jsonObj.put("raw", raw);
                String json = jsonObj.toString();

                // DEBUG: guardar json en internal storage para inspección
                if (DEBUG_SAVE_RAW) {
                    try {
                        String fname = "gmail_raw_" + System.currentTimeMillis() + ".json";
                        try (java.io.FileOutputStream fos = context.openFileOutput(fname, Context.MODE_PRIVATE)) {
                            fos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        // No bloquear el envío por error de debug
                    }
                }

                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Accept", "application/json")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String resp = response.body() != null ? response.body().string() : "";
                        callback.onError("HTTP " + response.code() + ": " + resp);
                    } else {
                        callback.onSuccess();
                    }
                }

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private static String queryFileName(Context context, Uri uri) {
        try {
            if ("content" .equals(uri.getScheme())) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    try {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            return cursor.getString(nameIndex);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            String path = uri.getPath();
            if (path == null) return null;
            int cut = path.lastIndexOf('/');
            if (cut != -1) return path.substring(cut + 1);
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readBytesFromUri(Context context, Uri uri) {
        try {
            if ("file".equals(uri.getScheme())) {
                File f = new File(uri.getPath());
                try (FileInputStream fis = new FileInputStream(f); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) baos.write(buffer, 0, read);
                    return baos.toByteArray();
                }
            } else {
                ContentResolver cr = context.getContentResolver();
                try (InputStream is = cr.openInputStream(uri); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    if (is == null) return null;
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) baos.write(buffer, 0, read);
                    return baos.toByteArray();
                }
            }
        } catch (Exception e) {
            return null;
        }
    }
}
