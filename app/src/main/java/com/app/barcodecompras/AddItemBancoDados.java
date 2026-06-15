package com.app.barcodecompras;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;  //manter esse
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;


public class AddItemBancoDados extends AppCompatActivity {
    private EditText bcImdbAdd, descrImdbAdd, catImdbAdd;
    private Button saveButton, cancelButton;
    private SQLiteDatabase db;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item_bancodados);

        // Inicializa views
        bcImdbAdd = findViewById(R.id.bc_DB_add);
        descrImdbAdd = findViewById(R.id.descr_DB_add);
        catImdbAdd = findViewById(R.id.cat_DB_add);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);

        catImdbAdd.setFocusable(false);
        catImdbAdd.setClickable(true);

        // Recebe o valor do código de barras
        String barcode = getIntent().getStringExtra("BARCODE_VALUE");
        bcImdbAdd.setText(barcode);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        firebaseHelper = new FirebaseHelper(this, db);

        // Configurar listeners
        saveButton.setOnClickListener(v -> saveItem());
        cancelButton.setOnClickListener(v -> finish());

        catImdbAdd.setOnClickListener(v -> abrirDialogCategorias());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.add_bancodados_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseHelper, bancoDadosBkp);

    }//FIM ON CREATE

    private void saveItem() {
        String barcode = bcImdbAdd.getText().toString().trim();
        String description = descrImdbAdd.getText().toString().trim();
        String category = catImdbAdd.getText().toString().trim();

        if (barcode.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        //long updateAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_DB", barcode);
        values.put("descr_DB", description);
        values.put("cat_DB", category);

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

    private void abrirDialogCategorias() {

        Cursor cursor = db.rawQuery(
                "SELECT cat_DB FROM bancodados_tab GROUP BY cat_DB ORDER BY cat_DB",
                null
        );

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhuma categoria encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> lista = new ArrayList<>();

        while (cursor.moveToNext()) {
            lista.add(cursor.getString(0));
        }
        cursor.close();

        // Campo de busca
        EditText searchInput = new EditText(this);
        searchInput.setHint("Buscar categoria...");

        // Lista
        ListView listView = new ListView(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                lista
        );

        listView.setAdapter(adapter);

        // Layout do dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        layout.addView(searchInput);
        layout.addView(listView);

        // CRIA O DIALOG AQUI
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Selecionar Categoria")
                .setView(layout)
                .setNegativeButton("Fechar", null)
                .create();

        // CLIQUE NA LISTA (AQUI É ONDE VAI O DISMISS)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String categoriaSelecionada = adapter.getItem(position);
            catImdbAdd.setText(categoriaSelecionada);

            dialog.dismiss(); // FECHA O DIALOG AQUI
        });

        // FILTRO
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // MOSTRA O DIALOG
        dialog.show();
    }







        @Override
        protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}