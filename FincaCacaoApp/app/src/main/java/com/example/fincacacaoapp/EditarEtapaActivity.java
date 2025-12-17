package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.util.Map;

public class EditarEtapaActivity extends AppCompatActivity {

    private EditText etFechaInicioEtapa, etFechaFinEtapa;
    private EditText etDescripcionEtapa;
    private Spinner spinnerNombreEtapa;
    private RadioGroup radioGroupEstado;
    private RadioButton radioPendiente, radioEnProgreso, radioCompletada;
    private Button btnGuardarCambios;
    private TextView tvTitulo;

    private FirebaseFirestore db;
    private String etapaId;
    private String cicloId;
    private String loteId;

    // Todas las etapas posibles
    private final String[] TODAS_ETAPAS = {"Siembra", "Crecimiento", "Cosecha", "Venta"};

    private static final String TAG = "EditarEtapaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_etapa);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener parámetros
        etapaId = getIntent().getStringExtra("ETAPA_ID");
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        Log.d(TAG, "📥 PARÁMETROS RECIBIDOS:");
        Log.d(TAG, "   - ETAPA_ID: " + etapaId);
        Log.d(TAG, "   - CICLO_ID: " + cicloId);
        Log.d(TAG, "   - LOTE_ID: " + loteId);

        if (etapaId == null || cicloId == null) {
            Toast.makeText(this, "Error: No se recibieron los datos necesarios", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        inicializarVistas();
        cargarDatosEtapa();
        configurarEventos();
    }

    private void inicializarVistas() {
        etFechaInicioEtapa = findViewById(R.id.etFechaInicioEtapa);
        etFechaFinEtapa = findViewById(R.id.etFechaFinEtapa);
        etDescripcionEtapa = findViewById(R.id.etDescripcionEtapa);
        spinnerNombreEtapa = findViewById(R.id.spinnerNombreEtapa);
        radioGroupEstado = findViewById(R.id.radioGroupEstado);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        tvTitulo = findViewById(R.id.tvTitulo);

        // Referencias a RadioButtons
        radioPendiente = findViewById(R.id.radioPendiente);
        radioEnProgreso = findViewById(R.id.radioEnProgreso);
        radioCompletada = findViewById(R.id.radioCompletada);

        // Configurar spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TODAS_ETAPAS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNombreEtapa.setAdapter(adapter);

        // Configurar título
        tvTitulo.setText("Editar Etapa");
        btnGuardarCambios.setText("Guardar Cambios");
    }

    private void cargarDatosEtapa() {
        Log.d(TAG, "🔄 Cargando datos de etapa: " + etapaId);

        db.collection("etapas").document(etapaId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();

                        String nombreEtapa = document.getString("Nombre_Etapa");
                        String descripcion = document.getString("Descripcion");
                        String fechaInicio = document.getString("Fecha_Inicio");
                        String fechaFin = document.getString("Fecha_Fin");
                        String estado = document.getString("Estado");

                        Log.d(TAG, "📋 Datos cargados:");
                        Log.d(TAG, "   - Nombre: " + nombreEtapa);
                        Log.d(TAG, "   - Descripción: " + descripcion);
                        Log.d(TAG, "   - Fecha Inicio: " + fechaInicio);
                        Log.d(TAG, "   - Fecha Fin: " + fechaFin);
                        Log.d(TAG, "   - Estado: " + estado);

                        // Autocompletar formulario
                        if (nombreEtapa != null) {
                            ArrayAdapter adapter = (ArrayAdapter) spinnerNombreEtapa.getAdapter();
                            int position = adapter.getPosition(nombreEtapa);
                            if (position >= 0) {
                                spinnerNombreEtapa.setSelection(position);
                                Log.d(TAG, "✅ Spinner seleccionado en posición: " + position);
                            } else {
                                Log.w(TAG, "⚠️ Etapa no encontrada en spinner: " + nombreEtapa);
                            }
                        }

                        if (descripcion != null) {
                            etDescripcionEtapa.setText(descripcion);
                        }

                        if (fechaInicio != null) {
                            etFechaInicioEtapa.setText(fechaInicio);
                        }

                        if (fechaFin != null && !fechaFin.isEmpty()) {
                            etFechaFinEtapa.setText(fechaFin);
                        }

                        if (estado != null) {
                            seleccionarEstado(estado);
                        }

                        Log.d(TAG, "✅ Datos de etapa cargados correctamente");

                    } else {
                        Log.e(TAG, "❌ Error al cargar etapa para edición");
                        Toast.makeText(this, "Error: No se pudieron cargar los datos de la etapa", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void seleccionarEstado(String estado) {
        if (estado == null) return;

        Log.d(TAG, "🎯 Seleccionando estado: " + estado);

        switch (estado.toLowerCase()) {
            case "en progreso":
                radioEnProgreso.setChecked(true);
                break;
            case "completada":
                radioCompletada.setChecked(true);
                break;
            case "pendiente":
            default:
                radioPendiente.setChecked(true);
                break;
        }
    }

    private void configurarEventos() {
        etFechaInicioEtapa.setOnClickListener(v -> mostrarDatePicker(etFechaInicioEtapa));
        etFechaFinEtapa.setOnClickListener(v -> mostrarDatePicker(etFechaFinEtapa));

        btnGuardarCambios.setOnClickListener(v -> {
            Log.d(TAG, "💾 Guardando cambios...");
            if (validarFormulario()) {
                actualizarEtapaEnFirestore();
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

    private void actualizarEtapaEnFirestore() {
        String nombreEtapa = spinnerNombreEtapa.getSelectedItem().toString();
        String descripcion = etDescripcionEtapa.getText().toString().trim();
        String fechaInicio = etFechaInicioEtapa.getText().toString().trim();
        String fechaFin = etFechaFinEtapa.getText().toString().trim();
        String estado = obtenerEstadoEtapa();

        Map<String, Object> updates = new HashMap<>();
        updates.put("Nombre_Etapa", nombreEtapa);
        updates.put("Descripcion", descripcion.isEmpty() ? null : descripcion);
        updates.put("Fecha_Inicio", fechaInicio);
        updates.put("Fecha_Fin", fechaFin.isEmpty() ? null : fechaFin);
        updates.put("Estado", estado);

        Log.d(TAG, "✏️ Actualizando etapa: " + etapaId);
        Log.d(TAG, "📋 Datos a actualizar:");
        Log.d(TAG, "   - Nombre: " + nombreEtapa);
        Log.d(TAG, "   - Descripción: " + descripcion);
        Log.d(TAG, "   - Fecha Inicio: " + fechaInicio);
        Log.d(TAG, "   - Fecha Fin: " + fechaFin);
        Log.d(TAG, "   - Estado: " + estado);

        db.collection("etapas").document(etapaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Etapa actualizada correctamente. ID: " + etapaId);
                    Toast.makeText(this, "✅ Etapa '" + nombreEtapa + "' actualizada correctamente", Toast.LENGTH_LONG).show();

                    // Regresar a DetalleEtapaActivity
                    regresarADetalleEtapa();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al actualizar etapa: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al actualizar etapa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void regresarADetalleEtapa() {
        Intent intent = new Intent(this, DetalleEtapaActivity.class);
        intent.putExtra("ETAPA_ID", etapaId);
        intent.putExtra("CICLO_ID", cicloId);
        intent.putExtra("LOTE_ID", loteId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
