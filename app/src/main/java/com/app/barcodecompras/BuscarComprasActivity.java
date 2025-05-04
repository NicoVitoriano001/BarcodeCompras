package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class BuscarComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private EditText etBuscaCodigo, etBuscaDescricao, etBuscaCategoria, etBuscaPeriodo;
    private Button btnBuscar, btnCancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_compras);

        // Inicializar views
        etBuscaCodigo = findViewById(R.id.etBuscaCodigo);
        etBuscaDescricao = findViewById(R.id.etBuscaDescricao);
        etBuscaCategoria = findViewById(R.id.etBuscaCategoria);
        etBuscaPeriodo = findViewById(R.id.etBuscaPeriodo);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnCancelar = findViewById(R.id.btnCancelarBusca);

        // Configurar listeners
        btnBuscar.setOnClickListener(v -> realizarBusca());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void realizarBusca() {
        Intent intent = new Intent(this, ResultComprasActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigo.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricao.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoria.getText().toString());
        intent.putExtra("PERIODO", etBuscaPeriodo.getText().toString());
        startActivity(intent);
    }

    // Adicione este método para tratar o retorno
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            finish();
            }
        }
    }
