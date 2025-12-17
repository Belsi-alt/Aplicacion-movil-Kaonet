package com.example.fincacacaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class FinanzasActivity extends AppCompatActivity {

    private Button btnSiembra, btnCrecimiento, btnCosecha, btnVenta, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finanzas);

        btnSiembra = findViewById(R.id.btnSiembra);
        btnCrecimiento = findViewById(R.id.btnCrecimiento);
        btnCosecha = findViewById(R.id.btnCosecha);
        btnVenta = findViewById(R.id.btnVenta);
        btnBack = findViewById(R.id.btnBack);

        btnSiembra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FinanzasActivity.this, SiembraActivity.class);
                startActivity(intent);
            }
        });

        btnCrecimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FinanzasActivity.this, CrecimientoActivity.class);
                startActivity(intent);
            }
        });

        btnCosecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FinanzasActivity.this, CosechaActivity.class);
                startActivity(intent);
            }
        });

        btnVenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FinanzasActivity.this, VentaActivity.class);
                startActivity(intent);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}