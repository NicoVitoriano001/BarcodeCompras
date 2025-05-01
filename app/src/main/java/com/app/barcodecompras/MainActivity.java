package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;

public class MainActivity extends AppCompatActivity {
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
        db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

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
        Cursor cursor = db.rawQuery("SELECT item, categoria FROM compras_tab WHERE bcCollected = ?", new String[]{bcComprasValue});
        if (cursor.moveToFirst()) {
            item.setText(cursor.getString(0));
            categoria.setText(cursor.getString(1));
        } else {
            item.setText("");
            categoria.setText("");
            Toast.makeText(this, "Item não encontrado no banco.", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
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