package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class EditBancoDadosActivity extends AppCompatActivity {
    private EditText etBcBancoDados, etDescrBancoDados, etCatBancoDados;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private String currentBarcode;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bancodados);

        // Inicializar views
        initViews();

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Receber dados do item bancodados selecionado
        Intent intent = getIntent();
        if (intent != null) {
            currentBarcode = intent.getStringExtra("CODIGO");
            String descricao = intent.getStringExtra("DESCRICAO");
            String categoria = intent.getStringExtra("CATEGORIA");

            // Preencher campos com os dados recebidos (com verificações de null)
            etBcBancoDados.setText(currentBarcode != null ? currentBarcode : "");
            etDescrBancoDados.setText(descricao != null ? descricao : "");
            etCatBancoDados.setText(categoria != null ? categoria : "");
            // Adicione este log para verificar os dados recebidos
            Log.d("EditBancoDados", "Dados recebidos - Código: " + currentBarcode +
                    ", Descrição: " + descricao +
                    ", Categoria: " + categoria);
        }

        // Configurar listeners
        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirItem());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.edit_bancodados_nav_view);
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
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay

            return true;
        }); //DRAWER -- INICIO
    }//fim on create

    private void initViews() {
        etBcBancoDados = findViewById(R.id.etBcBancoDados);
        etDescrBancoDados = findViewById(R.id.etDescrBancoDados);
        etCatBancoDados = findViewById(R.id.etCatBancoDados);
        btnSalvar = findViewById(R.id.btnSalvarBancoDados);
        btnCancelar = findViewById(R.id.btnCancelarBancoDados);
        btnExcluir = findViewById(R.id.btnExcluirBancoDados);
    }

    private void excluirItem() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir este item?")
                .setPositiveButton("Excluir", (dialog1, which) -> {
                    int rowsDeleted = db.delete(
                            "bancodados_tab",
                            "bc_imdb = ?",
                            new String[]{currentBarcode}
                    );

                    if (rowsDeleted > 0) {
                        Toast.makeText(this, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Erro ao excluir item", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(Color.WHITE);
            positiveButton.setBackgroundColor(Color.RED);

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.WHITE);
        });

        dialog.show();
    }

    private void salvarEdicao() {
        String novoBarcode = etBcBancoDados.getText().toString();
        String novaDescricao = etDescrBancoDados.getText().toString();
        String novaCategoria = etCatBancoDados.getText().toString();

        if (novoBarcode.isEmpty() || novaDescricao.isEmpty() || novaCategoria.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ContentValues values = new ContentValues();
            values.put("bc_imdb", novoBarcode);
            values.put("descr_imdb", novaDescricao);
            values.put("cat_imdb", novaCategoria);

            int rowsAffected = db.update(
                    "bancodados_tab",
                    values,
                    "bc_imdb = ?",
                    new String[]{currentBarcode}
            );

            if (rowsAffected > 0) {
                Toast.makeText(this, "Item atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Nenhuma alteração foi salva", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}