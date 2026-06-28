package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ExpandableListView;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.ContextMenuHelper;
import com.app.barcodecompras.util.DrawerUtil; //2026.06.07

import com.app.barcodecompras.util.ResumoExpandableAdapter;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class ResultComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria, currentPeriodo , currentObservacao;
    private RecyclerView recyclerView;
    private ComprasAdapter adapter;
    private SQLiteDatabase db;
    private List<Compra> comprasList = new ArrayList<>();
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_compras);

        recyclerView = findViewById(R.id.recyclerViewCompras);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ADICIONA O DIVISOR NO RV ENTRE OS ITENS 2026.06.14
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                LinearLayoutManager.VERTICAL
        );

        Drawable divider = ContextCompat.getDrawable(this, R.drawable.divider_itens_rv);
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider);
            recyclerView.addItemDecoration(dividerItemDecoration);
        } else {
            // Fallback: usa divisor padrão do sistema
            recyclerView.addItemDecoration(dividerItemDecoration);
        }
        // ===== FIM DO DIVISOR =====

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        // INICIALIZAR VARIÁVEL LOCAL
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);//2026.06.22 banco dados

        // >>> Sincronizar Firebase para local AO ABRIR o app
        // firebaseHelper.syncFirebaseParaLocal();
        // >>> Sincronizar local para Firebase
        // firebaseHelper.syncLocalParaFirebase();

        // Obter critérios de busca da intent
        String codigo = getIntent().getStringExtra("CODIGO");
        String descricao = getIntent().getStringExtra("DESCRICAO");
        String categoria = getIntent().getStringExtra("CATEGORIA");
        String periodo = getIntent().getStringExtra("PERIODO");
        String observacao = getIntent().getStringExtra("OBSERVACAO");

        loadCompras(codigo, descricao, categoria, periodo, observacao);


        // Obter e armazenar critérios de busca atuais
        currentCodigo = getIntent().getStringExtra("CODIGO") != null ? getIntent().getStringExtra("CODIGO") : "";
        currentDescricao = getIntent().getStringExtra("DESCRICAO") != null ? getIntent().getStringExtra("DESCRICAO") : "";
        currentCategoria = getIntent().getStringExtra("CATEGORIA") != null ? getIntent().getStringExtra("CATEGORIA") : "";
        currentPeriodo = getIntent().getStringExtra("PERIODO") != null ? getIntent().getStringExtra("PERIODO") : "";
        currentObservacao = getIntent().getStringExtra("OBSERVACAO") != null ? getIntent().getStringExtra("OBSERVACAO") : "";

        // Configurar clique nos itens da lista
        adapter.setOnItemClickListener(compra -> {
            Intent intent = new Intent(ResultComprasActivity.this, EditComprasActivity.class);
            intent.putExtra("compra_id", compra.getId()); // ← corrigido
            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
        });

        //2026.26.28
        adapter.setOnItemLongClickListener((view, compra) -> {
            ContextMenuHelper.showContextMenu(view, compra,
                    () -> { // Editar
                        Intent intent = new Intent(ResultComprasActivity.this, EditComprasActivity.class);
                        intent.putExtra("compra_id", compra.getId());
                        startActivityForResult(intent, EDIT_COMPRA_REQUEST);
                    },
                    () -> deletarCompra(compra),
                    () -> clonarCompra(compra),
                    () -> pesquisarCompra(compra)
            );
            return true;
        });
        //

        // DRAWER -- INICIO
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.resul_compras_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
        TextView tvMedia = findViewById(R.id.tvMedia);
        ExpandableListView expandable = findViewById(R.id.expandableResumo);

        tvMedia.setOnClickListener(v -> {

            if (expandable.getVisibility() == View.GONE) {
                expandable.setVisibility(View.VISIBLE);
            } else {
                expandable.setVisibility(View.GONE);
            }
        });

    }
    // FIM ONCREATE

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados com os mesmos critérios de busca
            loadCompras(currentCodigo, currentDescricao, currentCategoria, currentPeriodo, currentObservacao);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCompras(String codigo, String descricao, String categoria, String periodo, String observacao) {
        comprasList.clear();

        double somaTotal = 0.0;
        double somaPrecos = 0.0;
        int quantidadeItens = 0;
        double maiorPreco = Double.MIN_VALUE;
        double menorPreco = Double.MAX_VALUE;
        String maiorPeriodo = "", maiorObs = "";
        String menorPeriodo = "", menorObs = "";

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

        query += " ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras ASC";

        Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));


        // ===== CONSTRUIR A MESMA QUERY PARA CONTAGEM (SEM ORDER BY) =====
        String countQuery = "SELECT bc_compras, COUNT(*) as total FROM compras_tab WHERE 1=1";
        List<String> countParams = new ArrayList<>(params); // Copia os parâmetros

        // Reconstruir a query de contagem com os mesmos filtros
        if (!codigo.isEmpty()) {
            countQuery += " AND bc_compras LIKE ?";
        }
        if (!descricao.isEmpty()) {
            String[] termos = descricao.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    countQuery += " AND descr_compras NOT LIKE ?";
                } else {
                    countQuery += " AND descr_compras LIKE ?";
                }
            }
        }
        if (!categoria.isEmpty()) {
            String[] termos = categoria.split(" ");
            for (String termo : termos) {
                if (termo.startsWith("-") && termo.length() > 1) {
                    countQuery += " AND cat_compras NOT LIKE ?";
                } else {
                    countQuery += " AND cat_compras LIKE ?";
                }
            }
        }
        if (!periodo.isEmpty()) {
            countQuery += " AND periodo_compras LIKE ?";
        }
        if (!observacao.isEmpty()) {
            countQuery += " AND obs_compras LIKE ?";
        }

        countQuery += " GROUP BY bc_compras";

        // Executar query de contagem
        java.util.Map<String, Integer> contagemPorFiltro = new java.util.HashMap<>();
        Cursor countCursor = db.rawQuery(countQuery, countParams.toArray(new String[0]));

        if (countCursor != null && countCursor.moveToFirst()) {
            do {
                String bc = countCursor.getString(countCursor.getColumnIndexOrThrow("bc_compras"));
                int total = countCursor.getInt(countCursor.getColumnIndexOrThrow("total"));
                contagemPorFiltro.put(bc, total);
            } while (countCursor.moveToNext());
            countCursor.close();
        }
        // ================================================================

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

                // ===== ATRIBUI A CONTAGEM DO FILTRO ATUAL =====
                int contagemFiltrada = contagemPorFiltro.getOrDefault(bc, 0);
                compra.setContagemOcorrencias(contagemFiltrada);
                // ============================================

                comprasList.add(compra);

                if (preco > maiorPreco) {
                    maiorPreco = preco;
                    maiorPeriodo = periodoCompra;
                    maiorObs = obs;
                }

                if (preco < menorPreco) {
                    menorPreco = preco;
                    menorPeriodo = periodoCompra;
                    menorObs = obs;
                }
            } while (cursor.moveToNext());

            double mediaPreco = quantidadeItens > 0 ? somaPrecos / quantidadeItens : 0;

            // EXIBIR SOMA TOTAL
            TextView tvSomaTotal = findViewById(R.id.tvSomaTotal);
            tvSomaTotal.setText(String.format("Soma total: R$ %.2f (%d itens)", somaTotal, quantidadeItens));

            TextView tvMedia = findViewById(R.id.tvMedia);
            tvMedia.setText(String.format("Preço médio: R$ %.2f", mediaPreco));

            ExpandableListView expandable = findViewById(R.id.expandableResumo);
            List<String> groups = new ArrayList<>();
            java.util.Map<String, String> children = new java.util.HashMap<>();

            groups.add("Maior preço");
            groups.add("Menor preço");

            children.put("Maior preço",
                    "Preço: R$ " + String.format("%.2f", maiorPreco) +
                            "\nPeríodo: " + maiorPeriodo +
                            "\nObs: " + maiorObs);

            children.put("Menor preço",
                    "Preço: R$ " + String.format("%.2f", menorPreco) +
                            "\nPeríodo: " + menorPeriodo +
                            "\nObs: " + menorObs);

            ResumoExpandableAdapter expAdapter = new ResumoExpandableAdapter(this, groups, children);
            expandable.setAdapter(expAdapter);
        }

        cursor.close();

        if (adapter == null) {
            adapter = new ComprasAdapter(comprasList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        if (comprasList.isEmpty()) {
            Toast.makeText(this, "Nenhum resultado encontrado", Toast.LENGTH_SHORT).show();
        }
    }


    private void deletarCompra(Compra compra) {
        // Monta a mensagem de confirmação já formatada
        String mensagem = String.format(
                "Tem certeza que deseja excluir esta compra?\n\n" +
                        "Barcode: %s\n" +
                        "Descr: %s",
                compra.getBcCompras(),
                compra.getDescrCompras());

        // Exibe o dialog de confirmação
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage(mensagem)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    try {
                        if (firebaseComprasHelper != null) {
                            firebaseComprasHelper.deletarItem(String.valueOf(compra.getId())); //Exclusão do Firebase
                        }
                        int rowsDeleted = db.delete("compras_tab", "id = ?", //Exclusão do banco local (SQLite)
                                new String[]{String.valueOf(compra.getId())});
                        if (rowsDeleted > 0) { //Atualização da lista em tempo real
                            Toast.makeText(this, "Compra excluída com sucesso!", Toast.LENGTH_SHORT).show();
                            comprasList.remove(compra);
                            adapter.notifyDataSetChanged();
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
        intent.putExtra("bc", compra.getBcCompras());          // ← getBcCompras()
        intent.putExtra("descricao", compra.getDescrCompras()); // ← getDescrCompras()
        intent.putExtra("categoria", compra.getCatCompras());   // ← getCatCompras()
        intent.putExtra("preco", compra.getPrecoCompras());     // ← getPrecoCompras()
        intent.putExtra("quantidade", compra.getQntCompras());  // ← getQntCompras()
        intent.putExtra("total", compra.getTotalCompras());     // ← getTotalCompras()
        intent.putExtra("periodo", compra.getPeriodoCompras()); // ← getPeriodoCompras()
        intent.putExtra("obs", compra.getObsCompras());         // ← getObsCompras()
        startActivity(intent);
    }


    private void pesquisarCompra(Compra compra) {
        Intent intent = new Intent(ResultComprasActivity.this, ResultComprasActivity.class);
        intent.putExtra("CODIGO", compra.getBcCompras());
        intent.putExtra("DESCRICAO", compra.getDescrCompras());


        // Mantém os filtros que estavam sendo usados na tela atual
        intent.putExtra("CATEGORIA", currentCategoria != null ? currentCategoria : "");
        intent.putExtra("PERIODO", currentPeriodo != null ? currentPeriodo : "");
        intent.putExtra("OBSERVACAO", currentObservacao != null ? currentObservacao : "");


        startActivity(intent);
       // finish(); // Opcional: fecha a activity atual
    }




}

