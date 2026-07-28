package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.database.BancoDados;
import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.CompraUtil;
import com.app.barcodecompras.util.ContextMenuHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultBancoDadosActivity extends AppCompatActivity {
    private static final int EDIT_REQUEST_CODE = 1;
    private String currentCodigo, currentDescricao, currentCategoria;
    private RecyclerView recyclerView;
    private BancoDadosExpandableAdapter adapter;
    private SQLiteDatabase db;
    private List<BancoDadosAgrupado> groupList = new ArrayList<>();
    private TextView tvTitle;
    private Spinner spinnerSortField, spinnerSortOrder;
    private String currentSortField = "bc_DB";
    private String currentSortOrder = "ASC";
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_result_bancodados);

        tvTitle = findViewById(R.id.tvTitle);

        recyclerView = findViewById(R.id.recyclerViewResultBancoDados);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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

        // ===== SETUP SPINNERS DE ORDENAÇÃO =====
        spinnerSortField = findViewById(R.id.spinnerSortField);
        spinnerSortOrder = findViewById(R.id.spinnerSortOrder);

        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Código (bc_DB)", "Descrição (descr_DB)"});
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortField.setAdapter(fieldAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ASC ↑", "DESC ↓"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSortOrder.setAdapter(orderAdapter);

        spinnerSortField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentSortField = "bc_DB"; break;
                    case 1: currentSortField = "descr_DB"; break;
                }
                if (!groupList.isEmpty()) {
                    recarregarDados();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSortOrder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentSortOrder = "ASC"; break;
                    case 1: currentSortOrder = "DESC"; break;
                }
                if (!groupList.isEmpty()) {
                    recarregarDados();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // =========================================

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        Intent intent = getIntent();
        if (intent != null) {
            String codigo = intent.getStringExtra("CODIGO") != null ? intent.getStringExtra("CODIGO") : "";
            String descricao = intent.getStringExtra("DESCRICAO") != null ? intent.getStringExtra("DESCRICAO") : "";
            String categoria = intent.getStringExtra("CATEGORIA") != null ? intent.getStringExtra("CATEGORIA") : "";

            currentCodigo = codigo;
            currentDescricao = descricao;
            currentCategoria = categoria;

            loadBancoDados(codigo, descricao, categoria);
        }

        bancoDadosBkp = new BancoDadosBkp(this, dbHelper);
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        drawer = findViewById(R.id.result_bancodados_drawer_layout);
        navigationView = findViewById(R.id.resul_bancodados_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            recarregarDados();
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAdapters() {
        // Clique no cabeçalho = expandir/recolher
        adapter.setOnItemClickListener((group, position) -> {
            // Carregar compras relacionadas na primeira expansão
            if (group.getComprasRelacionadas() == null || group.getComprasRelacionadas().isEmpty()) {
                List<Compra> compras = buscarComprasPorCodigo(group.getBcDB());
                group.setComprasRelacionadas(compras);
            }
            adapter.expandItem(position);
        });

        // Clique nos itens expandidos (detalhes da compra) = editar compra
        adapter.setOnItemClickListenerDetalhe((compra, groupPosition, itemPosition) -> {
            Intent intent = new Intent(ResultBancoDadosActivity.this, EditComprasActivity.class);
            intent.putExtra("compra_id", compra.getId());
            startActivityForResult(intent, EDIT_REQUEST_CODE);
        });

        // Long click no cabeçalho = menu de contexto
        adapter.setOnItemLongClickListener((view, group, position) -> {
            String codigo = group.getBcDB();

            // Buscar dados da compra para o menu de contexto (pode ser null)
            Compra compraParaMenu = buscarPrimeiraCompraPorCodigo(codigo);

            ContextMenuHelper.showContextMenu(view, compraParaMenu,
                    () -> {
                        // Editar item do banco de dados
                        Intent editIntent = new Intent(ResultBancoDadosActivity.this, EditBancoDadosActivity.class);
                        editIntent.putExtra("ID", group.getId());
                        editIntent.putExtra("CODIGO", group.getBcDB());
                        editIntent.putExtra("DESCRICAO", group.getDescrDB());
                        editIntent.putExtra("CATEGORIA", group.getCatDB());
                        startActivityForResult(editIntent, EDIT_REQUEST_CODE);
                    },
                    () -> deletarItem(group, codigo),
                    () -> {
                        // Clonar - vai para MainActivity com dados
                        if (compraParaMenu != null) {
                            clonarCompra(compraParaMenu);
                        } else {
                            // Se não tem compra, criar intent genérica
                            Intent intent = new Intent(ResultBancoDadosActivity.this, MainActivity.class);
                            intent.putExtra("bc", group.getBcDB());
                            intent.putExtra("descricao", group.getDescrDB());
                            intent.putExtra("categoria", group.getCatDB());
                            startActivity(intent);
                        }
                    },
                    () -> {
                        // Pesquisar no banco de dados (já estamos aqui, então pesquisar em compras)
                        Intent searchIntent = new Intent(ResultBancoDadosActivity.this, ResultComprasActivity.class);
                        searchIntent.putExtra("CODIGO", group.getBcDB());
                        searchIntent.putExtra("DESCRICAO", group.getDescrDB());
                        searchIntent.putExtra("CATEGORIA", group.getCatDB());
                        startActivity(searchIntent);
                    }
            );
            return true;
        });
    }

    private void recarregarDados() {
        loadBancoDados(currentCodigo, currentDescricao, currentCategoria);
    }

    private void loadBancoDados(String codigo, String descricao, String categoria) {
        groupList.clear();

        if (codigo.isEmpty() && descricao.isEmpty() && categoria.isEmpty()) {
            Toast.makeText(this, "Informe pelo menos um critério de busca", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== CONTAGEM GLOBAL - TODOS OS REGISTROS DA TABELA compras_tab =====
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
        // ==========================================================

        String query = "SELECT id, bc_DB, descr_DB, cat_DB, updated_at FROM bancodados_tab WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (!codigo.isEmpty()) {
            query += " AND bc_DB LIKE ?";
            params.add("%" + codigo + "%");
        }

        if (!descricao.isEmpty()) {
            query += " AND REPLACE(descr_DB, ' ', '%') LIKE ?";
            params.add("%" + descricao.replace(" ", "%") + "%");
        }

        if (!categoria.isEmpty()) {
            query += " AND REPLACE(cat_DB, ' ', '%') LIKE ?";
            params.add("%" + categoria.replace(" ", "%") + "%");
        }

        // ===== ORDENAÇÃO DINÂMICA =====
        query += " ORDER BY " + currentSortField + " " + currentSortOrder;
        // ===============================

        Cursor cursor = null;
        int itemCount = 0;

        try {
            cursor = db.rawQuery(query, params.toArray(new String[0]));

            if (cursor != null && cursor.getCount() > 0) {
                itemCount = cursor.getCount();

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String bc = cursor.getString(1);
                    String desc = cursor.getString(2);
                    String cat = cursor.getString(3);
                    long updatedAt = cursor.getLong(4);

                    BancoDados bancodados = new BancoDados(id, bc, desc, cat, updatedAt);

                    // ===== ATRIBUI A CONTAGEM GLOBAL =====
                    int contagem = contagemGlobal.getOrDefault(bc, 0);
                    bancodados.setContagemOcorrencias(contagem);
                    // ====================================

                    // Criar grupo para cada item do banco de dados
                    BancoDadosAgrupado group = new BancoDadosAgrupado(id, bc, desc, cat, contagem);
                    groupList.add(group);
                }

                tvTitle.setText(String.format("Itens do Banco de Dados (%d itens)", itemCount));

            } else {
                tvTitle.setText("Itens do Banco de Dados (0 itens)");
                Toast.makeText(this, "Nenhum item encontrado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            tvTitle.setText("Itens do Banco de Dados (erro)");
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();

        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        // Criar adapter com a lista de grupos
        if (adapter == null) {
            adapter = new BancoDadosExpandableAdapter(groupList);
            adapter.setDatabase(db);
            recyclerView.setAdapter(adapter);
            setupAdapters();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private List<Compra> buscarComprasPorCodigo(String codigo) {
        return CompraUtil.buscarComprasPorCodigo(db, codigo);
    }

    private Compra buscarPrimeiraCompraPorCodigo(String codigo) {
        return CompraUtil.buscarPrimeiraCompraPorCodigo(db, codigo);
    }

    private void deletarItem(BancoDadosAgrupado group, String codigo) {
        String mensagem = String.format(
                "Tem certeza que deseja excluir este item?\n\n" +
                        "Código: %s\n" +
                        "Descrição: %s\n\n" +
                        "ATENÇÃO: Isso também removerá do Firebase.",
                group.getBcDB(),
                group.getDescrDB());

        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage(mensagem)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    try {
                        // Buscar ID pelo código
                        Cursor cursor = db.rawQuery(
                                "SELECT id FROM bancodados_tab WHERE bc_DB = ? LIMIT 1",
                                new String[]{codigo}
                        );
                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            cursor.close();

                            // FirebaseBancoDadosHelper.deletarItem() já remove do Firebase E do SQLite local
                            if (firebaseBancoHelper != null) {
                                firebaseBancoHelper.deletarItem(id);
                                Toast.makeText(this, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show();
                                recarregarDados();
                            } else {
                                // Fallback: deletar apenas localmente
                                int rowsDeleted = db.delete("bancodados_tab", "id = ?",
                                        new String[]{String.valueOf(id)});
                                if (rowsDeleted > 0) {
                                    Toast.makeText(this, "Item excluído localmente", Toast.LENGTH_SHORT).show();
                                    recarregarDados();
                                } else {
                                    Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            cursor.close();
                            Toast.makeText(this, "Item não encontrado no banco", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void clonarCompra(Compra compra) {
        Intent intent = new Intent(ResultBancoDadosActivity.this, MainActivity.class);
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
}
