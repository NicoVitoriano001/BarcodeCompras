package com.app.barcodecompras;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_ADD_ITEM = 1001;
    private EditText bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras;
    private Button scanButton, saveButton, cancelButton;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bc_compras = findViewById(R.id.bc_compras);
        descr_compras = findViewById(R.id.descr_compras);
        cat_compras = findViewById(R.id.cat_compras);
        preco_compras = findViewById(R.id.preco_compras);
        qnt_compras = findViewById(R.id.qnt_compras);
        total_compras = findViewById(R.id.total_compras);
        periodo_compras = findViewById(R.id.periodo_compras);
        obs_compras = findViewById(R.id.obs_compras);
        scanButton = findViewById(R.id.scanButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

       // SQLiteDatabase: /data/user/0/com.app.barcodecompras/databases/comprasDB.db
       db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });

        saveButton.setOnClickListener(v -> saveData());

        cancelButton.setOnClickListener(v -> clearFields());

        if (checkStoragePermission()) {
            initializeDatabase();
        }

    } // fim ONCREATE


    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    // Adicione este metodo para tratar a resposta da permissão
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeDatabase();
            } else {
                Toast.makeText(this,
                        "Permissão negada - o app não pode funcionar sem acesso ao armazenamento",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private void initializeDatabase() {
        //getFilesDir() retorna um diretório privado exclusivo da aplicação. Caminho típico: /data/user/0/com.app.barcodecompras/files/COMPRAS/comprasDB.db
        File dir = new File(getApplicationContext().getFilesDir(), "COMPRAS");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dbFile = new File(dir, "comprasDB.db");

        db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        //tabela collected_tab >>> //colunas bc_imdb NUMERIC, descr_imdb TEXT, cat_imdb TEXT
        db.execSQL("CREATE TABLE IF NOT EXISTS compras_tab (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bc_compras NUMERIC," +
                "descr_compras TEXT," +
                "cat_compras TEXT," +
                "preco_compras REAL," +
                "qnt_compras INTEGER," +
                "total_compras REAL," +
                "periodo_compras TEXT," +
                "obs_compras TEXT)");
    }


    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            bc_compras.setText(result.getContents());
            fetchItemDataCollectedTable(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = bc_compras.getText().toString();
            fetchItemDataCollectedTable(barcode);
        }
    }
    /**@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = bc_compras.getText().toString();
            fetchItemDataCollectedTable(barcode);
        }
    }
**/


// Busca descr_compras e cat_compras baseado no código escaneado
// Busca descrição e categoria na tabela collected_tab
private void fetchItemDataCollectedTable(String barcodeValue) {
    if (db == null || !db.isOpen()) {
        Toast.makeText(this, "Banco de dados não disponível", Toast.LENGTH_SHORT).show();
        return;
    }

    Cursor cursor = db.rawQuery(
            "SELECT descr_imdb, cat_imdb FROM collected_tab WHERE bc_imdb = ?",
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
        int qnt_comprasVal = Integer.parseInt(qnt_comprasStr);
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

        // Log dos dados que serão salvos
        android.util.Log.d("DB_SAVE", "Salvando dados:");
        android.util.Log.d("DB_SAVE", "bc_compras: " + bc_comprasVal);
        android.util.Log.d("DB_SAVE", "descr_compras: " + descr_comprasVal);
        android.util.Log.d("DB_SAVE", "cat_compras: " + cat_comprasVal);
        android.util.Log.d("DB_SAVE", "preco_compras: " + preco_comprasVal);
        android.util.Log.d("DB_SAVE", "qnt_compras: " + qnt_comprasVal);
        android.util.Log.d("DB_SAVE", "total_compras: " + total_comprasVal);
        android.util.Log.d("DB_SAVE", "periodo_compras: " + periodo_comprasVal);
        android.util.Log.d("DB_SAVE", "obs_compras: " + obs_comprasVal);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
            clearFields();
        } else {
            Toast.makeText(this, "Erro ao salvar!", Toast.LENGTH_SHORT).show();
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
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}