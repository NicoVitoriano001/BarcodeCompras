package com.app.barcodecompras;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CollectedAdapter extends RecyclerView.Adapter<CollectedAdapter.CollectedViewHolder> {
    private List<Collected> CollectedList;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(Collected collected);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public CollectedAdapter(List<Collected> collectedList) {
        this.CollectedList = collectedList; // Atualizado para usar CollectedList
    }

    @NonNull
    @Override
    public CollectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collected, parent, false);
        return new CollectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectedViewHolder holder, int position) {
        Collected collected = CollectedList.get(position); // Atualizado para CollectedList
        holder.tvBcCollected.setText(collected.getBcIMDB());
        holder.tvDescricao.setText(collected.getDescrIMDB());
        holder.tvCategoria.setText(collected.getCatIMDB());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(collected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return CollectedList != null ? CollectedList.size() : 0; // Adicionada verificação de null
    }

    static class CollectedViewHolder extends RecyclerView.ViewHolder {
        TextView tvBcCollected, tvDescricao, tvCategoria;

        public CollectedViewHolder(@NonNull View itemView) {
            super(itemView);
            // Verifique se esses IDs correspondem ao seu item_collected.xml
            tvBcCollected = itemView.findViewById(R.id.tvCollectedBarcode);
            tvDescricao = itemView.findViewById(R.id.tvCollectedDescription);
            tvCategoria = itemView.findViewById(R.id.tvCollectedCategory);
        }
    }
}