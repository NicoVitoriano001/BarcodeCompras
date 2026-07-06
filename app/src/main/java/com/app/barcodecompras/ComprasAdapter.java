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

import com.app.barcodecompras.database.DatabaseHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.CompraViewHolder> {
    private List<CompraAgrupada> comprasGroupList;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private OnItemLongClickListenerDetalhe longClickListenerDetalhe;
    private OnItemClickListenerDetalhe clickListenerDetalhe;
    private Context context;
    private SQLiteDatabase db;
    private int expandedPosition = -1;
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    public interface OnItemClickListener {
        void onItemClick(CompraAgrupada group, int position);
    }

    public interface OnItemLongClickListener {
        boolean onLongClick(View view, CompraAgrupada group, int position);
    }

    public interface OnItemLongClickListenerDetalhe {
        boolean onLongClickDetalhe(View view, Compra compra, int groupPosition, int itemPosition);
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

    public void setOnItemLongClickListenerDetalhe(OnItemLongClickListenerDetalhe listener) {
        this.longClickListenerDetalhe = listener;
    }

    public void setOnItemClickListenerDetalhe(OnItemClickListenerDetalhe listener) {
        this.clickListenerDetalhe = listener;
    }

    public ComprasAdapter(List<CompraAgrupada> comprasGroupList) {
        this.comprasGroupList = comprasGroupList;
    }

    @NonNull
    @Override
    public CompraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
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

        // Limpar conteúdo antes de adicionar novamente
        holder.expandableContent.removeAllViews();

        // Preencher detalhes com TODOS os registros do código (ignorando filtros)
        if (isExpanded) {
            List<Compra> todosRegistros = buscarTodosRegistrosPorCodigo(group.getBcCompras());

            // ===== CALCULAR ESTATÍSTICAS =====
            double somaPrecos = 0;
            double maiorPreco = Double.MIN_VALUE;
            double menorPreco = Double.MAX_VALUE;
            int totalItens = todosRegistros.size();

            // Variáveis para armazenar o período e observação do maior e menor preço
            String maiorPeriodo = "";
            String maiorObs = "";
            String menorPeriodo = "";
            String menorObs = "";

            for (Compra c : todosRegistros) {
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
                maiorPeriodo = "";
                maiorObs = "";
                menorPeriodo = "";
                menorObs = "";
            }
            // =================================

            // ===== EXTRAIR APENAS A DATA DO PERÍODO =====
            String maiorData = extrairData(maiorPeriodo);
            String menorData = extrairData(menorPeriodo);
            // ============================================

// ===== INFLAR O LAYOUT DE RESUMO =====
            View resumoView = LayoutInflater.from(context).inflate(R.layout.item_resumo_expandido, null);
            TextView tvResumoMedia = resumoView.findViewById(R.id.tvResumoMedia);
            TextView tvResumoMaior = resumoView.findViewById(R.id.tvResumoMaior);
            TextView tvResumoMenor = resumoView.findViewById(R.id.tvResumoMenor);
            TextView tvResumoMaiorPeriodo = resumoView.findViewById(R.id.tvResumoMaiorPeriodo);
            TextView tvResumoMenorPeriodo = resumoView.findViewById(R.id.tvResumoMenorPeriodo);
// Os TextViews das setas não precisam ser configurados, pois já têm texto fixo no XML

            tvResumoMedia.setText(String.format("Média: R$ %s (%d itens)", df.format(media), totalItens));
            tvResumoMaior.setText(String.format("R$ %s", df.format(maiorPreco)));
            tvResumoMenor.setText(String.format("R$ %s", df.format(menorPreco)));

// ===== ADICIONAR DATA E OBSERVAÇÃO AO LADO DOS VALORES =====
// ===== ADICIONAR DATA E OBSERVAÇÃO AO LADO DOS VALORES =====
            if (!maiorData.isEmpty()) {
                if (!maiorObs.isEmpty()) {
                    tvResumoMaiorPeriodo.setText(String.format("(%s - %s)", maiorData, maiorObs));
                } else {
                    tvResumoMaiorPeriodo.setText(String.format("(%s)", maiorData));
                }
            } else {
                tvResumoMaiorPeriodo.setText("");
            }

            if (!menorData.isEmpty()) {
                if (!menorObs.isEmpty()) {
                    tvResumoMenorPeriodo.setText(String.format("(%s - %s)", menorData, menorObs));
                } else {
                    tvResumoMenorPeriodo.setText(String.format("(%s)", menorData));
                }
            } else {
                tvResumoMenorPeriodo.setText("");
            }
// =============================================================

            holder.expandableContent.addView(resumoView);
// ====================================

            if (!todosRegistros.isEmpty()) {
                for (int i = 0; i < todosRegistros.size(); i++) {
                    Compra c = todosRegistros.get(i);
                    final int itemIndex = i;
                    final int groupPosition = position;

                    LinearLayout itemLayout = new LinearLayout(context);
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    itemLayout.setPadding(8, 8, 8, 8);

                    // Extrair apenas a data para exibição nos detalhes também
                    String dataApenas = extrairData(c.getPeriodoCompras());

                    TextView tvDetalhe = new TextView(context);
                    String detalhe = String.format(
                            "Item %d:\n  Preço: R$ %.2f\n  Qtd: %.1f\n  Total: R$ %.2f\n  Período: %s\n  Obs: %s",
                            i + 1,
                            c.getPrecoCompras(),
                            c.getQntCompras(),
                            c.getTotalCompras(),
                            dataApenas, // ← Usando apenas a data
                            c.getObsCompras()
                    );
                    tvDetalhe.setText(detalhe);
                    tvDetalhe.setTextColor(0xFFFFFFFF);
                    tvDetalhe.setTextSize(14);
                    itemLayout.addView(tvDetalhe);

                    if (i < todosRegistros.size() - 1) {
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

                    itemLayout.setOnLongClickListener(v -> {
                        if (longClickListenerDetalhe != null) {
                            return longClickListenerDetalhe.onLongClickDetalhe(v, compraFinal, groupPosition, itemIndex);
                        }
                        return false;
                    });
                }
            } else {
                TextView tvEmpty = new TextView(context);
                tvEmpty.setText("Nenhum registro encontrado para este código");
                tvEmpty.setTextColor(0xFFFFFFFF);
                tvEmpty.setPadding(8, 8, 8, 8);
                holder.expandableContent.addView(tvEmpty);
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

    // ===== MÉTODO AUXILIAR PARA EXTRAIR APENAS A DATA =====
    private String extrairData(String periodoCompleto) {
        if (periodoCompleto == null || periodoCompleto.isEmpty()) {
            return "";
        }

        // O formato é "sáb. 2026-07-01" - extrair apenas a data
        // Procura por padrão de data YYYY-MM-DD
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(periodoCompleto);

        if (matcher.find()) {
            return matcher.group();
        }

        return periodoCompleto; // Se não encontrar o padrão, retorna o original
    }
// =====================================================

    private List<Compra> buscarTodosRegistrosPorCodigo(String codigo) {
        List<Compra> registros = new ArrayList<>();

        try {
            if (db == null) {
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                db = dbHelper.getWritableDatabase();
            }

            String query = "SELECT * FROM compras_tab WHERE bc_compras = ? ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras ASC";
            Cursor cursor = db.rawQuery(query, new String[]{codigo});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
                    String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
                    String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));
                    double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
                    double quantidade = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"));
                    String periodoCompra = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
                    String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

                    Compra compra = new Compra(
                            id, bc, descr, cat, preco, quantidade, total, periodoCompra, obs
                    );
                    registros.add(compra);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return registros;
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