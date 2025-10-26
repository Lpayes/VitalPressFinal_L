package com.example.vitalpreesoficial;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalpreesoficial.db.DBVitalPress;

public class AjustesActivity extends AppCompatActivity {

    private ImageView circuloFotoPerfil;
    private Button btnCambiarPerfil;
    private ActivityResultLauncher<Intent> seleccionarImagenLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_ajustes);

            // Inicializar vistas
            circuloFotoPerfil = findViewById(R.id.circulo_foto_perfil);
            btnCambiarPerfil = findViewById(R.id.btn_cambiar_perfil);

            // Configurar launcher para seleccionar imagen
            configurarSelectorImagen();

            // Configurar clic en foto de perfil Y en botón cambiar
            if (circuloFotoPerfil != null) {
                circuloFotoPerfil.setOnClickListener(v -> mostrarOpcionesCambiarFoto());
            }

            if (btnCambiarPerfil != null) {
                btnCambiarPerfil.setOnClickListener(v -> mostrarOpcionesCambiarFoto());
            }

            // Mostrar nombre y apellido del usuario activo
            SharedPreferences prefs = getSharedPreferences("usuario_activo", MODE_PRIVATE);
            String correo = prefs.getString("correo", null);
            TextView txtNombreApellido = findViewById(R.id.txt_nombre_apellido_perfil);
            EditText editContactoAjustes = findViewById(R.id.edit_contacto_emergencia_ajustes);
            Button btnGuardarNombre = findViewById(R.id.btn_guardar_nombre);

            // Cargar contacto de emergencia guardado en SharedPreferences
            String contactoGuardado = prefs.getString("contacto_emergencia", "");
            if (contactoGuardado != null && editContactoAjustes != null) {
                editContactoAjustes.setText(contactoGuardado);
            }

            if (correo != null && txtNombreApellido != null) {
                try {
                    DBVitalPress db = new DBVitalPress(this);
                    Cursor cursor = db.buscarUsuarioPorCorreo(correo);
                    if (cursor != null && cursor.moveToFirst()) {
                        try {
                            // Obtener nombre completo (nombre y apellido) del usuario
                            String nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
                            String apellido = "";

                            try {
                                // Intentar obtener el apellido (podría no existir en bases de datos antiguas)
                                apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido"));
                            } catch (Exception e) {
                                // Si no existe la columna, simplemente continuamos sin el apellido
                            }

                            // Combinar nombre y apellido para mostrarlos
                            String nombreCompleto = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
                            nombreCompleto = nombreCompleto.trim(); // Eliminar espacios extras

                            if (nombreCompleto.isEmpty()) {
                                nombreCompleto = "Usuario"; // Valor por defecto si no hay datos
                            }

                            txtNombreApellido.setText(nombreCompleto);
                        } catch (Exception e) {
                            txtNombreApellido.setText("Usuario");
                        }
                        cursor.close();
                    } else {
                        txtNombreApellido.setText("Usuario");
                        if (cursor != null) {
                            cursor.close();
                        }
                    }

                    if (btnGuardarNombre != null) {
                        btnGuardarNombre.setOnClickListener(v -> {
                            try {
                                String nuevoNombre = txtNombreApellido.getText().toString().trim();
                                String nuevoContacto = "";

                                if (editContactoAjustes != null) {
                                    nuevoContacto = editContactoAjustes.getText().toString().trim();
                                }

                                if (!nuevoNombre.isEmpty()) {
                                    // Dividir el nombre completo en nombre y apellido (si es posible)
                                    String[] partes = nuevoNombre.split(" ", 2);
                                    String nombre = partes[0];
                                    String apellido = partes.length > 1 ? partes[1] : "";

                                    // Actualizar tanto nombre como apellido en la base de datos
                                    db.updateUsuarioNombreCompleto(correo, nombre, apellido);

                                    // Guardar contacto en SharedPreferences
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("contacto_emergencia", nuevoContacto);
                                    editor.apply();
                                    Toast.makeText(this, "Nombre y contacto actualizados", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    if (txtNombreApellido != null) {
                        txtNombreApellido.setText("Usuario");
                    }
                    Toast.makeText(this, "Error al cargar datos de usuario", Toast.LENGTH_SHORT).show();
                }
            } else if (txtNombreApellido != null) {
                txtNombreApellido.setText("Usuario");
            }

            // Botón Regresar
            Button btnRegresar = findViewById(R.id.btn_regresar_ajustes);
            if (btnRegresar != null) {
                btnRegresar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish(); // Regresar al menú principal
                    }
                });
            }

            // Botón Cerrar Sesión
            Button btnCerrarSesion = findViewById(R.id.btn_cerrar_sesion);
            if (btnCerrarSesion != null) {
                btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Mostrar diálogo de confirmación para cerrar sesión
                        new AlertDialog.Builder(AjustesActivity.this)
                                .setTitle("Cerrar Sesión")
                                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                                .setPositiveButton("Sí", (dialog, which) -> {
                                    try {
                                        // Cerrar sesión y regresar al login
                                        Intent intent = new Intent(AjustesActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } catch (Exception e) {
                                        Toast.makeText(AjustesActivity.this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                });
            }
        } catch (Exception e) {
            // Si ocurre cualquier error grave, mostrar un mensaje y volver al menú principal
            Toast.makeText(this, "Error al abrir ajustes. Volviendo al menú principal.", Toast.LENGTH_LONG).show();
            finish(); // Volver al menú principal de forma segura
        }
    }

    private void configurarSelectorImagen() {
        seleccionarImagenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Establecer la imagen seleccionada en el círculo de perfil
                            circuloFotoPerfil.setImageURI(imageUri);
                            circuloFotoPerfil.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void mostrarOpcionesCambiarFoto() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setMessage("Selecciona una opción:")
                .setPositiveButton("Galería", (dialog, which) -> {
                    // Abrir galería para seleccionar imagen
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    seleccionarImagenLauncher.launch(intent);
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Quitar foto", (dialog, which) -> {
                    // Restaurar imagen por defecto (círculo gris)
                    circuloFotoPerfil.setImageResource(0);
                    circuloFotoPerfil.setBackgroundResource(R.drawable.circulo_perfil);
                    Toast.makeText(this, "Foto de perfil eliminada", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
