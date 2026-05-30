package com.app.barcodecompras;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.ui.BuscarBancoDadosActivity;
import com.app.barcodecompras.database.DatabaseHelper;
import com.google.android.material.navigation.NavigationView;

public class AddItemDB extends AppCompatActivity {
    private EditText bcImdbAdd, descrImdbAdd, catImdbAdd;
    private Button saveButton, cancelButton;
    private SQLiteDatabase db;
    private DrawerLayout drawer;
    private NavigationView navigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_imdb);

        // Inicializa views
        bcImdbAdd = findViewById(R.id.bc_DB_add);
        descrImdbAdd = findViewById(R.id.descr_DB_add);
        catImdbAdd = findViewById(R.id.cat_DB_add);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        // Recebe o valor do código de barras
        String barcode = getIntent().getStringExtra("BARCODE_VALUE");
        bcImdbAdd.setText(barcode);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Configurar listeners
        saveButton.setOnClickListener(v -> saveItem());
        cancelButton.setOnClickListener(v -> finish());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.add_bancodados_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_add_bancodados) {
                    startActivity(new Intent(this, AddItemDB.class));
                } else if (id == R.id.nav_busca_bancodados) {
                    startActivity(new Intent(this, BuscarBancoDadosActivity.class));
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay

            return true;
        }); //DRAWER -- FIM
    }//FIM ON CREATE

    private void saveItem() {
        String barcode = bcImdbAdd.getText().toString().trim();
        String description = descrImdbAdd.getText().toString().trim();
        String category = catImdbAdd.getText().toString().trim();

        if (barcode.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        long updatedAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_DB", barcode);
        values.put("descr_DB", description);
        values.put("cat_DB", category);
        values.put("updated_at", updatedAt);

        long result = db.insertWithOnConflict(
                    "bancodados_tab",
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );


            if (result != -1) {
                Toast.makeText(this, "Item salvo com sucesso", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Erro ao salvar item", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}