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

public class ResultCollectedActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private String currentCodigo, currentDescricao, currentCategoria;
    private RecyclerView recyclerView;
    private CollectedAdapter adapter;
    private SQLiteDatabase db;
    private List<Collected> CollectedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_collected);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Obter critérios de busca da intent
        String codigo = getIntent().getStringExtra("CODIGO");
        String descricao = getIntent().getStringExtra("DESCRICAO");
        String categoria = getIntent().getStringExtra("CATEGORIA");

        loadCollected(codigo, descricao, categoria);

        // Obter e armazenar critérios de busca atuais
        currentCodigo = getIntent().getStringExtra("CODIGO") != null ? getIntent().getStringExtra("CODIGO") : "";
        currentDescricao = getIntent().getStringExtra("DESCRICAO") != null ? getIntent().getStringExtra("DESCRICAO") : "";
        currentCategoria = getIntent().getStringExtra("CATEGORIA") != null ? getIntent().getStringExtra("CATEGORIA") : "";


        /** Configurar clique nos itens da lista
        adapter.setOnItemClickListener(collected -> {
            Intent intent = new Intent(ResultCollectedActivity.this, EditCollectedActivity.class);
            intent.putExtra("collected_id", collected.getId());
            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
        });
**/
    } //fim oncreate

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados com os mesmos critérios de busca
            loadCollected(currentCodigo, currentDescricao, currentCategoria);
            Toast.makeText(this, "Lista atualizada", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadCollected(String codigo, String descricao, String categoria) {
        CollectedList.clear();


        // Construir query dinâmica baseada nos critérios de busca
        String query = "SELECT bc_imdb, descr_imdb, cat_imdb FROM collected_tab WHERE 1=1";
       // String query = "SELECT * FROM collected_tab WHERE 1=1";
        List<String> params = new ArrayList<>();

        if (!codigo.isEmpty()) {
            query += " AND bc_imdb LIKE ?";
            params.add("%" + codigo + "%");
        }

        if (!descricao.isEmpty()) {
            query += " AND descr_imdb LIKE ?";
            params.add("%" + descricao + "%");
        }

        if (!categoria.isEmpty()) {
            query += " AND cat_imdb LIKE ?";
            params.add("%" + categoria + "%");
        }

        query += " ORDER BY descr_imdb ASC";

        Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));
        //Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                Collected collected = new Collected(
                        cursor.getString(0), // bc_imdb //cursor.getLong(0),
                        cursor.getString(1), // descr_imdb
                        cursor.getString(2) // cat_imdb
                );
                CollectedList.add(collected);
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Atualizar o TextView com a soma total
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(String.format("Soma parcial: R$ %.2f"));

        if (adapter == null) {
            adapter = new CollectedAdapter(CollectedList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

}

