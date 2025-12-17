package com.example.fincacacaoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fincacacaoapp.adapters.CarruselAdapter;
import com.example.fincacacaoapp.adapters.RegistroAdapter;
import com.example.fincacacaoapp.adapters.ZoomOutPageTransformer;
import com.example.fincacacaoapp.models.CarruselItem;
import com.example.fincacacaoapp.models.Lote;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerRegistros;
    private FloatingActionButton fabAnadirRegistro;
    private TextView tvSaludo;
    private List<Lote> listaRegistrosRecientes;
    private ViewPager2 viewPagerCarrusel;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Para auto-scroll del carrusel
    private Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        inicializarVistas();
        cargarDatosUsuario();
        cargarLotesReales();
        configurarEventos();
    }

    private void inicializarVistas() {
        recyclerRegistros = findViewById(R.id.recyclerRegistros);
        fabAnadirRegistro = findViewById(R.id.fabAnadirRegistro);
        tvSaludo = findViewById(R.id.tvSaludo);
        viewPagerCarrusel = findViewById(R.id.viewPagerCarrusel); // ← AGREGADO
        listaRegistrosRecientes = new ArrayList<>();

        // Configurar RecyclerView
        recyclerRegistros.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar carrusel
        inicializarCarrusel();
    }

    private void inicializarCarrusel() {
        try {
            // Crear lista de imágenes
            List<CarruselItem> carruselItems = new ArrayList<>();

            // Siempre usar colores para evitar errores de drawables
            carruselItems.add(new CarruselItem(R.drawable.carrusel1,
                    "Finca Cacao App", "Gestión inteligente de cultivos"));
            carruselItems.add(new CarruselItem(R.drawable.carrusel2,
                    "Control de Lotes", "Monitorea cada etapa"));
            carruselItems.add(new CarruselItem(R.drawable.carrusel3,
                    "Reportes en Tiempo Real", "Toma decisiones informadas"));

            // Configurar adapter básico primero
            CarruselAdapter adapter = new CarruselAdapter(carruselItems);
            viewPagerCarrusel.setAdapter(adapter);
            viewPagerCarrusel.setOffscreenPageLimit(1);

            // Auto-scroll básico
            iniciarAutoScroll();

        } catch (Exception e) {
            Log.e("CARRUSEL", "Error inicializando carrusel: " + e.getMessage());
            // No ocultar, dejar que muestre algo
        }
    }

    private void iniciarAutoScroll() {
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (viewPagerCarrusel.getAdapter() != null) {
                    // Simplemente avanzar al siguiente item
                    // El loop infinito del adapter se encarga del ciclo
                    int nextItem = viewPagerCarrusel.getCurrentItem() + 1;
                    viewPagerCarrusel.setCurrentItem(nextItem, true);

                    autoScrollHandler.postDelayed(this, 5000); // 5 segundos
                }
            }
        };

        autoScrollHandler.postDelayed(autoScrollRunnable, 5000);
    }

    private void detenerAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private void cargarDatosUsuario() {
        // Primero intentar obtener datos de SharedPreferences (más rápido)
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String nombreGuardado = sharedPreferences.getString("nombre", null);

        if (nombreGuardado != null && !nombreGuardado.isEmpty()) {
            tvSaludo.setText("Hola, " + nombreGuardado);
            return;
        }

        String telefonoGuardado = sharedPreferences.getString("telefono", null);
        if (telefonoGuardado != null) {
            buscarUsuarioPorTelefono(telefonoGuardado);
        } else if (currentUser != null) {
            buscarUsuarioPorUserId(currentUser.getUid());
        } else {
            tvSaludo.setText("Hola, Usuario");
        }
    }

    private void buscarUsuarioPorTelefono(String telefono) {
        db.collection("usuarios")
                .whereEqualTo("id_telefono", telefono)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String nombre = document.getString("name");
                        if (nombre == null) nombre = document.getString("nombre");
                        if (nombre != null && !nombre.isEmpty()) {
                            tvSaludo.setText("Hola, " + nombre);
                            guardarNombreEnPrefs(nombre);
                        }
                    }
                });
    }

    private void buscarUsuarioPorUserId(String userId) {
        DocumentReference userRef = db.collection("usuarios").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String nombre = document.getString("name");
                    if (nombre == null) nombre = document.getString("nombre");
                    if (nombre != null && !nombre.isEmpty()) {
                        tvSaludo.setText("Hola, " + nombre);
                        guardarNombreEnPrefs(nombre);
                    }
                }
            }
        });
    }

    private void guardarNombreEnPrefs(String nombre) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("nombre", nombre);
        editor.apply();
    }

    private void cargarLotesReales() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String idUsuario = sharedPreferences.getString("telefono", "");

        if (idUsuario.isEmpty()) {
            Toast.makeText(this, "No se pudo identificar el usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("lotes")
                .whereEqualTo("id_usuario", idUsuario)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaRegistrosRecientes.clear();
                        int contador = 1;

                        for (DocumentSnapshot document : task.getResult()) {
                            String nombreLote = document.getString("Nombre_Lote");
                            Double tamano = document.getDouble("Tamano");
                            String fechaCreacion = document.getString("Fecha_Creacion");
                            String estado = document.getString("Estado");
                            String firestoreId = document.getId();

                            // Convertir fecha
                            Date fecha = new Date();
                            try {
                                if (fechaCreacion != null) {
                                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    fecha = format.parse(fechaCreacion);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Crear objeto Lote
                            Lote lote = new Lote(
                                    contador,
                                    nombreLote != null ? nombreLote : "Sin nombre",
                                    tamano != null ? tamano.intValue() : 0,
                                    fecha,
                                    estado != null ? estado : "Activo",
                                    "Lote registrado"
                            );
                            lote.setFirestoreId(firestoreId);
                            listaRegistrosRecientes.add(lote);
                            contador++;
                        }

                        // Configurar adapter con datos reales
                        configurarRecyclerView();

                        if (listaRegistrosRecientes.isEmpty()) {
                            Toast.makeText(HomeActivity.this, "No hay lotes registrados", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        Toast.makeText(HomeActivity.this, "Error al cargar lotes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarRecyclerView() {
        RegistroAdapter adapter = new RegistroAdapter(listaRegistrosRecientes, new RegistroAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Lote lote) {
                Intent intent = new Intent(HomeActivity.this, DetalleLoteActivity.class);
                intent.putExtra("LOTE_ID", lote.getFirestoreId());
                startActivity(intent);
            }
        });

        recyclerRegistros.setAdapter(adapter);
    }

    private void configurarEventos() {
        fabAnadirRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, CrearLoteActivity.class));
        });

        findViewById(R.id.boton_lotes).setOnClickListener(v -> {
            startActivity(new Intent(this, LotesActivity.class));
        });

        findViewById(R.id.boton_informes).setOnClickListener(v -> {
            startActivity(new Intent(this, InformesActivity.class));
        });

        findViewById(R.id.boton_perfil).setOnClickListener(v -> {
            startActivity(new Intent(this, PerfilActivity.class));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        detenerAutoScroll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLotesReales();
        iniciarAutoScroll();
    }
}