package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditCollectedActivity extends AppCompatActivity {
    private EditText etBcCollected, etDescrCollected, etCatCollected;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private String currentBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_collected);

        // Inicializar views
        initViews();

        // Abrir banco de dados
        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Receber dados do item collected selecionado
        Intent intent = getIntent();
        if (intent != null) {
            currentBarcode = intent.getStringExtra("CODIGO");
            String descricao = intent.getStringExtra("DESCRICAO");
            String categoria = intent.getStringExtra("CATEGORIA");

            // Preencher campos com os dados recebidos
            etBcCollected.setText(currentBarcode);
            etDescrCollected.setText(descricao);
            etCatCollected.setText(categoria);
        }

        // Configurar listeners
        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirItem());
    }

    private void initViews() {
        etBcCollected = findViewById(R.id.etBcCollected);
        etDescrCollected = findViewById(R.id.etDescrCollected);
        etCatCollected = findViewById(R.id.etCatCollected);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnExcluir = findViewById(R.id.btnExcluir);
    }

    private void excluirItem() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir este item?")
                .setPositiveButton("Excluir", (dialog1, which) -> {
                    int rowsDeleted = db.delete(
                            "collected_tab",
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
        String novoBarcode = etBcCollected.getText().toString();
        String novaDescricao = etDescrCollected.getText().toString();
        String novaCategoria = etCatCollected.getText().toString();

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
                    "collected_tab",
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