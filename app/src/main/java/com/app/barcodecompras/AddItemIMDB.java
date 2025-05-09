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

import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class AddItemIMDB extends AppCompatActivity {
    private EditText bcImdbAdd, descrImdbAdd, catImdbAdd;
    private Button saveButton, cancelButton;
    private SQLiteDatabase db;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    private void openDatabase() {
        File dbFile = getDatabasePath("comprasDB.db");

        // Verifica se o banco de dados existe
        if (!dbFile.exists()) {
            // Se não existir, você pode criar o banco de dados e a tabela aqui
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            createTable();
        } else {
            // Se existir, apenas abra o banco de dados
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        }
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS collected_tab (" +
           //     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bc_imdb INTEGER, " +
                "descr_imdb TEXT, " +
                "cat_imdb TEXT)";
        db.execSQL(createTableSQL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_imdb);

        // Inicializa views
        bcImdbAdd = findViewById(R.id.bc_imdb_add);
        descrImdbAdd = findViewById(R.id.descr_imdb_add);
        catImdbAdd = findViewById(R.id.cat_imdb_add);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        // Recebe o valor do código de barras
        String barcode = getIntent().getStringExtra("BARCODE_VALUE");
        bcImdbAdd.setText(barcode);

        openDatabase();

        // Configurar listeners
        saveButton.setOnClickListener(v -> saveItem());
        cancelButton.setOnClickListener(v -> finish());


        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.add_collected_nav_view);
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
        }); //DRAWER -- INICIO


    }//FIM ON CREATE

    private void saveItem() {
        String barcode = bcImdbAdd.getText().toString().trim();
        String description = descrImdbAdd.getText().toString().trim();
        String category = catImdbAdd.getText().toString().trim();

        if (barcode.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("bc_imdb", barcode);
        values.put("descr_imdb", description);
        values.put("cat_imdb", category);

        try {
            long result = db.insert("collected_tab", null, values);

            if (result != -1) {
                Toast.makeText(this, "Item salvo com sucesso", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Erro ao salvar item", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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