package com.app.barcodecompras;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BuscarCollectedActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CollectedAdapter adapter;
    private SQLiteDatabase db;
    private List<CollectedItem> collectedList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_collected);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        loadCollectedItems();
    }

    private void loadCollectedItems() {
        collectedList.clear();
        Cursor cursor = db.rawQuery("SELECT bc_imdb, descr_imdb, cat_imdb FROM collected_tab ORDER BY descr_imdb ASC", null);

        if (cursor.moveToFirst()) {
            do {
                CollectedItem item = new CollectedItem(
                        cursor.getString(0), // bc_imdb
                        cursor.getString(1), // descr_imdb
                        cursor.getString(2)  // cat_imdb
                );
                collectedList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new CollectedAdapter(collectedList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }

    // Classe modelo para os itens coletados
    public static class CollectedItem {
        private String barcode;
        private String description;
        private String category;

        public CollectedItem(String barcode, String description, String category) {
            this.barcode = barcode;
            this.description = description;
            this.category = category;
        }
        // Getters
        public String getBarcode() { return barcode; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }
}