package com.example.fincacacaoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrecimientoActivity extends AppCompatActivity {

    private EditText etFertilizantes, etRiego, etMantenimiento, etPlaguicidas;
    private Button btnGuardarCrecimiento, btnBackCrecimiento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crecimiento);

        etFertilizantes = findViewById(R.id.etFertilizantes);
        etRiego = findViewById(R.id.etRiego);
        etMantenimiento = findViewById(R.id.etMantenimiento);
        etPlaguicidas = findViewById(R.id.etPlaguicidas);
        btnGuardarCrecimiento = findViewById(R.id.btnGuardarCrecimiento);
        btnBackCrecimiento = findViewById(R.id.btnBackCrecimiento);

        btnGuardarCrecimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CrecimientoActivity.this, "Datos de crecimiento guardados", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackCrecimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}