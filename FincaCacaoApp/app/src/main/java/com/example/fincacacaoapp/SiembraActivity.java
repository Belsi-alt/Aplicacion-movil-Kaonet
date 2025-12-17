package com.example.fincacacaoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SiembraActivity extends AppCompatActivity {

    private EditText etCostoSemillas, etCostoManoObra, etOtrosCostos;
    private Button btnGuardarSiembra, btnBackSiembra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_siembra);

        // Configurar los elementos de la UI
        etCostoSemillas = findViewById(R.id.etCostoSemillas);
        etCostoManoObra = findViewById(R.id.etCostoManoObra);
        etOtrosCostos = findViewById(R.id.etOtrosCostos);
        btnGuardarSiembra = findViewById(R.id.btnGuardarSiembra);
        btnBackSiembra = findViewById(R.id.btnBackSiembra);

        btnGuardarSiembra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SiembraActivity.this, "Datos de siembra guardados", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackSiembra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}