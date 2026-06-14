package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class BuscarBancoDadosActivity extends AppCompatActivity {
    private static final int EDIT_DB_REQUEST = 1;
    private static final int BUSCA_DB_REQUEST = 1001;
    private EditText etBuscaCodigoBancoDados, etBuscaDescricaoBancoDados, etBuscaCategoriaBancoDados;
    private Button btnBuscarBancoDados, btnCancelarBancoDados;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private SQLiteDatabase db;
    private Button scanButtonBuscaDB;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseHelper firebaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_bancodados);

        // Inicializar views
        etBuscaCodigoBancoDados = findViewById(R.id.etBuscaCodigoBancoDados);
        etBuscaDescricaoBancoDados = findViewById(R.id.etBuscaDescricaoBancoDados);
        etBuscaCategoriaBancoDados = findViewById(R.id.etBuscaCategoriaBancoDados);
        btnBuscarBancoDados = findViewById(R.id.btnBuscarBancoDados);
        btnCancelarBancoDados = findViewById(R.id.btnCancelarBuscaBancoDados);

        // Inicializar botão de scan
        scanButtonBuscaDB = findViewById(R.id.scanButtonBuscaDB);
        scanButtonBuscaDB.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(BuscarBancoDadosActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });

        // Banco de dados
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        // INICIALIZAR VARIÁVEL LOCAL
        firebaseHelper = new FirebaseHelper(this, db);

        // Configurar listeners
        btnBuscarBancoDados.setOnClickListener(v -> realizarBuscaBancoDados());
        btnCancelarBancoDados.setOnClickListener(v -> finish());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.busca_bancodados_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseHelper, bancoDadosBkp);

    }
    // FIM ON CREATE

    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // recebe e rata resultado do scanner
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            etBuscaCodigoBancoDados.setText(result.getContents());
            fetchItemDataBancoDadosTable(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == BUSCA_DB_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = etBuscaCodigoBancoDados.getText().toString();
            fetchItemDataBancoDadosTable(barcode);
        }

        if (requestCode == EDIT_DB_REQUEST && resultCode == RESULT_OK) {
            finish();
        }
    }

    // Busca descrição e categoria na tabela bancodados_tab
    private void fetchItemDataBancoDadosTable(String barcodeValue) {
        if (db == null || !db.isOpen()) {
            db = getDatabase();
            Toast.makeText(this, "Banco de dados não disponível", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = db.rawQuery(
                "SELECT descr_DB, cat_DB FROM bancodados_tab WHERE bc_DB = ?",
                new String[]{barcodeValue}
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    etBuscaDescricaoBancoDados.setText(cursor.getString(0)); // descr_DB
                    etBuscaCategoriaBancoDados.setText(cursor.getString(1)); // cat_DB
                } else {
                    // Item não encontrado - abrir activity de cadastro
                    Intent intent = new Intent(BuscarBancoDadosActivity.this, AddItemBancoDados.class);
                    intent.putExtra("BARCODE_VALUE", barcodeValue);
                    startActivityForResult(intent, BUSCA_DB_REQUEST);
                }
            } finally {
                cursor.close();
            }
        }
    }

    // Metodo auxiliar para obter a instância do banco
    private SQLiteDatabase getDatabase() {
        if (db == null || !db.isOpen()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            db = dbHelper.getWritableDatabase();
        }
        return db;
    }

    private void realizarBuscaBancoDados() {
        Intent intent = new Intent(this, ResultBancoDadosActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigoBancoDados.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricaoBancoDados.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoriaBancoDados.getText().toString());
        startActivity(intent);
    }
}
