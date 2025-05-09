package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ResultCollectedActivity extends AppCompatActivity {
    private static final int EDIT_COLLECTED_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria;
    private RecyclerView recyclerView;
    private CollectedAdapter adapter;
    private SQLiteDatabase db;
    private List<Collected> CollectedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verifica se já existe uma instância
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_result_collected);

// Inicializa o RecyclerView primeiro
        recyclerView = findViewById(R.id.recyclerViewResultCollected); // Verifique se este ID está correto no XML
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializa o adapter com a lista vazia
        CollectedList = new ArrayList<>();
        adapter = new CollectedAdapter(CollectedList);
        recyclerView.setAdapter(adapter);

        // Configura o listener do adapter
        adapter.setOnItemClickListener(collected -> {
            Intent intent = new Intent(this, EditCollectedActivity.class);
            // Garanta que os dados não são nulos
            if (collected != null) {
                intent.putExtra("CODIGO", collected.getBcIMDB() != null ? collected.getBcIMDB() : "");
                intent.putExtra("DESCRICAO", collected.getDescrIMDB() != null ? collected.getDescrIMDB() : "");
                intent.putExtra("CATEGORIA", collected.getCatIMDB() != null ? collected.getCatIMDB() : "");
                startActivityForResult(intent, EDIT_COLLECTED_REQUEST);
            } else {
                Toast.makeText(this, "Item inválido", Toast.LENGTH_SHORT).show();
            }
        });

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);


        // Obter critérios de busca da intent
        Intent intent = getIntent();
        if (intent != null) {
            String codigo = intent.getStringExtra("CODIGO") != null ? intent.getStringExtra("CODIGO") : "";
            String descricao = intent.getStringExtra("DESCRICAO") != null ? intent.getStringExtra("DESCRICAO") : "";
            String categoria = intent.getStringExtra("CATEGORIA") != null ? intent.getStringExtra("CATEGORIA") : "";

            currentCodigo = codigo;
            currentDescricao = descricao;
            currentCategoria = categoria;

            loadCollected(codigo, descricao, categoria);
        }

    } //fim oncreate

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COLLECTED_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados com os mesmos critérios de busca
            loadCollected(currentCodigo, currentDescricao, currentCategoria);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadCollected(String codigo, String descricao, String categoria) {
        CollectedList.clear();

        // Verifica se todos os critérios de busca estão vazios
        if (codigo.isEmpty() && descricao.isEmpty() && categoria.isEmpty()) {
            Toast.makeText(this, "Informe pelo menos um critério de busca", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = "SELECT bc_imdb, descr_imdb, cat_imdb FROM collected_tab WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (!codigo.isEmpty()) {
            query += " AND bc_imdb LIKE ?";
            params.add("%" + codigo + "%");
        }

        if (!descricao.isEmpty()) {
            query += " AND REPLACE(descr_imdb, ' ', '%') LIKE ?";
            params.add("%" + descricao.replace(" ", "%") + "%");
        }

        if (!categoria.isEmpty()) {
            query += " AND REPLACE(cat_imdb, ' ', '%') LIKE ?";
            params.add("%" + categoria.replace(" ", "%") + "%");
        }

        query += " ORDER BY descr_imdb ASC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, params.toArray(new String[0]));

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String bc = cursor.getString(0);
                    String desc = cursor.getString(1);
                    String cat = cursor.getString(2);

                    Collected collected = new Collected(bc, desc, cat);
                    CollectedList.add(collected);
                }

                // Atualiza a UI na thread principal
                runOnUiThread(() -> {
                    if (adapter == null) {
                        adapter = new CollectedAdapter(CollectedList);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Nenhum item encontrado", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            runOnUiThread(() ->
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }
}

