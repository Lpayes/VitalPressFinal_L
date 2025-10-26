package com.example.vitalpreesoficial;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DocumentAiRestClient {

    public interface DocumentCallback {
        void onSuccess(String jsonResponse);
        void onError(String error);
    }

    /**
     * Envía un documento a Document AI usando REST v1. Ejecuta en background y notifica por callback.
     * @param context Contexto (no usado actualmente, pero útil para futuros cambios)
     * @param fileBytes bytes del archivo (PDF, imagen, etc.)
     * @param mimeType mime type (por ejemplo "application/pdf" o "image/jpeg")
     * @param projectId ID del proyecto de Google Cloud
     * @param location location del processor (ej. "us" o "eu")
     * @param processorId ID del processor
     * @param accessToken token OAuth 2.0 (Bearer)
     * @param callback callback para resultado
     */
    public static void processDocument(final Context context,
                                       final byte[] fileBytes,
                                       final String mimeType,
                                       final String projectId,
                                       final String location,
                                       final String processorId,
                                       final String accessToken,
                                       final DocumentCallback callback) {

        new AsyncTask<Void, Void, String>() {
            Exception error;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String url = String.format(
                            "https://documentai.googleapis.com/v1/projects/%s/locations/%s/processors/%s:process",
                            projectId, location, processorId
                    );

                    String base64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

                    JSONObject rawDocument = new JSONObject();
                    rawDocument.put("content", base64);
                    rawDocument.put("mimeType", mimeType);

                    JSONObject root = new JSONObject();
                    root.put("rawDocument", rawDocument);

                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    RequestBody body = RequestBody.create(root.toString(), JSON);

                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .addHeader("Accept", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Unexpected code " + response.code() + ": " + respBody);
                    }

                    String resp = response.body() != null ? response.body().string() : "";
                    return resp;
                } catch (Exception e) {
                    this.error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    callback.onSuccess(s);
                } else if (error != null) {
                    callback.onError(error.getMessage());
                } else {
                    callback.onError("Unknown error calling Document AI");
                }
            }
        }.execute();
    }
}

