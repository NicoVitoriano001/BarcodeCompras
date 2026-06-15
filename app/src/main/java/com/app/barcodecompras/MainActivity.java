package com.app.barcodecompras;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;
import com.app.barcodecompras.firebase.FirebaseHelper;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.util.DatePickerUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;

//FIREBASE REALTIME
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentResult;

import com.app.barcodecompras.util.DrawerUtil; //2026.06.07

public class MainActivity extends AppCompatActivity {
    private EditText bc_compras, descr_compras, cat_compras, preco_compras,
            qnt_compras, total_compras, periodo_compras, obs_compras;

    private EditText precoEditText, qntEditText, totalEditText;
    private MaterialButton scanButton, saveButton, cancelButton, addButton;
    private SQLiteDatabase db;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseHelper firebaseHelper; // MOVER PARA VARIÁVEL GLOBAL
    private static final int REQUEST_CODE_ADD_ITEM = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {

            new AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("Conceda permissão para backup")
                    .setPositiveButton("OK", (d, w) -> bancoDadosBkp.requestStoragePermission())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        bc_compras = findViewById(R.id.bc_compras);
        descr_compras = findViewById(R.id.descr_compras);
        cat_compras = findViewById(R.id.cat_compras);
        preco_compras = findViewById(R.id.preco_compras);
        qnt_compras = findViewById(R.id.qnt_compras);
        total_compras = findViewById(R.id.total_compras);
        periodo_compras = findViewById(R.id.periodo_compras);
        obs_compras = findViewById(R.id.obs_compras);

        addButton = findViewById(R.id.addButtonMain);
        scanButton = findViewById(R.id.scanButtonMain);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        precoEditText = preco_compras;
        qntEditText = qnt_compras;
        totalEditText = total_compras;

        periodo_compras.setText(DatePickerUtil.getDataHoraAtual());
        periodo_compras.setOnClickListener(v ->
                DatePickerUtil.showDatePickerDialog(this, periodo_compras)
        );

        addButton.setOnClickListener(v -> buscarItensParaAdicionar());

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escaneie o código");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
        });

        saveButton.setOnClickListener(v -> saveData());

        cancelButton.setOnClickListener(v -> {
            clearFields();
            Toast.makeText(this, "Campos limpos", Toast.LENGTH_SHORT).show();
        });

        FloatingActionButton fabSearch = findViewById(R.id.fab_searchITEM);
        fabSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BuscarComprasActivity.class);
            startActivity(intent);
        });

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        firebaseHelper = new FirebaseHelper(this, db);
        // Sincronizar Firebase para local AO ABRIR o app
        // firebaseHelper.syncFirebaseParaLocal();
        // Sincronizar local para Firebase
        // firebaseHelper.syncLocalParaFirebase();

        setupTextWatchers();

        // DRAWER INICIO
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.result_bancodados_drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view_mainactivity);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseHelper, bancoDadosBkp);
    }
    // FIM ONCREATE

// Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        // ESCANEAMENTO
        if (result != null && result.getContents() != null) {

            String barcode = result.getContents();

            bc_compras.setText(barcode);

            // busca no banco local
            fetchItemDataBancoDadosTable(barcode);

        } else if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {

            // voltou do cadastro → busca novamente
            String barcode = bc_compras.getText().toString();
            fetchItemDataBancoDadosTable(barcode);

        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchItemDataBancoDadosTable(String barcodeValue) {

        if (db == null || !db.isOpen()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            db = dbHelper.getWritableDatabase();
        }

        Cursor cursor = db.rawQuery(
                "SELECT descr_DB, cat_DB FROM bancodados_tab WHERE bc_DB = ?",
                new String[]{barcodeValue}
        );

        if (cursor != null) {
            try {

                if (cursor.moveToFirst()) {

                    // ENCONTROU → preencher campos
                    descr_compras.setText(cursor.getString(0));
                    cat_compras.setText(cursor.getString(1));

                } else {

                    // NÃO ENCONTROU → perguntar
                    showAddItemDialog(barcodeValue);
                }

            } finally {
                cursor.close();
            }
        }
    }

    private void showAddItemDialog(String barcodeValue) {
        new AlertDialog.Builder(this)
                .setTitle("Produto não encontrado")
                .setMessage("Deseja cadastrar esse item no banco de dados?")
                .setPositiveButton("Sim", (dialog, which) -> {

                    Intent intent = new Intent(MainActivity.this, AddItemBancoDados.class);
                    intent.putExtra("BARCODE_VALUE", barcodeValue);

                    startActivityForResult(intent, REQUEST_CODE_ADD_ITEM);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void buscarItensParaAdicionar() {

        if (db == null || !db.isOpen()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            db = dbHelper.getWritableDatabase();
        }

        String codigo = bc_compras.getText().toString().trim();
        String descricao = descr_compras.getText().toString().trim();
        String categoria = cat_compras.getText().toString().trim();

        if (descricao.isEmpty() && categoria.isEmpty()) {
            Toast.makeText(this,
                    "Informe Descrição ou Categoria",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (descricao.length() < 3 && categoria.length() < 3) {
            Toast.makeText(this,
                    "Digite pelo menos 2 caracteres para busca",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder query = new StringBuilder(
                "SELECT bc_DB, descr_DB, cat_DB FROM bancodados_tab WHERE 1=1"
        );

        java.util.List<String> params = new java.util.ArrayList<>();

        // APLICAR FILTROS
        adicionarFiltro(query, params, "bc_DB", codigo, false);
        adicionarFiltro(query, params, "descr_DB", descricao, true);
        adicionarFiltro(query, params, "cat_DB", categoria, true);

        query.append(" ORDER BY descr_DB");

        Cursor cursor = db.rawQuery(query.toString(), params.toArray(new String[0]));

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhum item encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itens = new String[cursor.getCount()];
        String[] codigos = new String[cursor.getCount()];

        int i = 0;
        while (cursor.moveToNext()) {
            String cod = cursor.getString(0);
            String desc = cursor.getString(1);
            String cat = cursor.getString(2);

            codigos[i] = cod;
            //itens[i] = desc + "\n" + cat + "\n" + "────────────";
            itens[i] = cod + "\n" + desc + "\n" + cat + "\n" + "────────────";
            i++;
        }

        int totalItens = cursor.getCount();

        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Escolha um item (" + totalItens + ")")
                .setItems(itens, (dialog, which) -> {

                    String codigoSelecionado = codigos[which];

                    // MESMA REGRA DO SCAN
                    bc_compras.setText(codigoSelecionado);
                    fetchItemDataBancoDadosTable(codigoSelecionado);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void adicionarFiltro(StringBuilder query,
                                 java.util.List<String> params,
                                 String campo,
                                 String valor,
                                 boolean usarReplace) {

        if (valor == null || valor.trim().isEmpty()) return;

        String[] termos = valor.trim().split(" ");

        for (String termo : termos) {

            if (termo.startsWith("-") && termo.length() > 1) {
                // EXCLUSÃO (NOT LIKE)
                String termoLimpo = termo.substring(1);

                if (usarReplace) {
                    query.append(" AND REPLACE(").append(campo).append(", ' ', '%') NOT LIKE ?");
                    params.add("%" + termoLimpo.replace(" ", "%") + "%");
                } else {
                    query.append(" AND ").append(campo).append(" NOT LIKE ?");
                    params.add("%" + termoLimpo + "%");
                }

            } else {
                // INCLUSÃO (LIKE)
                if (usarReplace) {
                    query.append(" AND REPLACE(").append(campo).append(", ' ', '%') LIKE ?");
                    params.add("%" + termo.replace(" ", "%") + "%");
                } else {
                    query.append(" AND ").append(campo).append(" LIKE ?");
                    params.add("%" + termo + "%");
                }
            }
        }
    }

    private void saveData() {

        String bc = bc_compras.getText().toString().trim();
        String descr = descr_compras.getText().toString().trim();
        String cat = cat_compras.getText().toString().trim();
        String obs = obs_compras.getText().toString().trim();
        String periodo = periodo_compras.getText().toString();

        // DEFINIR O QUE PODE FICAR VAZIO QUANDO SALVA
        if (bc.isEmpty() || descr.isEmpty() || obs.isEmpty() ) {
            Toast.makeText(this, "BC, Descr e OBS obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco = 0;
        double qnt = 0;

        try {
            String precoStr = preco_compras.getText().toString();
            String qntStr = qnt_compras.getText().toString();

            if (!precoStr.isEmpty()) {
                preco = Double.parseDouble(precoStr);
            }

            if (!qntStr.isEmpty()) {
                qnt = Double.parseDouble(qntStr);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro nos valores", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = preco * qnt;

        long updateAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_compras", bc);
        values.put("descr_compras", descr);
        values.put("cat_compras", cat);
        values.put("preco_compras", preco);
        values.put("qnt_compras", qnt);
        values.put("total_compras", total);
        values.put("periodo_compras", periodo);
        values.put("obs_compras", obs);
        values.put("updated_at", updateAt);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            enviarParaFirebase(result, bc, descr, cat, preco, qnt, total, periodo, obs, updateAt);
            Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
            clearFields();
        }
    }

    private void enviarParaFirebase(long id, String bc, String descr, String cat,
                                    double preco, double qnt, double total,
                                    String periodo, String obs, long updateAt){

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

        String key = String.valueOf(id);

        ref.child(key).child("id").setValue(id);
        ref.child(key).child("bc").setValue(bc);
        ref.child(key).child("descricao").setValue(descr);
        ref.child(key).child("categoria").setValue(cat);
        ref.child(key).child("preco").setValue(preco);
        ref.child(key).child("quantidade").setValue(qnt);
        ref.child(key).child("total").setValue(total);
        ref.child(key).child("periodo").setValue(periodo);
        ref.child(key).child("obs").setValue(obs);
        ref.child(key).child("updateAt").setValue(updateAt);
        ref.child(key).child("deleted").setValue(false);
    }

    private void clearFields() {
        bc_compras.setText("");
        descr_compras.setText("");
        cat_compras.setText("");
        preco_compras.setText("");
        qnt_compras.setText("");
        total_compras.setText("");
    }
    private void setupTextWatchers() {

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}

            public void afterTextChanged(Editable s) {
                try {
                    String precoStr = precoEditText.getText().toString();
                    String qntStr = qntEditText.getText().toString();

                    if (!precoStr.isEmpty() && !qntStr.isEmpty()) {
                        double preco = Double.parseDouble(precoStr);
                        double qnt = Double.parseDouble(qntStr);
                        totalEditText.setText(String.valueOf(preco * qnt));
                    } else {
                        totalEditText.setText("");
                    }

                } catch (Exception e) {
                    totalEditText.setText("");
                }
            }
        };
        precoEditText.addTextChangedListener(watcher);
        qntEditText.addTextChangedListener(watcher);
    }

}