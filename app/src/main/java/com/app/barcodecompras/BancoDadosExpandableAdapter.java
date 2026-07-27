package com.app.barcodecompras;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.util.CompraUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BancoDadosExpandableAdapter extends RecyclerView.Adapter<BancoDadosExpandableAdapter.BancoDadosViewHolder> {
    private List<BancoDadosAgrupado> groupList;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private OnItemClickListenerDetalhe clickListenerDetalhe;
    private Context context;
    private SQLiteDatabase db;
    private int expandedPosition = -1;

    public void setDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    public interface OnItemClickListener {
        void onItemClick(BancoDadosAgrupado group, int position);
    }

    public interface OnItemLongClickListener {
        boolean onLongClick(View view, BancoDadosAgrupado group, int position);
    }

    public interface OnItemClickListenerDetalhe {
        void onClickDetalhe(Compra compra, int groupPosition, int itemPosition);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemClickListenerDetalhe(OnItemClickListenerDetalhe listener) {
        this.clickListenerDetalhe = listener;
    }

    public BancoDadosExpandableAdapter(List<BancoDadosAgrupado> groupList) {
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public BancoDadosViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_bancodados, parent, false);
        return new BancoDadosViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BancoDadosViewHolder holder, int position) {
        BancoDadosAgrupado group = groupList.get(position);

        // Dados do cabeçalho
        holder.tvBcBancoDados.setText(group.getBcDB());
        holder.tvDescricao.setText(group.getDescrDB());
        holder.tvCategoria.setText(group.getCatDB());
        holder.tvContagemOcorrenciasDB.setText(String.format("(%d)", group.getContagemOcorrencias()));

        // Controle de expansão
        boolean isExpanded = (expandedPosition == position);
        holder.expandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Limpar conteúdo antes de adicionar novamente
        holder.expandableContent.removeAllViews();

        // Preencher detalhes com TODOS os registros de compras relacionados a este código
        if (isExpanded) {
            List<Compra> comprasRelacionadas = group.getComprasRelacionadas();

            // Se não tiver compras carregadas, buscar do banco
            if (comprasRelacionadas == null || comprasRelacionadas.isEmpty()) {
                if (db != null) {
                    comprasRelacionadas = CompraUtil.buscarComprasPorCodigo(db, group.getBcDB());
                    group.setComprasRelacionadas(comprasRelacionadas);
                }
            }

            if (!comprasRelacionadas.isEmpty()) {
                // ===== CALCULAR ESTATÍSTICAS =====
                double somaPrecos = 0;
                double maiorPreco = Double.MIN_VALUE;
                double menorPreco = Double.MAX_VALUE;
                int totalItens = comprasRelacionadas.size();

                String maiorPeriodo = "";
                String maiorObs = "";
                String menorPeriodo = "";
                String menorObs = "";

                for (Compra c : comprasRelacionadas) {
                    double preco = c.getPrecoCompras();
                    somaPrecos += preco;

                    if (preco > maiorPreco) {
                        maiorPreco = preco;
                        maiorPeriodo = c.getPeriodoCompras();
                        maiorObs = c.getObsCompras();
                    }
                    if (preco < menorPreco) {
                        menorPreco = preco;
                        menorPeriodo = c.getPeriodoCompras();
                        menorObs = c.getObsCompras();
                    }
                }

                double media = totalItens > 0 ? somaPrecos / totalItens : 0;
                if (totalItens == 0) {
                    maiorPreco = 0;
                    menorPreco = 0;
                }

                String maiorData = extrairData(maiorPeriodo);
                String menorData = extrairData(menorPeriodo);
                // =================================

                // ===== INFLAR LAYOUT DE RESUMO =====
                View resumoView = LayoutInflater.from(context).inflate(R.layout.item_resumo_expandido, null);
                TextView tvResumoMedia = resumoView.findViewById(R.id.tvResumoMedia);
                TextView tvResumoMaior = resumoView.findViewById(R.id.tvResumoMaior);
                TextView tvResumoMenor = resumoView.findViewById(R.id.tvResumoMenor);
                TextView tvResumoMaiorPeriodo = resumoView.findViewById(R.id.tvResumoMaiorPeriodo);
                TextView tvResumoMenorPeriodo = resumoView.findViewById(R.id.tvResumoMenorPeriodo);

                DecimalFormat df = new DecimalFormat("#,##0.00");
                tvResumoMedia.setText(String.format("Média: R$ %s (%d itens)", df.format(media), totalItens));
                tvResumoMaior.setText(String.format("R$ %s", df.format(maiorPreco)));
                tvResumoMenor.setText(String.format("R$ %s", df.format(menorPreco)));

                if (!maiorData.isEmpty()) {
                    tvResumoMaiorPeriodo.setText(String.format("(%s%s)", maiorData,
                            maiorObs.isEmpty() ? "" : " - " + maiorObs));
                } else {
                    tvResumoMaiorPeriodo.setText("");
                }

                if (!menorData.isEmpty()) {
                    tvResumoMenorPeriodo.setText(String.format("(%s%s)", menorData,
                            menorObs.isEmpty() ? "" : " - " + menorObs));
                } else {
                    tvResumoMenorPeriodo.setText("");
                }

                holder.expandableContent.addView(resumoView);
                // ====================================

                // ===== LISTAR COMPRAS RELACIONADAS =====
                for (int i = 0; i < comprasRelacionadas.size(); i++) {
                    Compra c = comprasRelacionadas.get(i);
                    final int itemIndex = i;
                    final int groupPosition = position;

                    LinearLayout itemLayout = new LinearLayout(context);
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLayout.setPadding(8, 8, 8, 8);

                    String dataApenas = extrairData(c.getPeriodoCompras());

                    TextView tvDetalhe = new TextView(context);
                    String detalhe = String.format(
                            "Item %d:\n  Preço: R$ %.2f\n  Qtd: %.1f\n  Total: R$ %.2f\n  Período: %s\n  Obs: %s",
                            i + 1,
                            c.getPrecoCompras(),
                            c.getQntCompras(),
                            c.getTotalCompras(),
                            dataApenas,
                            c.getObsCompras().isEmpty() ? "Sem observação" : c.getObsCompras()
                    );
                    tvDetalhe.setText(detalhe);
                    tvDetalhe.setTextColor(0xFFFFFFFF);
                    tvDetalhe.setTextSize(14);
                    itemLayout.addView(tvDetalhe);

                    if (i < comprasRelacionadas.size() - 1) {
                        View separator = new View(context);
                        separator.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 1
                        ));
                        separator.setBackgroundColor(0xFF666666);
                        itemLayout.addView(separator);
                    }

                    holder.expandableContent.addView(itemLayout);

                    final Compra compraFinal = c;
                    itemLayout.setOnClickListener(v -> {
                        if (clickListenerDetalhe != null) {
                            clickListenerDetalhe.onClickDetalhe(compraFinal, groupPosition, itemIndex);
                        }
                    });
                }
            } else {
                TextView tvEmpty = new TextView(context);
                tvEmpty.setText("Nenhuma compra encontrada para este código");
                tvEmpty.setTextColor(0xFFFFFFFF);
                tvEmpty.setPadding(8, 8, 8, 8);
                holder.expandableContent.addView(tvEmpty);
            }
        }

        // Clique no cabeçalho = expandir/recolher
        holder.headerLayout.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(group, position);
            } else {
                // Fallback: auto expandir
                expandItem(position);
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

    private String extrairData(String periodoCompleto) {
        if (periodoCompleto == null || periodoCompleto.isEmpty()) {
            return "";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(periodoCompleto);
        if (matcher.find()) {
            return matcher.group();
        }
        return periodoCompleto;
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public void expandItem(int position) {
        if (expandedPosition == position) {
            expandedPosition = -1;
        } else {
            expandedPosition = position;
        }
        notifyDataSetChanged();
    }

    static class BancoDadosViewHolder extends RecyclerView.ViewHolder {
        LinearLayout headerLayout;
        TextView tvBcBancoDados, tvDescricao, tvCategoria;
        TextView tvContagemOcorrenciasDB;
        LinearLayout expandableContent;

        public BancoDadosViewHolder(@NonNull View itemView) {
            super(itemView);
            headerLayout = itemView.findViewById(R.id.headerLayout);
            tvBcBancoDados = itemView.findViewById(R.id.tvBancoDadosBarcode);
            tvDescricao = itemView.findViewById(R.id.tvBancoDadosDescription);
            tvCategoria = itemView.findViewById(R.id.tvBancoDadosCategory);
            tvContagemOcorrenciasDB = itemView.findViewById(R.id.tvContagemOcorrenciasDB);
            expandableContent = itemView.findViewById(R.id.expandableContent);
        }
    }
}
