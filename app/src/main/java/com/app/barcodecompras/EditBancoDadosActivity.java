package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;

public class EditBancoDadosActivity extends AppCompatActivity {
    private EditText etBcBancoDados, etDescrBancoDados, etCatBancoDados;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private long currentId;
    private String currentBarcode;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private FirebaseComprasHelper firebaseComprasHelper;
    private BancoDadosBkp bancoDadosBkp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bancodados);

        // Inicializar views
        initViews();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Inicializar helpers
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        // Receber dados do item
        Intent intent = getIntent();
        if (intent != null) {
            currentId = intent.getLongExtra("ID", -1);
            currentBarcode = intent.getStringExtra("CODIGO");
            String descricao = intent.getStringExtra("DESCRICAO");
            String categoria = intent.getStringExtra("CATEGORIA");

            // Preencher campos
            etBcBancoDados.setText(currentBarcode != null ? currentBarcode : "");
            etDescrBancoDados.setText(descricao != null ? descricao : "");
            etCatBancoDados.setText(categoria != null ? categoria : "");

            Log.d("EditBancoDados", "Editando ID: " + currentId +
                    ", Código: " + currentBarcode);
        }

        // Configurar listeners
        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirItem());

        // DRAWER
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.edit_bancodados_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
    }

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
                    try {
                        // 1. Deletar do SQLite
                        int rowsDeleted = db.delete(
                                "bancodados_tab",
                                "id = ?",
                                new String[]{String.valueOf(currentId)}
                        );

                        if (rowsDeleted > 0) {
                            // 2. Deletar do Firebase (hard delete)
                            firebaseBancoHelper.deletarItem(currentId);

                            Toast.makeText(this, "Item excluído com sucesso!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Erro ao excluir item", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        String novoBarcode = etBcBancoDados.getText().toString().trim();
        String novaDescricao = etDescrBancoDados.getText().toString().trim();
        String novaCategoria = etCatBancoDados.getText().toString().trim();

        if (novoBarcode.isEmpty() || novaDescricao.isEmpty() || novaCategoria.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            long updateAt = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("bc_DB", novoBarcode);
            values.put("descr_DB", novaDescricao);
            values.put("cat_DB", novaCategoria);
            values.put("updated_at", updateAt);

            firebaseBancoHelper.atualizarItem(
                    currentId,
                    novoBarcode,
                    novaDescricao,
                    novaCategoria
            );

            Toast.makeText(this, "Item atualizado com sucesso!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
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