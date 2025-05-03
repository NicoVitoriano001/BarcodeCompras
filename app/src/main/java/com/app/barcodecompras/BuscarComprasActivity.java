package com.app.barcodecompras;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BuscarComprasActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ComprasAdapter adapter;
    private SQLiteDatabase db;
    private List<Compra> comprasList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_compras);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        loadCompras();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar os dados quando a atividade retornar ao primeiro plano
        loadCompras();
    }

    private void loadCompras() {
        comprasList.clear();
        Cursor cursor = db.rawQuery("SELECT * FROM compras_tab ORDER BY periodo_compras DESC", null);

        if (cursor.moveToFirst()) {
            do {
                // Certifique-se de que o total está sendo calculado corretamente
                double preco = cursor.getDouble(4);
                int quantidade = cursor.getInt(5);
                double total = preco * quantidade;  // Recalculando o total

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

        if (adapter == null) {
            adapter = new ComprasAdapter(comprasList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}