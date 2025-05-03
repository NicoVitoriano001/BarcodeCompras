package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ResultComprasActivity extends AppCompatActivity {
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

        loadCompras();
    }


    private void loadCompras() {
        Intent intent = getIntent();
        String bc_compras = intent.getStringExtra("bc_compras");
        String descr_compras = intent.getStringExtra("descr_compras");
        String cat_compras = intent.getStringExtra("cat_compras");
        String preco_compras = intent.getStringExtra("preco_compras");
        String total_compras = intent.getStringExtra("total_compras");
        String qnt_compras = intent.getStringExtra("qnt_compras");
        String periodo_compras = intent.getStringExtra("periodo_compras");
        String obs_compras = intent.getStringExtra("obs_compras");

        // Construir a consulta SQL
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM compras_tab WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (descr_compras != null && !descr_compras.isEmpty()) {
            queryBuilder.append(" AND REPLACE(descr_compras, ' ', '%') LIKE '%' || REPLACE(?, ' ', '%') || '%'");
            args.add(descr_compras);
        }
        if (bc_compras != null && !bc_compras.isEmpty()) {
            queryBuilder.append(" AND bc_compras = ?");
            args.add(bc_compras); // Aqui usamos '=' porque bc_compras é NUMERIC
        }
        if (cat_compras != null && !cat_compras.isEmpty()) {
            queryBuilder.append(" AND cat_compras LIKE '%' || ?");
            args.add(cat_compras);
        }
        if (preco_compras != null && !preco_compras.isEmpty()) {
            queryBuilder.append(" AND preco_compras = ?");
            args.add(preco_compras); // Aqui usamos '=' porque preco_compras é REAL
        }
        if (qnt_compras != null && !qnt_compras.isEmpty()) {
            queryBuilder.append(" AND qnt_compras = ?");
            args.add(qnt_compras); // Aqui usamos '=' porque qnt_compras é INTEGER
        }
        if (periodo_compras != null && !periodo_compras.isEmpty()) {
            queryBuilder.append(" AND periodo_compras LIKE '%' || ?");
            args.add(periodo_compras);
        }
        if (obs_compras != null && !obs_compras.isEmpty()) {
            queryBuilder.append(" AND obs_compras LIKE '%' || ?");
            args.add(obs_compras);
        }

        // Conversão dos campos double e int para String
       // if (bc_compras != null && !bc_compras.isEmpty()) {
            args.add(String.valueOf(Double.parseDouble(bc_compras))); // Converte para String
       // }
        if (preco_compras != null && !preco_compras.isEmpty()) {
            args.add(String.valueOf(Double.parseDouble(preco_compras))); // Converte para String
        }
        if (qnt_compras != null && !qnt_compras.isEmpty()) {
            args.add(String.valueOf(Integer.parseInt(qnt_compras))); // Converte para String
        }
        if (total_compras != null && !total_compras.isEmpty()) {
            args.add(String.valueOf(Integer.parseInt(total_compras))); // Converte para String
        }

        String query = queryBuilder.toString();
        comprasList.clear(); // Limpar a lista antes de carregar novos dados
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args.toArray(new String[0])); // Executar a consulta

            // Processar os resultados da consulta
            if (cursor.moveToFirst()) {
                do {
                    Compra compra = new Compra(
                            cursor.getLong(0), // ID
                            cursor.getString(1), // bc_compras
                            cursor.getString(2), // descr_compras
                            cursor.getString(3), // cat_compras
                            cursor.getDouble(4), // preco_compras
                            cursor.getInt(5), // qnt_compras
                            cursor.getDouble(6), // total_compras
                            cursor.getString(7), // periodo_compras
                            cursor.getString(8)  // obs_compras
                    );
                    comprasList.add(compra); // Adicionar à lista de compras
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace(); // Imprimir a pilha de chamadas no Logcat
        } finally {
            if (cursor != null) {
                cursor.close(); // Certifique-se de fechar o cursor
            }
        }

        // Configurar o adaptador e associá-lo ao RecyclerView
        adapter = new ComprasAdapter(comprasList);
        recyclerView.setAdapter(adapter);
    }






    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}