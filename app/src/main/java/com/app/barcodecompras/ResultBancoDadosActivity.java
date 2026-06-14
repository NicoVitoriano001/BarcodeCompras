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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.database.BancoDados;
import com.app.barcodecompras.database.BancoDadosAdapter;
import com.app.barcodecompras.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class ResultBancoDadosActivity extends AppCompatActivity {
    private static final int EDIT_COLLECTED_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria;
    private RecyclerView recyclerView;
    private BancoDadosAdapter adapter;
    private SQLiteDatabase db;
    private List<BancoDados> BancoDadosList = new ArrayList<>();
    private TextView tvTitle; // Adicionar referência ao TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifica se já existe uma instância
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_result_bancodados);

        // Inicializar TextView do título
        tvTitle = findViewById(R.id.tvTitle);

        // Inicializa o RecyclerView primeiro
        recyclerView = findViewById(R.id.recyclerViewResultBancoDados);
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


        // Inicializa o adapter com a lista vazia
        BancoDadosList = new ArrayList<>();
        adapter = new BancoDadosAdapter(BancoDadosList);
        recyclerView.setAdapter(adapter);

        // Configura o listener do adapter
        adapter.setOnItemClickListener(bancodados -> {
            Intent intent = new Intent(this, EditBancoDadosActivity.class);
            if (bancodados != null) {
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

        // Obter critérios de busca da intent
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COLLECTED_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados com os mesmos critérios de busca
            loadBancoDados(currentCodigo, currentDescricao, currentCategoria);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBancoDados(String codigo, String descricao, String categoria) {
        BancoDadosList.clear();

        // Verifica se todos os critérios estão vazios
        if (codigo.isEmpty() && descricao.isEmpty() && categoria.isEmpty()) {
            Toast.makeText(this, "Informe pelo menos um critério de busca", Toast.LENGTH_SHORT).show();
            // Atualizar título mesmo sem resultados
            tvTitle.setText("Itens do Banco de Dados (0 itens)");
            adapter.notifyDataSetChanged();
            return;
        }

        String query = "SELECT bc_DB, descr_DB, cat_DB FROM bancodados_tab WHERE 1=1";

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
        int itemCount = 0; // Contador de itens

        try {
            cursor = db.rawQuery(query, params.toArray(new String[0]));

            if (cursor != null && cursor.getCount() > 0) {
                itemCount = cursor.getCount(); // Obter total de itens

                while (cursor.moveToNext()) {
                    String bc = cursor.getString(0);
                    String desc = cursor.getString(1);
                    String cat = cursor.getString(2);

                    BancoDados bancodados = new BancoDados(bc, desc, cat);
                    BancoDadosList.add(bancodados);
                }

                // Atualizar título com a quantidade de itens
                tvTitle.setText(String.format("Itens do Banco de Dados (%d itens)", itemCount));

                // Atualiza lista
                adapter.notifyDataSetChanged();

            } else {
                // Nenhum item encontrado
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