package com.example.vitalpreesoficial;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.util.LinkedHashSet;
import java.util.Set;

public class VerInfoActivity extends AppCompatActivity {

    private DBVitalPress dbVitalPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_info);

        dbVitalPress = new DBVitalPress(this);
        cargarInformacionUsuario();

        Button btnRegresar = findViewById(R.id.btn_regresar_ver_info);
        btnRegresar.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarInformacionUsuario();
    }

    private void cargarInformacionUsuario() {
        TextView txtNombre = findViewById(R.id.txt_info_nombre);
        TextView txtCorreo = findViewById(R.id.txt_info_correo);
        TextView txtEdad = findViewById(R.id.txt_info_edad);
        TextView txtEstatura = findViewById(R.id.txt_info_estatura);
        TextView txtPeso = findViewById(R.id.txt_info_peso);
        TextView txtIMC = findViewById(R.id.txt_info_imc);
        TextView txtSexo = findViewById(R.id.txt_info_sexo);
        TextView txtFechaRegistro = findViewById(R.id.txt_info_fecha_registro);
        TextView txtFechaActualizacion = findViewById(R.id.txt_info_fecha_actualizacion);

        TextView txtSis = findViewById(R.id.txt_sistolica);
        TextView txtDia = findViewById(R.id.txt_diastolica);
        TextView txtPul = findViewById(R.id.txt_pulso);
        int idPaComp = getResources().getIdentifier("txt_pa_compuesta", "id", getPackageName());
        TextView txtPaCompuesta = idPaComp != 0 ? findViewById(idPaComp) : null;
        LinearLayout containerResumenes = findViewById(R.id.container_resumenes);
        LinearLayout containerAntecedentes = findViewById(R.id.container_antecedentes);
        LinearLayout containerUltimas = findViewById(R.id.container_ultimas_pa);

        // Tomar correo de sesión o activo
        String correo = null;
        SharedPreferences spSesion = getSharedPreferences("usuario_sesion", MODE_PRIVATE);
        SharedPreferences spActivo = getSharedPreferences("usuario_activo", MODE_PRIVATE);
        if (spSesion != null) correo = spSesion.getString("correo", null);
        if ((correo == null || correo.isEmpty()) && spActivo != null) correo = spActivo.getString("correo", null);

        if (correo != null && !correo.isEmpty()) {
            Cursor cursor = dbVitalPress.buscarUsuarioPorCorreo(correo);
            if (cursor != null && cursor.moveToFirst()) {
                try {
                    String nombre = getStringSafe(cursor, "nombre");
                    String apellido = getStringSafe(cursor, "apellido");
                    int edad = getIntSafe(cursor, "edad");
                    double estatura = getDoubleSafe(cursor, "estatura");
                    double peso = getDoubleSafe(cursor, "peso");
                    String sexo = getStringSafe(cursor, "sexo");

                    double imc = 0.0;
                    if (estatura > 0) imc = peso / (estatura * estatura);

                    String nombreCompleto = nombre + (apellido != null && !apellido.isEmpty() ? (" " + apellido) : "");
                    txtNombre.setText(nombreCompleto);
                    txtCorreo.setText(correo);
                    txtEdad.setText(edad + " años");
                    txtEstatura.setText(String.format(java.util.Locale.getDefault(), "%.2f m", estatura));
                    txtPeso.setText(String.format(java.util.Locale.getDefault(), "%.1f kg", peso));
                    txtIMC.setText(String.format(java.util.Locale.getDefault(), "%.2f", imc));
                    txtSexo.setText(sexo != null && !sexo.isEmpty() ? sexo : "No especificado");
                } catch (Exception e) {
                    txtNombre.setText("Error al cargar datos");
                    txtCorreo.setText(correo);
                    txtEdad.setText("--");
                    txtEstatura.setText("--");
                    txtPeso.setText("--");
                    txtIMC.setText("--");
                    txtSexo.setText("--");
                }
            } else {
                txtNombre.setText("Usuario no encontrado");
                txtCorreo.setText(correo);
                txtEdad.setText("--");
                txtEstatura.setText("--");
                txtPeso.setText("--");
                txtIMC.setText("--");
                txtSexo.setText("--");
            }
            if (cursor != null) cursor.close();

            // Última presión
            Cursor cu = dbVitalPress.getUltimaPresionDetallada(correo);
            Integer sis = null, dia = null, pul = null;
            if (cu != null && cu.moveToFirst()) {
                sis = cu.getInt(0);
                dia = cu.getInt(1);
                pul = cu.getInt(2);
                txtSis.setText(String.valueOf(sis));
                txtDia.setText(String.valueOf(dia));
                txtPul.setText(String.valueOf(pul));
            } else {
                String legacy = dbVitalPress.getPresionPorCorreo(correo);
                if (legacy != null && legacy.contains("/")) {
                    try {
                        String[] p = legacy.split("/");
                        sis = Integer.parseInt(p[0].trim());
                        dia = Integer.parseInt(p[1].trim());
                        txtSis.setText(String.valueOf(sis));
                        txtDia.setText(String.valueOf(dia));
                        txtPul.setText("--");
                    } catch (Exception ignore) {}
                }
            }
            if (cu != null) cu.close();
            if (txtPaCompuesta != null) {
                String comp = (sis != null && dia != null ? (sis + "/" + dia) : "--/--") + ", pulso: " + (pul != null ? pul : "--");
                txtPaCompuesta.setText(comp);
            }

            // 3 últimas mediciones de presión (fecha hora A/B, pulso: C)
            if (containerUltimas != null) {
                containerUltimas.removeAllViews();
                Cursor ch = dbVitalPress.getHistorialDetallado(correo);
                int n = 0;
                if (ch != null) {
                    while (ch.moveToNext() && n < 3) {
                        try {
                            int s = ch.getInt(0);
                            int d = ch.getInt(1);
                            int p = ch.getInt(2);
                            String f = ch.getString(3);
                            TextView row = new TextView(this);
                            row.setText((f != null ? f : "") + "  " + s + "/" + d + ", pulso: " + p);
                            row.setTextColor(ContextCompat.getColor(this, R.color.azul));
                            containerUltimas.addView(row);
                            n++;
                        } catch (Exception ignore) {}
                    }
                    ch.close();
                }
                if (n == 0) {
                    TextView vac = new TextView(this);
                    vac.setText("(Sin historial)");
                    containerUltimas.addView(vac);
                }
            }

            // Documentos y antecedentes
            containerResumenes.removeAllViews();
            containerAntecedentes.removeAllViews();
            Set<String> antecedentesDetectados = new LinkedHashSet<>();
            Cursor cd = dbVitalPress.getDocumentosPorCorreo(correo);
            if (cd != null) {
                while (cd.moveToNext()) {
                    String resumen = cd.getString(0);
                    String fecha = cd.getString(1);
                    TextView item = new TextView(this);
                    item.setText("• " + (fecha != null ? (fecha + " - ") : "") + (resumen != null ? resumen : ""));
                    item.setTextColor(ContextCompat.getColor(this, R.color.azul));
                    containerResumenes.addView(item);
                    String low = resumen != null ? resumen.toLowerCase() : "";
                    if (low.contains("hipertensi")) antecedentesDetectados.add("Hipertensión");
                    if (low.contains("diabet")) antecedentesDetectados.add("Diabetes");
                    if (low.contains("colesterol") || low.contains("dislipid")) antecedentesDetectados.add("Dislipidemia / Colesterol alto");
                    if (low.contains("asma")) antecedentesDetectados.add("Asma");
                    if (low.contains("cardiopat") || low.contains("infarto")) antecedentesDetectados.add("Antecedente cardiaco");
                }
                cd.close();
            }
            if (containerResumenes.getChildCount() == 0) {
                TextView vacio = new TextView(this);
                vacio.setText("(Sin documentos)");
                containerResumenes.addView(vacio);
            }
            if (antecedentesDetectados.isEmpty()) {
                TextView vacioAnt = new TextView(this);
                vacioAnt.setText("(Sin antecedentes detectados)");
                containerAntecedentes.addView(vacioAnt);
            } else {
                for (String ant : antecedentesDetectados) {
                    TextView tag = new TextView(this);
                    tag.setText("• " + ant);
                    tag.setTextColor(ContextCompat.getColor(this, R.color.azul));
                    containerAntecedentes.addView(tag);
                }
            }

            // Fechas de registro/actualización
            String fechaRegistro = spActivo != null ? spActivo.getString("fecha_registro", null) : null;
            String fechaActualizacion = spActivo != null ? spActivo.getString("fecha_actualizacion", null) : null;
            txtFechaRegistro.setText(fechaRegistro != null && !fechaRegistro.isEmpty() ? fechaRegistro : "No disponible");
            txtFechaActualizacion.setText(fechaActualizacion != null && !fechaActualizacion.isEmpty() ? fechaActualizacion : "No se ha actualizado");
        } else {
            // Sin sesión
            txtNombre.setText("Sin sesión activa");
            txtCorreo.setText("--");
            txtEdad.setText("--");
            txtEstatura.setText("--");
            txtPeso.setText("--");
            txtIMC.setText("--");
            txtSexo.setText("--");
        }
    }

    private String getStringSafe(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i == -1) return "";
        String v = c.getString(i);
        return v == null ? "" : v;
    }

    private int getIntSafe(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i == -1) return 0;
        try { return c.getInt(i); } catch (Exception e) { return 0; }
    }

    private double getDoubleSafe(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i == -1) return 0.0;
        try { return c.getDouble(i); } catch (Exception e) { return 0.0; }
    }
}
