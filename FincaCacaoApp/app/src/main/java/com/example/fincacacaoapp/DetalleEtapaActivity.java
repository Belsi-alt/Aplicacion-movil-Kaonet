package com.example.fincacacaoapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DetalleEtapaActivity extends AppCompatActivity {

    private TextView tvNombreEtapa, tvInfoCicloLote, tvDescripcionEtapa, tvFechaInicio, tvFechaFin, tvEstado;
    private TextView tvSinGastos, tvSinVentas;
    private Button btnAgregarGasto, btnAgregarVenta;
    private LinearLayout containerGastos, containerVentas, seccionVentas;

    // ✅ BOTONES DE EDITAR Y ELIMINAR
    private Button btnEditarEtapa, btnEliminarEtapa;

    private FirebaseFirestore db;
    private String etapaId;
    private String cicloId;
    private String loteId;

    private static final String TAG = "DetalleEtapaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_etapa);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener IDs de la etapa, ciclo y lote
        etapaId = getIntent().getStringExtra("ETAPA_ID");
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        Log.d(TAG, "🔍 onCreate - ETAPA_ID: " + etapaId + ", CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        if (etapaId == null) {
            Toast.makeText(this, "Error: No se recibió la etapa", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "❌ Error: ETAPA_ID es null");
            finish();
            return;
        }

        inicializarVistas();
        cargarDatosEtapa();
        configurarEventos();
    }

    private void inicializarVistas() {
        tvNombreEtapa = findViewById(R.id.tvNombreEtapa);
        tvInfoCicloLote = findViewById(R.id.tvInfoCicloLote);
        tvDescripcionEtapa = findViewById(R.id.tvDescripcionEtapa);
        tvFechaInicio = findViewById(R.id.tvFechaInicio);
        tvFechaFin = findViewById(R.id.tvFechaFin);
        tvEstado = findViewById(R.id.tvEstado);

        btnAgregarGasto = findViewById(R.id.btnAgregarGasto);
        btnAgregarVenta = findViewById(R.id.btnAgregarVenta);

        // ✅ INICIALIZAR BOTONES DE EDITAR Y ELIMINAR
        btnEditarEtapa = findViewById(R.id.btnEditarEtapa);
        btnEliminarEtapa = findViewById(R.id.btnEliminarEtapa);

        containerGastos = findViewById(R.id.containerGastos);
        containerVentas = findViewById(R.id.containerVentas);
        seccionVentas = findViewById(R.id.seccionVentas);

        tvSinGastos = findViewById(R.id.tvSinGastos);
        tvSinVentas = findViewById(R.id.tvSinVentas);

        // ✅ VERIFICACIÓN DE BOTONES
        if (btnEditarEtapa == null) {
            Log.e(TAG, "❌ ERROR: btnEditarEtapa es NULL - Revisa el ID en el XML");
        } else {
            Log.d(TAG, "✅ btnEditarEtapa inicializado correctamente");
        }

        if (btnEliminarEtapa == null) {
            Log.e(TAG, "❌ ERROR: btnEliminarEtapa es NULL - Revisa el ID en el XML");
        } else {
            Log.d(TAG, "✅ btnEliminarEtapa inicializado correctamente");
        }
    }

    private void cargarDatosEtapa() {
        Log.d(TAG, "🔄 cargarDatosEtapa - Consultando etapa: " + etapaId);

        db.collection("etapas").document(etapaId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "✅ Etapa encontrada en Firebase");

                            String nombreEtapa = document.getString("Nombre_Etapa");
                            String estado = document.getString("Estado");
                            String fechaInicio = document.getString("Fecha_Inicio");
                            String fechaFin = document.getString("Fecha_Fin");
                            String descripcion = document.getString("Descripcion");

                            // Mostrar datos en la UI
                            tvNombreEtapa.setText(nombreEtapa != null ? nombreEtapa : "Sin nombre");
                            tvDescripcionEtapa.setText(descripcion != null && !descripcion.isEmpty() ? descripcion : "Sin descripción");
                            tvFechaInicio.setText(fechaInicio != null ? fechaInicio : "No definida");
                            tvFechaFin.setText(fechaFin != null && !fechaFin.isEmpty() ? fechaFin : "No definida");

                            String estadoTexto = estado != null ? estado : "Pendiente";
                            tvEstado.setText(estadoTexto);
                            tvEstado.setTextColor(obtenerColorPorEstado(estado));

                            // Cargar información del ciclo y lote
                            cargarInfoCicloLote();

                            // Determinar si mostrar sección de ventas
                            if (esEtapaDeVenta(nombreEtapa)) {
                                seccionVentas.setVisibility(View.VISIBLE);
                                cargarVentas();
                            } else {
                                seccionVentas.setVisibility(View.GONE);
                            }

                            // Cargar gastos
                            cargarGastos();

                        } else {
                            Log.e(TAG, "❌ El documento de la etapa no existe");
                            Toast.makeText(this, "Error: La etapa no existe", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "❌ Error al cargar etapa: " + task.getException());
                        Toast.makeText(this, "Error al cargar datos de la etapa", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void cargarInfoCicloLote() {
        if (cicloId != null) {
            db.collection("ciclos").document(cicloId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot cicloDoc = task.getResult();
                            String nombreCiclo = cicloDoc.getString("Nombre_Ciclo");
                            Long numeroCiclo = cicloDoc.getLong("Numero_Ciclo");

                            String info = (numeroCiclo != null ? "Ciclo " + numeroCiclo : "Ciclo");
                            if (nombreCiclo != null && !nombreCiclo.isEmpty()) {
                                info += " - ";
                            }

                            tvInfoCicloLote.setText(info);

                            // Cargar nombre del lote por separado si existe
                            if (loteId != null) {
                                cargarNombreLote(info);
                            }
                        }
                    });
        }
    }

    private void cargarNombreLote(String infoBase) {
        db.collection("lotes").document(loteId)
                .get()
                .addOnCompleteListener(loteTask -> {
                    if (loteTask.isSuccessful() && loteTask.getResult().exists()) {
                        String nombreLote = loteTask.getResult().getString("Nombre_Lote");
                        if (nombreLote != null) {
                            String infoCompleta = infoBase + nombreLote;
                            tvInfoCicloLote.setText(infoCompleta);
                        }
                    }
                });
    }

    //  MÉTODO PARA EDITAR ETAPA
    private void editarEtapa() {
        Log.d(TAG, "✏️ Editando etapa: " + etapaId);

        if (etapaId == null || cicloId == null) {
            Toast.makeText(this, "Error: No se recibieron los datos necesarios", Toast.LENGTH_LONG).show();
            Log.e(TAG, "❌ etapaId o cicloId es NULL");
            return;
        }

        Intent intent = new Intent(this, EditarEtapaActivity.class);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);

        Log.d(TAG, "🎯 Iniciando EditarEtapaActivity...");
        startActivity(intent);
        Log.d(TAG, "✅ EditarEtapaActivity iniciada");
    }

    //  MÉTODO PARA ELIMINAR ETAPA
    private void eliminarEtapa() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Eliminar Etapa")
                .setMessage("¿Estás seguro de que quieres eliminar esta etapa?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Eliminar de Firebase
                    db.collection("etapas").document(etapaId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Etapa eliminada: " + etapaId);
                                Toast.makeText(this, "✅ Etapa eliminada correctamente", Toast.LENGTH_SHORT).show();

                                // Regresar al ciclo
                                Intent intent = new Intent(this, DetalleCicloActivity.class);
                                intent.putExtra("CICLO_ID", cicloId);
                                intent.putExtra("LOTE_ID", loteId);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error al eliminar etapa: " + e.getMessage());
                                Toast.makeText(this, "❌ Error al eliminar etapa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }


    private boolean esEtapaDeVenta(String nombreEtapa) {
        if (nombreEtapa == null) return false;
        String nombreLower = nombreEtapa.toLowerCase();
        return nombreLower.contains("venta");
    }

    private void cargarGastos() {
        Log.d(TAG, "🔄 cargarGastos - Consultando gastos para etapa: " + etapaId);

        db.collection("gastos")
                .whereEqualTo("ID_ETAPA", etapaId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> gastos = task.getResult().getDocuments();
                        containerGastos.removeAllViews();

                        Log.d(TAG, "✅ Consulta gastos exitosa. Encontrados: " + gastos.size());

                        if (gastos.isEmpty()) {
                            tvSinGastos.setVisibility(View.VISIBLE);
                            containerGastos.setVisibility(View.GONE);
                        } else {
                            tvSinGastos.setVisibility(View.GONE);
                            containerGastos.setVisibility(View.VISIBLE);

                            for (DocumentSnapshot gastoDoc : gastos) {
                                String nombreGasto = gastoDoc.getString("Nombre_Gasto");
                                String categoria = gastoDoc.getString("Categoria");
                                String descripcion = gastoDoc.getString("Descripcion");
                                Double monto = gastoDoc.getDouble("Monto");
                                String unidad = gastoDoc.getString("Unidad");
                                String fechaGasto = gastoDoc.getString("Fecha");
                                crearCardGasto(gastoDoc, nombreGasto, categoria, descripcion, monto, unidad, fechaGasto);
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ Error en consulta de gastos: " + task.getException());
                        tvSinGastos.setVisibility(View.VISIBLE);
                        containerGastos.setVisibility(View.GONE);
                    }
                });
    }

    private void cargarVentas() {
        Log.d(TAG, "🔄 cargarVentas - Consultando ventas para etapa: " + etapaId);

        db.collection("ventas")
                .whereEqualTo("ID_Etapa", etapaId) // ✅ CAMBIADO: Ahora busca por ID_Etapa
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> ventas = task.getResult().getDocuments();
                        containerVentas.removeAllViews();

                        Log.d(TAG, "✅ Consulta ventas exitosa. Encontradas: " + ventas.size());

                        if (ventas.isEmpty()) {
                            tvSinVentas.setVisibility(View.VISIBLE);
                            containerVentas.setVisibility(View.GONE);
                            Log.d(TAG, "📭 No hay ventas registradas para esta etapa");
                        } else {
                            tvSinVentas.setVisibility(View.GONE);
                            containerVentas.setVisibility(View.VISIBLE);

                            for (DocumentSnapshot ventaDoc : ventas) {
                                // ✅ OBTENER NUEVOS CAMPOS
                                String idVenta = ventaDoc.getString("ID_Venta");
                                String cliente = ventaDoc.getString("Cliente");
                                Double cantidad = ventaDoc.getDouble("Cantidad");
                                Double precioUnitario = ventaDoc.getDouble("Precio_Unitario");
                                Double total = ventaDoc.getDouble("Total");
                                String fecha = ventaDoc.getString("Fecha");
                                String metodoPago = ventaDoc.getString("Metodo_Pago");

                                Log.d(TAG, "📄 Venta cargada:");
                                Log.d(TAG, "   - ID: " + idVenta);
                                Log.d(TAG, "   - Cliente: " + cliente);
                                Log.d(TAG, "   - Cantidad: " + cantidad);
                                Log.d(TAG, "   - Precio Unitario: " + precioUnitario);
                                Log.d(TAG, "   - Total: " + total);
                                Log.d(TAG, "   - Fecha: " + fecha);
                                Log.d(TAG, "   - Método Pago: " + metodoPago);

                                // ✅ LLAMAR MÉTODO ACTUALIZADO
                                crearCardVenta(idVenta, cliente, cantidad, precioUnitario, total, fecha, metodoPago);
                            }

                            Log.d(TAG, "🎉 " + ventas.size() + " ventas mostradas en la interfaz");
                        }
                    } else {
                        Log.e(TAG, "❌ Error en consulta de ventas: " + task.getException());
                        tvSinVentas.setVisibility(View.VISIBLE);
                        containerVentas.setVisibility(View.GONE);
                    }
                });
    }

    private void crearCardGasto(DocumentSnapshot gastoDoc, String nombreGasto, String categoria, String descripcion, Double monto, String unidad, String fecha) {
        try {
            // ✅ OBTENER EL ID DEL DOCUMENTO DE FIRESTORE
            String gastoDocumentId = gastoDoc.getId();

            CardView card = new CardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 8);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(Color.WHITE);
            card.setCardElevation(2f);
            card.setRadius(8f);
            card.setContentPadding(16, 16, 16, 16);

            // Layout horizontal para el contenido
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.HORIZONTAL);
            cardContent.setPadding(8, 8, 8, 8);

            // Información del gasto (izquierda)
            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView tvNombre = new TextView(this);
            tvNombre.setText(nombreGasto != null ? nombreGasto : "Sin nombre");
            tvNombre.setTextSize(16);
            tvNombre.setTextColor(Color.BLACK);
            tvNombre.setTypeface(null, Typeface.BOLD);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(descripcion != null ? descripcion : "Sin descripción");
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(Color.GRAY);

            TextView tvFecha = new TextView(this);
            tvFecha.setText(fecha != null ? "Fecha: " + fecha : "Sin fecha");
            tvFecha.setTextSize(12);
            tvFecha.setTextColor(Color.GRAY);

            infoLayout.addView(tvNombre);
            infoLayout.addView(tvDesc);
            infoLayout.addView(tvFecha);

            // Monto y unidad (derecha)
            LinearLayout montoLayout = new LinearLayout(this);
            montoLayout.setOrientation(LinearLayout.VERTICAL);
            montoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            montoLayout.setGravity(android.view.Gravity.END);

            TextView tvMonto = new TextView(this);
            tvMonto.setText(monto != null ? String.format("$%,.0f", monto) : "$0");
            tvMonto.setTextSize(16);
            tvMonto.setTextColor(Color.BLACK);
            tvMonto.setTypeface(null, Typeface.BOLD);

            TextView tvUnidad = new TextView(this);
            tvUnidad.setText(unidad != null ? "+ " + unidad : "");
            tvUnidad.setTextSize(12);
            tvUnidad.setTextColor(Color.GRAY);

            montoLayout.addView(tvMonto);
            montoLayout.addView(tvUnidad);

            cardContent.addView(infoLayout);
            cardContent.addView(montoLayout);
            card.addView(cardContent);

            // ✅ CORREGIDO: Pasar el ID del documento, no el nombre
            card.setOnClickListener(v -> {
                Log.d(TAG, "🔄 Click en gasto para editar - Document ID: " + gastoDocumentId);
                abrirEditarGasto(gastoDocumentId);
            });

            containerGastos.addView(card);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al crear card de gasto: " + e.getMessage());
        }
    }

    // ✅ NUEVO MÉTODO PARA ABRIR EDICIÓN DE GASTO
    private void abrirEditarGasto(String gastoDocumentId) {
        Log.d(TAG, "✏️ Abriendo edición de gasto: " + gastoDocumentId);

        Intent intent = new Intent(this, EditarGastoActivity.class);
        intent.putExtra("GASTO_ID", gastoDocumentId);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);

        startActivityForResult(intent, 1002); // Código diferente para gastos
    }

    private void crearCardVenta(String idVenta, String cliente, Double cantidad, Double precioUnitario, Double total, String fecha, String metodoPago) {
        try {
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

            // Layout principal vertical
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);

            // Header con Cliente y Total
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Información del cliente (izquierda)
            TextView tvCliente = new TextView(this);
            tvCliente.setText(cliente != null ? cliente : "Cliente no especificado");
            tvCliente.setTextSize(16);
            tvCliente.setTextColor(Color.BLACK);
            tvCliente.setTypeface(null, Typeface.BOLD);
            tvCliente.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            // Total (derecha)
            TextView tvTotal = new TextView(this);
            tvTotal.setText(total != null ? String.format("$%,.0f", total) : "$0");
            tvTotal.setTextSize(16);
            tvTotal.setTextColor(Color.parseColor("#4CAF50")); // Verde
            tvTotal.setTypeface(null, Typeface.BOLD);

            headerLayout.addView(tvCliente);
            headerLayout.addView(tvTotal);

            // Línea separadora
            View separator = new View(this);
            separator.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            ));
            separator.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams separatorParams = (LinearLayout.LayoutParams) separator.getLayoutParams();
            separatorParams.setMargins(0, 8, 0, 8);
            separator.setLayoutParams(separatorParams);

            // Detalles de la venta
            LinearLayout detailsLayout = new LinearLayout(this);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);

            // Fila: Cantidad y Precio Unitario
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView tvCantidad = new TextView(this);
            tvCantidad.setText("Cantidad: " + (cantidad != null ? cantidad.toString() : "0"));
            tvCantidad.setTextSize(12);
            tvCantidad.setTextColor(Color.GRAY);
            tvCantidad.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView tvPrecio = new TextView(this);
            tvPrecio.setText(precioUnitario != null ? String.format("P/U: $%,.0f", precioUnitario) : "P/U: $0");
            tvPrecio.setTextSize(12);
            tvPrecio.setTextColor(Color.GRAY);
            tvPrecio.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            row1.addView(tvCantidad);
            row1.addView(tvPrecio);

            // Fila: Método de Pago y Fecha
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(LinearLayout.HORIZONTAL);
            row2.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView tvMetodoPago = new TextView(this);
            tvMetodoPago.setText("Pago: " + (metodoPago != null ? metodoPago : "No especificado"));
            tvMetodoPago.setTextSize(12);
            tvMetodoPago.setTextColor(Color.GRAY);
            tvMetodoPago.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView tvFecha = new TextView(this);
            tvFecha.setText("Fecha: " + (fecha != null ? fecha : "No especificada"));
            tvFecha.setTextSize(12);
            tvFecha.setTextColor(Color.GRAY);
            tvFecha.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            row2.addView(tvMetodoPago);
            row2.addView(tvFecha);

            detailsLayout.addView(row1);
            detailsLayout.addView(row2);

            // Agregar todos los elementos a la card
            cardContent.addView(headerLayout);
            cardContent.addView(separator);
            cardContent.addView(detailsLayout);
            card.addView(cardContent);

            // ✅ MODIFICAR ESTO: Agregar click listener para ABRIR EDICIÓN
            card.setOnClickListener(v -> {
                        Log.d(TAG, "🔄 Click en venta para editar - ID: " + idVenta);
                        abrirEditarVenta(idVenta);
            });
            containerVentas.addView(card);


        } catch (Exception e) {
            Log.e(TAG, "❌ Error al crear card de venta: " + e.getMessage());
        }
        }

    private int obtenerColorPorEstado(String estado) {
        if (estado == null) return Color.GRAY;

        switch (estado.toLowerCase()) {
            case "completada":
                return Color.parseColor("#4CAF50"); // Verde
            case "en progreso":
                return Color.parseColor("#FF9800"); // Naranja
            case "pendiente":
                return Color.parseColor("#2196F3"); // Azul
            case "activo":
                return Color.parseColor("#4CAF50"); // Verde
            case "terminado":
                return Color.parseColor("#F44336"); // Rojo
            default:
                return Color.GRAY;
        }
    }
        // ✅ NUEVO MÉTODO PARA ABRIR EDICIÓN DE VENTA
        private void abrirEditarVenta(String ventaDocumentId) {
            Log.d(TAG, "✏️ Abriendo edición de venta: " + ventaDocumentId);

            Intent intent = new Intent(this, EditarVentaActivity.class);
            intent.putExtra("VENTA_ID", ventaDocumentId);
            intent.putExtra("VENTA_ORIGINAL_ID", "V-" + ventaDocumentId.substring(0, 6)); // ID visual
            intent.putExtra("ETAPA_ID", etapaId);
            intent.putExtra("CICLO_ID", cicloId);
            intent.putExtra("LOTE_ID", loteId);

            // ✅ Importante: usar startActivityForResult para recibir el resultado
            startActivityForResult(intent, 1001);
        }

    private void abrirAgregarGasto() {
        Log.d(TAG, "🔄 abrirAgregarGasto - ETAPA_ID: " + etapaId + ", CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        Intent intent = new Intent(this, AgregarGastoActivity.class);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        startActivity(intent);
    }

    //  NUEVO MÉTODO PARA ABRIR AGREGAR VENTA
    private void abrirAgregarVenta() {
        Log.d(TAG, "🔄 abrirAgregarVenta - ETAPA_ID: " + etapaId + ", CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        Intent intent = new Intent(this, AgregarVentaActivity.class);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        startActivity(intent);
    }

    private void configurarEventos() {
        btnAgregarGasto.setOnClickListener(v -> {
            abrirAgregarGasto();
        });

        btnAgregarVenta.setOnClickListener(v -> {
            abrirAgregarVenta(); // ✅ CAMBIADO
        });

        // ✅ CONFIGURAR EVENTOS PARA EDITAR Y ELIMINAR
        if (btnEditarEtapa != null) {
            btnEditarEtapa.setOnClickListener(v -> {
                Log.d(TAG, "🖊️ Botón Editar presionado");
                editarEtapa();
            });
        } else {
            Log.e(TAG, "❌ btnEditarEtapa es null - no se puede asignar listener");
        }

        if (btnEliminarEtapa != null) {
            btnEliminarEtapa.setOnClickListener(v -> {
                Log.d(TAG, "🗑️ Botón Eliminar presionado");
                eliminarEtapa();
            });
        } else {
            Log.e(TAG, "❌ btnEliminarEtapa es null - no se puede asignar listener");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) { // Código para editar venta
            if (resultCode == RESULT_OK) {
                boolean ventaActualizada = data.getBooleanExtra("VENTA_ACTUALIZADA", false);
                boolean ventaEliminada = data.getBooleanExtra("VENTA_ELIMINADA", false);

                if (ventaActualizada) {
                    Toast.makeText(this, "✅ Venta actualizada correctamente", Toast.LENGTH_SHORT).show();
                } else if (ventaEliminada) {
                    Toast.makeText(this, "✅ Venta eliminada correctamente", Toast.LENGTH_SHORT).show();
                }

                // Recargar las ventas para reflejar cambios
                cargarVentas();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (etapaId != null) {
            Log.d(TAG, "🔄 onResume - Recargando datos de la etapa");
            cargarDatosEtapa();
        }
    }
}