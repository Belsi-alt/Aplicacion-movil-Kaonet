package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditarVentaActivity extends AppCompatActivity {

    // Views
    private EditText etCliente, etCantidad, etPrecioUnitario, etFechaVenta;
    private Spinner spinnerMetodoPago;
    private TextView tvTotal, tvIdVenta, tvInfoAdicional;
    private Button btnCalcular, btnGuardarCambios, btnEliminarVenta;

    // Firestore
    private FirebaseFirestore db;

    // Variables
    private String ventaId; // ID del documento en Firestore
    private String ventaOriginalId; // ID visual para mostrar

    // Formateadores
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_venta);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Obtener ID de la venta del Intent
        Intent intent = getIntent();
        ventaId = intent.getStringExtra("VENTA_ID");
        ventaOriginalId = intent.getStringExtra("VENTA_ORIGINAL_ID");

        // Inicializar formateadores
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Inicializar vistas
        inicializarVistas();

        // Configurar spinner
        configurarSpinnerMetodoPago();

        // Configurar listeners
        configurarListeners();

        // Cargar datos de la venta
        cargarDatosVenta();
    }

    private void inicializarVistas() {
        etCliente = findViewById(R.id.etCliente);
        etCantidad = findViewById(R.id.etCantidad);
        etPrecioUnitario = findViewById(R.id.etPrecioUnitario);
        etFechaVenta = findViewById(R.id.etFechaVenta);
        spinnerMetodoPago = findViewById(R.id.spinnerMetodoPago);
        tvTotal = findViewById(R.id.tvTotal);
        tvIdVenta = findViewById(R.id.tvIdVenta);
        tvInfoAdicional = findViewById(R.id.tvInfoAdicional);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnEliminarVenta = findViewById(R.id.btnEliminarVenta);


    }

    private void configurarSpinnerMetodoPago() {
        String[] metodosPago = {"Efectivo", "Transferencia", "Tarjeta", "Crédito", "Mixto"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, metodosPago);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMetodoPago.setAdapter(adapter);
    }

    private void configurarListeners() {
        // Calcular automáticamente al cambiar cantidad o precio
        etCantidad.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etPrecioUnitario.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        });



        // Selector de fecha
        etFechaVenta.setOnClickListener(v -> mostrarSelectorFecha());

        // Botón guardar cambios
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());

        // Botón eliminar venta
        btnEliminarVenta.setOnClickListener(v -> confirmarEliminacion());
    }

    private void cargarDatosVenta() {
        if (ventaId == null || ventaId.isEmpty()) {
            Toast.makeText(this, "Error: No se recibió la venta", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("ventas").document(ventaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mostrarDatosVenta(documentSnapshot);
                    } else {
                        Toast.makeText(this, "La venta no existe", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void mostrarDatosVenta(DocumentSnapshot document) {
        try {
            // Obtener datos
            String cliente = document.getString("Cliente");
            Double cantidad = document.getDouble("Cantidad");
            Double precioUnitario = document.getDouble("Precio_Unitario");
            String fecha = document.getString("Fecha");
            String metodoPago = document.getString("Metodo_Pago");
            Double total = document.getDouble("Total");

            // Mostrar en campos
            etCliente.setText(cliente != null ? cliente : "");

            if (cantidad != null) {
                etCantidad.setText(String.valueOf(cantidad));
            }

            if (precioUnitario != null) {
                etPrecioUnitario.setText(String.valueOf(precioUnitario));
            }

            etFechaVenta.setText(fecha != null ? fecha : "");

            // Seleccionar método de pago
            if (metodoPago != null) {
                for (int i = 0; i < spinnerMetodoPago.getCount(); i++) {
                    if (spinnerMetodoPago.getItemAtPosition(i).toString().equals(metodoPago)) {
                        spinnerMetodoPago.setSelection(i);
                        break;
                    }
                }
            }

            // Mostrar total
            if (total != null) {
                tvTotal.setText(currencyFormat.format(total));
            }

            // Actualizar info adicional
            tvInfoAdicional.setText("✏️ Editando venta registrada - " +
                    (fecha != null ? fecha : "Fecha no disponible"));

        } catch (Exception e) {
            Toast.makeText(this, "Error al mostrar datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void calcularTotal() {
        try {
            String cantidadStr = etCantidad.getText().toString().trim();
            String precioStr = etPrecioUnitario.getText().toString().trim();

            if (!cantidadStr.isEmpty() && !precioStr.isEmpty()) {
                double cantidad = Double.parseDouble(cantidadStr);
                double precio = Double.parseDouble(precioStr);
                double total = cantidad * precio;

                tvTotal.setText(currencyFormat.format(total));
            } else {
                tvTotal.setText("$0");
            }
        } catch (Exception e) {
            tvTotal.setText("$0");
        }
    }

    private void mostrarSelectorFecha() {
        final Calendar calendar = Calendar.getInstance();
        int año = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, añoSeleccionado, mesSeleccionado, diaSeleccionado) -> {
                    calendar.set(añoSeleccionado, mesSeleccionado, diaSeleccionado);
                    etFechaVenta.setText(dateFormat.format(calendar.getTime()));
                }, año, mes, dia);

        datePickerDialog.show();
    }

    private void guardarCambios() {
        if (!validarCampos()) {
            return;
        }

        // Obtener valores
        String cliente = etCliente.getText().toString().trim();
        double cantidad = Double.parseDouble(etCantidad.getText().toString().trim());
        double precioUnitario = Double.parseDouble(etPrecioUnitario.getText().toString().trim());
        String fecha = etFechaVenta.getText().toString().trim();
        String metodoPago = spinnerMetodoPago.getSelectedItem().toString();
        double total = cantidad * precioUnitario;

        // Crear mapa con datos actualizados
        Map<String, Object> ventaActualizada = new HashMap<>();
        ventaActualizada.put("Cliente", cliente);
        ventaActualizada.put("Cantidad", cantidad);
        ventaActualizada.put("Precio_Unitario", precioUnitario);
        ventaActualizada.put("Total", total);
        ventaActualizada.put("Fecha", fecha);
        ventaActualizada.put("Metodo_Pago", metodoPago);
        ventaActualizada.put("Fecha_Actualizacion", Calendar.getInstance().getTime());

        // Mostrar progreso
        btnGuardarCambios.setText("Guardando...");
        btnGuardarCambios.setEnabled(false);

        // Actualizar en Firestore
        db.collection("ventas").document(ventaId)
                .update(ventaActualizada)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Venta actualizada", Toast.LENGTH_SHORT).show();

                    // Devolver resultado
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("VENTA_ACTUALIZADA", true);
                    resultIntent.putExtra("VENTA_ID", ventaId);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGuardarCambios.setText("💾 Guardar Cambios");
                    btnGuardarCambios.setEnabled(true);
                });
    }

    private boolean validarCampos() {
        if (etCliente.getText().toString().trim().isEmpty()) {
            etCliente.setError("Ingrese el cliente");
            return false;
        }
        if (etCantidad.getText().toString().trim().isEmpty()) {
            etCantidad.setError("Ingrese la cantidad");
            return false;
        }
        if (etPrecioUnitario.getText().toString().trim().isEmpty()) {
            etPrecioUnitario.setError("Ingrese el precio");
            return false;
        }
        if (etFechaVenta.getText().toString().trim().isEmpty()) {
            etFechaVenta.setError("Seleccione la fecha");
            return false;
        }
        return true;
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Venta")
                .setMessage("¿Está seguro de eliminar esta venta?\nEsta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarVenta())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarVenta() {
        // Mostrar progreso
        btnEliminarVenta.setText("Eliminando...");
        btnEliminarVenta.setEnabled(false);

        db.collection("ventas").document(ventaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Venta eliminada", Toast.LENGTH_SHORT).show();

                    // Devolver resultado
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("VENTA_ELIMINADA", true);
                    resultIntent.putExtra("VENTA_ID", ventaId);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnEliminarVenta.setText("🗑️ Eliminar Venta");
                    btnEliminarVenta.setEnabled(true);
                });
    }


}