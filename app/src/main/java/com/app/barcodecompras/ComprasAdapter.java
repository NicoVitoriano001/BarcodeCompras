package com.app.barcodecompras;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.CompraViewHolder> {
    private List<CompraAgrupada> comprasGroupList;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private OnItemLongClickListenerDetalhe longClickListenerDetalhe;

    private int expandedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(CompraAgrupada group, int position);
    }

    public interface OnItemLongClickListener {
        boolean onLongClick(View view, CompraAgrupada group, int position);
    }

    public interface OnItemLongClickListenerDetalhe {
        boolean onLongClickDetalhe(View view, Compra compra, int groupPosition, int itemPosition);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemLongClickListenerDetalhe(OnItemLongClickListenerDetalhe listener) {
        this.longClickListenerDetalhe = listener;
    }

    public ComprasAdapter(List<CompraAgrupada> comprasGroupList) {
        this.comprasGroupList = comprasGroupList;
    }

    @NonNull
    @Override
    public CompraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_compra_rv, parent, false);
        return new CompraViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompraViewHolder holder, int position) {
        CompraAgrupada group = comprasGroupList.get(position);
        List<Compra> compras = group.getCompras();

        // Dados do cabeçalho
        holder.tvBcCompras.setText(group.getBcCompras());
        holder.tvDescricao.setText(group.getDescrCompras());
        holder.tvCategoria.setText(group.getCatCompras());
        holder.tvPeriodo.setText(group.getPeriodoCompras());
        holder.tvObsCompras.setText(group.getObsCompras());
        holder.tvContagemOcorrencias.setText(String.format("(%d)", group.getContagemOcorrencias()));

        if (!compras.isEmpty()) {
            Compra primeira = compras.get(0);
            holder.tvPreco.setText(String.format("R$ %.2f", primeira.getPrecoCompras()));
            holder.tvQuantidade.setText(String.valueOf(primeira.getQntCompras()));
            holder.tvTotal.setText(String.format("R$ %.2f", primeira.getTotalCompras()));
        }

        // Controle de expansão
        boolean isExpanded = (expandedPosition == position);
        holder.expandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // ===== LIMPAR O CONTEÚDO ANTES DE ADICIONAR NOVAMENTE =====
        holder.expandableContent.removeAllViews();

        // Preencher detalhes com todos os itens do grupo
        if (isExpanded && !compras.isEmpty()) {
            for (int i = 0; i < compras.size(); i++) {
                Compra c = compras.get(i);
                final int itemIndex = i;

                // Criar um LinearLayout para cada item
                LinearLayout itemLayout = new LinearLayout(holder.itemView.getContext());
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(8, 8, 8, 8);

                // Criar TextView para o item
                TextView tvDetalhe = new TextView(holder.itemView.getContext());
                String detalhe = String.format(
                        "Item %d:\n  Preço: R$ %.2f\n  Qtd: %.1f\n  Total: R$ %.2f\n  Período: %s",
                        i + 1,
                        c.getPrecoCompras(),
                        c.getQntCompras(),
                        c.getTotalCompras(),
                        c.getPeriodoCompras()
                );
                tvDetalhe.setText(detalhe);
                tvDetalhe.setTextColor(0xFFFFFFFF);
                tvDetalhe.setTextSize(14);
                itemLayout.addView(tvDetalhe);

                // Separador (exceto para o último item)
                if (i < compras.size() - 1) {
                    View separator = new View(holder.itemView.getContext());
                    separator.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 1
                    ));
                    separator.setBackgroundColor(0xFF666666);
                    itemLayout.addView(separator);
                }

                // Adicionar o layout do item ao expandableContent
                holder.expandableContent.addView(itemLayout);

                // Definir o long click no layout do item
                final Compra compraFinal = c;
                itemLayout.setOnLongClickListener(v -> {
                    if (longClickListenerDetalhe != null) {
                        return longClickListenerDetalhe.onLongClickDetalhe(v, compraFinal, position, itemIndex);
                    }
                    return false;
                });
            }
        }

        // Clique no cabeçalho = expandir/recolher
        holder.headerLayout.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(group, position);
            }
        });

        // Long click no cabeçalho = menu de contexto
        holder.headerLayout.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onLongClick(v, group, position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return comprasGroupList.size();
    }

    public void expandItem(int position) {
        if (expandedPosition == position) {
            expandedPosition = -1;
        } else {
            expandedPosition = position;
        }
        notifyDataSetChanged();
    }

    static class CompraViewHolder extends RecyclerView.ViewHolder {
        LinearLayout headerLayout;
        TextView tvBcCompras, tvDescricao, tvCategoria, tvPreco, tvQuantidade, tvTotal, tvPeriodo, tvObsCompras;
        TextView tvContagemOcorrencias;
        LinearLayout expandableContent;

        public CompraViewHolder(@NonNull View itemView) {
            super(itemView);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            tvBcCompras = itemView.findViewById(R.id.tvBcCompras);
            tvDescricao = itemView.findViewById(R.id.tvDescricao);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            tvPreco = itemView.findViewById(R.id.tvPreco);
            tvQuantidade = itemView.findViewById(R.id.tvQuantidade);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvPeriodo = itemView.findViewById(R.id.tvPeriodo);
            tvObsCompras = itemView.findViewById(R.id.tvObsCompras);
            tvContagemOcorrencias = itemView.findViewById(R.id.tvContagemOcorrencias);
            expandableContent = itemView.findViewById(R.id.expandableContent);
        }
    }
}