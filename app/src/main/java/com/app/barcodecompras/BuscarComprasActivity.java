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

    private void loadCompras() {
        comprasList.clear();
        Cursor cursor = db.rawQuery("SELECT * FROM compras_tab ORDER BY periodo_compras DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Compra compra = new Compra(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getDouble(4),
                        cursor.getInt(5),
                        cursor.getDouble(6),
                        cursor.getString(7),
                        cursor.getString(8)
                );
                comprasList.add(compra);
            } while (cursor.moveToNext());
        }
        cursor.close();

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