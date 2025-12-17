package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditarCicloActivity extends AppCompatActivity {

    private TextView tvSubtitulo;
    private EditText etFechaInicio, etFechaFin;
    private EditText etNombreCiclo, etDescripcionCiclo;
    private RadioButton radioActivo, radioTerminado;
    private Button btnActualizarCiclo, btnCancelar, btnEliminarCiclo;

    private FirebaseFirestore db;
    private String cicloId;
    private String loteId;
    private String nombreCicloActual;

    private static final String TAG = "EditarCicloActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_ciclo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Recibir ID del ciclo
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        if (cicloId == null) {
            Toast.makeText(this, "Error: No se recibió el ciclo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "🔍 Editando ciclo: " + cicloId);

        inicializarVistas();
        cargarDatosCiclo();
        configurarEventos();
    }

    private void inicializarVistas() {
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);
        etNombreCiclo = findViewById(R.id.etNombreCiclo);
        etDescripcionCiclo = findViewById(R.id.etDescripcionCiclo);
        radioActivo = findViewById(R.id.radioActivo);
        radioTerminado = findViewById(R.id.radioTerminado);
        btnActualizarCiclo = findViewById(R.id.btnActualizarCiclo);

    }

    private void cargarDatosCiclo() {
        Log.d(TAG, "🔄 Cargando datos del ciclo: " + cicloId);

        db.collection("ciclos").document(cicloId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var document = task.getResult();
                        if (document.exists()) {
                            String nombreCiclo = document.getString("Nombre_Ciclo");
                            String estado = document.getString("Estado");
                            String fechaInicio = document.getString("Fecha_Inicio");
                            String fechaFin = document.getString("Fecha_Fin");
                            String descripcion = document.getString("Descripcion");

                            // Guardar nombre actual para el subtítulo
                            nombreCicloActual = nombreCiclo;

                            // Llenar campos con datos actuales
                            if (tvSubtitulo != null) {
                                tvSubtitulo.setText("Editando: " + (nombreCiclo != null ? nombreCiclo : "Sin nombre"));
                            }

                            if (etNombreCiclo != null) {
                                etNombreCiclo.setText(nombreCiclo);
                            }

                            if (etFechaInicio != null && fechaInicio != null) {
                                etFechaInicio.setText(fechaInicio);
                            }

                            if (etFechaFin != null && fechaFin != null) {
                                etFechaFin.setText(fechaFin);
                            }

                            if (etDescripcionCiclo != null) {
                                etDescripcionCiclo.setText(descripcion != null ? descripcion : "");
                            }

                            // Configurar estado
                            if (estado != null) {
                                if (estado.equals("Activo")) {
                                    radioActivo.setChecked(true);
                                } else if (estado.equals("Terminado")) {
                                    radioTerminado.setChecked(true);
                                }
                            } else {
                                radioActivo.setChecked(true); // Por defecto
                            }

                            Log.d(TAG, "✅ Datos cargados correctamente");

                        } else {
                            Log.e(TAG, "❌ El ciclo no existe");
                            Toast.makeText(this, "Error: El ciclo no existe", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e(TAG, "❌ Error al cargar ciclo: " + task.getException());
                        Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void configurarEventos() {
        // Selectores de fecha
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(etFechaInicio));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(etFechaFin));

        // Botón Actualizar
        btnActualizarCiclo.setOnClickListener(v -> {
            if (validarFormulario()) {
                actualizarCiclo();
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
                    String fechaSeleccionada = String.format("%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    editText.setText(fechaSeleccionada);
                },
                year,
                month,
                day
        );
        datePicker.show();
    }

    private boolean validarFormulario() {
        String nombre = etNombreCiclo.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();

        boolean isValid = true;

        if (nombre.isEmpty()) {
            etNombreCiclo.setError("Ingresa el nombre del ciclo");
            isValid = false;
        }

        if (fechaInicio.isEmpty()) {
            etFechaInicio.setError("Selecciona la fecha de inicio");
            isValid = false;
        }

        if (fechaFin.isEmpty()) {
            etFechaFin.setError("Selecciona la fecha de fin");
            isValid = false;
        }

        // Validar que fecha fin sea posterior a fecha inicio
        if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            if (fechaFin.compareTo(fechaInicio) < 0) {
                etFechaFin.setError("La fecha fin debe ser posterior a la fecha inicio");
                isValid = false;
            }
        }

        return isValid;
    }

    private String obtenerEstadoCiclo() {
        return radioActivo.isChecked() ? "Activo" : "Terminado";
    }

    private void actualizarCiclo() {
        // Obtener valores de los campos
        String nombreCiclo = etNombreCiclo.getText().toString().trim();
        String descripcion = etDescripcionCiclo.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();
        String estado = obtenerEstadoCiclo();

        Map<String, Object> updates = new HashMap<>();
        updates.put("Nombre_Ciclo", nombreCiclo);
        updates.put("Descripcion", descripcion);
        updates.put("Fecha_Inicio", fechaInicio);
        updates.put("Fecha_Fin", fechaFin);
        updates.put("Estado", estado);

        Log.d(TAG, "🎯 Actualizando ciclo: " + updates);

        db.collection("ciclos").document(cicloId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Ciclo actualizado exitosamente");

                    Toast.makeText(this, "✅ Ciclo '" + nombreCiclo + "' actualizado",
                            Toast.LENGTH_LONG).show();

                    // Enviar resultado a DetalleCicloActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("CICLO_ACTUALIZADO", true);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al actualizar ciclo: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al actualizar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }


    private boolean hayCambiosSinGuardar() {
        // Verificar si los campos actuales son diferentes a los originales
        // Esta es una implementación simple, podrías mejorarla
        return etNombreCiclo.getText().toString().trim().length() > 0 ||
                etDescripcionCiclo.getText().toString().trim().length() > 0;
    }
}