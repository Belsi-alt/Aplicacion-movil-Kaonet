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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditarGastoActivity extends AppCompatActivity {

    private static final String TAG = "EditarGastoActivity";

    // Views - MISMOS IDs que AgregarGasto
    private Spinner spinnerNombreGasto;
    private EditText etDescripcionGasto, etMontoGasto, etUnidadGasto, etFechaGasto;
    private TextView tvTitulo, tvIdGasto, tvSubtitulo;
    private Button btnGuardarCambios, btnEliminarGasto;

    // Firestore
    private FirebaseFirestore db;

    // Variables
    private String gastoId; // ID del documento en Firestore
    private String etapaId;
    private String cicloId;
    private String loteId;

    // Fecha
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    // MISMOS valores del spinner que AgregarGasto
    private final String[] nombresGasto = {
            "Insumos",
            "Mano de obra",
            "Herramientas",
            "Transporte",
            "Semillas",
            "Servicios",
            "Gastos de operación"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_gasto);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Obtener IDs del Intent
        obtenerDatosIntent();

        // Inicializar fecha
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Inicializar vistas
        inicializarVistas();

        // Configurar spinner
        configurarSpinner();

        // Configurar eventos
        configurarEventos();

        // Cargar datos del gasto
        cargarDatosGasto();
    }

    private void obtenerDatosIntent() {
        Intent intent = getIntent();
        gastoId = intent.getStringExtra("GASTO_ID");
        etapaId = intent.getStringExtra("ETAPA_ID");
        cicloId = intent.getStringExtra("CICLO_ID");
        loteId = intent.getStringExtra("LOTE_ID");

        Log.d(TAG, "📋 Datos recibidos:");
        Log.d(TAG, "   - GASTO_ID: " + gastoId);
        Log.d(TAG, "   - ETAPA_ID: " + etapaId);
        Log.d(TAG, "   - CICLO_ID: " + cicloId);
        Log.d(TAG, "   - LOTE_ID: " + loteId);

        if (gastoId == null) {
            Toast.makeText(this, "Error: No se recibió el gasto a editar", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void inicializarVistas() {
        spinnerNombreGasto = findViewById(R.id.spinnerNombreGasto);
        etDescripcionGasto = findViewById(R.id.etDescripcionGasto);
        etMontoGasto = findViewById(R.id.etMontoGasto);
        etUnidadGasto = findViewById(R.id.etUnidadGasto);
        etFechaGasto = findViewById(R.id.etFechaGasto);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvIdGasto = findViewById(R.id.tvIdGasto);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarGasto = findViewById(R.id.btnEliminarGasto);

        // Configurar ID del gasto
        if (gastoId != null) {
            String idDisplay = gastoId.length() > 8 ?
                    "ID: #" + gastoId.substring(0, 8) + "..." :
                    "ID: #" + gastoId;
            tvIdGasto.setText(idDisplay);
        }
    }

    private void configurarSpinner() {
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

        // Botón guardar cambios
        btnGuardarCambios.setOnClickListener(v -> {
            if (validarFormulario()) {
                guardarCambios();
            }
        });

        // Botón eliminar gasto
        btnEliminarGasto.setOnClickListener(v -> confirmarEliminacion());
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Usar la fecha actual del campo o fecha actual
        String fechaActual = etFechaGasto.getText().toString().trim();
        if (!fechaActual.isEmpty() && fechaActual.length() == 10) {
            try {
                String[] partes = fechaActual.split("-");
                if (partes.length == 3) {
                    calendar.set(
                            Integer.parseInt(partes[0]),
                            Integer.parseInt(partes[1]) - 1,
                            Integer.parseInt(partes[2])
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al parsear fecha: " + e.getMessage());
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    String fechaSeleccionada = dateFormat.format(selectedDate.getTime());
                    etFechaGasto.setText(fechaSeleccionada);
                    Log.d(TAG, "📅 Fecha seleccionada: " + fechaSeleccionada);
                },
                year,
                month,
                day
        );

        datePicker.show();
    }

    private void cargarDatosGasto() {
        Log.d(TAG, "🔄 Cargando datos del gasto: " + gastoId);

        db.collection("gastos").document(gastoId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            mostrarDatosGasto(document);
                        } else {
                            Toast.makeText(this, "El gasto no existe", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void mostrarDatosGasto(DocumentSnapshot document) {
        try {
            // Obtener datos del documento - MISMOS nombres que AgregarGasto
            String nombreGasto = document.getString("Nombre_Gasto");
            String descripcion = document.getString("Descripcion");
            Double monto = document.getDouble("Monto");
            String unidad = document.getString("Unidad");
            String fecha = document.getString("Fecha");

            Log.d(TAG, "📄 Datos del gasto:");
            Log.d(TAG, "   - Nombre_Gasto: " + nombreGasto);
            Log.d(TAG, "   - Descripcion: " + descripcion);
            Log.d(TAG, "   - Monto: " + monto);
            Log.d(TAG, "   - Unidad: " + unidad);
            Log.d(TAG, "   - Fecha: " + fecha);

            // Mostrar en los campos
            etDescripcionGasto.setText(descripcion != null ? descripcion : "");

            if (monto != null) {
                etMontoGasto.setText(String.valueOf(monto));
            }

            etUnidadGasto.setText(unidad != null ? unidad : "");

            if (fecha != null) {
                etFechaGasto.setText(fecha);
                try {
                    String[] partes = fecha.split("-");
                    if (partes.length == 3) {
                        selectedDate.set(
                                Integer.parseInt(partes[0]),
                                Integer.parseInt(partes[1]) - 1,
                                Integer.parseInt(partes[2])
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al parsear fecha: " + e.getMessage());
                }
            }

            // Seleccionar tipo de gasto en spinner
            if (nombreGasto != null) {
                for (int i = 0; i < spinnerNombreGasto.getCount(); i++) {
                    if (spinnerNombreGasto.getItemAtPosition(i).toString().equals(nombreGasto)) {
                        spinnerNombreGasto.setSelection(i);
                        break;
                    }
                }
            }

            // Actualizar subtítulo
            if (fecha != null) {
                tvSubtitulo.setText("Editando gasto registrado - " + fecha);
            }

            Log.d(TAG, "✅ Datos del gasto cargados correctamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al mostrar datos: " + e.getMessage());
            Toast.makeText(this, "Error al cargar información", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validarFormulario() {
        String descripcion = etDescripcionGasto.getText().toString().trim();
        String montoStr = etMontoGasto.getText().toString().trim();
        String fecha = etFechaGasto.getText().toString().trim();

        boolean isValid = true;

        // Limpiar errores previos
        etDescripcionGasto.setError(null);
        etMontoGasto.setError(null);
        etFechaGasto.setError(null);

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

        Log.d(TAG, "✅ Validación de formulario: " + (isValid ? "PASÓ" : "FALLÓ"));
        return isValid;
    }

    private void guardarCambios() {
        // Obtener valores del formulario - MISMOS que AgregarGasto
        String nombreGasto = spinnerNombreGasto.getSelectedItem().toString();
        String descripcion = etDescripcionGasto.getText().toString().trim();
        String montoStr = etMontoGasto.getText().toString().trim();
        String unidad = etUnidadGasto.getText().toString().trim();
        String fecha = etFechaGasto.getText().toString().trim();

        try {
            double monto = Double.parseDouble(montoStr);

            // Si unidad está vacía, establecer como null (igual que AgregarGasto)
            if (unidad.isEmpty()) {
                unidad = null;
            }

            // Crear objeto gasto con la MISMA estructura que AgregarGasto
            Map<String, Object> gastoActualizado = new HashMap<>();
            gastoActualizado.put("ID_ETAPA", etapaId);
            gastoActualizado.put("Nombre_Gasto", nombreGasto);
            gastoActualizado.put("Categoria", nombreGasto); // Mismo que Nombre_Gasto
            gastoActualizado.put("Descripcion", descripcion);
            gastoActualizado.put("Monto", monto);
            gastoActualizado.put("Unidad", unidad);
            gastoActualizado.put("Fecha", fecha);

            Log.d(TAG, "🔄 Actualizando gasto: " + gastoId);
            Log.d(TAG, "   - Nombre_Gasto: " + nombreGasto);
            Log.d(TAG, "   - Monto: $" + monto);
            Log.d(TAG, "   - Fecha: " + fecha);

            // Mostrar progreso
            btnGuardarCambios.setText("Guardando...");
            btnGuardarCambios.setEnabled(false);
            btnEliminarGasto.setEnabled(false);

            // Actualizar en Firestore
            db.collection("gastos").document(gastoId)
                    .update(gastoActualizado)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Gasto actualizado correctamente");

                        // Actualizar totales del ciclo si el monto cambió
                        actualizarTotalesCicloDespuesEdicion(monto);

                        // Preparar resultado
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("GASTO_ACTUALIZADO", true);
                        resultIntent.putExtra("GASTO_ID", gastoId);
                        setResult(RESULT_OK, resultIntent);

                        Toast.makeText(this,
                                "✅ Gasto actualizado correctamente\n" +
                                        "Tipo: " + nombreGasto + "\n" +
                                        "Monto: $" + String.format("%,.0f", monto),
                                Toast.LENGTH_LONG).show();

                        // Cerrar después de un breve delay
                        new android.os.Handler().postDelayed(() -> {
                            finish();
                        }, 1500);

                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error al actualizar gasto: " + e.getMessage());
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                        // Restaurar botones
                        btnGuardarCambios.setText("💾 Guardar Cambios");
                        btnGuardarCambios.setEnabled(true);
                        btnEliminarGasto.setEnabled(true);
                    });

        } catch (NumberFormatException e) {
            Log.e(TAG, "❌ Error en formato numérico: " + e.getMessage());
            Toast.makeText(this, "Error en el formato del monto", Toast.LENGTH_SHORT).show();
            btnGuardarCambios.setText("💾 Guardar Cambios");
            btnGuardarCambios.setEnabled(true);
            btnEliminarGasto.setEnabled(true);
        }
    }

    private void actualizarTotalesCicloDespuesEdicion(double nuevoMonto) {
        // Para la edición, necesitamos obtener el monto anterior y recalcular
        if (cicloId != null) {
            // Primero obtenemos el monto antiguo
            db.collection("gastos").document(gastoId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot gastoDoc = task.getResult();
                            Double montoAnterior = gastoDoc.getDouble("Monto");

                            if (montoAnterior != null) {
                                // Calcular diferencia
                                double diferencia = nuevoMonto - montoAnterior;

                                if (diferencia != 0) {
                                    actualizarTotalesCiclo(diferencia);
                                }
                            }
                        }
                    });
        }
    }

    private void actualizarTotalesCiclo(double diferencia) {
        // Obtener el ciclo actual para actualizar totales
        db.collection("ciclos").document(cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot cicloDoc = task.getResult();
                        Double gastoActual = cicloDoc.getDouble("Gasto_Total");
                        Double ventasActual = cicloDoc.getDouble("Ventas_Total");

                        // Calcular nuevos totales
                        double nuevoGastoTotal = (gastoActual != null ? gastoActual : 0.0) + diferencia;
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

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Gasto")
                .setMessage("¿Está seguro de eliminar este gasto?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarGasto())
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void eliminarGasto() {
        Log.d(TAG, "🗑️ Eliminando gasto: " + gastoId);

        // Primero obtenemos el monto para actualizar los totales
        db.collection("gastos").document(gastoId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot gastoDoc = task.getResult();
                        Double monto = gastoDoc.getDouble("Monto");

                        // Mostrar progreso
                        btnEliminarGasto.setText("Eliminando...");
                        btnEliminarGasto.setEnabled(false);
                        btnGuardarCambios.setEnabled(false);

                        // Eliminar el documento
                        db.collection("gastos").document(gastoId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Gasto eliminado correctamente");

                                    // Actualizar totales del ciclo (restar el monto)
                                    if (monto != null && cicloId != null) {
                                        actualizarTotalesCiclo(-monto);
                                    }

                                    // Preparar resultado
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("GASTO_ELIMINADO", true);
                                    resultIntent.putExtra("GASTO_ID", gastoId);
                                    setResult(RESULT_OK, resultIntent);

                                    Toast.makeText(this, "✅ Gasto eliminado correctamente", Toast.LENGTH_LONG).show();

                                    // Cerrar actividad
                                    new android.os.Handler().postDelayed(() -> {
                                        finish();
                                    }, 1000);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Error al eliminar gasto: " + e.getMessage());
                                    Toast.makeText(this, "❌ Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    // Restaurar botones
                                    btnEliminarGasto.setText("🗑️ Eliminar Gasto");
                                    btnEliminarGasto.setEnabled(true);
                                    btnGuardarCambios.setEnabled(true);
                                });
                    }
                });
    }

}