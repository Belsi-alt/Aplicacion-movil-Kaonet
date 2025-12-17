package com.example.fincacacaoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etTelefono;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Ocultar action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Conectar variables con el XML
        etTelefono = findViewById(R.id.etTelefono);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Configurar botón Iniciar Sesión
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Configurar botón Crear Cuenta
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String telefono = etTelefono.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validarLogin(telefono, password)) {
            return;
        }

        // Buscar usuario por teléfono en Firestore
        buscarUsuarioPorTelefono(telefono, password);
    }

    private boolean validarLogin(String telefono, String password) {
        if (TextUtils.isEmpty(telefono)) {
            etTelefono.setError("Ingrese su número de teléfono");
            etTelefono.requestFocus();
            return false;
        }

        if (telefono.length() < 10) {
            etTelefono.setError("Ingrese un teléfono válido (10+ dígitos)");
            etTelefono.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingrese su contraseña");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void buscarUsuarioPorTelefono(String telefono, String password) {
        // Deshabilitar botones durante el login
        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);
        btnLogin.setText("Verificando...");

        // Buscar usuario por id_telefono en Firestore
        db.collection("usuarios")
                .whereEqualTo("id_telefono", telefono)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // Usuario encontrado
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                            // Buscar campos con nombres nuevos y viejos
                            String contraseñaGuardada = document.getString("contraseña");
                            String email = document.getString("correo");
                            String nombre = document.getString("name");

                            // Si no encuentra con nombres nuevos, buscar con nombres viejos
                            if (contraseñaGuardada == null) {
                                contraseñaGuardada = document.getString("password");
                            }
                            if (email == null) {
                                email = document.getString("email");
                            }
                            if (nombre == null) {
                                nombre = document.getString("nombre");
                            }

                            // Verificar contraseña
                            if (contraseñaGuardada != null && contraseñaGuardada.equals(password)) {
                                // Login exitoso
                                Toast.makeText(LoginActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();

                                // Guardar sesión
                                guardarSesionUsuario(document.getId(), nombre, email, telefono);

                                // Redirigir al Home
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();

                            } else {
                                Toast.makeText(LoginActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                                restaurarBotones();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Teléfono no registrado", Toast.LENGTH_SHORT).show();
                            restaurarBotones();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                        restaurarBotones();
                    }
                });
    }

    private void guardarSesionUsuario(String userId, String nombre, String email, String telefono) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);
        editor.putString("nombre", nombre != null ? nombre : "Usuario");
        editor.putString("email", email != null ? email : "");
        editor.putString("telefono", telefono);
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }

    private void restaurarBotones() {
        btnLogin.setEnabled(true);
        btnRegister.setEnabled(true);
        btnLogin.setText("Iniciar Sesión");
    }
}