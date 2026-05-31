package com.app.barcodecompras;

import android.app.DatePickerDialog;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.CompraFirebase;
import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;

// ✅ FIREBASE REALTIME
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText bc_compras, descr_compras, cat_compras, preco_compras,
            qnt_compras, total_compras, periodo_compras, obs_compras;

    private EditText precoEditText, qntEditText, totalEditText;

    private MaterialButton scanButton, saveButton, cancelButton;

    private SQLiteDatabase db;

    private DrawerLayout drawer;

    private ActionBarDrawerToggle toggle;

    private BancoDadosBkp bancoDadosBkp;

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

        scanButton = findViewById(R.id.scanButtonMain);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        precoEditText = preco_compras;
        qntEditText = qnt_compras;
        totalEditText = total_compras;

        periodo_compras.setText(getDataHoraAtual());

        periodo_compras.setOnClickListener(v -> showDatePickerDialog());
        
        saveButton.setOnClickListener(v -> saveData());       

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escaneie o código");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
        });

        cancelButton.setOnClickListener(v -> {
            clearFields();
            Toast.makeText(this, "Campos limpos", Toast.LENGTH_SHORT).show();
        });
        //cancelButton.setOnClickListener(v -> clearFields());


        FloatingActionButton fabSearch = findViewById(R.id.fab_searchITEM);
        fabSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BuscarComprasActivity.class);
            startActivity(intent);
        });


        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        FirebaseHelper firebaseHelper = new FirebaseHelper(this, db);

        //firebaseHelper.syncLocalParaFirebase();   //faz a mesma coisa enviarParaFirebase(...)
        firebaseHelper.syncFirebaseParaLocal();

        setupTextWatchers();

        NavigationView navigationView = findViewById(R.id.nav_view_mainactivity);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START); // Fecha o drawer imediatamente

                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                   // return true; // Indica que o clique foi tratado
                } else if (id == R.id.nav_gallery) {
                    // Ação para galeria
                } else if (id == R.id.nav_slideshow) {
                    // Ação para slideshow
                } else if (id == R.id.nav_add_bancodados) {
                    Intent intent = new Intent(MainActivity.this, AddItemBancoDados.class);
                    startActivity(intent);
                } else if (id == R.id.nav_busca_bancodados) {
                    Intent intent = new Intent(MainActivity.this, BuscarBancoDadosActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_busca_compras) {
                    Intent intent = new Intent(MainActivity.this, BuscarComprasActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_backup) {
                    bancoDadosBkp.showBackupConfirmationDialog(); //showBackupConfirmationDialog(); // Substitui a chamada direta a fazerBackup() fazerBackup();
                } else if (id == R.id.nav_restore) {
                    bancoDadosBkp.restaurarBackup(); //restaurarBackup();
                }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });


// Configurar Toolbar (usando a versão AppCompat)
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

// Configurar Navigation Drawer
        drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

/// FIM ONCREATE
    }


//inicio data calendário
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
                    periodo_compras.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day);
        datePickerDialog.show();
    }

    public String getDataHoraAtual() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
//fim data calendario


    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        // ESCANEAMENTO
        if (result != null && result.getContents() != null) {

            String barcode = result.getContents();

            bc_compras.setText(barcode);

            //busca no banco local
            fetchItemDataBancoDadosTable(barcode);

        } else if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {

            //voltou do cadastro → busca novamente
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

                    //ENCONTROU → preencher campos
                    descr_compras.setText(cursor.getString(0));
                    cat_compras.setText(cursor.getString(1));

                } else {

                    //NÃO ENCONTROU → perguntar
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


    private void saveData() {

        String bc = bc_compras.getText().toString().trim();
        String descr = descr_compras.getText().toString().trim();
        String cat = cat_compras.getText().toString().trim();
        String obs = obs_compras.getText().toString().trim();
        String periodo = periodo_compras.getText().toString();

        //DEFINIR O QUE PODE FICAR VAZIO QUANDO SALVA
        if (bc.isEmpty() || descr.isEmpty() || obs.isEmpty() ) {
            Toast.makeText(this, "Código obrigatório", Toast.LENGTH_SHORT).show();
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

        long updatedAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_compras", bc);
        values.put("descr_compras", descr);
        values.put("cat_compras", cat);
        values.put("preco_compras", preco);
        values.put("qnt_compras", qnt);
        values.put("total_compras", total);
        values.put("periodo_compras", periodo);
        values.put("obs_compras", obs);
        values.put("updated_at", updatedAt);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            enviarParaFirebase(bc, descr, cat, preco, qnt, total, periodo, obs, updatedAt);
            Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
            clearFields();
        }
    }


private void enviarParaFirebase(String bc, String descr, String cat,
                                double preco, double qnt, double total,
                                String periodo, String obs, long updatedAt){

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

    ref.push().setValue(new CompraFirebase(
            bc, descr, cat, preco, qnt, total, periodo, obs, updatedAt
    ));
}



    private void clearFields() {
        bc_compras.setText("");
        descr_compras.setText("");
        cat_compras.setText("");
        preco_compras.setText("");
        qnt_compras.setText("");
        total_compras.setText("");
    }

    //private String getDataHoraAtual() {
    //    return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
   // }



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