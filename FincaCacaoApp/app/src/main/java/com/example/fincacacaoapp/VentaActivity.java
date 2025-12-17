package com.example.fincacacaoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VentaActivity extends AppCompatActivity {

    private EditText etKgVendidos, etPrecioKg, etIngresos, etCliente;
    private Button btnCalcularVenta, btnGuardarVenta, btnBackVenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venta);

        etKgVendidos = findViewById(R.id.etKgVendidos);
        etPrecioKg = findViewById(R.id.etPrecioKg);
        etIngresos = findViewById(R.id.etIngresos);
        etCliente = findViewById(R.id.etCliente);
        btnCalcularVenta = findViewById(R.id.btnCalcularVenta);
        btnGuardarVenta = findViewById(R.id.btnGuardarVenta);
        btnBackVenta = findViewById(R.id.btnBackVenta);

        btnCalcularVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calcular ingresos automáticamente
                String kgText = etKgVendidos.getText().toString();
                String precioText = etPrecioKg.getText().toString();

                if (!kgText.isEmpty() && !precioText.isEmpty()) {
                    double kg = Double.parseDouble(kgText);
                    double precio = Double.parseDouble(precioText);
                    double ingresos = kg * precio;
                    etIngresos.setText(String.valueOf(ingresos));
                }
            }
        });

        btnGuardarVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(VentaActivity.this, "Datos de venta guardados", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}