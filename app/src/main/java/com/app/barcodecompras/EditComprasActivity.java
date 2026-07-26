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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class EditComprasActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_ITEM = 1001;
    private EditText bc_compras, descr_compras, cat_compras, preco_compras,
            qnt_compras, total_compras, periodo_compras, obs_compras;
    private Button btnSalvar, btnCancelar, btnExcluir;
    private MaterialButton scanButtonEditCompras;
    private SQLiteDatabase db;
    private BancoDadosBkp bancoDadosBkp;
    private long compraId;
    private String originalBcCompras, originalDescrCompras;
    private DrawerLayout drawer;
    private NavigationView navigationView;
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
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        periodo_compras.setText(DatePickerUtil.getDataHoraAtual());
        periodo_compras.setOnClickListener(v ->
                DatePickerUtil.showDatePickerDialog(this, periodo_compras)
        );

        // ===== CONFIGURAR BOTÃO DE SCANNER =====
        scanButtonEditCompras.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(EditComprasActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });
        // ======================================

        // Receber dados da compra selecionada
        Intent intent = getIntent();
        if (intent != null) {
            compraId = intent.getLongExtra("compra_id", -1);

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


        // ===== BOTÃO SALVAR COM CONFIRMAÇÃO =====
        btnSalvar.setOnClickListener(v -> {
            // Primeiro verifica se os campos estão preenchidos
            if (validarCampos()) {
                mostrarConfirmacaoSalvar();
            }
        });
        // ======================================

        //btnSalvar.setOnClickListener(v -> salvarEdicao());
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

        scanButtonEditCompras = findViewById(R.id.scanButtonEditCompras);
    }



    // ===== MÉTODO PARA VALIDAR CAMPOS =====
    private boolean validarCampos() {
        String bc = bc_compras.getText().toString().trim();
        String descr = descr_compras.getText().toString().trim();

        if (bc.isEmpty()) {
            Toast.makeText(this, "Código é obrigatório", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (descr.isEmpty()) {
            Toast.makeText(this, "Descrição é obrigatória", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
    // =====================================

    // ===== DIÁLOGO DE CONFIRMAÇÃO PARA SALVAR =====
    private void mostrarConfirmacaoSalvar() {
        String bc = bc_compras.getText().toString().trim();
        String descr = descr_compras.getText().toString().trim();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Alteração")
                .setMessage("Tem certeza que deseja salvar as alterações deste item?\n\n" +
                        "Produto: " + descr + "\n" +
                        "Código: " + bc)
                .setPositiveButton("Salvar", (dialog1, which) -> {
                    salvarEdicao();
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(Color.WHITE);
            positiveButton.setBackgroundColor(Color.GREEN);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.WHITE);
        });

        dialog.show();
    }
    // =============================================




    // ===== RESULTADO DO SCANNER =====
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        // ESCANEAMENTO
        if (result != null && result.getContents() != null) {
            String barcode = result.getContents();
            //bc_compras.setText(barcode);
            fetchItemDataBancoDadosTable(barcode);
        } else if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {
            // voltou do cadastro → busca novamente
            String barcode = bc_compras.getText().toString();
            fetchItemDataBancoDadosTable(barcode);
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
    }
    // =================================

    // ===== BUSCAR DADOS NA TABELA bancodados_tab =====
    private void fetchItemDataBancoDadosTable(String barcodeValue) {
        if (db == null || !db.isOpen()) {
            Toast.makeText(this, "Banco de dados não disponível", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor cursor = db.rawQuery(
                "SELECT descr_DB, cat_DB FROM bancodados_tab WHERE bc_DB = ?",
                new String[]{barcodeValue}
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    descr_compras.setText(cursor.getString(0)); // descr_DB
                    cat_compras.setText(cursor.getString(1));   // cat_DB
                    Toast.makeText(this, "Item encontrado no banco de dados!", Toast.LENGTH_SHORT).show();
                } else {
                    // ===== NÃO ENCONTROU → PERGUNTAR SE DESEJA CADASTRAR =====
                    showAddItemDialog(barcodeValue);
                }
            } finally {
                cursor.close();
            }
        }
    }
    // =================================================

    // ===== DIALOG PARA CADASTRAR ITEM NÃO ENCONTRADO =====
    private void showAddItemDialog(String barcodeValue) {
        new AlertDialog.Builder(this)
                .setTitle("Produto não encontrado")
                .setMessage("Deseja cadastrar esse item no banco de dados?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    Intent intent = new Intent(EditComprasActivity.this, AddItemBancoDados.class);
                    intent.putExtra("BARCODE_VALUE", barcodeValue);
                    startActivityForResult(intent, REQUEST_CODE_ADD_ITEM);
                })
                .setNegativeButton("Não", null)
                .show();
    }
    // ====================================================

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
                originalDescrCompras = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));

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

    // salvarEdicao com verificação


    // ===== SALVAR EDIÇÃO (AGORA CHAMADO PELO DIÁLOGO) =====
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

            // ===== VERIFICAR DUPLICATA (ignorando o próprio item) =====
            String[] duplicata = firebaseComprasHelper.verificarDuplicataCompras(bc, descr, periodo, obs);
            if (duplicata != null) {
                long duplicataId = Long.parseLong(duplicata[0]);
                if (duplicataId != compraId) {
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Compra Já Existe")
                            .setMessage("Já existe outra compra com os mesmos dados:\n\n" +
                                    "📦 Código: " + duplicata[1] + "\n" +
                                    "📝 Descrição: " + duplicata[2] + "\n" +
                                    "📅 Período: " + duplicata[3] + "\n" +
                                    "💬 Obs: " + duplicata[4] + "\n\n" +
                                    "Não é possível salvar esta alteração.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
            }
            // =========================================================

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
    // ===================================================


    // excluirCompra com mais verificações
    private void excluirCompra() {
        if (compraId == -1) {
            Toast.makeText(this, "Erro: ID inválido para exclusão", Toast.LENGTH_SHORT).show();
            return;
        }

        if (originalBcCompras == null || originalBcCompras.isEmpty()) {
            Toast.makeText(this, "Erro: Código do produto não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir este Item?\n\n" +
                        "Produto: " + originalDescrCompras + "\n" +
                        "Código: " + originalBcCompras)
                .setPositiveButton("Excluir", (dialog1, which) -> {

                    try {
                        Cursor checkCursor = db.rawQuery("SELECT id FROM compras_tab WHERE id = ?",
                                new String[]{String.valueOf(compraId)});

                        if (!checkCursor.moveToFirst()) {
                            Toast.makeText(this, "Registro já foi excluído", Toast.LENGTH_SHORT).show();
                            checkCursor.close();
                            finish();
                            return;
                        }
                        checkCursor.close();

                        if (firebaseComprasHelper != null && originalBcCompras != null) {
                            firebaseComprasHelper.deletarItem(String.valueOf(compraId));
                            Toast.makeText(this, "Sincronizando com Firebase...", Toast.LENGTH_SHORT).show();
                        }

                        int rowsDeleted = db.delete(
                                "compras_tab",
                                "id = ?",
                                new String[]{String.valueOf(compraId)}
                        );

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


    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }

}