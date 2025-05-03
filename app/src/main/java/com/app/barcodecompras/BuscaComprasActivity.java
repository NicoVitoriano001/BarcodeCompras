package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;


public class BuscaComprasActivity extends AppCompatActivity {
    private EditText etBcCompras, etDescrCompras, etCatCompras, etPrecoCompras,
            etQntCompras, etTotalCompras, etPeriodoCompras, etObsCompras;
    private Button btnBuscar, btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busca_compras);

        // Inicializar views
        etBcCompras = findViewById(R.id.bc_compras);
        etDescrCompras = findViewById(R.id.descr_compras);
        etCatCompras = findViewById(R.id.cat_compras);
        etPrecoCompras = findViewById(R.id.preco_compras);
        etQntCompras = findViewById(R.id.qnt_compras);
        etTotalCompras = findViewById(R.id.total_compras);
        etPeriodoCompras = findViewById(R.id.periodo_compras);
        etObsCompras = findViewById(R.id.obs_compras);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnCancelar = findViewById(R.id.btnCancelar);

    /* *
    tabela collected_tab >>> //colunas bc_imdb NUMERIC, descr_imdb TEXT, cat_imdb TEXT
    tabela compras_tab >>> //colunas id INTEGER, bc_compras NUMERIC, descr_compras TEXT, cat_compras TEXT, preco_compras REAL,
    qnt_compras INTEGER,total_compras REAL, periodo_compras TEXT, obs_compras TEXT
    * */

        btnBuscar.setOnClickListener(v -> realizarBusca());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void realizarBusca() {
        if (etDescrCompras.getText().toString().isEmpty()) {
            etDescrCompras.setError("Descrição é obrigatório");
            return;
        }
        Intent resultIntent = new Intent(this, ResultComprasActivity.class);
        resultIntent.putExtra("bc_compras", etBcCompras.getText().toString());
        resultIntent.putExtra("descr_compras", etDescrCompras.getText().toString());
        resultIntent.putExtra("cat_compras", etCatCompras.getText().toString());
        resultIntent.putExtra("preco_compras", etPrecoCompras.getText().toString());
        resultIntent.putExtra("qnt_compras", etQntCompras.getText().toString());
        resultIntent.putExtra("total_compras", etTotalCompras.getText().toString());
        resultIntent.putExtra("periodo_compras", etPeriodoCompras.getText().toString());
        resultIntent.putExtra("obs_compras", etObsCompras.getText().toString());
        startActivity(resultIntent);
    }
}