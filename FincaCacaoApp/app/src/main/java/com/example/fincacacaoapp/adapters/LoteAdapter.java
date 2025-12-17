package com.example.fincacacaoapp.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fincacacaoapp.R;
import com.example.fincacacaoapp.models.Lote;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LoteAdapter extends RecyclerView.Adapter<LoteAdapter.LoteViewHolder> {

    private List<Lote> listaLotes;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Lote lote);
    }

    public LoteAdapter(List<Lote> listaLotes, OnItemClickListener listener) {
        this.listaLotes = listaLotes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lote, parent, false);
        return new LoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoteViewHolder holder, int position) {
        Lote lote = listaLotes.get(position);
        holder.bind(lote, listener);
    }

    @Override
    public int getItemCount() {
        return listaLotes.size();
    }

    public static class LoteViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombreLote, tvPlantas, tvEstado, tvFechaSiembra;

        public LoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreLote = itemView.findViewById(R.id.tvNombreLote);
            tvPlantas = itemView.findViewById(R.id.tvPlantas);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFechaSiembra = itemView.findViewById(R.id.tvFechaSiembra);

        }

        public void bind(final Lote lote, final OnItemClickListener listener) {
            // Configurar nombre del lote
            tvNombreLote.setText(lote.getNombre());

            // Configurar cantidad de plantas
            tvPlantas.setText(lote.getCantidadPlantas() + " plantas");

            // Formatear fecha de siembra
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String fechaFormateada = sdf.format(lote.getFechaSiembra());
            tvFechaSiembra.setText("Siembra: " + fechaFormateada);



            // Configurar estado con color dinámico
            configurarEstado(lote.getEstado());

            // Configurar click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(lote);
                }
            });
        }

        private void configurarEstado(String estado) {
            int colorFondo = 0;
            String textoEstado = "";

            // Asignar color y texto según el estado
            switch (estado.toLowerCase()) {
                case "siembra":
                    colorFondo = R.color.verde_siembra;
                    textoEstado = "SIEMBRA";
                    break;
                case "crecimiento":
                    colorFondo = R.color.azul_crecimiento;
                    textoEstado = "CRECIMIENTO";
                    break;
                case "cosecha":
                    colorFondo = R.color.naranja_cosecha;
                    textoEstado = "COSECHA";
                    break;
                case "venta":
                    colorFondo = R.color.morado_venta;
                    textoEstado = "VENTA";
                    break;
                default:
                    colorFondo = R.color.verde_brillante;
                    textoEstado = estado.toUpperCase();
            }

            // Aplicar color de fondo al TextView del estado
            tvEstado.setText(textoEstado);
            tvEstado.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), colorFondo));
        }
    }

    // Método para actualizar la lista (útil para la búsqueda)
    public void actualizarLista(List<Lote> nuevaLista) {
        listaLotes = nuevaLista;
        notifyDataSetChanged();
    }
}