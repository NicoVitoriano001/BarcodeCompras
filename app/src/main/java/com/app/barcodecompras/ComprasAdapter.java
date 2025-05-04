package com.app.barcodecompras;
//adapter ComprasAdapter.java

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.CompraViewHolder> {
    private List<Compra> comprasList;
    private OnItemClickListener listener;
    public interface OnItemClickListener {
        void onItemClick(Compra compra);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ComprasAdapter(List<Compra> comprasList) {
        this.comprasList = comprasList;
    }

    @NonNull
    @Override
    public CompraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compra, parent, false);
        return new CompraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompraViewHolder holder, int position) {
        Compra compra = comprasList.get(position);
        holder.tvBcCompras.setText(compra.getBcCompras());
        holder.tvDescricao.setText(compra.getDescrCompras());
        holder.tvCategoria.setText(compra.getCatCompras());
        holder.tvPreco.setText(String.format("R$ %.2f", compra.getPrecoCompras()));
        holder.tvQuantidade.setText(String.valueOf(compra.getQntCompras()));
        holder.tvTotal.setText(String.format("R$ %.2f", compra.getTotalCompras()));
        holder.tvPeriodo.setText(compra.getPeriodoCompras());
        holder.tvObsCompras.setText(compra.getObsCompras());

        // No método onBindViewHolder do ComprasAdapter:
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(compra);
            }
        });

    }

    @Override
    public int getItemCount() {
        return comprasList.size();
    }

    static class CompraViewHolder extends RecyclerView.ViewHolder {
        TextView tvBcCompras, tvDescricao, tvCategoria, tvPreco, tvQuantidade, tvTotal, tvPeriodo, tvObsCompras;

        public CompraViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBcCompras = itemView.findViewById(R.id.tvBcCompras);
            tvDescricao = itemView.findViewById(R.id.tvDescricao);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            tvPreco = itemView.findViewById(R.id.tvPreco);
            tvQuantidade = itemView.findViewById(R.id.tvQuantidade);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);
            tvObsCompras = itemView.findViewById(R.id.tvObsCompras);
        }
    }
}