package com.app.barcodecompras;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BancoDadosAdapter extends RecyclerView.Adapter<BancoDadosAdapter.BancoDadosViewHolder> {
    private List<BancoDados> BancoDadosList;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(BancoDados bancodados);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public BancoDadosAdapter(List<BancoDados> bancodadosList) {
        this.BancoDadosList = bancodadosList; // Atualizado para usar BancoDadosList
    }

    @NonNull
    @Override
    public BancoDadosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bancodados, parent, false);
        return new BancoDadosViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BancoDadosViewHolder holder, int position) {
        BancoDados bancodados = BancoDadosList.get(position); // Atualizado para BancoDadosList
        holder.tvBcBancoDados.setText(bancodados.getBcIMDB());
        holder.tvDescricao.setText(bancodados.getDescrIMDB());
        holder.tvCategoria.setText(bancodados.getCatIMDB());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(bancodados);
            }
        });
    }

    @Override
    public int getItemCount() {
        return BancoDadosList != null ? BancoDadosList.size() : 0; // Adicionada verificação de null
    }

    static class BancoDadosViewHolder extends RecyclerView.ViewHolder {
        TextView tvBcBancoDados, tvDescricao, tvCategoria;

        public BancoDadosViewHolder(@NonNull View itemView) {
            super(itemView);
            // Verifique se esses IDs correspondem ao seu item_bancodados.xml
            tvBcBancoDados = itemView.findViewById(R.id.tvBancoDadosBarcode);
            tvDescricao = itemView.findViewById(R.id.tvBancoDadosDescription);
            tvCategoria = itemView.findViewById(R.id.tvBancoDadosCategory);
        }
    }
}