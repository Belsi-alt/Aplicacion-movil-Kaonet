package com.example.fincacacaoapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fincacacaoapp.R;
import com.example.fincacacaoapp.models.Lote;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.RegistroViewHolder> {

    private List<Lote> listaRegistros;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Lote lote);
    }

    public RegistroAdapter(List<Lote> listaRegistros, OnItemClickListener listener) {
        this.listaRegistros = listaRegistros;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RegistroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registro, parent, false);
        return new RegistroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RegistroViewHolder holder, int position) {
        Lote registro = listaRegistros.get(position);
        holder.bind(registro, listener);
    }

    @Override
    public int getItemCount() {
        return listaRegistros.size();
    }

    public static class RegistroViewHolder extends RecyclerView.ViewHolder {
        private CardView cardRegistro;
        private TextView tvNombreLote, tvPlantas, tvEstado, tvFecha;

        public RegistroViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRegistro = itemView.findViewById(R.id.cardRegistro);
            tvNombreLote = itemView.findViewById(R.id.tvNombreLote);
            tvPlantas = itemView.findViewById(R.id.tvPlantas);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha);
        }

        public void bind(final Lote registro, final OnItemClickListener listener) {
            tvNombreLote.setText(registro.getNombre());
            tvPlantas.setText(registro.getCantidadPlantas() + " plantas");
            tvEstado.setText(registro.getEstado().toUpperCase());

            // Formatear fecha
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvFecha.setText("Creado: " + sdf.format(registro.getFechaSiembra()));

            cardRegistro.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(registro);
                }
            });

            // Configurar color del estado
            configurarColorEstado(registro.getEstado());
        }

        private void configurarColorEstado(String estado) {
            int colorFondo = R.color.verde_siembra; // default

            switch (estado.toLowerCase()) {
                case "siembra":
                    colorFondo = R.color.verde_siembra;
                    break;
                case "crecimiento":
                    colorFondo = R.color.azul_crecimiento;
                    break;
                case "cosecha":
                    colorFondo = R.color.naranja_cosecha;
                    break;
                case "venta":
                    colorFondo = R.color.morado_venta;
                    break;
            }

            tvEstado.setBackgroundColor(itemView.getContext().getResources().getColor(colorFondo));
        }
    }
}