package com.example.fincacacaoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserPhone, tvUserCorreo, tvUserPassword;
    private LinearLayout optionInfoUsuario, contentInfoUsuario, optionAyuda;
    private TextView arrowInfo;
    private Button btnCerrarSesion;

    // Firebase - Mismo que en HomeActivity
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // Inicializar Firebase igual que en HomeActivity
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        cargarDatosUsuario();
        configurarEventos();
        configurarBottomNavigation();
    }

    private void inicializarVistas() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvUserCorreo = findViewById(R.id.tvUserCorreo); // Cambiado de tvUserFinca
        tvUserPassword = findViewById(R.id.tvUserPassword); // Nuevo
        optionInfoUsuario = findViewById(R.id.optionInfoUsuario);
        contentInfoUsuario = findViewById(R.id.contentInfoUsuario);
        arrowInfo = findViewById(R.id.arrowInfo);
        optionAyuda = findViewById(R.id.optionAyuda);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }
    private String obtenerUsuarioId() {
        // MISMA LÓGICA QUE HOME ACTIVITY
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String telefono = sharedPreferences.getString("telefono", "");

        if (!telefono.isEmpty()) {
            return telefono; // O el ID que uses en Firestore
        } else if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }

        return ""; // Si no hay usuario
    }

    private void guardarDatosEnPrefs(String nombre, String telefono, String correo, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (nombre != null) editor.putString("nombre", nombre);
        if (telefono != null) editor.putString("telefono", telefono);
        if (correo != null) editor.putString("correo", correo); // Cambiado de "finca" a "correo"
        // NO guardamos password en SharedPreferences por seguridad

        editor.apply();
    }
    private void cargarDatosUsuario() {
        // MISMA LÓGICA QUE HOME ACTIVITY
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String nombreGuardado = sharedPreferences.getString("nombre", null);
        String telefonoGuardado = sharedPreferences.getString("telefono", null);

        if (nombreGuardado != null) {
            tvUserName.setText("Nombre: " + nombreGuardado);
        }

        if (telefonoGuardado != null) {
            tvUserPhone.setText("Teléfono: " + telefonoGuardado);
            // Buscar más datos del usuario por teléfono
            buscarUsuarioCompletoPorTelefono(telefonoGuardado);
        } else if (mAuth.getCurrentUser() != null) {
            // Buscar por UserId de Firebase Auth
            buscarUsuarioCompletoPorUserId(mAuth.getCurrentUser().getUid());
        } else {
            tvUserName.setText("Nombre: No disponible");
            tvUserPhone.setText("Teléfono: No disponible");
        }
    }

    private void buscarUsuarioCompletoPorTelefono(String telefono) {
        db.collection("usuarios")
                .whereEqualTo("id_telefono", telefono)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        mostrarDatosCompletosUsuario(document);
                    }
                });
    }

    private void buscarUsuarioCompletoPorUserId(String userId) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        mostrarDatosCompletosUsuario(document);
                    }
                });
    }

    private void mostrarDatosCompletosUsuario(DocumentSnapshot document) {
        String nombre = document.getString("name");
        if (nombre == null) nombre = document.getString("nombre");
        String telefono = document.getString("id_telefono");
        String correo = document.getString("correo");
        String password = document.getString("password"); // Nueva contraseña

        // Mostrar todos los datos
        if (nombre != null) tvUserName.setText("Nombre: " + nombre);
        if (telefono != null) tvUserPhone.setText("Teléfono: " + telefono);
        if (correo != null) tvUserCorreo.setText("Correo: " + correo);
        if (password != null) {
            // Mostrar contraseña con asteriscos
            String maskedPassword = "••••••••";
            tvUserPassword.setText("Contraseña: " + maskedPassword);
            // Guardar la real para poder editarla
            tvUserPassword.setTag(password); // Guardar contraseña real como tag
        }

        // Hacer los campos editables con click
        hacerCamposEditables();

        // Guardar en SharedPreferences
        guardarDatosEnPrefs(nombre, telefono, correo, password);
    }

    private void hacerCamposEditables() {
        // Configurar click listeners para editar
        tvUserName.setOnClickListener(v -> mostrarDialogoEditar("Nombre", tvUserName.getText().toString().replace("Nombre: ", ""), "nombre"));
        tvUserPhone.setOnClickListener(v -> mostrarDialogoEditar("Teléfono", tvUserPhone.getText().toString().replace("Teléfono: ", ""), "telefono"));
        tvUserCorreo.setOnClickListener(v -> mostrarDialogoEditar("Correo", tvUserCorreo.getText().toString().replace("Correo: ", ""), "correo"));
        tvUserPassword.setOnClickListener(v -> mostrarDialogoCambiarContraseña());
    }

    private void mostrarDialogoCambiarContraseña() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Cambiar Contraseña");

        // Diseño personalizado
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Contraseña actual
        android.widget.TextView tvActual = new android.widget.TextView(this);
        tvActual.setText("Contraseña actual: ••••••••");
        layout.addView(tvActual);

        // Nueva contraseña
        android.widget.EditText etNueva = new android.widget.EditText(this);
        etNueva.setHint("Nueva contraseña");
        etNueva.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNueva);

        // Confirmar nueva
        android.widget.EditText etConfirmar = new android.widget.EditText(this);
        etConfirmar.setHint("Confirmar nueva contraseña");
        etConfirmar.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmar);

        builder.setView(layout);

        builder.setPositiveButton("Cambiar", (dialog, which) -> {
            String nueva = etNueva.getText().toString();
            String confirmar = etConfirmar.getText().toString();

            if (nueva.isEmpty() || confirmar.isEmpty()) {
                Toast.makeText(this, "Debe completar ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!nueva.equals(confirmar)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nueva.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar en Firebase
            actualizarContraseñaEnFirebase(nueva);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarDialogoEditar(String titulo, String valorActual, String campo) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Editar " + titulo);

        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(valorActual);

        if (campo.equals("telefono")) {
            input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else if (campo.equals("correo")) {
            input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoValor = input.getText().toString().trim();
            if (!nuevoValor.isEmpty()) {
                actualizarCampoEnFirebase(campo, nuevoValor);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarContraseñaEnFirebase(String nuevaPassword) {
        // Obtener userId (ajusta según tu sistema)
        String userId = obtenerUsuarioId(); // Tu método para obtener ID

        db.collection("usuarios").document(userId)
                .update("password", nuevaPassword)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    // Actualizar en la vista (mantener asteriscos)
                    tvUserPassword.setText("Contraseña: ••••••••");
                    tvUserPassword.setTag(nuevaPassword); // Actualizar tag
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarCampoEnFirebase(String campo, String nuevoValor) {
        String userId = obtenerUsuarioId();

        // Mapear nombres de campos
        String campoFirebase = campo;
        if (campo.equals("nombre")) campoFirebase = "name";
        if (campo.equals("telefono")) campoFirebase = "id_telefono";
        if (campo.equals("correo")) campoFirebase = "correo";

        db.collection("usuarios").document(userId)
                .update(campoFirebase, nuevoValor)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ " + campo + " actualizado", Toast.LENGTH_SHORT).show();

                    // Actualizar TextView correspondiente
                    switch (campo) {
                        case "nombre":
                            tvUserName.setText("Nombre: " + nuevoValor);
                            break;
                        case "telefono":
                            tvUserPhone.setText("Teléfono: " + nuevoValor);
                            break;
                        case "correo":
                            tvUserCorreo.setText("Correo: " + nuevoValor);
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void configurarEventos() {
        // Expandir/contraer información de usuario
        optionInfoUsuario.setOnClickListener(v -> {
            if (contentInfoUsuario.getVisibility() == View.VISIBLE) {
                contentInfoUsuario.setVisibility(View.GONE);
                arrowInfo.setText("▼");
            } else {
                contentInfoUsuario.setVisibility(View.VISIBLE);
                arrowInfo.setText("▲");
            }
        });

        // Ayuda y soporte - abre Gmail
        optionAyuda.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:leongelvezcamilo@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Soporte - Finca Cacao App");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hola, necesito ayuda con la aplicación...");

            try {
                startActivity(Intent.createChooser(emailIntent, "Enviar correo..."));
            } catch (Exception e) {
                Toast.makeText(PerfilActivity.this, "No hay aplicación de correo instalada", Toast.LENGTH_SHORT).show();
            }
        });

        // CERRAR SESIÓN COMPLETO
        btnCerrarSesion.setOnClickListener(v -> {
            cerrarSesion();
        });
    }

    private void cerrarSesion() {
        // 1. Cerrar Firebase Auth
        mAuth.signOut();

        // 2. Limpiar SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // 3. Ir al Login y limpiar historial
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(PerfilActivity.this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
    }

    private void configurarBottomNavigation() {
        findViewById(R.id.boton_inicio).setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_lotes).setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, LotesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_informes).setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, InformesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Perfil ya está activo, no necesita acción
        findViewById(R.id.boton_perfil).setOnClickListener(v -> {
            // Ya estamos en Perfil, no hacer nada
        });
    }

}