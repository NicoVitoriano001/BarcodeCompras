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

import com.app.barcodecompras.firebase.FirebaseHelper;
import com.google.android.material.navigation.NavigationView;

public class EditComprasActivity extends AppCompatActivity {
    private EditText etBcCompras, etDescrCompras, etCatCompras, etPrecoCompras,
            etQntCompras, etPeriodoCompras, etObsCompras, etTotalCompras;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private FirebaseHelper firebaseHelper;
    private long compraId;
    private String originalBcCompras;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_compras);

        initViews();

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);
        firebaseHelper = new FirebaseHelper(this, db);

        // Receber dados da compra selecionada
        Intent intent = getIntent();
        if (intent != null) {
            compraId = intent.getLongExtra("compra_id", -1);

            // VERIFICAR SE O ID É VÁLIDO
            if (compraId == -1) {
                Toast.makeText(this, "Erro: ID da compra inválido", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            loadCompraData(compraId);
        }

        // Configurar listeners
        etPrecoCompras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        etQntCompras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirCompra());

        // Drawer
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.edit_compras_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(EditComprasActivity.this, MainActivity.class));
                } else if (id == R.id.nav_add_bancodados) {
                    startActivity(new Intent(EditComprasActivity.this, AddItemBancoDados.class));
                } else if (id == R.id.nav_busca_bancodados) {
                    startActivity(new Intent(EditComprasActivity.this, BuscarBancoDadosActivity.class));
                }
            }, 200);

            return true;
        });
    }

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
            String precoStr = etPrecoCompras.getText().toString();
            String qntStr = etQntCompras.getText().toString();

            if (!precoStr.isEmpty() && !qntStr.isEmpty()) {
                double preco = Double.parseDouble(precoStr);
                double quantidade = Double.parseDouble(qntStr);
                double total = preco * quantidade;
                etTotalCompras.setText(String.format("%.2f", total));
            } else {
                etTotalCompras.setText("");
            }
        } catch (NumberFormatException e) {
            etTotalCompras.setText("");
        }
    }

    // CORRIGIDO: loadCompraData com verificação
    private void loadCompraData(long id) {
        Cursor cursor = null;
        try {
            // USAR "id" como inteiro (sem aspas)
            cursor = db.rawQuery("SELECT * FROM compras_tab WHERE id = ?",
                    new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                originalBcCompras = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));

                etBcCompras.setText(cursor.getString(cursor.getColumnIndexOrThrow("bc_compras")));
                etDescrCompras.setText(cursor.getString(cursor.getColumnIndexOrThrow("descr_compras")));
                etCatCompras.setText(cursor.getString(cursor.getColumnIndexOrThrow("cat_compras")));
                etPrecoCompras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"))));
                etQntCompras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"))));
                etTotalCompras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"))));
                etPeriodoCompras.setText(cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras")));
                etObsCompras.setText(cursor.getString(cursor.getColumnIndexOrThrow("obs_compras")));
            } else {
                Toast.makeText(this, "Compra não encontrada", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // CORRIGIDO: excluirCompra com mais verificações
    private void excluirCompra() {
        // Verificar se o ID é válido
        if (compraId == -1) {
            Toast.makeText(this, "Erro: ID inválido para exclusão", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar se o código original existe
        if (originalBcCompras == null || originalBcCompras.isEmpty()) {
            Toast.makeText(this, "Erro: Código do produto não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir esta compra?\n\nProduto: " + originalBcCompras)
                .setPositiveButton("Excluir", (dialog1, which) -> {

                    try {
                        // 1. Primeiro, verificar se o registro ainda existe
                        Cursor checkCursor = db.rawQuery("SELECT id FROM compras_tab WHERE id = ?",
                                new String[]{String.valueOf(compraId)});

                        if (!checkCursor.moveToFirst()) {
                            Toast.makeText(this, "Registro já foi excluído", Toast.LENGTH_SHORT).show();
                            checkCursor.close();
                            finish();
                            return;
                        }
                        checkCursor.close();

                        // 2. Deletar do Firebase
                        if (firebaseHelper != null && originalBcCompras != null) {
                            firebaseHelper.deletarItem(originalBcCompras);
                            Toast.makeText(this, "Sincronizando com Firebase...", Toast.LENGTH_SHORT).show();
                        }

                        // 3. Deletar do banco local
                        int rowsDeleted = db.delete(
                                "compras_tab",
                                "id = ?",
                                new String[]{String.valueOf(compraId)}
                        );

                        // 4. Verificar resultado
                        if (rowsDeleted > 0) {
                            Toast.makeText(this, "Compra excluída com sucesso!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "Erro ao excluir: registro não encontrado", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
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

    // CORRIGIDO: salvarEdicao com verificação
    private void salvarEdicao() {
        try {
            String bc = etBcCompras.getText().toString().trim();
            String descr = etDescrCompras.getText().toString().trim();
            String cat = etCatCompras.getText().toString().trim();
            String periodo = etPeriodoCompras.getText().toString().trim();
            String obs = etObsCompras.getText().toString().trim();

            if (bc.isEmpty()) {
                Toast.makeText(this, "Código é obrigatório", Toast.LENGTH_SHORT).show();
                return;
            }

            if (descr.isEmpty()) {
                Toast.makeText(this, "Descrição é obrigatória", Toast.LENGTH_SHORT).show();
                return;
            }

            double preco = 0;
            double quantidade = 0;

            try {
                String precoStr = etPrecoCompras.getText().toString();
                String qntStr = etQntCompras.getText().toString();

                if (!precoStr.isEmpty()) preco = Double.parseDouble(precoStr);
                if (!qntStr.isEmpty()) quantidade = Double.parseDouble(qntStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valores inválidos para preço ou quantidade", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = preco * quantidade;
            long updatedAt = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("bc_compras", bc);
            values.put("descr_compras", descr);
            values.put("cat_compras", cat);
            values.put("preco_compras", preco);
            values.put("qnt_compras", quantidade);
            values.put("total_compras", total);
            values.put("periodo_compras", periodo);
            values.put("obs_compras", obs);
            values.put("updated_at", updatedAt);

            int rowsAffected = db.update(
                    "compras_tab",
                    values,
                    "id = ?",
                    new String[]{String.valueOf(compraId)}
            );

            if (rowsAffected > 0) {
                // Enviar atualização para o Firebase
                if (firebaseHelper != null) {
                    new Handler().postDelayed(() -> {
                        firebaseHelper.syncLocalParaFirebase();
                    }, 500);
                }

                Toast.makeText(this, "Compra atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Erro ao atualizar compra", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
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