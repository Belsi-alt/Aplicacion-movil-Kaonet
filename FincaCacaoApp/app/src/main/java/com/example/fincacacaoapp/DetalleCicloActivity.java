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

import com.google.common.util.concurrent.AtomicDouble;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DetalleCicloActivity extends AppCompatActivity {

    private TextView tvNombreCiclo, tvInfoCiclo, tvFechaInicio, tvFechaFin, tvDescripcionCiclo, tvEtapasVacios;
    private Button btnEditarCiclo, btnEliminarCiclo;
    private FloatingActionButton fabAgregarEtapa;
    private LinearLayout containerEtapas;

    private FirebaseFirestore db;
    private String cicloId;
    private String loteId;
    private String fechaInicioCiclo;
    private String fechaFinCiclo;

    public interface OnGastosCalculadosListener {
        void onGastosCalculados(double totalGastos);
    }

    public interface OnVentasCalculadasListener {
        void onVentasCalculadas(double totalVentas);
    }

    // Tag para logs
    private static final String TAG = "DetalleCicloActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_ciclo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener ID del ciclo y lote
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        Log.d(TAG, "🔍 onCreate - CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        if (cicloId == null) {
            Toast.makeText(this, "Error: No se recibió el ciclo", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "❌ Error: CICLO_ID es null");
            finish();
            return;
        }

        inicializarVistas();
        cargarDatosCiclo();
        configurarEventos();

        verificarEstructuraDatos();

    }

    class AtomicDouble {
        private double value;

        public AtomicDouble(double initialValue) {
            this.value = initialValue;
        }

        public synchronized double get() {
            return value;
        }

        public synchronized void set(double value) {
            this.value = value;
        }

        public synchronized double addAndGet(double delta) {
            this.value += delta;
            return this.value;
        }
    }

    private void inicializarVistas() {
        tvNombreCiclo = findViewById(R.id.tvNombreCiclo);
        tvInfoCiclo = findViewById(R.id.tvInfoCiclo);
        tvFechaInicio = findViewById(R.id.tvFechaInicio);
        tvFechaFin = findViewById(R.id.tvFechaFin);
        tvDescripcionCiclo = findViewById(R.id.tvDescripcionCiclo);
        tvEtapasVacios = findViewById(R.id.tvEtapasVacios);
        btnEditarCiclo = findViewById(R.id.btnEditarCiclo);
        btnEliminarCiclo = findViewById(R.id.btnEliminarCiclo);
        fabAgregarEtapa = findViewById(R.id.fabAgregarEtapa);
        containerEtapas = findViewById(R.id.containerEtapas);
    }

    private void cargarDatosCiclo() {
        Log.d(TAG, "🔄 cargarDatosCiclo - Consultando ciclo: " + cicloId);

        db.collection("ciclos").document(cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "✅ Ciclo encontrado en Firebase");

                            String nombreCiclo = document.getString("Nombre_Ciclo");
                            Long numeroCiclo = document.getLong("Numero_Ciclo");
                            String estado = document.getString("Estado");
                            fechaInicioCiclo = document.getString("Fecha_Inicio");
                            fechaFinCiclo = document.getString("Fecha_Fin");
                            String descripcion = document.getString("Descripcion");
                            Double gananciaTotal = document.getDouble("Ganancia_Total");

                            // Si no tenemos LOTE_ID, intentar obtenerlo del ciclo
                            if (loteId == null) {
                                loteId = document.getString("ID_LOTE");
                                Log.d(TAG, "🔍 LOTE_ID obtenido del ciclo: " + loteId);
                            }

                            // SOLO MOSTRAR DATOS BÁSICOS PRIMERO
                            if (tvNombreCiclo != null)
                                tvNombreCiclo.setText(nombreCiclo != null ? nombreCiclo : "Sin nombre");

                            if (tvFechaInicio != null)
                                tvFechaInicio.setText("Inicio: " + (fechaInicioCiclo != null ? fechaInicioCiclo : "No definida"));

                            if (tvFechaFin != null)
                                tvFechaFin.setText("Fin: " + (fechaFinCiclo != null ? fechaFinCiclo : "No definida"));

                            if (tvDescripcionCiclo != null)
                                tvDescripcionCiclo.setText(descripcion != null ? descripcion : "Sin descripción");

                            // Cargar etapas reales desde Firebase
                            cargarEtapasReales();

                            // Calcular ganancia al abrir - ESTO ACTUALIZARÁ tvInfoCiclo
                            calcularGananciaTotalCiclo();

                            // MOSTRAR FORMATO TEMPORAL MIENTRAS SE CALCULA
                            if (tvInfoCiclo != null) {
                                // Dejar vacío o mostrar "Calculando..."
                                tvInfoCiclo.setText("Calculando ganancia...");
                                tvInfoCiclo.setTextColor(Color.parseColor("#FF9800")); // Naranja
                                tvInfoCiclo.setTypeface(tvInfoCiclo.getTypeface(), Typeface.BOLD);
                                tvInfoCiclo.setTextSize(16);
                            }

                        } else {
                            Log.e(TAG, "❌ El documento del ciclo no existe");
                            Toast.makeText(this, "Error: El ciclo no existe", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "❌ Error al cargar ciclo: " + task.getException());
                        Toast.makeText(this, "Error al cargar datos del ciclo", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
    private void verificarEstructuraDatos() {
        Log.d(TAG, "🔍 Verificando estructura de datos...");

        // Verificar etapas
        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "📊 ETAPAS del ciclo " + cicloId + ":");
                        for (DocumentSnapshot etapa : task.getResult()) {
                            Log.d(TAG, "   ID: " + etapa.getId());
                            Log.d(TAG, "   Nombre_Etapa: " + etapa.getString("Nombre_Etapa"));
                            Log.d(TAG, "   ID_CICLO: " + etapa.getString("ID_CICLO"));
                            Log.d(TAG, "   --------------------");
                        }
                    }
                });

        // Verificar gastos
        db.collection("gastos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d(TAG, "💰 MUESTRA de GASTOS:");
                        for (int i = 0; i < Math.min(3, task.getResult().size()); i++) {
                            DocumentSnapshot gasto = task.getResult().getDocuments().get(i);
                            Log.d(TAG, "   ID_ETAPA: " + gasto.getString("ID_ETAPA"));
                            Log.d(TAG, "   Monto: " + gasto.getDouble("Monto"));
                            Log.d(TAG, "   --------------------");
                        }
                    }
                });

        // Verificar ventas
        db.collection("ventas")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Log.d(TAG, "💰 MUESTRA de VENTAS:");
                        for (int i = 0; i < Math.min(3, task.getResult().size()); i++) {
                            DocumentSnapshot venta = task.getResult().getDocuments().get(i);
                            Log.d(TAG, "   ID_ETAPA: " + venta.getString("ID_ETAPA"));
                            // Verificar todos los campos posibles
                            Log.d(TAG, "   Campos disponibles: " + venta.getData().keySet());
                            Log.d(TAG, "   --------------------");
                        }
                    }
                });
    }

    private void cargarEtapasReales() {
        Log.d(TAG, "🔄 cargarEtapasReales - Consultando etapas para ciclo: " + cicloId);

        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> etapas = task.getResult().getDocuments();
                        containerEtapas.removeAllViews();

                        Log.d(TAG, "✅ Consulta exitosa. Encontradas " + etapas.size() + " etapas");

                        if (etapas.isEmpty()) {
                            Log.d(TAG, "ℹ️ No hay etapas para este ciclo");
                            tvEtapasVacios.setVisibility(View.VISIBLE);
                            containerEtapas.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "🎉 Mostrando " + etapas.size() + " etapas");
                            tvEtapasVacios.setVisibility(View.GONE);
                            containerEtapas.setVisibility(View.VISIBLE);

                            for (DocumentSnapshot etapaDoc : etapas) {
                                String etapaId = etapaDoc.getId();
                                String nombreEtapa = etapaDoc.getString("Nombre_Etapa");
                                String estado = etapaDoc.getString("Estado");
                                String fechaInicio = etapaDoc.getString("Fecha_Inicio");
                                String fechaFin = etapaDoc.getString("Fecha_Fin");
                                String descripcion = etapaDoc.getString("Descripcion");

                                Log.d(TAG, "📄 Etapa: " + nombreEtapa + " - " + estado + " - " + fechaInicio);

                                crearCardEtapa(etapaId, nombreEtapa, estado, fechaInicio, fechaFin, descripcion);
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ Error en consulta de etapas: " + task.getException());
                        Toast.makeText(this, "Error al cargar etapas: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        tvEtapasVacios.setVisibility(View.VISIBLE);
                        containerEtapas.setVisibility(View.GONE);
                    }
                });
    }

    private void calcularGananciaTotalCiclo() {
        Log.d(TAG, "🔄 Calculando ganancia total...");

        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> etapas = task.getResult().getDocuments();

                        if (etapas.isEmpty()) {
                            Log.d(TAG, "ℹ️ No hay etapas");
                            actualizarGananciaEnFirestore(0.0, 0.0, 0.0);
                            return;
                        }

                        AtomicDouble totalGastos = new AtomicDouble(0.0);
                        AtomicDouble totalVentas = new AtomicDouble(0.0);
                        AtomicInteger etapasProcesadas = new AtomicInteger(0);

                        for (DocumentSnapshot etapa : etapas) {
                            String etapaId = etapa.getId();
                            String nombreEtapa = etapa.getString("Nombre_Etapa");

                            final String etapaIdFinal = etapaId;
                            final String nombreEtapaFinal = nombreEtapa;

                            // 1. Calcular gastos
                            db.collection("gastos")
                                    .whereEqualTo("ID_ETAPA", etapaIdFinal)
                                    .get()
                                    .addOnCompleteListener(gastosTask -> {
                                        double gastosEtapa = 0.0;
                                        if (gastosTask.isSuccessful()) {
                                            for (DocumentSnapshot gasto : gastosTask.getResult()) {
                                                Double monto = gasto.getDouble("Monto");
                                                if (monto != null) {
                                                    gastosEtapa += monto;
                                                }
                                            }
                                        }

                                        totalGastos.addAndGet(gastosEtapa);

                                        // 2. Si es etapa de VENTA, calcular ventas
                                        if (nombreEtapaFinal != null && nombreEtapaFinal.equalsIgnoreCase("venta")) {
                                            db.collection("ventas")
                                                    .whereEqualTo("ID_Etapa", etapaIdFinal)
                                                    .get()
                                                    .addOnCompleteListener(ventasTask -> {
                                                        double ventasEtapa = 0.0;
                                                        if (ventasTask.isSuccessful()) {
                                                            for (DocumentSnapshot venta : ventasTask.getResult()) {
                                                                Double monto = venta.getDouble("Total");
                                                                if (monto != null) {
                                                                    ventasEtapa += monto;
                                                                }
                                                            }
                                                        }

                                                        totalVentas.addAndGet(ventasEtapa);

                                                        int procesadas = etapasProcesadas.incrementAndGet();
                                                        if (procesadas == etapas.size()) {
                                                            calcularYActualizarGanancia(totalGastos.get(), totalVentas.get());
                                                        }
                                                    });
                                        } else {
                                            int procesadas = etapasProcesadas.incrementAndGet();
                                            if (procesadas == etapas.size()) {
                                                calcularYActualizarGanancia(totalGastos.get(), totalVentas.get());
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    private void calcularYActualizarGanancia(double totalGastos, double totalVentas) {
        // Fórmula: Ganancia = Ventas - Gastos
        double gananciaTotal = totalVentas - totalGastos;

        Log.d(TAG, "💰 RESUMEN FINAL DEL CICLO:");
        Log.d(TAG, "   Total Gastos: $" + String.format("%,.2f", totalGastos));
        Log.d(TAG, "   Total Ventas: $" + String.format("%,.2f", totalVentas));
        Log.d(TAG, "   Ganancia Total: $" + String.format("%,.2f", gananciaTotal));

        // Obtener el estado actual antes de actualizar UI
        db.collection("ciclos").document(cicloId)
                .get()
                .addOnSuccessListener(document -> {
                    String estado = "Activo";
                    if (document.exists() && document.getString("Estado") != null) {
                        estado = document.getString("Estado");
                    }

                    // Actualizar UI inmediatamente
                    actualizarUIConGanancia(estado, gananciaTotal);

                    // Luego actualizar Firestore
                    actualizarGananciaEnFirestore(totalGastos, totalVentas, gananciaTotal);
                })
                .addOnFailureListener(e -> {
                    // Si falla obtener estado, usar "Activo" por defecto
                    actualizarUIConGanancia("Activo", gananciaTotal);
                    actualizarGananciaEnFirestore(totalGastos, totalVentas, gananciaTotal);
                });
    }


    private void actualizarGananciaEnFirestore(double gastosTotales, double ventasTotales, double gananciaTotal) {
        // Formatear los números
        String gananciaFormateado = String.format("%,.2f", gananciaTotal);

        Map<String, Object> updates = new HashMap<>();
        updates.put("Ganancia_Total", gananciaTotal);

        Log.d(TAG, "📤 Actualizando Firestore con Ganancia_Total: $" + gananciaFormateado);

        db.collection("ciclos").document(cicloId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ ÉXITO: Ganancia actualizada en Firestore");

                    // OBTENER EL ESTADO ACTUAL PARA MOSTRARLO CORRECTAMENTE
                    db.collection("ciclos").document(cicloId)
                            .get()
                            .addOnSuccessListener(document -> {
                                if (document.exists()) {
                                    String estado = document.getString("Estado");
                                    if (estado == null) estado = "Activo";

                                    // Actualizar la UI con el formato final
                                    actualizarUIConGanancia(estado, gananciaTotal);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ERROR al actualizar ganancia: " + e.getMessage());

                    runOnUiThread(() -> {
                        // Mostrar error pero con formato
                        if (tvInfoCiclo != null) {
                            tvInfoCiclo.setText("Error • Reintentar");
                            tvInfoCiclo.setTextColor(Color.parseColor("#D32F2F"));
                            tvInfoCiclo.setTypeface(tvInfoCiclo.getTypeface(), Typeface.BOLD);
                            tvInfoCiclo.setTextSize(16);
                        }
                    });
                });
    }
    private void actualizarUIConGanancia(String estado, double gananciaTotal) {
        runOnUiThread(() -> {
            String info;
            int colorTexto;
            String gananciaFormateado = String.format("%,.2f", gananciaTotal);

            // Configurar negrita y tamaño
            tvInfoCiclo.setTypeface(tvInfoCiclo.getTypeface(), Typeface.BOLD);
            tvInfoCiclo.setTextSize(16);

            // Determinar texto y color según ganancia
            if (gananciaTotal > 0) {
                // GANANCIA POSITIVA - Verde oscuro
                info = estado + " • 💰 +$" + gananciaFormateado;
                colorTexto = Color.parseColor("#388E3C"); // Verde oscuro
            } else if (gananciaTotal < 0) {
                // PÉRDIDA - Rojo oscuro
                double perdidaAbsoluta = Math.abs(gananciaTotal);
                String perdidaFormateada = String.format("%,.2f", perdidaAbsoluta);
                info = estado + " • 📉 -$" + perdidaFormateada;
                colorTexto = Color.parseColor("#D32F2F"); // Rojo oscuro
            } else {
                // CERO - Gris oscuro
                info = estado + " • $0.00";
                colorTexto = Color.parseColor("#616161"); // Gris oscuro
            }

            if (tvInfoCiclo != null) {
                tvInfoCiclo.setText(info);
                tvInfoCiclo.setTextColor(colorTexto);
            }
        });
    }

    private String obtenerEstadoCiclo() {
        // Intenta obtener el estado actual del ciclo desde tvInfoCiclo
        if (tvInfoCiclo != null && tvInfoCiclo.getText() != null) {
            String texto = tvInfoCiclo.getText().toString();
            if (texto.contains("•")) {
                return texto.split("•")[0].trim();
            }
        }
        return "Activo"; // Valor por defecto
    }

    private void crearCardEtapa(String etapaId, String nombreEtapa, String estado, String fechaInicio, String fechaFin, String descripcion) {
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

            // Contenido de la card
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.HORIZONTAL);
            cardContent.setPadding(8, 8, 8, 8);

            // Indicador de color basado en el estado
            View colorIndicator = new View(this);
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT);
            colorParams.setMargins(0, 0, 16, 0);
            colorIndicator.setLayoutParams(colorParams);
            colorIndicator.setBackgroundColor(obtenerColorPorEstado(estado));

            // Textos
            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));

            TextView tvNombre = new TextView(this);
            tvNombre.setText(nombreEtapa != null ? nombreEtapa : "Sin nombre");
            tvNombre.setTextSize(16);
            tvNombre.setTextColor(Color.BLACK);
            tvNombre.setTypeface(null, Typeface.BOLD);

            TextView tvEstado = new TextView(this);
            tvEstado.setText("Estado: " + (estado != null ? estado : "Pendiente"));
            tvEstado.setTextSize(12);
            tvEstado.setTextColor(Color.GRAY);

            TextView tvFechas = new TextView(this);
            String textoFechas = "Inicio: " + (fechaInicio != null ? fechaInicio : "No definida");
            if (fechaFin != null && !fechaFin.isEmpty()) {
                textoFechas += " - Fin: " + fechaFin;
            }
            tvFechas.setText(textoFechas);
            tvFechas.setTextSize(10);
            tvFechas.setTextColor(Color.DKGRAY);

            textLayout.addView(tvNombre);
            textLayout.addView(tvEstado);
            textLayout.addView(tvFechas);

            cardContent.addView(colorIndicator);
            cardContent.addView(textLayout);
            card.addView(cardContent);

            containerEtapas.addView(card);

            // Click listener para la etapa
            card.setOnClickListener(v -> {
                abrirDetalleEtapa(etapaId, nombreEtapa);
            });

            Log.d(TAG, "✅ Card creada para etapa: " + nombreEtapa);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al crear card de etapa: " + e.getMessage());
        }

    }

    private int obtenerColorPorEstado(String estado) {
        if (estado == null) return Color.GRAY;

        switch (estado) {
            case "Completada":
                return Color.parseColor("#4CAF50"); // Verde
            case "En Progreso":
                return Color.parseColor("#FF9800"); // Naranja
            case "Pendiente":
                return Color.parseColor("#2196F3"); // Azul
            default:
                return Color.GRAY;
        }
    }

    private void abrirDetalleEtapa(String etapaId, String nombreEtapa) {
        Log.d(TAG, "🔄 abrirDetalleEtapa - ETAPA_ID: " + etapaId + ", CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        Intent intent = new Intent(this, DetalleEtapaActivity.class);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        startActivity(intent);
    }

    private void configurarEventos() {
        btnEditarCiclo.setOnClickListener(v -> {
            abrirEditarCiclo();
        });

        btnEliminarCiclo.setOnClickListener(v -> {
            eliminarCiclo();
        });

        fabAgregarEtapa.setOnClickListener(v -> {
            abrirAgregarEtapa();
        });


    }
    private void abrirEditarCiclo() {
        Intent intent = new Intent(this, EditarCicloActivity.class);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        startActivityForResult(intent, 1); // Usar startActivityForResult para recibir actualizaciones
    }

    // Añade onActivityResult para actualizar después de editar:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Recargar datos después de editar
            if (data != null) {
                boolean cicloActualizado = data.getBooleanExtra("CICLO_ACTUALIZADO", false);
                boolean cicloEliminado = data.getBooleanExtra("CICLO_ELIMINADO", false);

                if (cicloActualizado) {
                    Toast.makeText(this, "Ciclo actualizado", Toast.LENGTH_SHORT).show();
                    cargarDatosCiclo(); // Recargar datos
                } else if (cicloEliminado) {
                    Toast.makeText(this, "Ciclo eliminado", Toast.LENGTH_SHORT).show();
                    finish(); // Cerrar esta actividad
                }
            }
        }
    }
    private void abrirAgregarEtapa() {
        Log.d(TAG, "🔄 abrirAgregarEtapa - CICLO_ID: " + cicloId + ", LOTE_ID: " + loteId);

        Intent intent = new Intent(this, AgregarEtapaActivity.class);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        intent.putExtra("FECHA_INICIO_CICLO", fechaInicioCiclo);
        intent.putExtra("FECHA_FIN_CICLO", fechaFinCiclo);
        startActivity(intent);
    }

    private void eliminarCiclo() {
        // Primero verificar si hay etapas asociadas
        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int cantidadEtapas = task.getResult().size();
                        if (cantidadEtapas > 0) {
                            Toast.makeText(this, "No se puede eliminar: Hay " + cantidadEtapas + " etapas asociadas a este ciclo", Toast.LENGTH_LONG).show();
                        } else {
                            confirmarEliminacionCiclo();
                        }
                    } else {
                        Toast.makeText(this, "Error al verificar etapas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmarEliminacionCiclo() {
        db.collection("ciclos").document(cicloId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Ciclo eliminado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error al eliminar ciclo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cicloId != null) {
            Log.d(TAG, "🔄 onResume - Recalculando ganancia");
            calcularGananciaTotalCiclo(); // Recalcular cada vez que vuelvas
        }
    }
}
