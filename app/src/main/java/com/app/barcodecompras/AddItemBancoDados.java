package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class AddItemBancoDados extends AppCompatActivity {
    private EditText bcImdbAdd, descrImdbAdd, catImdbAdd;
    private Button saveButton, cancelButton;
    private SQLiteDatabase db;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;

    // Constantes para retorno dos dados
    public static final String EXTRA_BARCODE = "EXTRA_BARCODE";
    public static final String EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION";
    public static final String EXTRA_CATEGORY = "EXTRA_CATEGORY";

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
        if (barcode != null) {
            bcImdbAdd.setText(barcode);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        // Configurar listeners
        saveButton.setOnClickListener(v -> saveItem());
        cancelButton.setOnClickListener(v -> {
            // Ao cancelar, retorna sem dados
            setResult(RESULT_CANCELED);
            finish();
        });
        catImdbAdd.setOnClickListener(v -> abrirDialogCategorias());

        // DRAWER
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.add_bancodados_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
    }

    private void saveItem() {
        String barcode = bcImdbAdd.getText().toString().trim();
        String description = descrImdbAdd.getText().toString().trim();
        String category = catImdbAdd.getText().toString().trim();

        if (barcode.isEmpty() || description.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // ===== VERIFICAR DUPLICATA (apenas bc_DB) =====
        Cursor cursorDuplicata = db.rawQuery(
                "SELECT id, bc_DB, descr_DB, cat_DB FROM bancodados_tab WHERE bc_DB = ? LIMIT 1",
                new String[]{barcode}
        );
        if (cursorDuplicata.moveToFirst()) {
            String idExistente = String.valueOf(cursorDuplicata.getLong(0));
            String bcExistente = cursorDuplicata.getString(1);
            String descrExistente = cursorDuplicata.getString(2);
            String catExistente = cursorDuplicata.getString(3);
            cursorDuplicata.close();

            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Item Já Existe")
                    .setMessage("Já existe um item com este código de barras:\n\n" +
                            "📦 Código: " + bcExistente + "\n" +
                            "📝 Descrição: " + descrExistente + "\n" +
                            "📂 Categoria: " + catExistente + "\n\n" +
                            "Deseja editar o item existente?")
                    .setPositiveButton("Editar", (dialog, which) -> {
                        Intent editIntent = new Intent(AddItemBancoDados.this, EditBancoDadosActivity.class);
                        editIntent.putExtra("ID", Long.parseLong(idExistente));
                        editIntent.putExtra("CODIGO", bcExistente);
                        editIntent.putExtra("DESCRICAO", descrExistente);
                        editIntent.putExtra("CATEGORIA", catExistente);
                        startActivity(editIntent);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        cursorDuplicata.close();
        // ===================================================

        try {
            long result = firebaseBancoHelper.inserirItem(barcode, description, category);

            if (result != -1) {
                Toast.makeText(this, "Item salvo com sucesso", Toast.LENGTH_SHORT).show();

                // Cria Intent para retornar os dados
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_BARCODE, barcode);
                resultIntent.putExtra(EXTRA_DESCRIPTION, description);
                resultIntent.putExtra(EXTRA_CATEGORY, category);

                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Erro ao salvar item", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirDialogCategorias() {
        Cursor cursor = db.rawQuery(
                "SELECT cat_DB FROM bancodados_tab GROUP BY cat_DB ORDER BY cat_DB",
                null
        );

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhuma categoria encontrada", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
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

        // Criar dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Selecionar Categoria")
                .setView(layout)
                .setNegativeButton("Fechar", null)
                .create();

        // Clique na lista
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String categoriaSelecionada = adapter.getItem(position);
            catImdbAdd.setText(categoriaSelecionada);
            dialog.dismiss();
        });

        // Filtro
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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