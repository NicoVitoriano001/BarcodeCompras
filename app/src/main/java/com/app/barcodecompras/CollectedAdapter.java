package com.app.barcodecompras;
//adapter ComprasAdapter.java

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CollectedAdapter extends RecyclerView.Adapter<CollectedAdapter.CollectedViewHolder> {
    private List<Collected> collectedList;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(Collected collected);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public CollectedAdapter(List<Collected> collectedList) {
        this.collectedList = collectedList;
    }

    @NonNull
    @Override
    public CollectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collected, parent, false); //onCreateViewHolder que infla o res/layout/item_compra.xml
        return new CollectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectedViewHolder holder, int position) { //O metodo onBindViewHolder do adapter preenche os dados de cada item (definido em item_compra.xml) com os dados da lista
        Collected collected = collectedList.get(position);
        holder.tvBcCollected.setText(collected.getBcIMDB()); // Já é String
        holder.tvDescricao.setText(collected.getDescrIMDB());
        holder.tvCategoria.setText(collected.getCatIMDB());

        // No metodo onBindViewHolder do ComprasAdapter:
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(collected);
            }
        });

    }

    @Override
    public int getItemCount() {
        return collectedList.size();
    }

    static class CollectedViewHolder extends RecyclerView.ViewHolder {
        TextView tvBcCollected, tvDescricao, tvCategoria;

        public CollectedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBcCollected = itemView.findViewById(R.id.tvCollectedBarcode);
            tvDescricao = itemView.findViewById(R.id.tvCollectedDescription);
            tvCategoria = itemView.findViewById(R.id.tvCollectedCategory);

        }
    }
}