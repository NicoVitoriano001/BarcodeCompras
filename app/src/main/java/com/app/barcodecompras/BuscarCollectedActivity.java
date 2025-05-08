package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class BuscarCollectedActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private EditText etBuscaCodigoCollected, etBuscaDescricaoCollected, etBuscaCategoriaCollected;
    private Button btnBuscarCollected, btnCancelarCollected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_collected);

        // Inicializar views
        etBuscaCodigoCollected = findViewById(R.id.etBuscaCodigoCollected);
        etBuscaDescricaoCollected = findViewById(R.id.etBuscaDescricaoCollected);
        etBuscaCategoriaCollected = findViewById(R.id.etBuscaCategoriaCollected);
        btnBuscarCollected = findViewById(R.id.btnBuscarCollected);
        btnCancelarCollected = findViewById(R.id.btnCancelarBuscaCollected);

        // Configurar listeners
        btnBuscarCollected.setOnClickListener(v -> realizarBuscaCollected());
        btnCancelarCollected.setOnClickListener(v -> finish());
    }

    private void realizarBuscaCollected() {
        Intent intent = new Intent(this, ResultCollectedActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigoCollected.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricaoCollected.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoriaCollected.getText().toString());
        startActivity(intent);
    }

    // Adicione este metodo para tratar o retorno
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            finish();
        }
    }
}
