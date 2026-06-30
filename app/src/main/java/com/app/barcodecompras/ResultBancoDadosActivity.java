package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.database.BancoDados;
import com.app.barcodecompras.database.BancoDadosAdapter;
import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultBancoDadosActivity extends AppCompatActivity {
    private static final int EDIT_COLLECTED_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria;
    private RecyclerView recyclerView;
    private BancoDadosAdapter adapter;
    private SQLiteDatabase db;
    private List<BancoDados> BancoDadosList = new ArrayList<>();
    private TextView tvTitle;
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

        BancoDadosList = new ArrayList<>();
        adapter = new BancoDadosAdapter(BancoDadosList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(bancodados -> {
            Intent intent = new Intent(this, EditBancoDadosActivity.class);
            if (bancodados != null) {
                intent.putExtra("ID", bancodados.getId());
                intent.putExtra("CODIGO", bancodados.getBcIMDB() != null ? bancodados.getBcIMDB() : "");
                intent.putExtra("DESCRICAO", bancodados.getDescrIMDB() != null ? bancodados.getDescrIMDB() : "");
                intent.putExtra("CATEGORIA", bancodados.getCatIMDB() != null ? bancodados.getCatIMDB() : "");
                startActivityForResult(intent, EDIT_COLLECTED_REQUEST);
            } else {
                Toast.makeText(this, "Item inválido", Toast.LENGTH_SHORT).show();
            }
        });

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

        if (requestCode == EDIT_COLLECTED_REQUEST && resultCode == RESULT_OK) {
            loadBancoDados(currentCodigo, currentDescricao, currentCategoria);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBancoDados(String codigo, String descricao, String categoria) {
        BancoDadosList.clear();

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

        query += " ORDER BY descr_DB ASC";

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

                    BancoDadosList.add(bancodados);
                }

                tvTitle.setText(String.format("Itens do Banco de Dados (%d itens)", itemCount));
                adapter.notifyDataSetChanged();

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
    }
}