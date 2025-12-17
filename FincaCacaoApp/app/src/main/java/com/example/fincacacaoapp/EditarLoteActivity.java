package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditarLoteActivity extends AppCompatActivity {

    private EditText etFecha;
    private EditText etNombreLote;
    private EditText etCantidad;
    private Switch switchEstado;
    private Button btnActualizar;

    // Firebase
    private FirebaseFirestore db;

    // Variables para almacenar datos del lote
    private String loteId;
    private String nombreLote;
    private String cantidadPlantas;
    private String fechaSiembra;
    private boolean estadoActivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_lote);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Conectar variables con el XML
        etFecha = findViewById(R.id.etFecha);
        etNombreLote = findViewById(R.id.etNombreLote);
        etCantidad = findViewById(R.id.etCantidad);
        switchEstado = findViewById(R.id.switchEstado);
        btnActualizar = findViewById(R.id.btnActualizarLote);

        // Recibir datos del lote a editar
        recibirDatosLote();

        // Precargar datos en el formulario
        precargarDatosFormulario();

        // Configurar el selector de fecha
        etFecha.setOnClickListener(v -> mostrarDatePicker());

        // Configurar botón Actualizar
        btnActualizar.setOnClickListener(v -> {
            if (validarFormulario()) {
                actualizarLoteEnFirestore();
            }
        });
    }

    private void recibirDatosLote() {
        Intent intent = getIntent();
        loteId = intent.getStringExtra("LOTE_ID");

        if (loteId == null) {
            Toast.makeText(this, "Error: No se recibió el lote", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar datos reales desde Firestore
        cargarDatosLoteDesdeFirestore();
    }

    private void cargarDatosLoteDesdeFirestore() {
        db.collection("lotes").document(loteId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        var document = task.getResult();

                        nombreLote = document.getString("Nombre_Lote");
                        Long tamanoLong = document.getLong("Tamano");
                        cantidadPlantas = tamanoLong != null ? tamanoLong.toString() : "0";
                        fechaSiembra = document.getString("Fecha_Creacion");
                        String estado = document.getString("Estado");
                        estadoActivo = "Activo".equals(estado);

                        precargarDatosFormulario();

                    } else {
                        Toast.makeText(this, "Error al cargar datos del lote", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void precargarDatosFormulario() {
        if (etNombreLote != null && nombreLote != null) {
            etNombreLote.setText(nombreLote);
        }
        if (etCantidad != null && cantidadPlantas != null) {
            etCantidad.setText(cantidadPlantas);
        }
        if (etFecha != null && fechaSiembra != null) {
            etFecha.setText(fechaSiembra);
        }
        if (switchEstado != null) {
            switchEstado.setChecked(estadoActivo);
        }
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Formato YYYY-MM-DD para Firestore
                    String fechaSeleccionada = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    etFecha.setText(fechaSeleccionada);
                },
                year,
                month,
                day
        );
        datePicker.show();
    }

    private boolean validarFormulario() {
        String nombre = etNombreLote.getText().toString().trim();
        String cantidad = etCantidad.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        boolean isValid = true;

        if (nombre.isEmpty()) {
            etNombreLote.setError("Ingresa el nombre del lote");
            isValid = false;
        }

        if (cantidad.isEmpty()) {
            etCantidad.setError("Ingresa la cantidad de plantas");
            isValid = false;
        } else {
            try {
                Integer.parseInt(cantidad);
            } catch (NumberFormatException e) {
                etCantidad.setError("La cantidad debe ser un número");
                isValid = false;
            }
        }

        if (fecha.isEmpty()) {
            etFecha.setError("Selecciona una fecha");
            isValid = false;
        }

        return isValid;
    }

    private void actualizarLoteEnFirestore() {
        // Obtener los nuevos valores del formulario
        String nuevoNombre = etNombreLote.getText().toString().trim();
        String nuevaCantidadStr = etCantidad.getText().toString().trim();
        String nuevaFecha = etFecha.getText().toString().trim();
        boolean nuevoEstado = switchEstado.isChecked();

        // Convertir cantidad a número
        int nuevaCantidad;
        try {
            nuevaCantidad = Integer.parseInt(nuevaCantidadStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "La cantidad debe ser un número válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear mapa con los datos actualizados
        Map<String, Object> updates = new HashMap<>();
        updates.put("Nombre_Lote", nuevoNombre);
        updates.put("Tamano", nuevaCantidad);
        updates.put("Fecha_Creacion", nuevaFecha);
        updates.put("Estado", nuevoEstado ? "Activo" : "Inactivo");

        // Actualizar en Firestore
        db.collection("lotes").document(loteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditarLoteActivity.this, "✅ Lote actualizado exitosamente", Toast.LENGTH_SHORT).show();

                    // Regresar al DetalleLoteActivity
                    Intent intent = new Intent(EditarLoteActivity.this, DetalleLoteActivity.class);
                    intent.putExtra("LOTE_ID", loteId);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditarLoteActivity.this, "❌ Error al actualizar lote", Toast.LENGTH_SHORT).show();
                });
    }
}