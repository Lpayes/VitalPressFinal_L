package com.example.vitalpreesoficial;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import com.example.vitalpreesoficial.db.DBVitalPress;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DocxExporter {

    public static File generateUserDocx(Context context, String correoParam) throws Exception {
        String correo = correoParam;
        if (correo == null || correo.isEmpty()) {
            SharedPreferences p1 = context.getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE);
            correo = p1.getString("correo", null);
            if (correo == null) {
                SharedPreferences p2 = context.getSharedPreferences("usuario_activo", Context.MODE_PRIVATE);
                // mantener null si no existe para distinguir ausencia de valor
                correo = p2.getString("correo", null);
            }
        }
        DBVitalPress db = new DBVitalPress(context);
        String nombre = ""; String apellido = ""; int edad = 0; double estatura = 0; double peso = 0; String sexo = "";
        double imc = 0.0;
        Cursor c = db.buscarUsuarioPorCorreo(correo);
        if (c != null && c.moveToFirst()) {
            int idx;
            idx = c.getColumnIndex("nombre"); if (idx!=-1) nombre = c.getString(idx);
            idx = c.getColumnIndex("apellido"); if (idx!=-1) apellido = c.getString(idx);
            idx = c.getColumnIndex("edad"); if (idx!=-1) edad = c.getInt(idx);
            idx = c.getColumnIndex("estatura"); if (idx!=-1) estatura = c.getDouble(idx);
            idx = c.getColumnIndex("peso"); if (idx!=-1) peso = c.getDouble(idx);
            idx = c.getColumnIndex("sexo"); if (idx!=-1) sexo = c.getString(idx);
        }
        if (c != null) c.close();
        if (estatura > 0) imc = peso / (estatura*estatura);

        Integer sis = null, dia = null, pul = null; String fechaUlt = null;
        Cursor cu = db.getUltimaPresionDetallada(correo);
        if (cu != null && cu.moveToFirst()) {
            try {
                sis = cu.getInt(0); dia = cu.getInt(1); pul = cu.getInt(2); fechaUlt = cu.getString(3);
            } catch (Exception ignore) {}
        }
        if (cu != null) cu.close();
        // fallback legacy
        if (sis == null || dia == null) {
            String legacy = db.getPresionPorCorreo(correo);
            if (legacy != null && !legacy.isEmpty()) {
                try {
                    // Normalizar posibles errores de OCR comunes antes de extraer números
                    String norm = legacy;
                    norm = norm.replace('O', '0').replace('o', '0');
                    norm = norm.replace('I', '1').replace('l', '1').replace('L', '1');
                    // Reemplazar caracteres no numéricos relevantes por espacios para facilitar la extracción
                    norm = norm.replaceAll("[^0-9/\\- \\\t]", " ");

                    // Extraer secuencias de 1 a 3 dígitos (acepta números de 1,2 o 3 dígitos)
                    Pattern pat = Pattern.compile("\\d{1,3}");
                    Matcher m = pat.matcher(norm);
                    List<Integer> nums = new ArrayList<>();
                    while (m.find()) {
                        try {
                            nums.add(Integer.parseInt(m.group()));
                        } catch (Exception ignore) {}
                    }
                    if (nums.size() >= 2) {
                        sis = nums.get(0);
                        dia = nums.get(1);
                        if (nums.size() >= 3) pul = nums.get(2);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Documentos agrupados por fecha (yyyy-MM-dd)
        Map<String, List<String>> docsPorFecha = new LinkedHashMap<>();
        Cursor cd = db.getDocumentosPorCorreo(correo);
        if (cd != null) {
            while (cd.moveToNext()) {
                String resumen = null;
                String fecha = null;
                try {
                    int idxRes = -1, idxFecha = -1;
                    try { idxRes = cd.getColumnIndex("resumen"); } catch (Exception ignore) {}
                    try { idxFecha = cd.getColumnIndex("fecha"); } catch (Exception ignore) {}
                    if (idxRes != -1) resumen = cd.getString(idxRes);
                    else resumen = cd.getString(0);
                    if (idxFecha != -1) fecha = cd.getString(idxFecha);
                    else if (cd.getColumnCount() > 1) fecha = cd.getString(1);
                } catch (Exception ignore) {}
                String fechaDia = extraerDia(fecha);
                docsPorFecha.computeIfAbsent(fechaDia, k -> new ArrayList<>()).add((resumen != null ? resumen : ""));
            }
            cd.close();
        }

        // Historial detallado de presión (lista y estadísticas)
        List<int[]> presiones = new ArrayList<>(); // [sis,dia,pul] con fecha en otra lista
        List<String> fechasPa = new ArrayList<>();
        Cursor ch = db.getHistorialDetallado(correo);
        if (ch != null) {
            while (ch.moveToNext()) {
                try {
                    int s = ch.getInt(0);
                    int d = ch.getInt(1);
                    int p = ch.getInt(2);
                    presiones.add(new int[]{ s, d, p });
                    fechasPa.add(ch.getString(3));
                } catch (Exception ignore) {}
            }
            ch.close();
        }
        AnalisisPa analisis = analizarPresiones(presiones);

        // Sección: Resumen IA (modo enseñanza y aprendizaje) agrupado por fecha
        // Obtener logs IA desde la base de datos por usuario (no usar SharedPreferences globales)
        Map<String, List<String>> iaEnsPorFecha = new LinkedHashMap<>();
        Map<String, List<String>> iaAprPorFecha = new LinkedHashMap<>();
        Cursor cEns = db.getIaEnsenanza(correo);
        if (cEns != null) {
            while (cEns.moveToNext()) {
                String q = null, a = null, f = null;
                try { q = cEns.getString(0); a = cEns.getString(1); f = cEns.getString(2); } catch (Exception ignore) {}
                String day = extraerDia(f);
                String line = (q!=null && !q.isEmpty() ? ("P: "+q+" ") : "") + (a!=null && !a.isEmpty() ? ("R: "+a) : "");
                iaEnsPorFecha.computeIfAbsent(day, k -> new ArrayList<>()).add(line.trim());
            }
            cEns.close();
        }
        Cursor cApr = db.getIaAprendizaje(correo);
        if (cApr != null) {
            while (cApr.moveToNext()) {
                String q = null, a = null, f = null;
                try { q = cApr.getString(0); a = cApr.getString(1); f = cApr.getString(2); } catch (Exception ignore) {}
                String day = extraerDia(f);
                String line = (q!=null && !q.isEmpty() ? ("P: "+q+" ") : "") + (a!=null && !a.isEmpty() ? ("R: "+a) : "");
                iaAprPorFecha.computeIfAbsent(day, k -> new ArrayList<>()).add(line.trim());
            }
            cApr.close();
        }

        // Recomendaciones simples basadas en IMC y presión (informativo, no diagnóstico)
        String recoImc = recomendacionImc(imc);
        String recoPa = recomendacionPresion(sis, dia);
        String recoAnalitica = generarRecomendacionesDesdeAnalisis(analisis);

        // Construir contenido de word/document.xml con párrafos simples y secciones
        StringBuilder body = new StringBuilder();
        appendH1(body, "Expediente Médico Personal - VitalPress");

        // Perfil
        appendH2(body, "Datos del usuario");
        appendParagraph(body, "Nombre: " + safe(nombre) + " " + safe(apellido));
        appendParagraph(body, "Correo: " + safe(correo));
        appendParagraph(body, "Edad: " + edad + " - Sexo: " + safe(sexo));
        appendParagraph(body, String.format(Locale.getDefault(), "Peso: %.1f kg  Estatura: %.2f m  IMC: %.2f", peso, estatura, imc));

        // Última presión
        String pres = "Sistólica: " + (sis!=null?sis:"--") + "   Diastólica: " + (dia!=null?dia:"--") + "   Pulso: " + (pul!=null?pul:"--");
        appendH2(body, "Última presión arterial");
        appendParagraph(body, pres);
        if (fechaUlt != null) appendParagraph(body, "Fecha: " + fechaUlt);

        // Historial de presión (todos los registros)
        appendH2(body, "Historial de presión");
        if (presiones.isEmpty()) {
            appendParagraph(body, "(Sin registros)");
        } else {
            for (int i = 0; i < presiones.size(); i++) {
                int[] v = presiones.get(i);
                String f = (i < fechasPa.size() ? fechasPa.get(i) : "");
                appendBullet(body, String.format(Locale.getDefault(), "%s  %d/%d mmHg  Pulso: %d", f, v[0], v[1], v[2]));
            }
            // Estadísticas
            appendParagraph(body, String.format(Locale.getDefault(), "Promedios: %d/%d mmHg  Pulso: %d", analisis.avgSis, analisis.avgDia, analisis.avgPul));
            appendParagraph(body, String.format(Locale.getDefault(), "Máximos: %d/%d mmHg  Pulso: %d", analisis.maxSis, analisis.maxDia, analisis.maxPul));
            appendParagraph(body, String.format(Locale.getDefault(), "Mínimos: %d/%d mmHg  Pulso: %d", analisis.minSis, analisis.minDia, analisis.minPul));
            appendParagraph(body, String.format(Locale.getDefault(), "Tendencia sistólica (últimos %d): %s", analisis.trendWindow, analisis.trendSis));
            appendParagraph(body, String.format(Locale.getDefault(), "Tendencia diastólica (últimos %d): %s", analisis.trendWindow, analisis.trendDia));
        }

        // Documentos por fecha
        appendH2(body, "Resúmenes de documentos importados por fecha");
        if (docsPorFecha.isEmpty()) {
            appendParagraph(body, "(Sin documentos)");
        } else {
            for (Map.Entry<String, List<String>> e : docsPorFecha.entrySet()) {
                appendH3(body, "Fecha: " + e.getKey());
                for (String d : e.getValue()) {
                    appendBullet(body, d);
                }
            }
        }

        // IA por fecha (modo aprendizaje)
        appendH2(body, "Interacciones IA (Modo Aprendizaje)");
        if (iaAprPorFecha.isEmpty()) {
            appendParagraph(body, "(Sin interacciones)");
        } else {
            for (Map.Entry<String, List<String>> e : iaAprPorFecha.entrySet()) {
                appendH3(body, "Fecha: " + e.getKey());
                for (String line : e.getValue()) appendBullet(body, line);
            }
        }
        // IA por fecha (modo enseñanza)
        appendH2(body, "Interacciones IA (Modo Enseñanza)");
        if (iaEnsPorFecha.isEmpty()) {
            appendParagraph(body, "(Sin interacciones)");
        } else {
            for (Map.Entry<String, List<String>> e : iaEnsPorFecha.entrySet()) {
                appendH3(body, "Fecha: " + e.getKey());
                for (String line : e.getValue()) appendBullet(body, line);
            }
        }

        // Recomendaciones y conclusiones
        appendH2(body, "Análisis y recomendaciones (informativo, no diagnóstico)");
        boolean anyReco = false;
        if (recoImc != null) { appendBullet(body, "IMC: " + recoImc); anyReco = true; }
        if (recoPa != null) { appendBullet(body, "Presión (última): " + recoPa); anyReco = true; }
        if (recoAnalitica != null && !recoAnalitica.isEmpty()) { appendBullet(body, recoAnalitica); anyReco = true; }
        if (!anyReco) appendParagraph(body, "(Sin recomendaciones automáticas)");

        String documentXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<w:document xmlns:wpc=\"http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas\" " +
                "xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" " +
                "xmlns:o=\"urn:schemas-microsoft-com:office:office\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" " +
                "xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" " +
                "xmlns:v=\"urn:schemas-microsoft-com:vml\" " +
                "xmlns:wp14=\"http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing\" " +
                "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
                "xmlns:w10=\"urn:schemas-microsoft-com:office:word\" " +
                "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
                "xmlns:w14=\"http://schemas.microsoft.com/office/word/2010/wordml\" " +
                "xmlns:wpg=\"http://schemas.microsoft.com/office/word/2010/wordprocessingGroup\" " +
                "xmlns:wpi=\"http://schemas.microsoft.com/office/word/2010/wordprocessingInk\" " +
                "xmlns:wne=\"http://schemas.microsoft.com/office/2006/wordml\" mc:Ignorable=\"w14 wp14\">" +
                "<w:body>" + body.toString() + "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/></w:sectPr></w:body></w:document>";

        String relsRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                "</Relationships>";

        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                "</Types>";

        // Escribir ZIP
        File dir = context.getExternalFilesDir("Documents");
        if (dir == null) dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        File out = new File(dir, "Expediente_" + (correo!=null?correo.replaceAll("[^A-Za-z0-9._-]","_"):"usuario") + ".docx");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        // [Content_Types].xml
        zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
        zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // _rels/.rels
        zos.putNextEntry(new ZipEntry("_rels/.rels"));
        zos.write(relsRels.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        // word/document.xml
        zos.putNextEntry(new ZipEntry("word/document.xml"));
        zos.write(documentXml.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
        zos.finish();
        zos.close();
        // Guardar un registro del documento en la base de datos (vinculado al correo del usuario)
        try {
            // crear un resumen corto para la tabla (última presión + recomendaciones breves)
            String resumenCorto = "Última: " + (sis!=null?sis:"--") + "/" + (dia!=null?dia:"--") + " mmHg.";
            if (recoPa != null && !recoPa.isEmpty()) resumenCorto += " " + recoPa;
            String fechaNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            // usar la instancia 'db' ya existente en el método
            db.insertDocumento(correo, out.getAbsolutePath(), resumenCorto, fechaNow);
         } catch (Exception ignore) {}
         return out;
     }

    // Nuevo: generar archivo con nombre único (timestamp) reutilizando el DOCX base
    public static File generateUserDocxUnique(Context context, String correoParam) throws Exception {
        File base = generateUserDocx(context, correoParam);
        File dir = base.getParentFile();
        if (dir == null) dir = context.getFilesDir();
        String baseName = base.getName();
        int dot = baseName.lastIndexOf('.');
        String nameNoExt = dot > 0 ? baseName.substring(0, dot) : baseName;
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File out = new File(dir, nameNoExt + "_" + ts + ".docx");
        java.io.FileInputStream in = null;
        java.io.FileOutputStream outS = null;
        try {
            in = new java.io.FileInputStream(base);
            outS = new java.io.FileOutputStream(out);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                outS.write(buf, 0, len);
            }
            outS.flush();
            return out;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignore) {}
            try { if (outS != null) outS.close(); } catch (Exception ignore) {}
        }
    }

    // Generar/actualizar documentos para todos los usuarios registrados
    public static int generateDocsForAllUsers(Context context) {
        DBVitalPress db = new DBVitalPress(context);
        Cursor cur = null;
        int success = 0;
        try {
            cur = db.getAllCorreos();
            if (cur != null) {
                while (cur.moveToNext()) {
                    String correo = null;
                    try {
                        correo = cur.getString(0);
                    } catch (Exception ignore) {}
                    if (correo == null || correo.isEmpty()) continue;
                    try {
                        // Generar documento único (base ya registra un doc; aquí guardamos la copia única también)
                        File f = generateUserDocxUnique(context, correo);
                        String resumenCorto = "Expediente generado para " + correo;
                        String fechaNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                        try {
                            db.insertDocumento(correo, f.getAbsolutePath(), resumenCorto, fechaNow);
                        } catch (Exception ignore) {}
                        success++;
                    } catch (Exception e) {
                        // continuar con siguiente usuario
                        try { System.err.println("Error generando doc para: " + correo + " -> " + e.getMessage()); } catch (Exception ignore) {}
                    }
                }
            }
        } finally {
            try { if (cur != null) cur.close(); } catch (Exception ignore) {}
        }
        return success;
    }

    private static void appendP(StringBuilder body, String text) {
        String xml = "<w:p><w:r><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    // Encabezados y estructura con estilo (negrita, color, tamaño)
    private static void appendH1(StringBuilder body, String text) {
        // Heading1: bold, azul distintivo y tamaño más grande (valores w:sz en half-points)
        String xml = "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr>" +
                "<w:r><w:rPr><w:b/><w:color w:val=\"2F75B5\"/><w:sz w:val=\"36\"/></w:rPr><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    private static void appendH2(StringBuilder body, String text) {
        // Heading2: bold, color oscuro y tamaño mediano
        String xml = "<w:p><w:pPr><w:pStyle w:val=\"Heading2\"/></w:pPr>" +
                "<w:r><w:rPr><w:b/><w:color w:val=\"1F497D\"/><w:sz w:val=\"28\"/></w:rPr><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    private static void appendH3(StringBuilder body, String text) {
        // Heading3: bold, gris y tamaño ligeramente menor
        String xml = "<w:p><w:pPr><w:pStyle w:val=\"Heading3\"/></w:pPr>" +
                "<w:r><w:rPr><w:b/><w:color w:val=\"4F4F4F\"/><w:sz w:val=\"24\"/></w:rPr><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    private static void appendParagraph(StringBuilder body, String text) {
        // Párrafo normal con tamaño legible y color negro
        String xml = "<w:p><w:pPr/><w:r><w:rPr><w:color w:val=\"000000\"/><w:sz w:val=\"22\"/></w:rPr><w:t>" + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    private static void appendBullet(StringBuilder body, String text) {
        // Viñeta simple con indentación y texto en negrita ligera
        String xml = "<w:p><w:pPr><w:ind w:left=\"720\"/></w:pPr>" +
                "<w:r><w:rPr><w:b/><w:color w:val=\"333333\"/><w:sz w:val=\"22\"/></w:rPr><w:t>• " + escapeXml(text) + "</w:t></w:r></w:p>";
        body.append(xml);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String recomendacionImc(double imc) {
        if (imc <= 0) return null;
        if (imc < 18.5) return String.format(Locale.getDefault(), "IMC %.2f: por debajo del rango saludable. Consulta hábitos nutricionales.", imc);
        if (imc < 25) return String.format(Locale.getDefault(), "IMC %.2f: dentro del rango saludable.", imc);
        if (imc < 30) return String.format(Locale.getDefault(), "IMC %.2f: sobrepeso. Considera hábitos y actividad física.", imc);
        return String.format(Locale.getDefault(), "IMC %.2f: obesidad. Consulta con un profesional de salud.", imc);
    }

    private static String recomendacionPresion(Integer sis, Integer dia) {
        if (sis == null || dia == null) return null;
        if (sis < 120 && dia < 80) return "Valores dentro de rangos habituales (informativo).";
        if (sis >= 120 && sis <=129 && dia < 80) return "Sistólica elevada (informativo).";
        if ((sis >= 130 && sis <= 139) || (dia >= 80 && dia <= 89)) return "Posible elevación (informativo).";
        if (sis >= 140 || dia >= 90) return "Valores altos (informativo).";
        return "Valores informativos, revisa tendencias con tu historial.";
    }

    // Utilidades nuevas
    private static String extraerDia(String fecha) {
        if (fecha == null || fecha.isEmpty()) return "(sin fecha)";
        int sp = fecha.indexOf(' ');
        if (sp > 0) return fecha.substring(0, sp);
        // Si ya es solo día
        return fecha;
    }

    private static Map<String, List<String>> parseIaLogPorFecha(String rawJson) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (rawJson == null || rawJson.isEmpty()) return out;
        try {
            org.json.JSONArray arr = new org.json.JSONArray(rawJson);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                long ts = o.optLong("ts", 0);
                String q = o.optString("q", null);
                String a = o.optString("a", null);
                String day = ts > 0 ? sdf.format(new Date(ts)) : "(sin fecha)";
                String line = (q!=null?("P: "+q+" "):"") + (a!=null?("R: "+a):"");
                out.computeIfAbsent(day, k -> new ArrayList<>()).add(line.trim());
            }
        } catch (Exception ignore) {}
        return out;
    }

    private static AnalisisPa analizarPresiones(List<int[]> pres) {
        AnalisisPa a = new AnalisisPa();
        if (pres == null || pres.isEmpty()) return a;
        int n = pres.size();
        int sumSis=0,sumDia=0,sumPul=0;
        a.minSis=999;a.minDia=999;a.minPul=999;a.maxSis=0;a.maxDia=0;a.maxPul=0;
        for (int[] v : pres) {
            int s=v[0], d=v[1], p=v[2];
            sumSis+=s; sumDia+=d; sumPul+=p;
            if (s<a.minSis)a.minSis=s; if (d<a.minDia)a.minDia=d; if (p<a.minPul)a.minPul=p;
            if (s>a.maxSis)a.maxSis=s; if (d>a.maxDia)a.maxDia=d; if (p>a.maxPul)a.maxPul=p;
        }
        a.avgSis = Math.round(sumSis/(float)n);
        a.avgDia = Math.round(sumDia/(float)n);
        a.avgPul = Math.round(sumPul/(float)n);
        // Tendencia simple: comparar primer y último en ventana de hasta 5
        int w = Math.min(5, n);
        a.trendWindow = w;
        int firstSis = pres.get(w-1)[0];
        int lastSis = pres.get(0)[0];
        int firstDia = pres.get(w-1)[1];
        int lastDia = pres.get(0)[1];
        a.trendSis = lastSis>firstSis?"Subiendo":(lastSis<firstSis?"Bajando":"Estable");
        a.trendDia = lastDia>firstDia?"Subiendo":(lastDia<firstDia?"Bajando":"Estable");
        // Conteos en rangos
        for (int[] v: pres) {
            int s=v[0], d=v[1];
            if (s<120 && d<80) a.cNormales++;
            else if (s>=120 && s<=129 && d<80) a.cElevadas++;
            else if ((s>=130 && s<=139) || (d>=80 && d<=89)) a.cHTA1++;
            else if (s>=140 || d>=90) a.cHTA2++;
        }
        return a;
    }

    private static String generarRecomendacionesDesdeAnalisis(AnalisisPa a) {
        if (a == null || a.avgSis == 0) return "";
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.getDefault(), "Promedio global ~ %d/%d mmHg; tendencia SIS: %s, DIA: %s.", a.avgSis, a.avgDia, a.trendSis, a.trendDia));
        if (a.cHTA2 > 0) lines.add("Se observan varios valores altos. Considera consulta médica.");
        else if (a.cHTA1 > 0) lines.add("Se observan valores elevados. Revisa hábitos y seguimiento.");
        if (a.trendSis.equals("Subiendo") || a.trendDia.equals("Subiendo")) lines.add("Tendencia al alza reciente. Vigila mediciones y hábitos.");
        if (a.avgPul > 100) lines.add("Pulso promedio alto; confirma en reposo y consulta si persiste.");
        return String.join(" ", lines);
    }

    private static class AnalisisPa {
        int avgSis, avgDia, avgPul;
        int minSis, minDia, minPul;
        int maxSis, maxDia, maxPul;
        String trendSis = "N/D", trendDia = "N/D";
        int trendWindow = 0;
        int cNormales=0, cElevadas=0, cHTA1=0, cHTA2=0;
    }
}
