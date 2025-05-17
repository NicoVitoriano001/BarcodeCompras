package com.app.barcodecompras;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import android.os.Environment;
import androidx.core.view.GravityCompat;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {
private static final int REQUEST_CODE = 1;
private static final int REQUEST_CODE_ADD_ITEM = 1001;
private EditText bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras;
private Button scanButton, saveButton, cancelButton;
private SQLiteDatabase db;
private DrawerLayout drawer;
private ActionBarDrawerToggle toggle;
private BancoDadosBkp bancoDadosBkp;
private EditText precoEditText;
private EditText qntEditText;
private EditText totalEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa o helper de backup (Adicionado)
        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        // Verificar permissão (modificado para usar a nova classe)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("Para salvar backups na pasta Downloads, por favor conceda a permissão 'Gerenciar todos os arquivos'")
                    .setPositiveButton("OK", (dialog, which) -> bancoDadosBkp.requestStoragePermission())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        // Inicializa os EditTexts
        precoEditText = findViewById(R.id.preco_compras);
        qntEditText = findViewById(R.id.qnt_compras);
        totalEditText = findViewById(R.id.total_compras);

        // Configura os TextWatchers
        setupTextWatchers();

        //inicialização de views
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

        periodo_compras.setText(getDataHoraAtual()); // Define a data atual ao inicia

        FloatingActionButton fabSearch = findViewById(R.id.fab_searchITEM);
        fabSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BuscarComprasActivity.class);
            startActivity(intent);
        });

    // Banco de dados (modificado para usar DatabaseHelper)
    //data/data/com.app.barcodecompras/databases/comprasDB.db
       DatabaseHelper dbHelper = new DatabaseHelper(this);
       db = dbHelper.getWritableDatabase();
    // SQLiteDatabase: /data/user/0/com.app.barcodecompras/databases/comprasDB.db
    //db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

       Button scanButton = findViewById(R.id.scanButtonMain);

       scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
       });

       periodo_compras.setOnClickListener(v -> showDatePickerDialog());
       saveButton.setOnClickListener(v -> saveData());
       cancelButton.setOnClickListener(v -> clearFields());

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
                    Intent intent = new Intent(MainActivity.this, AddItemIMDB.class);
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
    } // fim ONCREATE

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
        if (result != null && result.getContents() != null) {
            bc_compras.setText(result.getContents());
            fetchItemDataBancoDadosTable(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = bc_compras.getText().toString();
            fetchItemDataBancoDadosTable(barcode);
        }
    }

// Busca descr_compras e cat_compras baseado no código escaneado
// Busca descrição e categoria na tabela bancodados_tab
    private void fetchItemDataBancoDadosTable(String barcodeValue) {
    if (db == null || !db.isOpen()) {
        db = getDatabase();
        Toast.makeText(this, "Banco de dados não disponível", Toast.LENGTH_SHORT).show();
        return;
    }

    Cursor cursor = db.rawQuery(
            "SELECT descr_imdb, cat_imdb FROM bancodados_tab WHERE bc_imdb = ?",
            new String[]{barcodeValue}
    );

    if (cursor != null) {
        try {
            if (cursor.moveToFirst()) {
                descr_compras.setText(cursor.getString(0)); // descr_imdb
                cat_compras.setText(cursor.getString(1));   // cat_imdb
            } else {
                // Item não encontrado - abrir activity de cadastro
                Intent intent = new Intent(MainActivity.this, AddItemIMDB.class);
                intent.putExtra("BARCODE_VALUE", barcodeValue);
                startActivityForResult(intent, REQUEST_CODE_ADD_ITEM);
            }
        } finally {
            cursor.close();
        }
    }
}

    // Metodo auxiliar para obter a instância do banco
    private SQLiteDatabase getDatabase() {
        if (db == null || !db.isOpen()) {
            File dbFile = getDatabasePath("comprasDB.db");
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        }
        return db;
    }
    private void saveData() {
        String bc_comprasVal = bc_compras.getText().toString().trim();
        String descr_comprasVal = descr_compras.getText().toString().trim();
        String cat_comprasVal = cat_compras.getText().toString().trim();
        String preco_comprasStr = preco_compras.getText().toString().trim();
        String qnt_comprasStr = qnt_compras.getText().toString().trim();
        String periodo_comprasVal = periodo_compras.getText().toString().trim();
        String obs_comprasVal = obs_compras.getText().toString().trim();

        if (bc_comprasVal.isEmpty() || preco_comprasStr.isEmpty() || qnt_comprasStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco_comprasVal = Double.parseDouble(preco_comprasStr);
        double qnt_comprasVal = Double.parseDouble(qnt_comprasStr);
        double total_comprasVal = preco_comprasVal * qnt_comprasVal;

        total_compras.setText(String.valueOf(total_comprasVal));

        ContentValues values = new ContentValues();
        values.put("bc_compras", bc_comprasVal);
        values.put("descr_compras", descr_comprasVal);
        values.put("cat_compras", cat_comprasVal);
        values.put("preco_compras", preco_comprasVal);
        values.put("qnt_compras", qnt_comprasVal);
        values.put("total_compras", total_comprasVal);
        values.put("periodo_compras", periodo_comprasVal);
        values.put("obs_compras", obs_comprasVal);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
            clearFields();
        } else {
            Toast.makeText(this, "Erro ao salvar!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, pode continuar
            } else {
                Toast.makeText(this, "Permissão negada. Não é possível fazer backup/restore.", Toast.LENGTH_SHORT).show();
            }
        }
    }
   private void clearFields() {
        bc_compras.setText("");
        descr_compras.setText("");
        cat_compras.setText("");
        preco_compras.setText("");
        qnt_compras.setText("");
        total_compras.setText("");
        periodo_compras.setText("");
        obs_compras.setText("");
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            if (db.isOpen()) {
                db.close();
            }
            db = null;
        }
        super.onDestroy();
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calcularTotal();
            }
        };

        precoEditText.addTextChangedListener(textWatcher);
        qntEditText.addTextChangedListener(textWatcher);
    }

    private void calcularTotal() {
        try {
            // Obtém os valores dos campos
            String precoStr = precoEditText.getText().toString();
            String qntStr = qntEditText.getText().toString();

            // Converte para números
            double preco = precoStr.isEmpty() ? 0 : Double.parseDouble(precoStr);
            double qnt = qntStr.isEmpty() ? 0 : Double.parseDouble(qntStr);

            // Calcula o total
            double total = preco * qnt;

            // Atualiza o campo total
            totalEditText.setText(String.format(Locale.getDefault(), "%.2f", total));
        } catch (NumberFormatException e) {
            // Caso ocorra erro na conversão
            totalEditText.setText("");
        }
    }
}