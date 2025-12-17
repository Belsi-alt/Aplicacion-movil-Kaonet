package com.example.fincacacaoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CosechaActivity extends AppCompatActivity {

    private EditText etManoObraCosecha, etTransporte, etAlmacenamiento, etKgCosechados;
    private Button btnGuardarCosecha, btnBackCosecha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cosecha);

        etManoObraCosecha = findViewById(R.id.etManoObraCosecha);
        etTransporte = findViewById(R.id.etTransporte);
        etAlmacenamiento = findViewById(R.id.etAlmacenamiento);
        etKgCosechados = findViewById(R.id.etKgCosechados);
        btnGuardarCosecha = findViewById(R.id.btnGuardarCosecha);
        btnBackCosecha = findViewById(R.id.btnBackCosecha);

        btnGuardarCosecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CosechaActivity.this, "Datos de cosecha guardados", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackCosecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}