package com.example.vitalpreesoficial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {
    private List<Cita> citas;

    public CitaAdapter(List<Cita> citas) {
        this.citas = citas;
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cita, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        Cita cita = citas.get(position);
        holder.tvFecha.setText("Fecha: " + cita.fecha);
        holder.tvSistolica.setText("Sistólica: " + cita.sistolica);
        holder.tvDiastolica.setText("Diastólica: " + cita.diastolica);
        holder.tvPulso.setText("Pulso: " + cita.pulso);
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvSistolica, tvDiastolica, tvPulso;
        public CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvSistolica = itemView.findViewById(R.id.tvSistolica);
            tvDiastolica = itemView.findViewById(R.id.tvDiastolica);
            tvPulso = itemView.findViewById(R.id.tvPulso);
        }
    }
}

