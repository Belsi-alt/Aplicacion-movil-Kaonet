package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AgregarGastoActivity extends AppCompatActivity {

    private Spinner spinnerNombreGasto;
    private EditText etDescripcionGasto, etMontoGasto, etUnidadGasto, etFechaGasto;
    private Button btnAgregarGasto;

    private FirebaseFirestore db;
    private String etapaId;
    private String cicloId;
    private String loteId;

    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    private static final String TAG = "AgregarGastoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_gasto);

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

        // Inicializar fecha
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        inicializarVistas();
        configurarSpinner();
        configurarEventos();
    }

    private void inicializarVistas() {
        spinnerNombreGasto = findViewById(R.id.spinnerNombreGasto);
        etDescripcionGasto = findViewById(R.id.etDescripcionGasto);
        etMontoGasto = findViewById(R.id.etMontoGasto);
        etUnidadGasto = findViewById(R.id.etUnidadGasto);
        etFechaGasto = findViewById(R.id.etFechaGasto);
        btnAgregarGasto = findViewById(R.id.btnAgregarGasto);

        // Establecer fecha actual por defecto
        etFechaGasto.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void configurarSpinner() {
        // Opciones predefinidas para nombres de gasto
        String[] nombresGasto = {
                "Insumos",
                "Mano de obra",
                "Herramientas",
                "Transporte",
                "Semillas",
                "Servicios",
                "Gastos de operación",

        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                nombresGasto
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNombreGasto.setAdapter(adapter);
    }

    private void configurarEventos() {
        // DatePicker para fecha
        etFechaGasto.setOnClickListener(v -> mostrarDatePicker());

        // Botón agregar gasto
        btnAgregarGasto.setOnClickListener(v -> {
            if (validarFormulario()) {
                agregarGastoAFirestore();
            }
        });
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    String fechaSeleccionada = dateFormat.format(selectedDate.getTime());
                    etFechaGasto.setText(fechaSeleccionada);
                },
                year,
                month,
                day
        );
        datePicker.show();
    }

    private boolean validarFormulario() {
        String descripcion = etDescripcionGasto.getText().toString().trim();
        String montoStr = etMontoGasto.getText().toString().trim();
        String fecha = etFechaGasto.getText().toString().trim();

        boolean isValid = true;

        if (descripcion.isEmpty()) {
            etDescripcionGasto.setError("Ingresa la descripción del gasto");
            isValid = false;
        }

        if (montoStr.isEmpty()) {
            etMontoGasto.setError("Ingresa el monto del gasto");
            isValid = false;
        } else {
            try {
                double monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    etMontoGasto.setError("El monto debe ser mayor a 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etMontoGasto.setError("Ingresa un monto válido");
                isValid = false;
            }
        }

        if (fecha.isEmpty()) {
            etFechaGasto.setError("Selecciona la fecha del gasto");
            isValid = false;
        }

        return isValid;
    }

    private void agregarGastoAFirestore() {
        // Obtener valores del formulario
        String nombreGasto = spinnerNombreGasto.getSelectedItem().toString();
        String descripcion = etDescripcionGasto.getText().toString().trim();
        String montoStr = etMontoGasto.getText().toString().trim();
        String unidad = etUnidadGasto.getText().toString().trim();
        String fecha = etFechaGasto.getText().toString().trim();

        double monto = Double.parseDouble(montoStr);

        // Si unidad está vacía, establecer como null
        if (unidad.isEmpty()) {
            unidad = null;
        }

        // Crear objeto gasto con la estructura correcta
        Map<String, Object> gasto = new HashMap<>();
        gasto.put("ID_ETAPA", etapaId);
        gasto.put("Nombre_Gasto", nombreGasto);  // Nombre principal (tipo)
        gasto.put("Categoria", nombreGasto);     // Categoría también como nombre
        gasto.put("Descripcion", descripcion);   // Descripción detallada
        gasto.put("Monto", monto);
        gasto.put("Unidad", unidad);
        gasto.put("Fecha", fecha);

        Log.d(TAG, "🔄 Agregando gasto: " + nombreGasto + " - $" + monto + " para etapa: " + etapaId);

        // Insertar en Firestore
        db.collection("gastos")
                .add(gasto)
                .addOnSuccessListener(documentReference -> {
                    String gastoId = documentReference.getId();
                    Log.d(TAG, "✅ Gasto agregado correctamente. ID: " + gastoId);

                    // Actualizar totales del ciclo
                    actualizarTotalesCiclo(monto);

                    String mensaje = String.format("✅ Gasto '%s' agregado\nMonto: $%,.0f", nombreGasto, monto);
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

                    // Regresar a DetalleEtapaActivity
                    Intent intent = new Intent(this, DetalleEtapaActivity.class);
                    intent.putExtra("ETAPA_ID", etapaId);
                    intent.putExtra("CICLO_ID", cicloId);
                    intent.putExtra("LOTE_ID", loteId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al agregar gasto: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al agregar gasto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarTotalesCiclo(double montoGasto) {
        if (cicloId != null) {
            // Obtener el ciclo actual para sumar al gasto total
            db.collection("ciclos").document(cicloId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot cicloDoc = task.getResult();
                            Double gananciaActual = cicloDoc.getDouble("Ganancia_Total");
                            Double gastoActual = cicloDoc.getDouble("Gasto_Total");

                            // Si no existe el campo Gasto_Total, inicializarlo
                            double nuevoGastoTotal = (gastoActual != null ? gastoActual : 0.0) + montoGasto;

                            // Calcular nueva ganancia (Ventas - Gastos)
                            // Primero obtener ventas totales
                            Double ventasActual = cicloDoc.getDouble("Ventas_Total");
                            double ventas = ventasActual != null ? ventasActual : 0.0;
                            double nuevaGanancia = ventas - nuevoGastoTotal;

                            // Actualizar el ciclo
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("Gasto_Total", nuevoGastoTotal);
                            updates.put("Ganancia_Total", nuevaGanancia);

                            db.collection("ciclos").document(cicloId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Totales del ciclo actualizados - Gastos: $" + nuevoGastoTotal + ", Ganancia: $" + nuevaGanancia);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Error al actualizar totales del ciclo: " + e.getMessage());
                                    });
                        }
                    });
        }
    }
}