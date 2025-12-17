package com.example.fincacacaoapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AgregarVentaActivity extends AppCompatActivity {

    private EditText etCantidad, etPrecioUnitario, etFechaVenta, etCliente;
    private Spinner spinnerMetodoPago;
    private TextView tvTotal, tvTitulo;
    private Button  btnAgregarVenta;

    private FirebaseFirestore db;
    private String etapaId;
    private String cicloId;
    private String loteId;

    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    // Métodos de pago disponibles
    private final String[] METODOS_PAGO = {"Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "Transferencia", "Cheque"};

    private static final String TAG = "AgregarVentaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_venta);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Obtener IDs
        etapaId = getIntent().getStringExtra("ETAPA_ID");
        cicloId = getIntent().getStringExtra("CICLO_ID");
        loteId = getIntent().getStringExtra("LOTE_ID");

        Log.d(TAG, "📥 PARÁMETROS RECIBIDOS:");
        Log.d(TAG, "   - ETAPA_ID: " + etapaId);
        Log.d(TAG, "   - CICLO_ID: " + cicloId);
        Log.d(TAG, "   - LOTE_ID: " + loteId);

        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if (etapaId == null) {
            Toast.makeText(this, "Error: No se recibió la etapa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        inicializarVistas();
        configurarSpinner();
        configurarEventos();
        configurarListeners();
    }

    private void inicializarVistas() {
        etCantidad = findViewById(R.id.etCantidad);
        etPrecioUnitario = findViewById(R.id.etPrecioUnitario);
        etFechaVenta = findViewById(R.id.etFechaVenta);
        etCliente = findViewById(R.id.etCliente);
        spinnerMetodoPago = findViewById(R.id.spinnerMetodoPago);
        tvTotal = findViewById(R.id.tvTotal);
        tvTitulo = findViewById(R.id.tvTitulo);
        btnAgregarVenta = findViewById(R.id.btnAgregarVenta);

        // Configurar título
        tvTitulo.setText("Registrar Venta");
        btnAgregarVenta.setText("Agregar Venta");
    }

    private void configurarSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, METODOS_PAGO);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMetodoPago.setAdapter(adapter);
    }

    private void configurarEventos() {
        etFechaVenta.setOnClickListener(v -> mostrarDatePicker());

        btnAgregarVenta.setOnClickListener(v -> {
            if (validarFormulario()) {
                agregarVentaAFirestore();
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
                    String fechaSeleccionada = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    etFechaVenta.setText(fechaSeleccionada);
                    Log.d(TAG, "📅 Fecha seleccionada: " + fechaSeleccionada);
                },
                year,
                month,
                day
        );
        datePicker.show();
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
    private void configurarListeners() {
        // Calcular automáticamente al cambiar cantidad o precio
        etCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etPrecioUnitario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private boolean validarFormulario() {
        String cantidad = etCantidad.getText().toString().trim();
        String precio = etPrecioUnitario.getText().toString().trim();
        String fecha = etFechaVenta.getText().toString().trim();
        String cliente = etCliente.getText().toString().trim();

        boolean isValid = true;

        // Limpiar errores previos
        etCantidad.setError(null);
        etPrecioUnitario.setError(null);
        etFechaVenta.setError(null);
        etCliente.setError(null);

        if (cantidad.isEmpty()) {
            etCantidad.setError("Ingresa la cantidad");
            isValid = false;
        } else {
            try {
                // ✅ Limpiar antes de validar
                String cantidadLimpia = cantidad.replace(".", "").replace(",", ".");
                double cant = Double.parseDouble(cantidadLimpia);
                if (cant <= 0) {
                    etCantidad.setError("La cantidad debe ser mayor a 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etCantidad.setError("Cantidad inválida. Ej: 10.5 o 10,5");
                isValid = false;
            }
        }

        if (precio.isEmpty()) {
            etPrecioUnitario.setError("Ingresa el precio unitario");
            isValid = false;
        } else {
            try {
                // ✅ Limpiar antes de validar
                String precioLimpio = precio.replace(".", "").replace(",", ".");
                double prec = Double.parseDouble(precioLimpio);
                if (prec <= 0) {
                    etPrecioUnitario.setError("El precio debe ser mayor a 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etPrecioUnitario.setError("Precio inválido. Ej: 1500.50 o 1500,50");
                isValid = false;
            }
        }

        if (fecha.isEmpty()) {
            etFechaVenta.setError("Selecciona la fecha de venta");
            isValid = false;
        }

        if (cliente.isEmpty()) {
            etCliente.setError("Ingresa el nombre del cliente");
            isValid = false;
        }

        Log.d(TAG, "✅ Validación de formulario: " + (isValid ? "PASÓ" : "FALLÓ"));
        return isValid;
    }

    private void agregarVentaAFirestore() {
        String cantidadStr = etCantidad.getText().toString().trim();
        String precioStr = etPrecioUnitario.getText().toString().trim();
        String fecha = etFechaVenta.getText().toString().trim();
        String cliente = etCliente.getText().toString().trim();
        String metodoPago = spinnerMetodoPago.getSelectedItem().toString();

        // ✅ CORRECCIÓN: NO usar el total formateado, calcularlo directamente
        try {
            // 1. Limpiar los números (quitar comas de miles, estandarizar decimales)
            cantidadStr = cantidadStr.replace(".", "").replace(",", ".");
            precioStr = precioStr.replace(".", "").replace(",", ".");

            // 2. Convertir a double
            double cantidad = Double.parseDouble(cantidadStr);
            double precioUnitario = Double.parseDouble(precioStr);

            // 3. Calcular total directamente
            double total = cantidad * precioUnitario;

            // 4. Log para depurar
            Log.d(TAG, "📊 Valores convertidos:");
            Log.d(TAG, "   - Cantidad: " + cantidad);
            Log.d(TAG, "   - Precio Unitario: " + precioUnitario);
            Log.d(TAG, "   - Total: " + total);

            // Generar ID único para la venta
            String idVenta = UUID.randomUUID().toString();

            Map<String, Object> venta = new HashMap<>();
            venta.put("ID_Venta", idVenta);
            venta.put("ID_Etapa", etapaId);
            venta.put("Cantidad", cantidad);
            venta.put("Precio_Unitario", precioUnitario);
            venta.put("Total", total);
            venta.put("Fecha", fecha);
            venta.put("Cliente", cliente);
            venta.put("Metodo_Pago", metodoPago);
            venta.put("Fecha_Registro", Calendar.getInstance().getTime());

            Log.d(TAG, "➕ Agregando venta - ID: " + idVenta);

            db.collection("ventas")
                    .document(idVenta)
                    .set(venta)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Venta agregada. ID: " + idVenta);

                        String mensaje = "✅ Venta registrada correctamente\n";
                        mensaje += "Cliente: " + cliente + "\n";
                        mensaje += "Total: " + String.format("$%,.0f", total);

                        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();

                        // Regresar a DetalleEtapaActivity
                        regresarADetalleEtapa();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error al agregar venta: " + e.getMessage());
                        Toast.makeText(this, "❌ Error al registrar venta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Log.e(TAG, "❌ Error en formato de números: " + e.getMessage());
            Log.e(TAG, "   - cantidadStr original: " + etCantidad.getText().toString());
            Log.e(TAG, "   - precioStr original: " + etPrecioUnitario.getText().toString());
            Toast.makeText(this, "Error en los valores numéricos: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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