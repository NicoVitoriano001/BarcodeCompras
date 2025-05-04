package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ResultComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria, currentPeriodo;
    private RecyclerView recyclerView;
    private ComprasAdapter adapter;
    private SQLiteDatabase db;
    private List<Compra> comprasList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_compras);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Obter critérios de busca da intent
        String codigo = getIntent().getStringExtra("CODIGO");
        String descricao = getIntent().getStringExtra("DESCRICAO");
        String categoria = getIntent().getStringExtra("CATEGORIA");
        String periodo = getIntent().getStringExtra("PERIODO");

        loadCompras(codigo, descricao, categoria, periodo);

        // Obter e armazenar critérios de busca atuais
        currentCodigo = getIntent().getStringExtra("CODIGO") != null ? getIntent().getStringExtra("CODIGO") : "";
        currentDescricao = getIntent().getStringExtra("DESCRICAO") != null ? getIntent().getStringExtra("DESCRICAO") : "";
        currentCategoria = getIntent().getStringExtra("CATEGORIA") != null ? getIntent().getStringExtra("CATEGORIA") : "";
        currentPeriodo = getIntent().getStringExtra("PERIODO") != null ? getIntent().getStringExtra("PERIODO") : "";

        // Configurar clique nos itens da lista
        adapter.setOnItemClickListener(compra -> {
            Intent intent = new Intent(ResultComprasActivity.this, EditComprasActivity.class);
            intent.putExtra("compra_id", compra.getId());
            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
        });

    } //fim oncreate

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados com os mesmos critérios de busca
            loadCompras(currentCodigo, currentDescricao, currentCategoria, currentPeriodo);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadCompras(String codigo, String descricao, String categoria, String periodo) {
        comprasList.clear();
        double somaTotal = 0.0; // Variável para acumular a soma

        // Construir query dinâmica baseada nos critérios de busca
        String query = "SELECT * FROM compras_tab WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (!codigo.isEmpty()) {
            query += " AND bc_compras LIKE ?";
            params.add("%" + codigo + "%");
        }

        if (!descricao.isEmpty()) {
            query += " AND descr_compras LIKE ?";
            params.add("%" + descricao + "%");
        }

        if (!categoria.isEmpty()) {
            query += " AND cat_compras LIKE ?";
            params.add("%" + categoria + "%");
        }

        if (!periodo.isEmpty()) {
            query += " AND periodo_compras LIKE ?";
            params.add("%" + periodo + "%");
        }

        query += " ORDER BY descr_compras ASC";

        Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                // Certifique-se de que o total está sendo calculado corretamente
                double preco = cursor.getDouble(4);
                double quantidade = cursor.getDouble(5);
                double total = preco * quantidade;  // Recalculando o total
                somaTotal += total; // Acumula o total

                Compra compra = new Compra(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        preco,
                        quantidade,
                        total,  // Usando o valor recalculado
                        cursor.getString(7),
                        cursor.getString(8)
                );
                comprasList.add(compra);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Atualizar o TextView com a soma total
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(String.format("Soma parcial: R$ %.2f", somaTotal));

        if (adapter == null) {
            adapter = new ComprasAdapter(comprasList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

}

