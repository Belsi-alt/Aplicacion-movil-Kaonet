package com.example.fincacacaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import androidx.cardview.widget.CardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // ✅ NUEVO IMPORT

public class DetalleLoteActivity extends AppCompatActivity {

    private TextView tvDetalleNombre, tvDetallePlantas, tvDetalleFecha, tvDetalleDescripcion, tvCiclosVacios;
    private Button btnEditarLote, btnEliminarLote;
    private FloatingActionButton fabAgregarCiclo; // ✅ CAMBIADO de Button a FAB

    // Firebase
    private FirebaseFirestore db;

    // Variables para almacenar datos del lote
    private String loteId;
    private String nombreLote;
    private int cantidadPlantas;
    private String fechaSiembra;
    private String estado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_lote);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener ID del lote
        loteId = getIntent().getStringExtra("LOTE_ID");

        if (loteId == null || loteId.isEmpty()) {
            Toast.makeText(this, "Error: No se recibió el lote", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        cargarDatosLoteDesdeFirestore(); // ✅ Cargar datos reales
        configurarEventos();
    }

    private void cargarDatosLoteDesdeFirestore() {
        db.collection("lotes").document(loteId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        // Obtener datos reales de Firestore
                        nombreLote = document.getString("Nombre_Lote");
                        Long tamanoLong = document.getLong("Tamano");
                        cantidadPlantas = tamanoLong != null ? tamanoLong.intValue() : 0;
                        fechaSiembra = document.getString("Fecha_Creacion");
                        estado = document.getString("Estado");

                        //  Mostrar datos en pantalla
                        mostrarDatosEnPantalla();

                        //  Cargar ciclos del lote
                        cargarCiclosDelLote();

                    } else {
                        Toast.makeText(this, "Error al cargar datos del lote", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void cargarCiclosDelLote() {
        System.out.println("🔍 DEBUG: Buscando ciclos para lote: " + loteId);

        db.collection("ciclos")
                .whereEqualTo("ID_LOTE", loteId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        LinearLayout containerCiclos = findViewById(R.id.containerCiclos);
                        TextView tvCiclosVacios = findViewById(R.id.tvCiclosVacios);

                        containerCiclos.removeAllViews();

                        int totalCiclos = querySnapshot != null ? querySnapshot.size() : 0;
                        System.out.println("✅ DEBUG: Encontrados " + totalCiclos + " ciclos en Firestore");

                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            // ✅ MOSTRAR TODOS los ciclos sin filtrar primero
                            for (DocumentSnapshot cicloDoc : querySnapshot) {
                                String cicloId = cicloDoc.getId();
                                String nombreCiclo = cicloDoc.getString("Nombre_Ciclo");
                                String idLote = cicloDoc.getString("ID_LOTE");
                                Long numeroCiclo = cicloDoc.getLong("Numero_Ciclo");

                                System.out.println("📄 DEBUG Ciclo: ID=" + cicloId +
                                        ", Nombre=" + nombreCiclo +
                                        ", ID_LOTE=" + idLote +
                                        ", Numero=" + numeroCiclo);

                                // ✅ CREAR CARD para TODOS los ciclos (sin filtros)
                                crearCardCiclo(cicloDoc, containerCiclos);
                            }

                            if (containerCiclos.getChildCount() > 0) {
                                tvCiclosVacios.setVisibility(View.GONE);
                                containerCiclos.setVisibility(View.VISIBLE);
                                System.out.println("🎉 DEBUG: " + containerCiclos.getChildCount() + " ciclos mostrados en UI");
                            } else {
                                tvCiclosVacios.setVisibility(View.VISIBLE);
                                containerCiclos.setVisibility(View.GONE);
                                System.out.println("❌ DEBUG: No se pudieron crear cards para los ciclos");
                            }
                        } else {
                            tvCiclosVacios.setVisibility(View.VISIBLE);
                            containerCiclos.setVisibility(View.GONE);
                            System.out.println("❌ DEBUG: No se encontraron ciclos en la consulta");
                        }
                    } else {
                        System.out.println("💥 DEBUG: Error en consulta: " + task.getException());
                        Toast.makeText(this, "Error al cargar ciclos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void crearCardCiclo(DocumentSnapshot cicloDoc, LinearLayout container) {
        // Obtener datos CORRECTAMENTE
        Long numeroCicloLong = cicloDoc.getLong("Numero_Ciclo");
        String numeroCiclo = numeroCicloLong != null ? "Ciclo " + numeroCicloLong : "Ciclo";
        String nombreCiclo = cicloDoc.getString("Nombre_Ciclo");
        String estado = cicloDoc.getString("Estado");
        String fechaInicio = cicloDoc.getString("Fecha_Inicio");
        String fechaFin = cicloDoc.getString("Fecha_Fin");
        String cicloId = cicloDoc.getId();

        // Crear CardView
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.WHITE);
        card.setCardElevation(4f);
        card.setRadius(12f);
        card.setContentPadding(16, 16, 16, 16);

        // Crear contenido
        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);

        // Nombre del ciclo
        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombreCiclo != null ? nombreCiclo : "Sin nombre");
        tvNombre.setTextSize(16);
        tvNombre.setTextColor(Color.BLACK);
        tvNombre.setTypeface(null, Typeface.BOLD);

        // Número y estado
        TextView tvInfo = new TextView(this);
        tvInfo.setText((estado != null ? estado : "Activo"));
        tvInfo.setTextSize(14);
        tvInfo.setTextColor(Color.DKGRAY);

        // Fechas
        TextView tvFechas = new TextView(this);
        String textoFechas = "Inicio: " + (fechaInicio != null ? fechaInicio : "No definida");
        if (fechaFin != null) {
            textoFechas += "\n"+"Fin: " + fechaFin;
        }
        tvFechas.setText(textoFechas);
        tvFechas.setTextSize(12);
        tvFechas.setTextColor(Color.GRAY);

        // Agregar a la card
        cardContent.addView(tvNombre);
        cardContent.addView(tvInfo);
        cardContent.addView(tvFechas);
        card.addView(cardContent);

        // Agregar al contenedor
        container.addView(card);

        // ✅ CORREGIDO: Pasar ambos IDs al hacer click
        card.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleLoteActivity.this, DetalleCicloActivity.class);
            intent.putExtra("CICLO_ID", cicloId);
            intent.putExtra("LOTE_ID", loteId); // ✅ AÑADIDO: Pasar también el LOTE_ID
            startActivity(intent);
        });

    }

    private void inicializarVistas() {
        tvDetalleNombre = findViewById(R.id.tvDetalleNombre);
        tvDetallePlantas = findViewById(R.id.tvDetallePlantas);
        tvDetalleFecha = findViewById(R.id.tvDetalleFecha);
        tvDetalleDescripcion = findViewById(R.id.tvDetalleDescripcion);
        tvCiclosVacios = findViewById(R.id.tvCiclosVacios);
        btnEditarLote = findViewById(R.id.btnEditarLote);
        btnEliminarLote = findViewById(R.id.btnEliminarLote);
        fabAgregarCiclo = findViewById(R.id.fabAgregarCiclo); // ✅ CAMBIADO

        // ❌ ELIMINADO: btnBack y btnAgregarCiclo antiguo
    }

    private void mostrarDatosEnPantalla() {
        if (tvDetalleNombre != null) tvDetalleNombre.setText(nombreLote != null ? nombreLote : "Sin nombre");
        if (tvDetallePlantas != null) tvDetallePlantas.setText(cantidadPlantas + " plantas");
        if (tvDetalleFecha != null) tvDetalleFecha.setText("Fecha siembra: " + (fechaSiembra != null ? fechaSiembra : "No especificada"));
        if (tvDetalleDescripcion != null) tvDetalleDescripcion.setText("Estado: " + (estado != null ? estado : "Activo"));
    }

    private void configurarEventos() {
        // ❌ ELIMINADO: btnBack.setOnClickListener (ya no existe el botón)

        btnEditarLote.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleLoteActivity.this, EditarLoteActivity.class);
            intent.putExtra("LOTE_ID", loteId);
            startActivity(intent);
        });

        btnEliminarLote.setOnClickListener(v -> eliminarLote());

        // ✅ CAMBIADO: Ahora es FAB
        fabAgregarCiclo.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleLoteActivity.this, AgregarCicloActivity.class);
            intent.putExtra("LOTE_ID", loteId);
            startActivity(intent);
        });
    }

    private void eliminarLote() {
        // ✅ Eliminar de Firestore
        db.collection("lotes").document(loteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lote eliminado correctamente", Toast.LENGTH_SHORT).show();
                    finish(); // Regresar al home
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar lote", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Recargar datos cuando regresemos de editar/agregar ciclo
        if (loteId != null) {
            cargarDatosLoteDesdeFirestore();
        }
    }
}