package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class EditComprasActivity extends AppCompatActivity {
    private EditText etBcCompras, etDescrCompras, etCatCompras, etPrecoCompras,
            etQntCompras, etPeriodoCompras, etObsCompras, etTotalCompras;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private long compraId;
    private DrawerLayout drawer;
    // Adicione no início da classe, após a declaração das variáveis
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_compras);

        initViews();

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        // Receber dados da compra selecionada
        Intent intent = getIntent();
        if (intent != null) {
            compraId = intent.getLongExtra("compra_id", -1);
            loadCompraData(compraId);
        }

        // Configurar listeners para calcular total quando preço ou quantidade mudar
        etPrecoCompras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        etQntCompras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        // Configurar listeners
        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirCompra());

        // inicio drawer
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.edit_compras_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(EditComprasActivity.this, MainActivity.class));
                } else if (id == R.id.nav_add_bancodados) {
                    startActivity(new Intent(EditComprasActivity.this, AddItemIMDB.class));
                } else if (id == R.id.nav_busca_bancodados) {
                    startActivity(new Intent(EditComprasActivity.this, BuscarBancoDadosActivity.class));
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay

            return true;
        }); // fim drawer
    }//fim ON CREATE

    private void initViews() {
        etBcCompras = findViewById(R.id.etBcCompras);
        etDescrCompras = findViewById(R.id.etDescrCompras);
        etCatCompras = findViewById(R.id.etCatCompras);
        etPrecoCompras = findViewById(R.id.etPrecoCompras);
        etQntCompras = findViewById(R.id.etQntCompras);
        etTotalCompras = findViewById(R.id.etTotalCompras);
        etPeriodoCompras = findViewById(R.id.etPeriodoCompras);
        etObsCompras = findViewById(R.id.etObsCompras);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnExcluir = findViewById(R.id.btnExcluir);
    }

    private void calculateTotal() {
        try {
            double preco = Double.parseDouble(etPrecoCompras.getText().toString());
            double quantidade = Integer.parseInt(etQntCompras.getText().toString());
            double total = preco * quantidade;
            etTotalCompras.setText(String.valueOf(total));
        } catch (NumberFormatException e) {
            // Handle empty or invalid input
            etTotalCompras.setText("");
        }
    }

    private void loadCompraData(long id) {
        Cursor cursor = db.rawQuery("SELECT * FROM compras_tab WHERE id = ?",
                new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            etBcCompras.setText(cursor.getString(1));
            etDescrCompras.setText(cursor.getString(2));
            etCatCompras.setText(cursor.getString(3));
            etPrecoCompras.setText(String.valueOf(cursor.getDouble(4)));
            etQntCompras.setText(String.valueOf(cursor.getDouble(5)));
            etTotalCompras.setText(String.valueOf(cursor.getDouble(6)));
            etPeriodoCompras.setText(cursor.getString(7));
            etObsCompras.setText(cursor.getString(8));
        }
        cursor.close();
    }

    private void excluirCompra() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir esta compra?")
                .setPositiveButton("Excluir", (dialog1, which) -> {
                    int rowsDeleted = db.delete(
                            "compras_tab",
                            "id = ?",
                            new String[]{String.valueOf(compraId)}
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
            // Calcular o total antes de salvar
            double preco = Double.parseDouble(etPrecoCompras.getText().toString());
            double quantidade = Double.parseDouble(etQntCompras.getText().toString());
            double total = preco * quantidade;

            ContentValues values = new ContentValues();
            values.put("bc_compras", etBcCompras.getText().toString());
            values.put("descr_compras", etDescrCompras.getText().toString());
            values.put("cat_compras", etCatCompras.getText().toString());
            values.put("preco_compras", preco);
            values.put("qnt_compras", quantidade);
            values.put("total_compras", total);  // Usando o valor calculado
            values.put("periodo_compras", etPeriodoCompras.getText().toString());
            values.put("obs_compras", etObsCompras.getText().toString());

            int rowsAffected = db.update(
                    "compras_tab",
                    values,
                    "id = ?",
                    new String[]{String.valueOf(compraId)}
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