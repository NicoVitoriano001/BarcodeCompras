package com.app.barcodecompras;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.Toast;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.DatePickerUtil;
import com.app.barcodecompras.util.DrawerUtil; //2026.06.07

import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class BuscarComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private static final int BUSCA_COMPRA_REQUEST = 1001;
    private EditText etBuscaCodigo, etBuscaDescricao, etBuscaCategoria, etBuscaPeriodo , etBuscaOBS;
    private Button btnBuscar, btnCancelar;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private SQLiteDatabase db;
    private Button scanButtonBuscaCompras;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private BancoDadosBkp bancoDadosBkp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_compras);

        // Inicializar views
        etBuscaCodigo = findViewById(R.id.etBuscaCodigo);
        etBuscaDescricao = findViewById(R.id.etBuscaDescricao);
        etBuscaCategoria = findViewById(R.id.etBuscaCategoria);
        etBuscaPeriodo = findViewById(R.id.etBuscaPeriodo);
        etBuscaOBS = findViewById(R.id.etBuscaOBS);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnCancelar = findViewById(R.id.btnCancelarBusca);

        // Inicializar botão de scan
        scanButtonBuscaCompras = findViewById(R.id.scanButtonBuscaCompras);
        scanButtonBuscaCompras.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(BuscarComprasActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });

        // Banco de dados
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);//2026.06.22 banco dados

        etBuscaPeriodo.setText(DatePickerUtil.getDataHoraAtual2());
        etBuscaPeriodo.setOnClickListener(v ->
                DatePickerUtil.showDatePickerDialog(this, etBuscaPeriodo)
        );

        // Configurar listeners
        btnBuscar.setOnClickListener(v -> realizarBusca());

        btnCancelar.setOnClickListener(v -> finish());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.busca_compras_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);

    }
    // FIM ON CREATE


    private void realizarBusca() {
        Intent intent = new Intent(this, ResultComprasActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigo.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricao.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoria.getText().toString());
        intent.putExtra("PERIODO", etBuscaPeriodo.getText().toString());
        intent.putExtra("OBSERVACAO", etBuscaOBS.getText().toString());
        startActivity(intent);
    }

    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Tratar resultado do scanner
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            etBuscaCodigo.setText(result.getContents());
            fetchItemDataBancoDadosTable(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == BUSCA_COMPRA_REQUEST && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = etBuscaCodigo.getText().toString();
            fetchItemDataBancoDadosTable(barcode);
        }

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
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
                    etBuscaDescricao.setText(cursor.getString(0)); // descr_DB
                    etBuscaCategoria.setText(cursor.getString(1)); // cat_DB
                } else {
                    // Item não encontrado - abrir activity de cadastro
                    Intent intent = new Intent(BuscarComprasActivity.this, AddItemBancoDados.class);
                    intent.putExtra("BARCODE_VALUE", barcodeValue);
                    startActivityForResult(intent,BUSCA_COMPRA_REQUEST);
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

}
