package com.example.fincacacaoapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fincacacaoapp.R;
import com.example.fincacacaoapp.models.CarruselItem;
import java.util.List;

public class CarruselAdapter extends RecyclerView.Adapter<CarruselAdapter.CarruselViewHolder> {

    private List<CarruselItem> carruselItems;
    private static final int LOOP_MULTIPLIER = 1000; // Para efecto infinito

    public CarruselAdapter(List<CarruselItem> carruselItems) {
        this.carruselItems = carruselItems;
    }

    @NonNull
    @Override
    public CarruselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrusel, parent, false);

        // CRÍTICO: Configurar LayoutParams MATCH_PARENT en AMBOS
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        view.setLayoutParams(params);

        return new CarruselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarruselViewHolder holder, int position) {
        // Usar módulo para navegar cíclicamente entre las imágenes reales
        int realPosition = position % carruselItems.size();
        CarruselItem item = carruselItems.get(realPosition);

        // Establecer imagen
        holder.imageView.setImageResource(item.getImageResId());

        // Establecer textos
        holder.tvTitulo.setText(item.getTitulo());
        holder.tvDescripcion.setText(item.getDescripcion());
    }

    @Override
    public int getItemCount() {
        if (carruselItems == null || carruselItems.size() == 0) {
            return 0;
        }
        // Multiplicamos por un número grande para simular scroll infinito
        return carruselItems.size() * LOOP_MULTIPLIER;
    }

    // ViewHolder interno
    static class CarruselViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitulo, tvDescripcion;

        CarruselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageCarrusel);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);

            // Asegurar match_parent
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
    }
}