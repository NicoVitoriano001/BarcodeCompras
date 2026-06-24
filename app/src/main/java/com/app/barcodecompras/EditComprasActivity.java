package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.DatePickerUtil;
import com.app.barcodecompras.util.DrawerUtil;
import com.google.android.material.navigation.NavigationView;


public class EditComprasActivity extends AppCompatActivity {
    private EditText bc_compras, descr_compras, cat_compras, preco_compras,
            qnt_compras, total_compras, periodo_compras, obs_compras;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private SQLiteDatabase db;
    private long compraId;
    private String originalBcCompras;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private BancoDadosBkp bancoDadosBkp;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_compras);

        initViews();

        db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);//2026.06.22 banco dados

        periodo_compras.setText(DatePickerUtil.getDataHoraAtual());
        periodo_compras.setOnClickListener(v ->
                DatePickerUtil.showDatePickerDialog(this, periodo_compras)
        );

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
        preco_compras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });
        qnt_compras.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        btnSalvar.setOnClickListener(v -> salvarEdicao());
        btnCancelar.setOnClickListener(v -> finish());
        btnExcluir.setOnClickListener(v -> excluirCompra());

        // DRAWER INICIO
        drawer = findViewById(R.id.result_compras_drawer_layout);
        navigationView = findViewById(R.id.edit_compras_nav_view);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
    }
// FIM ON CREATE

    private void initViews() {
        bc_compras = findViewById(R.id.etBcCompras);
        descr_compras = findViewById(R.id.etDescrCompras);
        cat_compras = findViewById(R.id.etCatCompras);
        preco_compras = findViewById(R.id.etPrecoCompras);
        qnt_compras = findViewById(R.id.etQntCompras);
        total_compras = findViewById(R.id.etTotalCompras);
        periodo_compras = findViewById(R.id.etPeriodoCompras);
        obs_compras = findViewById(R.id.etObsCompras);

        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnExcluir = findViewById(R.id.btnExcluir);
    }

    private void calculateTotal() {
        try {
            String precoStr = preco_compras.getText().toString();
            String qntStr = qnt_compras.getText().toString();

            if (!precoStr.isEmpty() && !qntStr.isEmpty()) {
                double preco = Double.parseDouble(precoStr);
                double quantidade = Double.parseDouble(qntStr);
                double total = preco * quantidade;
                total_compras.setText(String.format("%.2f", total));
            } else {
                total_compras.setText("");
            }
        } catch (NumberFormatException e) {
            total_compras.setText("");
        }
    }

    // loadCompraData com verificação
    private void loadCompraData(long id) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM compras_tab WHERE id = ?",
                    new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                originalBcCompras = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));

                bc_compras.setText(cursor.getString(cursor.getColumnIndexOrThrow("bc_compras")));
                descr_compras.setText(cursor.getString(cursor.getColumnIndexOrThrow("descr_compras")));
                cat_compras.setText(cursor.getString(cursor.getColumnIndexOrThrow("cat_compras")));
                preco_compras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"))));
                qnt_compras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"))));
                total_compras.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"))));
                periodo_compras.setText(cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras")));
                obs_compras.setText(cursor.getString(cursor.getColumnIndexOrThrow("obs_compras")));
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

    // excluirCompra com mais verificações
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
                        if (firebaseComprasHelper != null && originalBcCompras != null) {
                            firebaseComprasHelper.deletarItem(String.valueOf(compraId));
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

    // salvarEdicao com verificação
    private void salvarEdicao() {
        try {
            String bc = bc_compras.getText().toString().trim();
            String descr = descr_compras.getText().toString().trim();
            String cat = cat_compras.getText().toString().trim();
            String periodo = periodo_compras.getText().toString().trim();
            String obs = obs_compras.getText().toString().trim();

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
                String precoStr = preco_compras.getText().toString();
                String qntStr = qnt_compras.getText().toString();

                if (!precoStr.isEmpty()) preco = Double.parseDouble(precoStr);
                if (!qntStr.isEmpty()) quantidade = Double.parseDouble(qntStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Valores inválidos para preço ou quantidade", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = preco * quantidade;
            long updateAt = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("bc_compras", bc);
            values.put("descr_compras", descr);
            values.put("cat_compras", cat);
            values.put("preco_compras", preco);
            values.put("qnt_compras", quantidade);
            values.put("total_compras", total);
            values.put("periodo_compras", periodo);
            values.put("obs_compras", obs);
            values.put("updated_at", updateAt);

            int rowsAffected = db.update(
                    "compras_tab",
                    values,
                    "id = ?",
                    new String[]{String.valueOf(compraId)}
            );

            if (rowsAffected > 0) {
                // Enviar atualização para o Firebase
                if (firebaseComprasHelper != null) {
                    new Handler().postDelayed(() -> {
                        firebaseComprasHelper.syncLocalParaFirebase();
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