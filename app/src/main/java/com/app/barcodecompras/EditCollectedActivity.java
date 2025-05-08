package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
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
    private long collectedId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_collected);

        // Inicializar views
        initViews();

        // Abrir banco de dados
        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Receber dados da compra selecionada
        Intent intent = getIntent();
        if (intent != null) {
            collectedId = intent.getLongExtra("compra_id", -1);
            loadCollectedData(collectedId);
        }

        // Configurar listeners
        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirCompra());
    }

    private void initViews() {
        etBcCollected = findViewById(R.id.etBcCollected);
        etDescrCollected = findViewById(R.id.etBuscaDescricaoCollected);
        etCatCollected = findViewById(R.id.etCatCollected);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnExcluir = findViewById(R.id.btnExcluir);
    }


    private void loadCollectedData(long id) {
        Cursor cursor = db.rawQuery("SELECT * FROM collected_tab WHERE id = ?",
                new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            etBcCollected.setText(cursor.getString(0));
            etDescrCollected.setText(cursor.getString(1));
            etCatCollected.setText(cursor.getString(2));
        }
        cursor.close();
    }

    private void excluirCompra() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir esta compra?")
                .setPositiveButton("Excluir", (dialog1, which) -> {
                    int rowsDeleted = db.delete(
                            "collected_tab",
                            "id = ?",
                            new String[]{String.valueOf(collectedId)}
                    );

                    if (rowsDeleted > 0) {
                        Toast.makeText(this, "Compra excluída com sucesso!", Toast.LENGTH_SHORT).show();
                        // Indica que a operação foi concluída com sucesso
                        setResult(RESULT_OK);
                        // Fecha a activity e retorna para a tela anterior
                        finish();
                    } else {
                        Toast.makeText(this, "Erro ao excluir compra", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        // Personalização das cores dos botões
        dialog.setOnShowListener(dialogInterface -> {
            // Botão Excluir (positivo) - Vermelho com texto branco
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(Color.WHITE);
            positiveButton.setBackgroundColor(Color.RED);

            // Botão Cancelar (negativo) - Texto branco
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.WHITE);
        });

        dialog.show();
    }

    private void salvarEdicao() {
        try {

            ContentValues values = new ContentValues();
            values.put("bc_collected", etBcCollected.getText().toString());
            values.put("descr_collected", etDescrCollected.getText().toString());
            values.put("cat_collected", etCatCollected.getText().toString());
            int rowsAffected = db.update(
                    "compras_tab",
                    values,
                    "id = ?",
                    new String[]{String.valueOf(collectedId)}
            );

            if (rowsAffected > 0) {
                Toast.makeText(this, "Compra atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                // Alterado para RESULT_OK para indicar que houve uma mudança
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Erro ao atualizar compra", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor, insira valores válidos para preço e quantidade", Toast.LENGTH_SHORT).show();
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