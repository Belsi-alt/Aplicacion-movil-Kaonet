package com.example.fincacacaoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.fincacacaoapp.adapters.LoteAdapter;
import com.example.fincacacaoapp.models.Lote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LotesActivity extends AppCompatActivity {

    private RecyclerView recyclerLotes;
    private LoteAdapter loteAdapter;
    private FloatingActionButton fabAgregarLote;
    private EditText etBuscarLote;
    private List<Lote> listaLotes;
    private List<Lote> listaLotesFiltrados;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lotes);

        db = FirebaseFirestore.getInstance();
        inicializarVistas();
        configurarRecyclerView();
        cargarLotesReales();
        configurarEventos();
    }

    private void inicializarVistas() {
        recyclerLotes = findViewById(R.id.recyclerLotes);
        fabAgregarLote = findViewById(R.id.fabAgregarLote);
        etBuscarLote = findViewById(R.id.etBuscarLote);

        listaLotes = new ArrayList<>();
        listaLotesFiltrados = new ArrayList<>();
    }

    private void configurarRecyclerView() {
        recyclerLotes.setLayoutManager(new LinearLayoutManager(this));
        loteAdapter = new LoteAdapter(listaLotesFiltrados, new LoteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Lote lote) {
                Intent intent = new Intent(LotesActivity.this, DetalleLoteActivity.class);
                intent.putExtra("LOTE_ID", lote.getFirestoreId());
                startActivity(intent);
            }
        });
        recyclerLotes.setAdapter(loteAdapter);
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
                        listaLotes.clear();
                        listaLotesFiltrados.clear();

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String nombreLote = document.getString("Nombre_Lote");
                                Double tamano = document.getDouble("Tamano");
                                String fechaCreacionStr = document.getString("Fecha_Creacion");
                                String estado = document.getString("Estado");
                                String loteId = document.getId();
                                // ELIMINAR descripción - usar string vacío o null
                                String descripcion = ""; // O simplemente null

                                // Convertir fecha de String a Date
                                Date fechaSiembra = new Date();
                                if (fechaCreacionStr != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    fechaSiembra = sdf.parse(fechaCreacionStr);
                                }

                                // CORRECCIÓN: Constructor sin descripción o con string vacío
                                Lote lote = new Lote(
                                        0, // loteId temporal
                                        nombreLote != null ? nombreLote : "Sin nombre",
                                        tamano != null ? tamano.intValue() : 0,
                                        fechaSiembra,
                                        estado != null ? estado : "Activo",
                                        "" // DESCRIPCIÓN VACÍA
                                );

                                // Agregar el ID de Firestore
                                lote.setFirestoreId(loteId);

                                listaLotes.add(lote);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Actualizar lista filtrada y adapter
                        listaLotesFiltrados.clear();
                        listaLotesFiltrados.addAll(listaLotes);
                        loteAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(LotesActivity.this, "Error al cargar lotes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarEventos() {
        fabAgregarLote.setOnClickListener(v -> {
            Intent intent = new Intent(LotesActivity.this, CrearLoteActivity.class);
            startActivity(intent);
        });

        // Búsqueda automática
        etBuscarLote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarLotes();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // CONEXIÓN DE LA TOOLBAR
        findViewById(R.id.boton_inicio).setOnClickListener(v -> {
            Intent intent = new Intent(LotesActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_lotes).setOnClickListener(v -> {
            // Ya estamos en Lotes
        });

        findViewById(R.id.boton_informes).setOnClickListener(v -> {
            Intent intent = new Intent(LotesActivity.this, InformesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_perfil).setOnClickListener(v -> {
            Intent intent = new Intent(LotesActivity.this, PerfilActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    private void filtrarLotes() {
        String textoBusqueda = etBuscarLote.getText().toString().toLowerCase().trim();

        listaLotesFiltrados.clear();

        if (textoBusqueda.isEmpty()) {
            listaLotesFiltrados.addAll(listaLotes);
        } else {
            for (Lote lote : listaLotes) {
                if (lote.getNombre().toLowerCase().contains(textoBusqueda)) {
                    listaLotesFiltrados.add(lote);
                }
            }
        }

        loteAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLotesReales();
    }
}