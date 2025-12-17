package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CrearLoteActivity extends AppCompatActivity {

    private EditText etFecha;
    private EditText etNombreLote;
    private EditText etCantidad;
    private Switch switchEstado;
    private Button btnAgregar;

    private FirebaseFirestore db;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_lote);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();

        // Conectar variables con el XML
        etFecha = findViewById(R.id.etFecha);
        etNombreLote = findViewById(R.id.etNombreLote);
        etCantidad = findViewById(R.id.etCantidad);
        switchEstado = findViewById(R.id.switchEstado);
        btnAgregar = findViewById(R.id.btnAgregarLote);

        // Configurar el selector de fecha
        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDatePicker();
            }
        });

        // Configurar botón Agregar - Guardar en Firestore
        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validarFormulario()) {
                    guardarLoteEnFirestore();
                }
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
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        String fechaSeleccionada = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etFecha.setText(fechaSeleccionada);
                    }
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

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del lote", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (cantidad.isEmpty()) {
            Toast.makeText(this, "Ingresa la cantidad de plantas", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fecha.isEmpty()) {
            Toast.makeText(this, "Selecciona una fecha", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void guardarLoteEnFirestore() {
        // Obtener datos del formulario
        String nombreLote = etNombreLote.getText().toString().trim();
        String cantidadStr = etCantidad.getText().toString().trim();
        boolean estado = switchEstado.isChecked();

        // Convertir cantidad a número
        double tamano;
        try {
            tamano = Double.parseDouble(cantidadStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "La cantidad debe ser un número válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Formatear fecha para Firestore
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaCreacion = dateFormat.format(selectedDate.getTime());

        // Obtener ID del usuario actual (teléfono)
        String idUsuario = obtenerIdUsuarioActual();

        // Crear objeto lote según MER
        Map<String, Object> lote = new HashMap<>();
        lote.put("Nombre_Lote", nombreLote);
        lote.put("Tamano", tamano);
        lote.put("Fecha_Creacion", fechaCreacion);
        lote.put("Estado", estado ? "Activo" : "Inactivo");
        lote.put("id_usuario", idUsuario);
        lote.put("Cantidad_Plantas", tamano); // Mantener compatibilidad

        // Guardar en Firestore
        db.collection("lotes")
                .add(lote)
                .addOnSuccessListener(documentReference -> {
                    String loteId = documentReference.getId();

                    Toast.makeText(CrearLoteActivity.this, "Lote guardado exitosamente", Toast.LENGTH_SHORT).show();

                    // Redireccionar al Home
                    Intent intent = new Intent(CrearLoteActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CrearLoteActivity.this, "Error al guardar lote: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String obtenerIdUsuarioActual() {
        // Obtener el teléfono del usuario desde SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return sharedPreferences.getString("telefono", "usuario_desconocido");
    }
}