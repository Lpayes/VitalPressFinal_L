package com.example.vitalpreesoficial.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// Clase de base de datos central para VitalPress
public class DBVitalPress extends SQLiteOpenHelper {
    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "vitalpress.db";
    private static final int DATABASE_VERSION = 4;

    public DBVitalPress(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear tablas necesarias aquí (incluye usuario y tablas historial)
        db.execSQL("CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, apellido TEXT, correo TEXT UNIQUE, edad INTEGER, estatura REAL, peso REAL, contrasena TEXT, sexo TEXT, presion_arterial TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS presion_historial (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, valor TEXT, fecha TEXT)");
        // Nueva: registros detallados de presión
        db.execSQL("CREATE TABLE IF NOT EXISTS presion_registros (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, sistolica INTEGER, diastolica INTEGER, pulso INTEGER, fecha TEXT)");
        // Nueva: documentos importados con resumen
        db.execSQL("CREATE TABLE IF NOT EXISTS documentos (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, uri TEXT, resumen TEXT, fecha TEXT)");
        // Índices útiles
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_presion_registros_correo_fecha ON presion_registros(correo, fecha)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_documentos_correo_fecha ON documentos(correo, fecha)");
        // NUEVAS: tablas IA
        db.execSQL("CREATE TABLE IF NOT EXISTS ia_ensenanza (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, pregunta TEXT, respuesta TEXT, fecha TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ia_aprendizaje (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, pregunta TEXT, respuesta TEXT, fecha TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS ia_sugerencias (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, categoria TEXT, sugerencia TEXT, fecha TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_ensenanza_correo_fecha ON ia_ensenanza(correo, fecha)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_aprendizaje_correo_fecha ON ia_aprendizaje(correo, fecha)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_sugerencias_correo_fecha ON ia_sugerencias(correo, fecha)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Migraciones incrementales sin perder datos existentes
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, apellido TEXT, correo TEXT UNIQUE, edad INTEGER, estatura REAL, peso REAL, contrasena TEXT, sexo TEXT, presion_arterial TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS presion_historial (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, valor TEXT, fecha TEXT)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS presion_registros (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, sistolica INTEGER, diastolica INTEGER, pulso INTEGER, fecha TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS documentos (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, uri TEXT, resumen TEXT, fecha TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_presion_registros_correo_fecha ON presion_registros(correo, fecha)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_documentos_correo_fecha ON documentos(correo, fecha)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS ia_ensenanza (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, pregunta TEXT, respuesta TEXT, fecha TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS ia_aprendizaje (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, pregunta TEXT, respuesta TEXT, fecha TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS ia_sugerencias (id INTEGER PRIMARY KEY AUTOINCREMENT, correo TEXT, categoria TEXT, sugerencia TEXT, fecha TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_ensenanza_correo_fecha ON ia_ensenanza(correo, fecha)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_aprendizaje_correo_fecha ON ia_aprendizaje(correo, fecha)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_ia_sugerencias_correo_fecha ON ia_sugerencias(correo, fecha)");
        }
    }

    // Método para insertar usuario
    public long insertUsuario(String nombre, String apellido, String correo, int edad, double estatura, double peso, String contrasena, String sexo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("apellido", apellido);
        values.put("correo", correo);
        values.put("edad", edad);
        values.put("estatura", estatura);
        values.put("peso", peso);
        values.put("contrasena", contrasena);
        values.put("sexo", sexo);
        return db.insert("usuario", null, values);
    }

    // Método para actualizar usuario por id
    public int updateUsuario(int id, String nombre, String correo, int edad, double estatura, double peso, String contrasena, String sexo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("correo", correo);
        values.put("edad", edad);
        values.put("estatura", estatura);
        values.put("peso", peso);
        values.put("contrasena", contrasena);
        values.put("sexo", sexo);
        return db.update("usuario", values, "id=?", new String[]{String.valueOf(id)});
    }

    // Actualizar solo el nombre del usuario por correo
    public int updateUsuarioNombre(String correo, String nuevoNombre) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nuevoNombre);
        return db.update("usuario", values, "correo = ?", new String[]{correo});
    }

    // Actualizar nombre y apellido del usuario por correo
    public int updateUsuarioNombreCompleto(String correo, String nombre, String apellido) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("apellido", apellido);
        return db.update("usuario", values, "correo = ?", new String[]{correo});
    }

    // Método para exportar todos los usuarios
    public List<String> exportarUsuarios() {
        List<String> usuarios = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuario", null);
        if (cursor.moveToFirst()) {
            do {
                String datos = "ID: " + cursor.getInt(0) + ", Nombre: " + cursor.getString(1) + ", Correo: " + cursor.getString(2) + ", Edad: " + cursor.getInt(3) + ", Estatura: " + cursor.getDouble(4) + ", Peso: " + cursor.getDouble(5);
                usuarios.add(datos);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return usuarios;
    }

    // Buscar usuario por correo
    public Cursor buscarUsuarioPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM usuario WHERE correo = ?", new String[]{correo});
    }

    // Actualizar presión arterial por correo (string legacy)
    public int updatePresionPorCorreo(String correo, String presion) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("presion_arterial", presion);
        return db.update("usuario", values, "correo = ?", new String[]{correo});
    }

    // Insertar registro de presión en historial (string legacy)
    public long insertPresionHistorial(String correo, String valor, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("valor", valor);
        values.put("fecha", fecha);
        return db.insert("presion_historial", null, values);
    }

    // Obtener historial de presión por correo (string legacy)
    public Cursor getHistorialPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT valor, fecha FROM presion_historial WHERE correo = ? ORDER BY id DESC", new String[]{correo});
    }

    // Obtener presión arterial por correo (string legacy)
    public String getPresionPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT presion_arterial FROM usuario WHERE correo = ?", new String[]{correo});
        String presion = "";
        if (cursor.moveToFirst()) {
            presion = cursor.getString(0);
            if (presion == null) presion = "";
        }
        cursor.close();
        return presion;
    }

