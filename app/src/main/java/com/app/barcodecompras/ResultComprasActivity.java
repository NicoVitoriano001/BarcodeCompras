package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.CompraUtil;
import com.app.barcodecompras.util.ContextMenuHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria, currentPeriodo, currentObservacao;
    private RecyclerView recyclerView;
    private ComprasAdapter adapter;
    private SQLiteDatabase db;
    private List<CompraAgrupada> comprasGroupList = new ArrayList<>();
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private double precoDiffItem1 = 0;
    private double precoDiffItem2 = 0;
    private int itemDiffPosition1 = -1;
    private int itemDiffPosition2 = -1;
    private boolean isFirstItemSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_compras);

        recyclerView = findViewById(R.id.recyclerViewCompras);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ADICIONA O DIVISOR NO RV ENTRE OS ITENS
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                LinearLayoutManager.VERTICAL
        );

        Drawable divider = ContextCompat.getDrawable(this, R.drawable.divider_itens_rv);
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider);
            recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            recyclerView.addItemDecoration(dividerItemDecoration);
        }

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        // Obter critérios de busca da intent
        String codigo = getIntent().getStringExtra("CODIGO");
        String descricao = getIntent().getStringExtra("DESCRICAO");
        String categoria = getIntent().getStringExtra("CATEGORIA");
        String periodo = getIntent().getStringExtra("PERIODO");
        String observacao = getIntent().getStringExtra("OBSERVACAO");

        // Obter e armazenar critérios de busca atuais
        currentCodigo = codigo != null ? codigo : "";
        currentDescricao = descricao != null ? descricao : "";
        currentCategoria = categoria != null ? categoria : "";
        currentPeriodo = periodo != null ? periodo : "";
        currentObservacao = observacao != null ? observacao : "";

        loadCompras(codigo, descricao, categoria, periodo, observacao);

        // DRAWER
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.resul_compras_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);

        TextView tvMedia = findViewById(R.id.tvMedia);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            loadCompras(currentCodigo, currentDescricao, currentCategoria, currentPeriodo, currentObservacao);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCompras(String codigo, String descricao, String categoria, String periodo, String observacao) {
        comprasGroupList.clear();

        double somaTotal = 0.0;
        double somaPrecos = 0.0;
        int quantidadeItens = 0;
        double maiorPreco = Double.MIN_VALUE;
        double menorPreco = Double.MAX_VALUE;
        String maiorPeriodo = "", maiorObs = "";
        String menorPeriodo = "", menorObs = "";

        // ===== CONTAGEM GLOBAL - TODOS OS REGISTROS DA TABELA =====
        Map<String, Integer> contagemGlobal = new HashMap<>();
        Cursor countGlobalCursor = db.rawQuery(
                "SELECT bc_compras, COUNT(*) as total FROM compras_tab GROUP BY bc_compras",
                null
        );
        if (countGlobalCursor != null && countGlobalCursor.moveToFirst()) {
            do {
                String bc = countGlobalCursor.getString(countGlobalCursor.getColumnIndexOrThrow("bc_compras"));
                int total = countGlobalCursor.getInt(countGlobalCursor.getColumnIndexOrThrow("total"));
                contagemGlobal.put(bc, total);
            } while (countGlobalCursor.moveToNext());
            countGlobalCursor.close();
        }

        // Construir query dinâmica para os itens
        String query = "SELECT * FROM compras_tab WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (!codigo.isEmpty()) {
            query += " AND bc_compras LIKE ?";
            params.add("%" + codigo + "%");
        }

        if (!descricao.isEmpty()) {
            String[] termos = descricao.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    String valor = termo.substring(1);
                    query += " AND descr_compras NOT LIKE ?";
                    params.add("%" + valor + "%");
                } else {
                    query += " AND descr_compras LIKE ?";
                    params.add("%" + termo + "%");
                }
            }
        }

        if (!categoria.isEmpty()) {
            String[] termos = categoria.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    String valor = termo.substring(1);
                    query += " AND cat_compras NOT LIKE ?";
                    params.add("%" + valor + "%");
                } else {
                    query += " AND cat_compras LIKE ?";
                    params.add("%" + termo + "%");
                }
            }
        }

        if (!periodo.isEmpty()) {
            query += " AND periodo_compras LIKE ?";
            params.add("%" + periodo + "%");
        }

        if (!observacao.isEmpty()) {
            query += " AND obs_compras LIKE ?";
            params.add("%" + observacao + "%");
        }

        query += " ORDER BY SUBSTR(periodo_compras, 5) DESC, descr_compras ASC";
      //query += " ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras ASC";

        // ===== CONSTRUIR A MESMA QUERY PARA CONTAGEM =====
        String countQuery = "SELECT bc_compras, COUNT(*) as total FROM compras_tab WHERE 1=1";
        List<String> countParams = new ArrayList<>();

        if (!codigo.isEmpty()) {
            countQuery += " AND bc_compras LIKE ?";
            countParams.add("%" + codigo + "%");
        }
        if (!descricao.isEmpty()) {
            String[] termos = descricao.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    String valor = termo.substring(1);
                    countQuery += " AND descr_compras NOT LIKE ?";
                    countParams.add("%" + valor + "%");
                } else {
                    countQuery += " AND descr_compras LIKE ?";
                    countParams.add("%" + termo + "%");
                }
            }
        }
        if (!categoria.isEmpty()) {
            String[] termos = categoria.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    String valor = termo.substring(1);
                    countQuery += " AND cat_compras NOT LIKE ?";
                    countParams.add("%" + valor + "%");
                } else {
                    countQuery += " AND cat_compras LIKE ?";
                    countParams.add("%" + termo + "%");
                }
            }
        }
        if (!periodo.isEmpty()) {
            countQuery += " AND periodo_compras LIKE ?";
            countParams.add("%" + periodo + "%");
        }
        if (!observacao.isEmpty()) {
            countQuery += " AND obs_compras LIKE ?";
            countParams.add("%" + observacao + "%");
        }

        countQuery += " GROUP BY bc_compras";

        // Executar query de contagem
        Map<String, Integer> contagemPorFiltro = new HashMap<>();
        Cursor countCursor = db.rawQuery(countQuery, countParams.toArray(new String[0]));

        if (countCursor != null && countCursor.moveToFirst()) {
            do {
                String bc = countCursor.getString(countCursor.getColumnIndexOrThrow("bc_compras"));
                int total = countCursor.getInt(countCursor.getColumnIndexOrThrow("total"));
                contagemPorFiltro.put(bc, total);
            } while (countCursor.moveToNext());
            countCursor.close();
        }

        // Executar query principal
        Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));

        // Mapas para agrupar por código (LinkedHashMap mantém ordem)
        Map<String, List<Compra>> comprasPorCodigo = new LinkedHashMap<>();
        Map<String, CompraAgrupada> grupoInfo = new LinkedHashMap<>();

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
                String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));
                double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
                double quantidade = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
                double total = preco * quantidade;
                String periodoCompra = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
                String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

                somaTotal += total;
                somaPrecos += preco;
                quantidadeItens++;

                Compra compra = new Compra(
                        id, bc, descr, cat, preco, quantidade, total, periodoCompra, obs
                );

                // ATRIBUI A CONTAGEM GLOBAL (TODA A TABELA)
                int contagemGlobalItem = contagemGlobal.getOrDefault(bc, 0);
                compra.setContagemOcorrencias(contagemGlobalItem);

                // Agrupar por código
                if (!comprasPorCodigo.containsKey(bc)) {
                    comprasPorCodigo.put(bc, new ArrayList<>());
                    CompraAgrupada group = new CompraAgrupada(bc, descr, cat, periodoCompra, obs, contagemGlobalItem);
                    grupoInfo.put(bc, group);
                }
                comprasPorCodigo.get(bc).add(compra);
                if (preco > maiorPreco) {
                    maiorPreco = preco;
                //2026.08.21    maiorPeriodo = periodoCompra;
                //2026.08.21    maiorObs = obs;
                }

                if (preco < menorPreco) {
                    menorPreco = preco;
                //2026.08.21    menorPeriodo = periodoCompra;
                //2026.08.21    menorObs = obs;
                }
            } while (cursor.moveToNext());

            // Criar lista de grupos
            for (String bc : comprasPorCodigo.keySet()) {
                CompraAgrupada group = grupoInfo.get(bc);
                group.setCompras(comprasPorCodigo.get(bc));
                comprasGroupList.add(group);
            }

          //2026.08.21  double mediaPreco = quantidadeItens > 0 ? somaPrecos / quantidadeItens : 0;

            TextView tvSomaTotal = findViewById(R.id.tvSomaTotal);
            tvSomaTotal.setText(String.format("Soma total: R$ %.2f (%d itens)", somaTotal, quantidadeItens));

        }

        cursor.close();

        // Criar adapter com a lista de grupos
        if (adapter == null) {
            adapter = new ComprasAdapter(comprasGroupList);
            adapter.setDatabase(db);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        // Configurar listeners
        setupAdapters();

        if (comprasGroupList.isEmpty()) {
            Toast.makeText(this, "Nenhum resultado encontrado", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAdapters() {
        // Clique no item = expandir/recolher
        adapter.setOnItemClickListener((group, position) -> {
            adapter.expandItem(position);
        });

        // ===== CLICK NOS ITENS EXPANDIDOS =====
        adapter.setOnItemClickListenerDetalhe((compra, groupPosition, itemPosition) -> {
            Intent intent = new Intent(ResultComprasActivity.this, EditComprasActivity.class);
            intent.putExtra("compra_id", compra.getId());
            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
        });
        // =====================================

        // Long click no cabeçalho = menu de contexto
        adapter.setOnItemLongClickListener((view, group, position) -> {
            String codigo = group.getBcCompras();

            Compra compraParaMenu = buscarPrimeiraCompraPorCodigo(codigo);
            if (compraParaMenu != null) {
                ContextMenuHelper.showContextMenu(view, compraParaMenu,
                        () -> {
                            Intent intent = new Intent(ResultComprasActivity.this, EditComprasActivity.class);
                            intent.putExtra("compra_id", compraParaMenu.getId());
                            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
                        },
                        () -> deletarCompra(compraParaMenu),
                        () -> clonarCompra(compraParaMenu),
                        () -> pesquisarCompra(compraParaMenu)
                );
            } else {
                Toast.makeText(this, "Erro ao carregar dados do item", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Long click nos detalhes = selecionar para calcular diferença
        adapter.setOnItemLongClickListenerDetalhe((view, compra, groupPosition, itemPosition) -> {
            calcularDiferencaPreco(compra, groupPosition, itemPosition);
            return true;
        });
    }

    // Metodo auxiliar para buscar a primeira compra de um código
    private Compra buscarPrimeiraCompraPorCodigo(String codigo) {
        return CompraUtil.buscarPrimeiraCompraPorCodigo(db, codigo);
    }

  // Metodo para calcular diferença de preço
    private void calcularDiferencaPreco(Compra compra, int groupPosition, int itemPosition) {
        double precoAtual = compra.getPrecoCompras();

        if (!isFirstItemSelected) {
            precoDiffItem1 = precoAtual;
            itemDiffPosition1 = groupPosition;
            isFirstItemSelected = true;
            Toast.makeText(this, "Item 1 selecionado: R$ " + String.format("%.2f", precoAtual), Toast.LENGTH_SHORT).show();
        } else {
            precoDiffItem2 = precoAtual;
            itemDiffPosition2 = groupPosition;
            isFirstItemSelected = false;

            double diferenca = precoDiffItem2 - precoDiffItem1;
            double porcentagem = (precoDiffItem1 != 0) ? (diferenca / precoDiffItem1) * 100 : 0;

            DecimalFormat df = new DecimalFormat("#,##0.00");
            DecimalFormat dfPercent = new DecimalFormat("#,##0.00");

            //"Item 1 (grupo %d): R$ %s\n" +
            String mensagem = String.format(
                    "Comparação de Preços:\n\n" +
                            "[%d] Item 1: R$ %s\n" +
                            "[%d] Item 2: R$ %s\n\n" +
                            "Diferença: R$ %s\n" +
                            "Percentual: %s%%",
                    itemDiffPosition1 + 1,
                    df.format(precoDiffItem1),
                    itemDiffPosition2 + 1,
                    df.format(precoDiffItem2),
                    df.format(diferenca),
                    dfPercent.format(porcentagem)
            );

            new AlertDialog.Builder(this)
                    .setTitle("Diferença de Preço")
                    .setMessage(mensagem)
                    .setPositiveButton("OK", null)
                    .show();

            precoDiffItem1 = 0;
            precoDiffItem2 = 0;
            itemDiffPosition1 = -1;
            itemDiffPosition2 = -1;
        }
    }

    private void deletarCompra(Compra compra) {
        String mensagem = String.format(
                "Tem certeza que deseja excluir este item?\n\n" +
                        "Barcode: %s\n" +
                        "Descr: %s",
                compra.getBcCompras(),
                compra.getDescrCompras());

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage(mensagem)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    try {
                        if (firebaseComprasHelper != null) {
                            firebaseComprasHelper.deletarItem(String.valueOf(compra.getId()));
                        }
                        int rowsDeleted = db.delete("compras_tab", "id = ?",
                                new String[]{String.valueOf(compra.getId())});
                        if (rowsDeleted > 0) {
                            Toast.makeText(this, "Compra excluída com sucesso!", Toast.LENGTH_SHORT).show();
                            loadCompras(currentCodigo, currentDescricao, currentCategoria, currentPeriodo, currentObservacao);
                        } else {
                            Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void clonarCompra(Compra compra) {
        Intent intent = new Intent(ResultComprasActivity.this, MainActivity.class);
        intent.putExtra("CLONE_MODE", true);
        intent.putExtra("bc", compra.getBcCompras());
        intent.putExtra("descricao", compra.getDescrCompras());
        intent.putExtra("categoria", compra.getCatCompras());
        intent.putExtra("preco", compra.getPrecoCompras());
        intent.putExtra("quantidade", compra.getQntCompras());
        intent.putExtra("total", compra.getTotalCompras());
        intent.putExtra("periodo", compra.getPeriodoCompras());
        intent.putExtra("obs", compra.getObsCompras());
        startActivity(intent);
    }


    //////modificar nome do método para pesquisarCompraDB
    private void pesquisarCompra(Compra compra) {
        // Buscar no banco de dados de itens (bancodados_tab) usando código e descrição
        Intent intent = new Intent(ResultComprasActivity.this, ResultBancoDadosActivity.class);
        //OKOK Intent intent = new Intent(ResultComprasActivity.this, EditBancoDadosActivity.class);

        // Usa os dados da compra para buscar no banco de dados
        intent.putExtra("CODIGO", compra.getBcCompras() != null ? compra.getBcCompras() : "");
        intent.putExtra("DESCRICAO", compra.getDescrCompras() != null ? compra.getDescrCompras() : "");
        intent.putExtra("CATEGORIA", compra.getCatCompras() != null ? compra.getCatCompras() : "");
      //OKOK intent.putExtra("CATEGORIA", currentCategoria != null ? currentCategoria : "");
        startActivity(intent);
    }
}