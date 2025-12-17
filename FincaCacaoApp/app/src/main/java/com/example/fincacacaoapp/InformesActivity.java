package com.example.fincacacaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InformesActivity extends AppCompatActivity {

    // Views
    private TextView tvGastosTotales, tvBalanceGeneral, tvLotesActivos, tvCiclosActivos;
    private TextView tvVentasTotales, tvPerdidasReales; // Nuevos TextViews
    private LinearLayout containerTopCiclos;
    private Button btnDescargarPDF;

    // Firestore
    private FirebaseFirestore db;
    private String usuarioId;

    // Variables para cálculos
    private double gastosTotales = 0;
    private double ventasTotales = 0;
    private double balanceGeneral = 0;
    private double perdidasReales = 0; // Solo si balance es negativo
    private int lotesActivosCount = 0;
    private int ciclosActivosCount = 0;
    private List<Map<String, Object>> topCiclos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informes);

        db = FirebaseFirestore.getInstance();
        usuarioId = obtenerUsuarioId();

        inicializarVistas();
        cargarDatosInformes();
        configurarEventos();
        configurarBottomNavigation();
    }

    private String obtenerUsuarioId() {
        return getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("telefono", "");
    }

    private void inicializarVistas() {
        // IDs existentes en tu XML
        tvGastosTotales = findViewById(R.id.tvInversionTotal); // Muestra "Gastos Totales"
        tvBalanceGeneral = findViewById(R.id.tvGananciasTotal); // Muestra "Balance General"
        tvLotesActivos = findViewById(R.id.tvLotesActivos);
        tvCiclosActivos = findViewById(R.id.tvCiclosActivos);
        containerTopCiclos = findViewById(R.id.containerTopCiclos);
        btnDescargarPDF = findViewById(R.id.btnDescargarPDF);

        // Si agregas estos TextViews en tu XML para mostrar más información
        // tvVentasTotales = findViewById(R.id.tvVentasTotales);
        // tvPerdidasReales = findViewById(R.id.tvPerdidasReales);

        // Opcional: Cambiar etiquetas si están hardcodeadas en XML
        cambiarEtiquetasSiEsNecesario();
    }

    private void cambiarEtiquetasSiEsNecesario() {
        // Buscar el LinearLayout padre de tvGastosTotales
        if (tvGastosTotales.getParent() instanceof LinearLayout) {
            LinearLayout card = (LinearLayout) tvGastosTotales.getParent();
            for (int i = 0; i < card.getChildCount(); i++) {
                if (card.getChildAt(i) instanceof TextView) {
                    TextView tv = (TextView) card.getChildAt(i);
                    if (tv.getText().toString().equals("Pérdidas Totales")) {
                        tv.setText("Gastos Totales");
                        break;
                    }
                }
            }
        }
    }

    private void cargarDatosInformes() {
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No se pudo identificar el usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cargar lotes activos
        cargarLotesUsuario();

        // Cargar y calcular todos los datos financieros
        cargarYCalcularFinanzas();
    }

    private void cargarLotesUsuario() {
        db.collection("lotes")
                .whereEqualTo("id_usuario", usuarioId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        lotesActivosCount = 0;

                        for (DocumentSnapshot lote : querySnapshot) {
                            String estado = lote.getString("Estado");
                            if ("Activo".equals(estado)) {
                                lotesActivosCount++;
                            }
                        }

                        tvLotesActivos.setText(String.valueOf(lotesActivosCount));
                    }
                });
    }

    private void cargarYCalcularFinanzas() {
        // Resetear contadores
        gastosTotales = 0;
        ventasTotales = 0;
        balanceGeneral = 0;
        perdidasReales = 0;
        ciclosActivosCount = 0;
        topCiclos.clear();

        // 1. Obtener todos los lotes del usuario
        db.collection("lotes")
                .whereEqualTo("id_usuario", usuarioId)
                .get()
                .addOnCompleteListener(lotesTask -> {
                    if (lotesTask.isSuccessful()) {
                        List<DocumentSnapshot> lotes = lotesTask.getResult().getDocuments();

                        if (lotes.isEmpty()) {
                            actualizarUI();
                            return;
                        }

                        List<String> lotesIds = new ArrayList<>();
                        for (DocumentSnapshot lote : lotes) {
                            lotesIds.add(lote.getId());
                        }

                        // 2. Obtener todos los ciclos de esos lotes
                        db.collection("ciclos")
                                .whereIn("ID_LOTE", lotesIds)
                                .get()
                                .addOnCompleteListener(ciclosTask -> {
                                    if (ciclosTask.isSuccessful()) {
                                        List<DocumentSnapshot> ciclos = ciclosTask.getResult().getDocuments();

                                        if (ciclos.isEmpty()) {
                                            actualizarUI();
                                            return;
                                        }

                                        // Contadores para cálculos concurrentes
                                        final double[] totalGastos = {0.0};
                                        final double[] totalVentas = {0.0};
                                        final AtomicInteger ciclosProcesados = new AtomicInteger(0);
                                        final int totalCiclos = ciclos.size();

                                        for (DocumentSnapshot ciclo : ciclos) {
                                            String cicloId = ciclo.getId();

                                            // Contar ciclos activos
                                            String estado = ciclo.getString("Estado");
                                            if ("Activo".equals(estado) || "En Progreso".equals(estado)) {
                                                ciclosActivosCount++;
                                            }

                                            // Calcular finanzas de este ciclo
                                            calcularFinanzasCiclo(cicloId, new CalculoCicloListener() {
                                                @Override
                                                public void onCalculoCompletado(double gastosCiclo, double ventasCiclo, double gananciaCiclo) {
                                                    synchronized (this) {
                                                        totalGastos[0] += gastosCiclo;
                                                        totalVentas[0] += ventasCiclo;
                                                    }

                                                    // Agregar a top ciclos si tiene ganancia
                                                    if (gananciaCiclo != 0) {
                                                        Map<String, Object> cicloData = new HashMap<>();
                                                        cicloData.put("nombre", ciclo.getString("Nombre_Ciclo"));
                                                        cicloData.put("ganancia", gananciaCiclo);
                                                        cicloData.put("gastos", gastosCiclo);
                                                        cicloData.put("ventas", ventasCiclo);
                                                        topCiclos.add(cicloData);
                                                    }

                                                    int procesados = ciclosProcesados.incrementAndGet();
                                                    if (procesados == totalCiclos) {
                                                        // Todos los ciclos procesados
                                                        gastosTotales = totalGastos[0];
                                                        ventasTotales = totalVentas[0];
                                                        balanceGeneral = ventasTotales - gastosTotales;

                                                        // Calcular pérdidas reales (solo si balance es negativo)
                                                        if (balanceGeneral < 0) {
                                                            perdidasReales = Math.abs(balanceGeneral);
                                                        }

                                                        actualizarUI();
                                                    }
                                                }

                                                @Override
                                                public void onError(String mensaje) {
                                                    android.util.Log.e("Informes", "Error en ciclo: " + mensaje);
                                                    int procesados = ciclosProcesados.incrementAndGet();
                                                    if (procesados == totalCiclos) {
                                                        gastosTotales = totalGastos[0];
                                                        ventasTotales = totalVentas[0];
                                                        balanceGeneral = ventasTotales - gastosTotales;
                                                        actualizarUI();
                                                    }
                                                }
                                            });
                                        }
                                    } else {
                                        Toast.makeText(this, "Error al cargar ciclos", Toast.LENGTH_SHORT).show();
                                        actualizarUI();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Error al cargar lotes", Toast.LENGTH_SHORT).show();
                        actualizarUI();
                    }
                });
    }

    // Método para calcular finanzas de un ciclo específico
    private void calcularFinanzasCiclo(String cicloId, CalculoCicloListener listener) {
        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> etapas = task.getResult().getDocuments();

                        if (etapas.isEmpty()) {
                            listener.onCalculoCompletado(0.0, 0.0, 0.0);
                            return;
                        }

                        final double[] gastosCiclo = {0.0};
                        final double[] ventasCiclo = {0.0};
                        final AtomicInteger etapasProcesadas = new AtomicInteger(0);
                        final int totalEtapas = etapas.size();

                        for (DocumentSnapshot etapa : etapas) {
                            String etapaId = etapa.getId();
                            String nombreEtapa = etapa.getString("Nombre_Etapa");

                            // Calcular gastos de la etapa
                            db.collection("gastos")
                                    .whereEqualTo("ID_ETAPA", etapaId)
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

                                        synchronized (this) {
                                            gastosCiclo[0] += gastosEtapa;
                                        }

                                        // Si es etapa de VENTA, calcular ventas
                                        if (nombreEtapa != null && nombreEtapa.equalsIgnoreCase("venta")) {
                                            db.collection("ventas")
                                                    .whereEqualTo("ID_Etapa", etapaId)
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

                                                        synchronized (this) {
                                                            ventasCiclo[0] += ventasEtapa;
                                                        }

                                                        int procesadas = etapasProcesadas.incrementAndGet();
                                                        if (procesadas == totalEtapas) {
                                                            double ganancia = ventasCiclo[0] - gastosCiclo[0];
                                                            listener.onCalculoCompletado(gastosCiclo[0], ventasCiclo[0], ganancia);
                                                        }
                                                    });
                                        } else {
                                            int procesadas = etapasProcesadas.incrementAndGet();
                                            if (procesadas == totalEtapas) {
                                                double ganancia = ventasCiclo[0] - gastosCiclo[0];
                                                listener.onCalculoCompletado(gastosCiclo[0], ventasCiclo[0], ganancia);
                                            }
                                        }
                                    });
                        }
                    } else {
                        listener.onError("Error al cargar etapas");
                    }
                });
    }

    // Interfaz para el cálculo de ciclos
    interface CalculoCicloListener {
        void onCalculoCompletado(double gastosCiclo, double ventasCiclo, double gananciaCiclo);
        void onError(String mensaje);
    }

    private void actualizarUI() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        // Mostrar GASTOS totales
        tvGastosTotales.setText(formatter.format(gastosTotales));
        tvGastosTotales.setTextColor(0xFF2196F3); // Azul para gastos

        // Mostrar BALANCE general
        tvBalanceGeneral.setText(formatter.format(balanceGeneral));
        if (balanceGeneral >= 0) {
            tvBalanceGeneral.setTextColor(0xFF4CAF50); // Verde para ganancia
        } else {
            tvBalanceGeneral.setTextColor(0xFFF44336); // Rojo para pérdida
        }

        // Actualizar contadores
        tvCiclosActivos.setText(String.valueOf(ciclosActivosCount));

        // Mostrar top ciclos
        mostrarTopCiclos();

        // Mostrar información en Logs
        android.util.Log.d("Informes", "💰 RESUMEN FINANCIERO:");
        android.util.Log.d("Informes", "  - Ventas Totales: " + formatter.format(ventasTotales));
        android.util.Log.d("Informes", "  - Gastos Totales: " + formatter.format(gastosTotales));
        android.util.Log.d("Informes", "  - Balance General: " + formatter.format(balanceGeneral));

        if (balanceGeneral < 0) {
            android.util.Log.d("Informes", "  - Pérdidas Reales: " + formatter.format(perdidasReales));
        }

        // Calcular y mostrar métricas si hay ventas
        if (ventasTotales > 0) {
            double porcentajeGastos = (gastosTotales / ventasTotales) * 100;
            double margen = (balanceGeneral / ventasTotales) * 100;
            double eficiencia = ventasTotales / (gastosTotales > 0 ? gastosTotales : 1);

            android.util.Log.d("Informes", "📊 MÉTRICAS:");
            android.util.Log.d("Informes", "  - Gastos/Ventas: " + String.format("%.1f%%", porcentajeGastos));

            if (balanceGeneral >= 0) {
                android.util.Log.d("Informes", "  - Margen Ganancia: +" + String.format("%.1f%%", margen));
            } else {
                android.util.Log.d("Informes", "  - Margen Pérdida: " + String.format("%.1f%%", margen));
            }

            android.util.Log.d("Informes", "  - Eficiencia: " + String.format("%.1f", eficiencia) + "x");
        }

        // Toast informativo
        String mensaje = "📊 Resumen:\n";
        mensaje += "Ventas: " + formatter.format(ventasTotales) + "\n";
        mensaje += "Gastos: " + formatter.format(gastosTotales) + "\n";

        if (balanceGeneral >= 0) {
            mensaje += "✅ Ganancia: " + formatter.format(balanceGeneral);
        } else {
            mensaje += "⚠️ Pérdida: " + formatter.format(Math.abs(balanceGeneral));
        }

        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    private void mostrarTopCiclos() {
        containerTopCiclos.removeAllViews();

        // Ordenar por ganancia (mayor a menor)
        java.util.Collections.sort(topCiclos, (a, b) ->
                ((Double) b.get("ganancia")).compareTo((Double) a.get("ganancia")));

        int count = Math.min(topCiclos.size(), 3);

        if (count == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay ciclos con datos financieros");
            tvEmpty.setTextColor(0xFF999999);
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(0, 20, 0, 20);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            containerTopCiclos.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < count; i++) {
            Map<String, Object> ciclo = topCiclos.get(i);
            crearCardTopCiclo(ciclo, i + 1);
        }
    }

    private void crearCardTopCiclo(Map<String, Object> ciclo, int posicion) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setCardElevation(4f);
        card.setRadius(12f);
        card.setContentPadding(16, 16, 16, 16);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        cardContent.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Emoji según posición
        TextView tvPosicion = new TextView(this);
        String[] emojis = {"🥇", "🥈", "🥉"};
        String emoji = (posicion <= 3) ? emojis[posicion - 1] : String.valueOf(posicion);
        tvPosicion.setText(emoji);
        tvPosicion.setTextColor(0xFF12980B);
        tvPosicion.setTextSize(16);
        tvPosicion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPosicion.setPadding(0, 0, 16, 0);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView tvNombre = new TextView(this);
        tvNombre.setText((String) ciclo.get("nombre"));
        tvNombre.setTextColor(0xFF333333);
        tvNombre.setTextSize(14);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);

        // Mostrar resultado
        TextView tvResultado = new TextView(this);
        double ganancia = (Double) ciclo.get("ganancia");
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        if (ganancia >= 0) {
            tvResultado.setText("Ganancia: " + formatter.format(ganancia));
            tvResultado.setTextColor(0xFF4CAF50);
        } else {
            tvResultado.setText("Pérdida: " + formatter.format(Math.abs(ganancia)));
            tvResultado.setTextColor(0xFFF44336);
        }
        tvResultado.setTextSize(12);

        // Mostrar detalles
        TextView tvDetalles = new TextView(this);
        double gastos = (Double) ciclo.get("gastos");
        double ventas = (Double) ciclo.get("ventas");
        tvDetalles.setText(String.format("G: %s | V: %s",
                formatter.format(gastos),
                formatter.format(ventas)));
        tvDetalles.setTextColor(0xFF666666);
        tvDetalles.setTextSize(10);

        textLayout.addView(tvNombre);
        textLayout.addView(tvResultado);
        textLayout.addView(tvDetalles);
        cardContent.addView(tvPosicion);
        cardContent.addView(textLayout);
        card.addView(cardContent);

        containerTopCiclos.addView(card);
    }

    private void configurarEventos() {
        btnDescargarPDF.setOnClickListener(v -> {
            Toast.makeText(this, "📊 Generando PDF...", Toast.LENGTH_SHORT).show();
            generarPDF();
        });
    }

    private void generarPDF() {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            // Configurar
            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setColor(android.graphics.Color.BLACK);
            titlePaint.setTextSize(20);
            titlePaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            android.graphics.Paint normalPaint = new android.graphics.Paint();
            normalPaint.setColor(android.graphics.Color.BLACK);
            normalPaint.setTextSize(12);

            // Fondo
            canvas.drawColor(android.graphics.Color.WHITE);

            int y = 50;
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

            // Título
            canvas.drawText("INFORME FINANCIERO - FINCA CACAO", 50, y, titlePaint);
            y += 40;

            // RESUMEN FINANCIERO COMPLETO
            canvas.drawText("📊 RESUMEN FINANCIERO", 50, y, titlePaint);
            y += 25;

            // Ventas Totales
            canvas.drawText("💰 VENTAS TOTALES: " + nf.format(ventasTotales), 70, y, normalPaint);
            y += 20;

            // Gastos Totales
            canvas.drawText("💸 GASTOS TOTALES: " + nf.format(gastosTotales), 70, y, normalPaint);
            y += 20;

            // Resultado Final
            canvas.drawText("📈 RESULTADO FINAL:", 70, y, normalPaint);
            y += 20;

            if (balanceGeneral >= 0) {
                canvas.drawText("   ✅ GANANCIA: " + nf.format(balanceGeneral), 80, y, getGreenPaint());
            } else {
                canvas.drawText("   ⚠️ PÉRDIDA: " + nf.format(Math.abs(balanceGeneral)), 80, y, getRedPaint());
            }
            y += 30;

            // MÉTRICAS
            if (ventasTotales > 0) {
                canvas.drawText("📊 MÉTRICAS DE RENTABILIDAD", 50, y, titlePaint);
                y += 25;

                double porcentajeGastos = (gastosTotales / ventasTotales) * 100;
                canvas.drawText(String.format("• Gastos/Ventas: %.1f%%", porcentajeGastos), 70, y, normalPaint);
                y += 20;

                double margen = (balanceGeneral / ventasTotales) * 100;
                if (balanceGeneral >= 0) {
                    canvas.drawText(String.format("• Margen Ganancia: +%.1f%%", margen), 70, y, getGreenPaint());
                } else {
                    canvas.drawText(String.format("• Margen Pérdida: %.1f%%", margen), 70, y, getRedPaint());
                }
                y += 20;

                double eficiencia = ventasTotales / (gastosTotales > 0 ? gastosTotales : 1);
                canvas.drawText(String.format("• Eficiencia: %.1fx", eficiencia), 70, y, normalPaint);
                y += 30;
            }

            // PRODUCCIÓN
            canvas.drawText("🌱 PRODUCCIÓN", 50, y, titlePaint);
            y += 25;
            canvas.drawText("• Lotes Activos: " + lotesActivosCount, 70, y, normalPaint);
            y += 20;
            canvas.drawText("• Ciclos Activos: " + ciclosActivosCount, 70, y, normalPaint);
            y += 30;

            // TOP CICLOS
            if (!topCiclos.isEmpty()) {
                canvas.drawText("🏆 TOP 3 CICLOS", 50, y, titlePaint);
                y += 25;

                for (int i = 0; i < Math.min(topCiclos.size(), 3); i++) {
                    Map<String, Object> ciclo = topCiclos.get(i);
                    String nombre = (String) ciclo.get("nombre");
                    Double ganancia = (Double) ciclo.get("ganancia");
                    Double gastos = (Double) ciclo.get("gastos");
                    Double ventasCiclo = (Double) ciclo.get("ventas");

                    String[] emojis = {"🥇", "🥈", "🥉"};
                    String emoji = (i < 3) ? emojis[i] : "•";

                    canvas.drawText(emoji + " " + nombre, 70, y, normalPaint);
                    y += 18;
                    canvas.drawText("   Ventas: " + nf.format(ventasCiclo), 80, y, normalPaint);
                    y += 18;
                    canvas.drawText("   Gastos: " + nf.format(gastos), 80, y, normalPaint);
                    y += 18;

                    if (ganancia >= 0) {
                        canvas.drawText("   Ganancia: " + nf.format(ganancia), 80, y, getGreenPaint());
                    } else {
                        canvas.drawText("   Pérdida: " + nf.format(Math.abs(ganancia)), 80, y, getRedPaint());
                    }
                    y += 25;
                }
            }

            // Pie de página
            y += 20;
            canvas.drawText("---", 50, y, normalPaint);
            y += 20;
            String fecha = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            canvas.drawText("Generado el: " + fecha, 50, y, normalPaint);

            // Finalizar
            document.finishPage(page);

            // Guardar
            java.io.File cacheDir = getCacheDir();
            java.io.File pdfFile = new java.io.File(cacheDir, "informe_" +
                    System.currentTimeMillis() + ".pdf");

            java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Compartir
            compartirPDF(pdfFile);

        } catch (Exception e) {
            mostrarResumenComoTexto();
        }
    }

    private android.graphics.Paint getGreenPaint() {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setColor(android.graphics.Color.parseColor("#4CAF50"));
        p.setTextSize(12);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return p;
    }

    private android.graphics.Paint getRedPaint() {
        android.graphics.Paint p = new android.graphics.Paint();
        p.setColor(android.graphics.Color.parseColor("#F44336"));
        p.setTextSize(12);
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return p;
    }

    private void compartirPDF(java.io.File pdfFile) {
        android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                pdfFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Compartir PDF..."));
    }

    private void mostrarResumenComoTexto() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        String resumen = "📊 INFORME FINANCIERO\n\n" +
                "💰 VENTAS TOTALES: " + nf.format(ventasTotales) + "\n" +
                "💸 GASTOS TOTALES: " + nf.format(gastosTotales) + "\n";

        if (balanceGeneral >= 0) {
            resumen += "✅ GANANCIA: " + nf.format(balanceGeneral);
        } else {
            resumen += "⚠️ PÉRDIDA: " + nf.format(Math.abs(balanceGeneral));
        }

        resumen += "\n\n🌱 PRODUCCIÓN:\n" +
                "• Lotes Activos: " + lotesActivosCount + "\n" +
                "• Ciclos Activos: " + ciclosActivosCount;

        // Agregar métricas si hay ventas
        if (ventasTotales > 0) {
            double porcentajeGastos = (gastosTotales / ventasTotales) * 100;
            double margen = (balanceGeneral / ventasTotales) * 100;

            resumen += "\n\n📊 MÉTRICAS:\n";
            resumen += String.format("• Gastos/Ventas: %.1f%%\n", porcentajeGastos);
            if (balanceGeneral >= 0) {
                resumen += String.format("• Margen Ganancia: +%.1f%%", margen);
            } else {
                resumen += String.format("• Margen Pérdida: %.1f%%", margen);
            }
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Informe Completo")
                .setMessage(resumen)
                .setPositiveButton("OK", null)
                .show();
    }

    private void configurarBottomNavigation() {
        findViewById(R.id.boton_inicio).setOnClickListener(v -> {
            Intent intent = new Intent(InformesActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_lotes).setOnClickListener(v -> {
            Intent intent = new Intent(InformesActivity.this, LotesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.boton_informes).setOnClickListener(v -> {
            // Ya estamos en Informes
        });

        findViewById(R.id.boton_perfil).setOnClickListener(v -> {
            Intent intent = new Intent(InformesActivity.this, PerfilActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}