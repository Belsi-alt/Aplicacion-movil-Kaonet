package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgregarEtapaActivity extends AppCompatActivity {

    private EditText etFechaInicioEtapa, etFechaFinEtapa;
    private EditText etDescripcionEtapa;
    private Spinner spinnerNombreEtapa;
    private RadioGroup radioGroupEstado;
    private RadioButton radioPendiente, radioEnProgreso, radioCompletada;
    private Button btnAgregarEtapa;
    private TextView tvTitulo;

    private FirebaseFirestore db;
    private String cicloId;
    private String loteId;
    private int numeroCiclo;

    // Todas las etapas posibles
    private final String[] TODAS_ETAPAS = {"Siembra", "Crecimiento", "Cosecha", "Venta"};
    // Etapas para ciclos subsiguientes (sin Siembra)
    private final String[] ETAPAS_CICLOS_SUBSIGUIENTES = {"Crecimiento", "Cosecha", "Venta"};

    private static final String TAG = "AgregarEtapaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_etapa);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener parámetros
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        Log.d(TAG, "📥 PARÁMETROS RECIBIDOS:");
        Log.d(TAG, "   - CICLO_ID: " + cicloId);
        Log.d(TAG, "   - LOTE_ID: " + loteId);

        if (cicloId == null) {
            Toast.makeText(this, "Error: No se recibió el ciclo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarVistas();
        obtenerNumeroCicloYConfigurar();
        configurarEventos();
    }

    private void inicializarVistas() {
        etFechaInicioEtapa = findViewById(R.id.etFechaInicioEtapa);
        etFechaFinEtapa = findViewById(R.id.etFechaFinEtapa);
        etDescripcionEtapa = findViewById(R.id.etDescripcionEtapa);
        spinnerNombreEtapa = findViewById(R.id.spinnerNombreEtapa);
        radioGroupEstado = findViewById(R.id.radioGroupEstado);
        btnAgregarEtapa = findViewById(R.id.btnAgregarEtapa);
        tvTitulo = findViewById(R.id.tvTitulo);

        // Referencias a RadioButtons
        radioPendiente = findViewById(R.id.radioPendiente);
        radioEnProgreso = findViewById(R.id.radioEnProgreso);
        radioCompletada = findViewById(R.id.radioCompletada);

        // Establecer estado por defecto
        radioPendiente.setChecked(true);

        // Configurar título
        tvTitulo.setText("Agregar Nueva Etapa");
        btnAgregarEtapa.setText("Agregar Etapa");
    }

    private void obtenerNumeroCicloYConfigurar() {
        if (cicloId == null) {
            Log.e(TAG, "❌ cicloId es null");
            numeroCiclo = 1;
            configurarSpinner();
            return;
        }

        // Obtener el número del ciclo actual para determinar qué etapas mostrar
        db.collection("ciclos").document(cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot cicloDoc = task.getResult();
                        Long numeroCicloLong = cicloDoc.getLong("Numero_Ciclo");
                        numeroCiclo = numeroCicloLong != null ? numeroCicloLong.intValue() : 1;

                        Log.d(TAG, "🔢 Número de ciclo obtenido: " + numeroCiclo);
                        configurarSpinner();
                    } else {
                        numeroCiclo = 1;
                        Log.w(TAG, "⚠️ No se pudo obtener ciclo, usando valor por defecto: " + numeroCiclo);
                        configurarSpinner();
                    }
                });
    }

    private void configurarSpinner() {
        String[] etapasParaSpinner;

        if (numeroCiclo == 1) {
            etapasParaSpinner = TODAS_ETAPAS;
            Log.d(TAG, "🔄 Configurando spinner para CICLO 1 (con Siembra)");
        } else {
            etapasParaSpinner = ETAPAS_CICLOS_SUBSIGUIENTES;
            Log.d(TAG, "🔄 Configurando spinner para CICLO " + numeroCiclo + " (sin Siembra)");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, etapasParaSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNombreEtapa.setAdapter(adapter);
    }

    private void configurarEventos() {
        etFechaInicioEtapa.setOnClickListener(v -> mostrarDatePicker(etFechaInicioEtapa));
        etFechaFinEtapa.setOnClickListener(v -> mostrarDatePicker(etFechaFinEtapa));

        btnAgregarEtapa.setOnClickListener(v -> {
            Log.d(TAG, "🔄 Botón Agregar presionado");
            if (validarFormulario()) {
                verificarYAgregarEtapa();
            }
        });
    }

    private void mostrarDatePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String fechaSeleccionada = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    editText.setText(fechaSeleccionada);
                },
                year,
                month,
                day
        );
        datePicker.show();
    }

    private boolean validarFormulario() {
        String nombreEtapa = spinnerNombreEtapa.getSelectedItem().toString();
        String fechaInicio = etFechaInicioEtapa.getText().toString().trim();
        String fechaFin = etFechaFinEtapa.getText().toString().trim();

        boolean isValid = true;

        // Limpiar errores previos
        etFechaInicioEtapa.setError(null);
        etFechaFinEtapa.setError(null);

        if (nombreEtapa.isEmpty()) {
            Toast.makeText(this, "Selecciona un nombre de etapa", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (fechaInicio.isEmpty()) {
            etFechaInicioEtapa.setError("Selecciona la fecha de inicio");
            isValid = false;
        }

        if (!fechaFin.isEmpty()) {
            if (fechaInicio.isEmpty()) {
                etFechaFinEtapa.setError("Primero selecciona la fecha de inicio");
                isValid = false;
            } else if (!esFechaPosterior(fechaFin, fechaInicio)) {
                etFechaFinEtapa.setError("La fecha fin debe ser posterior a la fecha inicio");
                isValid = false;
            }
        }

        return isValid;
    }

    private boolean esFechaPosterior(String fechaFin, String fechaInicio) {
        return fechaFin.compareTo(fechaInicio) >= 0;
    }

    private String obtenerEstadoEtapa() {
        int selectedId = radioGroupEstado.getCheckedRadioButtonId();

        if (selectedId == R.id.radioPendiente) {
            return "Pendiente";
        } else if (selectedId == R.id.radioEnProgreso) {
            return "En Progreso";
        } else if (selectedId == R.id.radioCompletada) {
            return "Completada";
        }
        return "Pendiente";
    }

    private void verificarYAgregarEtapa() {
        String nombreEtapaSeleccionada = spinnerNombreEtapa.getSelectedItem().toString();

        // Verificar si ya existe esta etapa en el ciclo actual
        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .whereEqualTo("Nombre_Etapa", nombreEtapaSeleccionada)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Toast.makeText(this, "❌ Ya existe una etapa de " + nombreEtapaSeleccionada + " en este ciclo", Toast.LENGTH_LONG).show();
                        } else {
                            verificarFlujoEtapas(nombreEtapaSeleccionada);
                        }
                    } else {
                        Toast.makeText(this, "Error al verificar etapas existentes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verificarFlujoEtapas(String nombreEtapaSeleccionada) {
        db.collection("etapas")
                .whereEqualTo("ID_CICLO", cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> etapasCicloActual = task.getResult().getDocuments();

                        if (validarFlujoSecuencial(nombreEtapaSeleccionada, etapasCicloActual)) {
                            agregarEtapaAFirestore();
                        }
                    } else {
                        Toast.makeText(this, "Error al verificar flujo de etapas", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validarFlujoSecuencial(String nuevaEtapa, List<DocumentSnapshot> etapasCicloActual) {
        // Definir el orden de etapas según el ciclo
        String[] ordenEtapas = (numeroCiclo == 1) ? TODAS_ETAPAS : ETAPAS_CICLOS_SUBSIGUIENTES;

        int indiceNuevaEtapa = obtenerIndiceEtapa(nuevaEtapa, ordenEtapas);

        if (indiceNuevaEtapa == -1) {
            return false;
        }

        // Para la primera etapa del ciclo, no hay validaciones anteriores
        if (indiceNuevaEtapa == 0) {
            return true;
        }

        // Verificar etapas anteriores en el mismo ciclo
        for (int i = 0; i < indiceNuevaEtapa; i++) {
            String etapaAnterior = ordenEtapas[i];
            boolean etapaAnteriorExiste = false;
            boolean etapaAnteriorCompletada = false;

            for (DocumentSnapshot etapaDoc : etapasCicloActual) {
                String nombreEtapaExistente = etapaDoc.getString("Nombre_Etapa");
                String estadoEtapaExistente = etapaDoc.getString("Estado");

                if (nombreEtapaExistente != null && nombreEtapaExistente.equals(etapaAnterior)) {
                    etapaAnteriorExiste = true;
                    etapaAnteriorCompletada = "Completada".equals(estadoEtapaExistente);
                    break;
                }
            }

            if (!etapaAnteriorExiste) {
                Toast.makeText(this,
                        "❌ No puedes crear " + nuevaEtapa + " sin completar primero " + etapaAnterior,
                        Toast.LENGTH_LONG).show();
                return false;
            }

            if (!etapaAnteriorCompletada) {
                Toast.makeText(this,
                        "❌ No puedes crear " + nuevaEtapa + " porque " + etapaAnterior + " no está completada",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    private int obtenerIndiceEtapa(String nombreEtapa, String[] ordenEtapas) {
        for (int i = 0; i < ordenEtapas.length; i++) {
            if (ordenEtapas[i].equals(nombreEtapa)) {
                return i;
            }
        }
        return -1;
    }

    private void agregarEtapaAFirestore() {
        String nombreEtapa = spinnerNombreEtapa.getSelectedItem().toString();
        String descripcion = etDescripcionEtapa.getText().toString().trim();
        String fechaInicio = etFechaInicioEtapa.getText().toString().trim();
        String fechaFin = etFechaFinEtapa.getText().toString().trim();
        String estado = obtenerEstadoEtapa();

        Map<String, Object> etapa = new HashMap<>();
        etapa.put("ID_CICLO", cicloId);
        etapa.put("Nombre_Etapa", nombreEtapa);
        etapa.put("Descripcion", descripcion);
        etapa.put("Fecha_Inicio", fechaInicio);
        etapa.put("Fecha_Fin", fechaFin.isEmpty() ? null : fechaFin);
        etapa.put("Estado", estado);
        etapa.put("Numero_Ciclo", numeroCiclo);

        Log.d(TAG, "➕ Agregando nueva etapa: " + nombreEtapa);

        db.collection("etapas")
                .add(etapa)
                .addOnSuccessListener(documentReference -> {
                    String mensaje = "✅ Etapa " + nombreEtapa + " agregada correctamente";
                    if (numeroCiclo > 1 && nombreEtapa.equals("Crecimiento")) {
                        mensaje += "\n🌱 Recordatorio: Las plantas ya están establecidas del ciclo anterior";
                    }
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

                    // Regresar a DetalleCicloActivity
                    regresarADetalleCiclo();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al agregar etapa: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al agregar etapa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void regresarADetalleCiclo() {
        Intent intent = new Intent(this, DetalleCicloActivity.class);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}