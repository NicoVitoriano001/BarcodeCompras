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
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;


public class MainActivity extends AppCompatActivity {
    // Adicione no início da classe
    private static final int REQUEST_CODE = 1;

    private EditText bcCompras, item, categoria, preco, qnt, total, periodo, obs;
    private Button scanButton, saveButton, cancelButton;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bcCompras = findViewById(R.id.bcCompras);
        item = findViewById(R.id.item);
        categoria = findViewById(R.id.categoria);
        preco = findViewById(R.id.preco);
        qnt = findViewById(R.id.qnt);
        total = findViewById(R.id.total);
        periodo = findViewById(R.id.periodo);
        obs = findViewById(R.id.obs);
        scanButton = findViewById(R.id.scanButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        File dir = new File("/storage/emulated/0/Download/COMPRAS/");
        if (!dir.exists()) {
            dir.mkdirs(); // cria diretório
        }

        File dbFile = new File("/storage/emulated/0/Download/COMPRAS/comprasDB.db");
        //db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        // Android 10+ Substituir a inicialização do banco por:
        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Botão de scan
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


    // Adicione este método para tratar a resposta da permissão
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
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "COMPRAS");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dbFile = new File(dir, "comprasDB.db");
        db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        // Criar tabela se não existir
        db.execSQL("CREATE TABLE IF NOT EXISTS compras_tab (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bcCompras TEXT," +
                "item TEXT," +
                "categoria TEXT," +
                "preco REAL," +
                "qnt INTEGER," +
                "total REAL," +
                "periodo TEXT," +
                "obs TEXT)");
    }



    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            bcCompras.setText(result.getContents());
            fetchItemData(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
    }

    // Busca item e categoria baseado no código escaneado
    private void fetchItemData(String bcComprasValue) {
        // Corrigindo o nome da coluna para bcCompras (consistente com o insert)
        Cursor cursor = db.rawQuery("SELECT item, categoria FROM compras_tab WHERE bcCompras = ?",
                new String[]{bcComprasValue});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    item.setText(cursor.getString(0));
                    categoria.setText(cursor.getString(1));
                } else {
                    item.setText("");
                    categoria.setText("");
                    Toast.makeText(this, "Item não encontrado no banco.", Toast.LENGTH_SHORT).show();
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void saveData() {
        String bcComprasVal = bcCompras.getText().toString().trim();
        String itemVal = item.getText().toString().trim();
        String categoriaVal = categoria.getText().toString().trim();
        String precoStr = preco.getText().toString().trim();
        String qntStr = qnt.getText().toString().trim();
        String periodoVal = periodo.getText().toString().trim();
        String obsVal = obs.getText().toString().trim();

        if (bcComprasVal.isEmpty() || precoStr.isEmpty() || qntStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        double precoVal = Double.parseDouble(precoStr);
        int qntVal = Integer.parseInt(qntStr);
        double totalVal = precoVal * qntVal;

        total.setText(String.valueOf(totalVal));

        ContentValues values = new ContentValues();
        values.put("bcCompras", bcComprasVal);
        values.put("item", itemVal);
        values.put("categoria", categoriaVal);
        values.put("preco", precoVal);
        values.put("qnt", qntVal);
        values.put("total", totalVal);
        values.put("periodo", periodoVal);
        values.put("obs", obsVal);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
            clearFields();
        } else {
            Toast.makeText(this, "Erro ao salvar!", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearFields() {
        bcCompras.setText("");
        item.setText("");
        categoria.setText("");
        preco.setText("");
        qnt.setText("");
        total.setText("");
        periodo.setText("");
        obs.setText("");
    }

    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}