    // NUEVO: Insertar registro detallado de presión
    public long insertPresionDetallada(String correo, int sistolica, int diastolica, int pulso, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("sistolica", sistolica);
        values.put("diastolica", diastolica);
        values.put("pulso", pulso);
        values.put("fecha", fecha);
        return db.insert("presion_registros", null, values);
    }

    // NUEVO: Obtener último registro detallado de presión
    public Cursor getUltimaPresionDetallada(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT sistolica, diastolica, pulso, fecha FROM presion_registros WHERE correo = ? ORDER BY fecha DESC LIMIT 1", new String[]{correo});
    }

    // NUEVO: Obtener historial detallado de presión
    public Cursor getHistorialDetallado(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT sistolica, diastolica, pulso, fecha FROM presion_registros WHERE correo = ? ORDER BY fecha DESC", new String[]{correo});
    }

    // NUEVO: Insertar documento con resumen
    public long insertDocumento(String correo, String uri, String resumen, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("uri", uri);
        values.put("resumen", resumen);
        values.put("fecha", fecha);
        return db.insert("documentos", null, values);
    }

    // NUEVO: Listar documentos por correo
    public Cursor getDocumentosPorCorreo(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT resumen, fecha, uri FROM documentos WHERE correo = ? ORDER BY id DESC", new String[]{correo});
    }

    // Validar usuario y contraseña
    public boolean validarUsuario(String correo, String contrasena) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM usuario WHERE correo = ? AND contrasena = ?", new String[]{correo, contrasena});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    // Registrar usuario si no existe
    public boolean registrarUsuarioSiNoExiste(String nombre, String apellido, String correo, int edad, double estatura, double peso, String contrasena, String sexo) {
        Cursor cursor = buscarUsuarioPorCorreo(correo);
        boolean existe = cursor.moveToFirst();
        cursor.close();
        if (!existe) {
            insertUsuario(nombre, apellido, correo, edad, estatura, peso, contrasena, sexo);
            return true;
        }
        return false;
    }

    // NUEVO: IA Enseñanza
    public long insertIaEnsenanza(String correo, String pregunta, String respuesta, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("pregunta", pregunta);
        values.put("respuesta", respuesta);
        values.put("fecha", fecha);
        return db.insert("ia_ensenanza", null, values);
    }

    public Cursor getIaEnsenanza(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT pregunta, respuesta, fecha FROM ia_ensenanza WHERE correo = ? ORDER BY id DESC", new String[]{correo});
    }

    // NUEVO: IA Aprendizaje
    public long insertIaAprendizaje(String correo, String pregunta, String respuesta, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("pregunta", pregunta);
        values.put("respuesta", respuesta);
        values.put("fecha", fecha);
        return db.insert("ia_aprendizaje", null, values);
    }

    public Cursor getIaAprendizaje(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT pregunta, respuesta, fecha FROM ia_aprendizaje WHERE correo = ? ORDER BY id DESC", new String[]{correo});
    }

    // NUEVO: Sugerencias personalizadas
    public long insertSugerencia(String correo, String categoria, String sugerencia, String fecha) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("correo", correo);
        values.put("categoria", categoria);
        values.put("sugerencia", sugerencia);
        values.put("fecha", fecha);
        return db.insert("ia_sugerencias", null, values);
    }

    public Cursor getSugerencias(String correo) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT categoria, sugerencia, fecha FROM ia_sugerencias WHERE correo = ? ORDER BY id DESC", new String[]{correo});
    }

    // Obtener todos los correos de usuarios (para operaciones en lote)
    public Cursor getAllCorreos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT correo FROM usuario WHERE correo IS NOT NULL AND correo <> ''", null);
    }
}
