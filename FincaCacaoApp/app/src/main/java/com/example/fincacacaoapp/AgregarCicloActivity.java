package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AgregarCicloActivity extends AppCompatActivity {

    private EditText etFechaInicio, etFechaFin;
    private EditText etNombreCiclo, etDescripcionCiclo;
    private RadioButton radioActivo, radioTerminado;
    private Button btnAgregarCiclo;

    private FirebaseFirestore db;
    private String loteId;

    private static final String TAG = "AgregarCicloActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_ciclo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Recibir ID del lote
        loteId = getIntent().getStringExtra("LOTE_ID");
        if (loteId == null) {
            Toast.makeText(this, "Error: No se recibió el lote", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "🔍 Iniciando AgregarCiclo para lote: " + loteId);

        inicializarVistas();
        configurarEventos();
    }

    private void inicializarVistas() {
        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);
        etNombreCiclo = findViewById(R.id.etNombreCiclo);
        etDescripcionCiclo = findViewById(R.id.etDescripcionCiclo);
        radioActivo = findViewById(R.id.radioActivo);
        radioTerminado = findViewById(R.id.radioTerminado);
        btnAgregarCiclo = findViewById(R.id.btnAgregarCiclo);

        // Establecer estado por defecto
        radioActivo.setChecked(true);
    }

    private void configurarEventos() {
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(etFechaInicio));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(etFechaFin));

        btnAgregarCiclo.setOnClickListener(v -> {
            if (validarFormulario()) {
                // SOLUCIÓN SIMPLE: Siempre empezar con ciclo 1
                // El usuario puede editar el número después si es necesario
                crearNuevoCiclo(1);
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
                    // Formato YYYY-MM-DD para Firestore
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

    private void crearNuevoCiclo(int numeroCiclo) {
        // Obtener valores de los campos
        String nombreCiclo = etNombreCiclo.getText().toString().trim();
        String descripcion = etDescripcionCiclo.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();
        String estado = obtenerEstadoCiclo();

        Map<String, Object> ciclo = new HashMap<>();
        ciclo.put("ID_LOTE", loteId);
        ciclo.put("Numero_Ciclo", numeroCiclo);
        ciclo.put("Nombre_Ciclo", nombreCiclo);
        ciclo.put("Descripcion", descripcion);
        ciclo.put("Fecha_Inicio", fechaInicio);
        ciclo.put("Fecha_Fin", fechaFin);
        ciclo.put("Estado", estado);
        ciclo.put("Ganancia_Total", 0.0);

        Log.d(TAG, "🎯 Creando ciclo: " + ciclo);

        db.collection("ciclos")
                .add(ciclo)
                .addOnSuccessListener(documentReference -> {
                    String cicloId = documentReference.getId();
                    Log.d(TAG, "✅ Ciclo creado exitosamente. ID: " + cicloId);

                    Toast.makeText(this, "✅ Ciclo '" + nombreCiclo + "' creado correctamente", Toast.LENGTH_LONG).show();

                    // Regresar a DetalleLoteActivity
                    Intent intent = new Intent(this, DetalleLoteActivity.class);
                    intent.putExtra("LOTE_ID", loteId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al crear ciclo: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al crear ciclo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}