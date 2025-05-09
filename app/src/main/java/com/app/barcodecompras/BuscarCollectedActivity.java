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

public class BuscarCollectedActivity extends AppCompatActivity {
    private static final int EDIT_COLLECTED_REQUEST = 1;
    private EditText etBuscaCodigoCollected, etBuscaDescricaoCollected, etBuscaCategoriaCollected;
    private Button btnBuscarCollected, btnCancelarCollected;
    private DrawerLayout drawer;
    private NavigationView navigationView;

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

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.busca_collected_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_add_collected) {
                    startActivity(new Intent(this, AddItemIMDB.class));
                } else if (id == R.id.nav_busca_collected) {
                    startActivity(new Intent(this, BuscarCollectedActivity.class));
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

        if (requestCode == EDIT_COLLECTED_REQUEST && resultCode == RESULT_OK) {
            finish();
        }
    }
}
