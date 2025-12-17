package com.example.fincacacaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombreCompleto, etEmail, etTelefono, etPassword, etConfirmarPassword;
    private Button btnRegistrarse, btnVolverLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inicializarVistas();
        configurarEventos();
    }

    private void inicializarVistas() {
        etNombreCompleto = findViewById(R.id.etNombreCompleto);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etPassword = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);
    }

    private void configurarEventos() {
        // Botón Registrarse
        btnRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validarCampos()) {
                    registrarUsuario();
                }
            }
        });

        // Botón Volver al Login
        btnVolverLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Regresa al LoginActivity
            }
        });
    }

    private boolean validarCampos() {
        String nombre = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmarPassword = etConfirmarPassword.getText().toString();

        // Validaciones básicas
        if (nombre.isEmpty()) {
            mostrarError("Ingrese su nombre completo");
            return false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Ingrese un email válido");
            return false;
        }

        if (telefono.isEmpty() || telefono.length() < 10) {
            mostrarError("Ingrese un número de teléfono válido");
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }

        if (!password.equals(confirmarPassword)) {
            mostrarError("Las contraseñas no coinciden");
            return false;
        }

        return true;
    }

    private void registrarUsuario() {
        String nombre = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Mostrar progreso
        btnRegistrarse.setEnabled(false);
        btnRegistrarse.setText("Registrando...");

        // 1. Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 2. Usuario creado en Auth, ahora guardar datos en Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        guardarUsuarioEnFirestore(nombre, email, telefono, password);
                    } else {
                        // Error en registro
                        String error = task.getException().getMessage();
                        Toast.makeText(RegistroActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        btnRegistrarse.setEnabled(true);
                        btnRegistrarse.setText("Registrarse");
                    }
                });
    }

    private void guardarUsuarioEnFirestore(String nombre, String email, String telefono, String password) {
        // Crear objeto con datos del usuario - SEGÚN NUEVO FORMATO
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id_telefono", telefono);
        usuario.put("name", nombre);
        usuario.put("correo", email);
        usuario.put("contraseña", password);
        usuario.put("fechaRegistro", System.currentTimeMillis());

        // Guardar usando el TELÉFONO como ID del documento
        db.collection("usuarios").document(telefono)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    // Registro completado exitosamente
                    Toast.makeText(RegistroActivity.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();

                    // Ir al Home
                    Intent intent = new Intent(RegistroActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegistroActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRegistrarse.setEnabled(true);
                    btnRegistrarse.setText("Registrarse");
                });
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}