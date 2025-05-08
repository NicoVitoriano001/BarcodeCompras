package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_compras);

        recyclerView = findViewById(R.id.recyclerViewCompras);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

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
            intent.putExtra("compra_id", compra.getId());
            startActivityForResult(intent, EDIT_COMPRA_REQUEST);
        });

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.resul_compras_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_add_collected) {
                    startActivity(new Intent(this, AddItemIMDB.class));
                } else if (id == R.id.nav_busca_collected) {
                    startActivity(new Intent(this, BuscarCollectedActivity.class));
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay
            return true;
        });//DRAWER -- INICIO
    } //fim oncreate

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

        if (!observacao.isEmpty()) {
            query += " AND obs_compras LIKE ?";
            params.add("%" + observacao + "%");
        }

      //  query += " ORDER BY descr_imdb ASC, SUBSTR(periodo_compras, 5) ASC"; // Pega a partir do 5º caractere
      //  query += " ORDER BY periodo_compras ASC, descr_compras ASC";
       query += " ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras ASC";

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

