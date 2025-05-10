package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class BuscarBancoDadosActivity extends AppCompatActivity {
    private static final int EDIT_COLLECTED_REQUEST = 1;
    private EditText etBuscaCodigoBancoDados, etBuscaDescricaoBancoDados, etBuscaCategoriaBancoDados;
    private Button btnBuscarBancoDados, btnCancelarBancoDados;
    private DrawerLayout drawer;
    private NavigationView navigationView;

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

        // Configurar listeners
        btnBuscarBancoDados.setOnClickListener(v -> realizarBuscaBancoDados());
        btnCancelarBancoDados.setOnClickListener(v -> finish());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.busca_bancodados_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_add_bancodados) {
                    startActivity(new Intent(this, AddItemIMDB.class));
                } else if (id == R.id.nav_busca_bancodados) {
                    startActivity(new Intent(this, BuscarBancoDadosActivity.class));
                } else if (id == R.id.nav_backup) {
                    // Agora usando o padrão de Intent com extra
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("ACTION", "BACKUP");
                    startActivity(intent);
                } else if (id == R.id.nav_restore) {
                    // Agora usando o padrão de Intent com extra
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("ACTION", "RESTORE");
                    startActivity(intent);
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay
            return true;
        });//DRAWER -- FIM
    } // FIM ON CREATE

    private void realizarBuscaBancoDados() {
        Intent intent = new Intent(this, ResultBancoDadosActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigoBancoDados.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricaoBancoDados.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoriaBancoDados.getText().toString());
        startActivity(intent);
    }

    // Adicione este metodo para tratar o retorno
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COLLECTED_REQUEST && resultCode == RESULT_OK) {
            finish();
        }
    }
}
