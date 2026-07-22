package com.app.barcodecompras;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.util.DatePickerUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.zxing.integration.android.IntentIntegrator;

//FIREBASE REALTIME
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentResult;

import com.app.barcodecompras.util.DrawerUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText bc_compras, descr_compras, cat_compras, preco_compras,
            qnt_compras, total_compras, periodo_compras, obs_compras;

    private EditText precoEditText, qntEditText, totalEditText;
    private MaterialButton scanButton, saveButton, cancelButton, addButton;
    private SwitchMaterial switchExpandir; // ← NOVO
    private SQLiteDatabase db;
    private BancoDadosBkp bancoDadosBkp;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private static final int REQUEST_CODE_ADD_ITEM = 1001;

    // ===== VARIÁVEIS PARA CONTROLE DE EXPANSÃO =====
    private ScrollView scrollExpandableMain;
    private LinearLayout expandableContentMain;
    private TextView tvResumoMediaMain, tvResumoMaiorMain, tvResumoMenorMain;
    private TextView tvResumoMaiorPeriodoMain, tvResumoMenorPeriodoMain;
    private LinearLayout itensContainerMain;
    private boolean isExpanded = false;
    private DecimalFormat df = new DecimalFormat("#,##0.00");
    // =============================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bancoDadosBkp = new BancoDadosBkp(this, new DatabaseHelper(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {

            new AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("Conceda permissão para backup")
                    .setPositiveButton("OK", (d, w) -> bancoDadosBkp.requestStoragePermission())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        bc_compras = findViewById(R.id.bc_compras);
        descr_compras = findViewById(R.id.descr_compras);
        cat_compras = findViewById(R.id.cat_compras);
        preco_compras = findViewById(R.id.preco_compras);
        qnt_compras = findViewById(R.id.qnt_compras);
        total_compras = findViewById(R.id.total_compras);
        periodo_compras = findViewById(R.id.periodo_compras);
        obs_compras = findViewById(R.id.obs_compras);

        addButton = findViewById(R.id.addButtonMain);
        scanButton = findViewById(R.id.scanButtonMain);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // ===== INICIALIZAR SWITCH =====
        switchExpandir = findViewById(R.id.switchExpandir);
        // ==============================

        // ===== INICIALIZAR VIEWS DE EXPANSÃO =====
        scrollExpandableMain = findViewById(R.id.scrollExpandableMain);
        expandableContentMain = findViewById(R.id.expandableContentMain);
        tvResumoMediaMain = findViewById(R.id.tvResumoMediaMain);
        tvResumoMaiorMain = findViewById(R.id.tvResumoMaiorMain);
        tvResumoMenorMain = findViewById(R.id.tvResumoMenorMain);
        tvResumoMaiorPeriodoMain = findViewById(R.id.tvResumoMaiorPeriodoMain);
        tvResumoMenorPeriodoMain = findViewById(R.id.tvResumoMenorPeriodoMain);
        itensContainerMain = findViewById(R.id.itensContainerMain);
        // ========================================

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("CLONE_MODE", false)) {
            bc_compras.setText(intent.getStringExtra("bc"));
            descr_compras.setText(intent.getStringExtra("descricao"));
            cat_compras.setText(intent.getStringExtra("categoria"));
            double preco = intent.getDoubleExtra("preco", 0);
            double qnt = intent.getDoubleExtra("quantidade", 0);
            double total = intent.getDoubleExtra("total", 0);
            preco_compras.setText(String.valueOf(preco));
            qnt_compras.setText(String.valueOf(qnt));
            total_compras.setText(String.valueOf(total));
            periodo_compras.setText(intent.getStringExtra("periodo"));
            obs_compras.setText(intent.getStringExtra("obs"));
        }

        precoEditText = preco_compras;
        qntEditText = qnt_compras;
        totalEditText = total_compras;

        periodo_compras.setText(DatePickerUtil.getDataHoraAtual());
        periodo_compras.setOnClickListener(v ->
                DatePickerUtil.showDatePickerDialog(this, periodo_compras)
        );

// ===== SWITCH PARA ATIVAR/DESATIVAR EXPANSÃO =====
        switchExpandir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String codigoBarras = bc_compras.getText().toString().trim();

                if (codigoBarras.isEmpty()) {
                    Toast.makeText(this, "Informe um código de barras primeiro", Toast.LENGTH_SHORT).show();
                    switchExpandir.setChecked(false);
                    return;
                }

                // Verifica se há registros no banco
                Cursor checkCursor = db.rawQuery(
                        "SELECT COUNT(*) FROM compras_tab WHERE bc_compras = ?",
                        new String[]{codigoBarras}
                );

                int count = 0;
                if (checkCursor != null && checkCursor.moveToFirst()) {
                    count = checkCursor.getInt(0);
                    checkCursor.close();
                }

                if (count == 0) {
                    Toast.makeText(this, "Nenhum registro encontrado para este código. Adicione uma compra primeiro.", Toast.LENGTH_LONG).show();
                    switchExpandir.setChecked(false);
                    return;
                }

                carregarEstatisticasItem();
            } else {
                scrollExpandableMain.setVisibility(View.GONE);
                isExpanded = false;
            }
        });
// =================================================

        addButton.setOnClickListener(v -> buscarItensParaAdicionar());

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Escaneie o código");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
        });

        saveButton.setOnClickListener(v -> saveData());

        cancelButton.setOnClickListener(v -> {
            clearFields();
            scrollExpandableMain.setVisibility(View.GONE);
            isExpanded = false;
            switchExpandir.setChecked(false); // Desmarca o switch
            Toast.makeText(this, "Campos limpos", Toast.LENGTH_SHORT).show();
        });

        FloatingActionButton fabSearch = findViewById(R.id.fab_searchITEM);
        fabSearch.setOnClickListener(v -> {
            Intent searchIntent = new Intent(MainActivity.this, BuscarComprasActivity.class);
            startActivity(searchIntent);
        });

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        firebaseComprasHelper = new FirebaseComprasHelper(this, db);

        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        setupTextWatchers();

        // DRAWER INICIO
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.result_bancodados_drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view_mainactivity);
        DrawerUtil.setupDrawer(this, drawer, navigationView, firebaseComprasHelper, firebaseBancoHelper, bancoDadosBkp);
    }
    // FIM ONCREATE

    // ===== MÉTODO PARA CARREGAR ESTATÍSTICAS DO ITEM =====
    private void carregarEstatisticasItem() {
        String codigoBarras = bc_compras.getText().toString().trim();

        if (codigoBarras.isEmpty()) {
            Toast.makeText(this, "Informe um código de barras primeiro", Toast.LENGTH_SHORT).show();
            switchExpandir.setChecked(false); // Desmarca o switch
            return;
        }

        Cursor cursor = db.rawQuery(
                "SELECT id, preco_compras, periodo_compras, obs_compras, descr_compras, cat_compras, qnt_compras, total_compras FROM compras_tab WHERE bc_compras = ? ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras DESC",
                new String[]{codigoBarras}
        );

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhum registro encontrado para este código", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
            switchExpandir.setChecked(false); // Desmarca o switch
            return;
        }

        itensContainerMain.removeAllViews();

        double somaPrecos = 0;
        double maiorPreco = Double.MIN_VALUE;
        double menorPreco = Double.MAX_VALUE;
        String maiorPeriodo = "", maiorObs = "";
        String menorPeriodo = "", menorObs = "";
        int totalItens = cursor.getCount();

        List<Compra> listaCompras = new ArrayList<>();

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            double preco = cursor.getDouble(1);
            String periodo = cursor.getString(2);
            String obs = cursor.getString(3);
            String descr = cursor.getString(4);
            String cat = cursor.getString(5);
            double qnt = cursor.getDouble(6);
            double total = cursor.getDouble(7);

            Compra compra = new Compra(id, codigoBarras, descr, cat, preco, qnt, total, periodo, obs);
            listaCompras.add(compra);

            somaPrecos += preco;

            if (preco > maiorPreco) {
                maiorPreco = preco;
                maiorPeriodo = periodo;
                maiorObs = obs;
            }

            if (preco < menorPreco) {
                menorPreco = preco;
                menorPeriodo = periodo;
                menorObs = obs;
            }
        }
        cursor.close();

        double media = totalItens > 0 ? somaPrecos / totalItens : 0;

        String maiorData = extrairData(maiorPeriodo);
        String menorData = extrairData(menorPeriodo);

        tvResumoMediaMain.setText(String.format("Média: R$ %s (%d itens)", df.format(media), totalItens));
        tvResumoMaiorMain.setText(String.format("R$ %s", df.format(maiorPreco)));
        tvResumoMenorMain.setText(String.format("R$ %s", df.format(menorPreco)));

        if (!maiorData.isEmpty()) {
            if (!maiorObs.isEmpty()) {
                tvResumoMaiorPeriodoMain.setText(String.format("(%s - %s)", maiorData, maiorObs));
            } else {
                tvResumoMaiorPeriodoMain.setText(String.format("(%s)", maiorData));
            }
        } else {
            tvResumoMaiorPeriodoMain.setText("");
        }

        if (!menorData.isEmpty()) {
            if (!menorObs.isEmpty()) {
                tvResumoMenorPeriodoMain.setText(String.format("(%s - %s)", menorData, menorObs));
            } else {
                tvResumoMenorPeriodoMain.setText(String.format("(%s)", menorData));
            }
        } else {
            tvResumoMenorPeriodoMain.setText("");
        }

        for (int i = 0; i < listaCompras.size(); i++) {
            Compra c = listaCompras.get(i);
            String dataApenas = extrairData(c.getPeriodoCompras());

            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(8, 8, 8, 8);

            TextView tvDetalhe = new TextView(this);
            String detalhe = String.format(
                    "Item %d:\n  Preço: R$ %.2f\n  Qtd: %.1f\n  Total: R$ %.2f\n  Período: %s\n  Obs: %s",
                    i + 1,
                    c.getPrecoCompras(),
                    c.getQntCompras(),
                    c.getTotalCompras(),
                    dataApenas,
                    c.getObsCompras().isEmpty() ? "Sem observação" : c.getObsCompras()
            );
            tvDetalhe.setText(detalhe);
            tvDetalhe.setTextColor(0xFFFFFFFF);
            tvDetalhe.setTextSize(14);
            itemLayout.addView(tvDetalhe);

            if (i < listaCompras.size() - 1) {
                View separator = new View(this);
                separator.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1
                ));
                separator.setBackgroundColor(0xFF666666);
                itemLayout.addView(separator);
            }

            itensContainerMain.addView(itemLayout);
        }

        scrollExpandableMain.setVisibility(View.VISIBLE);
        isExpanded = true;
    }
    // =============================================

    // ===== METODO AUXILIAR PARA EXTRAIR APENAS A DATA =====
    private String extrairData(String periodoCompleto) {
        if (periodoCompleto == null || periodoCompleto.isEmpty()) {
            return "";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
        java.util.regex.Matcher matcher = pattern.matcher(periodoCompleto);
        if (matcher.find()) {
            return matcher.group();
        }
        return periodoCompleto;
    }
    // =====================================================

    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {

            String barcode = result.getContents();

            // SÓ PREENCHE O CÓDIGO SE TIVER RETORNO
            // Verifica se o código existe no banco antes de preencher
            Cursor checkCursor = db.rawQuery(
                    "SELECT bc_DB FROM bancodados_tab WHERE bc_DB = ?",
                    new String[]{barcode}
            );

            if (checkCursor != null && checkCursor.moveToFirst()) {
                // Existe → preenche o campo
                bc_compras.setText(barcode);
                fetchItemDataBancoDadosTable(barcode);
                checkCursor.close();
            } else {
                // Não existe → NÃO preenche o campo, apenas pergunta
                if (checkCursor != null) checkCursor.close();
                // Não preenche bc_compras
                showAddItemDialog(barcode);
            }

        } else if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {

            // 2026.07.21 Capturar dados retornados do AddItemBancoDados =====
            if (data != null) {
                String barcode = data.getStringExtra(AddItemBancoDados.EXTRA_BARCODE);
                String description = data.getStringExtra(AddItemBancoDados.EXTRA_DESCRIPTION);
                String category = data.getStringExtra(AddItemBancoDados.EXTRA_CATEGORY);

                if (barcode != null && !barcode.isEmpty()) {
                    // 2026.07.21 Preenche os campos da MainActivity com os dados cadastrados
                    bc_compras.setText(barcode);
                    descr_compras.setText(description != null ? description : "");
                    cat_compras.setText(category != null ? category : "");

                    Toast.makeText(this, "Item cadastrado e carregado!", Toast.LENGTH_SHORT).show();
                }
            }
            // ==============================================================

        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchItemDataBancoDadosTable(String barcodeValue) {

        if (db == null || !db.isOpen()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            db = dbHelper.getWritableDatabase();
        }

        Cursor cursor = db.rawQuery(
                "SELECT descr_DB, cat_DB FROM bancodados_tab WHERE bc_DB = ?",
                new String[]{barcodeValue}
        );

        if (cursor != null) {
            try {

                if (cursor.moveToFirst()) {

                    descr_compras.setText(cursor.getString(0));
                    cat_compras.setText(cursor.getString(1));

                    Toast.makeText(this, "Item encontrado! Ative o Switch para ver os registros.", Toast.LENGTH_LONG).show();

                } else {

                    showAddItemDialog(barcodeValue);
                }

            } finally {
                cursor.close();
            }
        }
    }

    private void showAddItemDialog(String barcodeValue) {
        new AlertDialog.Builder(this)
                .setTitle("Produto não encontrado")
                .setMessage("Deseja cadastrar esse item no banco de dados?")
                .setPositiveButton("Sim", (dialog, which) -> {

                    Intent intent = new Intent(MainActivity.this, AddItemBancoDados.class);
                    intent.putExtra("BARCODE_VALUE", barcodeValue);

                    startActivityForResult(intent, REQUEST_CODE_ADD_ITEM);
                })
                .setNegativeButton("Não", (dialog, which) -> {
                    // Se cancelar, NÃO preenche o código
                    Toast.makeText(this, "Código não foi preenchido", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void buscarItensParaAdicionar() {

        if (db == null || !db.isOpen()) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            db = dbHelper.getWritableDatabase();
        }

        String codigo = bc_compras.getText().toString().trim();
        String descricao = descr_compras.getText().toString().trim();
        String categoria = cat_compras.getText().toString().trim();

        if (descricao.isEmpty() && categoria.isEmpty()) {
            Toast.makeText(this,
                    "Informe Descrição ou Categoria",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (descricao.length() < 2 && categoria.length() < 2) {
            Toast.makeText(this,
                    "Digite pelo menos 2 caracteres para busca",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder query = new StringBuilder(
                "SELECT bc_DB, descr_DB, cat_DB FROM bancodados_tab WHERE 1=1"
        );

        java.util.List<String> params = new java.util.ArrayList<>();

        adicionarFiltro(query, params, "bc_DB", codigo, false);
        adicionarFiltro(query, params, "descr_DB", descricao, true);
        adicionarFiltro(query, params, "cat_DB", categoria, true);

        query.append(" ORDER BY descr_DB");

        Cursor cursor = db.rawQuery(query.toString(), params.toArray(new String[0]));

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhum item encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itens = new String[cursor.getCount()];
        String[] codigos = new String[cursor.getCount()];

        int i = 0;
        while (cursor.moveToNext()) {
            String cod = cursor.getString(0);
            String desc = cursor.getString(1);
            String cat = cursor.getString(2);

            codigos[i] = cod;
            itens[i] = cod + "\n" + desc + "\n" + cat + "\n" + "────────────";
            i++;
        }

        int totalItens = cursor.getCount();

        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("Escolha um item (" + totalItens + ")")
                .setItems(itens, (dialog, which) -> {

                    String codigoSelecionado = codigos[which];

                    bc_compras.setText(codigoSelecionado);
                    fetchItemDataBancoDadosTable(codigoSelecionado);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void adicionarFiltro(StringBuilder query,
                                 java.util.List<String> params,
                                 String campo,
                                 String valor,
                                 boolean usarReplace) {

        if (valor == null || valor.trim().isEmpty()) return;

        String[] termos = valor.trim().split(" ");

        for (String termo : termos) {

            if (termo.startsWith("-") && termo.length() > 1) {
                String termoLimpo = termo.substring(1);

                if (usarReplace) {
                    query.append(" AND REPLACE(").append(campo).append(", ' ', '%') NOT LIKE ?");
                    params.add("%" + termoLimpo.replace(" ", "%") + "%");
                } else {
                    query.append(" AND ").append(campo).append(" NOT LIKE ?");
                    params.add("%" + termoLimpo + "%");
                }

            } else {
                if (usarReplace) {
                    query.append(" AND REPLACE(").append(campo).append(", ' ', '%') LIKE ?");
                    params.add("%" + termo.replace(" ", "%") + "%");
                } else {
                    query.append(" AND ").append(campo).append(" LIKE ?");
                    params.add("%" + termo + "%");
                }
            }
        }
    }

    private void saveData() {

        String bc = bc_compras.getText().toString().trim();
        String descr = descr_compras.getText().toString().trim();
        String cat = cat_compras.getText().toString().trim();
        String obs = obs_compras.getText().toString().trim();
        String periodo = periodo_compras.getText().toString();

        if (bc.isEmpty() || descr.isEmpty() || obs.isEmpty() ) {
            Toast.makeText(this, "BC, Descr e OBS obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco = 0;
        double qnt = 0;

        try {
            String precoStr = preco_compras.getText().toString();
            String qntStr = qnt_compras.getText().toString();

            if (!precoStr.isEmpty()) {
                preco = Double.parseDouble(precoStr);
            }

            if (!qntStr.isEmpty()) {
                qnt = Double.parseDouble(qntStr);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro nos valores", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = preco * qnt;

        long updateAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_compras", bc);
        values.put("descr_compras", descr);
        values.put("cat_compras", cat);
        values.put("preco_compras", preco);
        values.put("qnt_compras", qnt);
        values.put("total_compras", total);
        values.put("periodo_compras", periodo);
        values.put("obs_compras", obs);
        values.put("updated_at", updateAt);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            enviarParaFirebase(result, bc, descr, cat, preco, qnt, total, periodo, obs, updateAt);
            Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show();
            clearFields();
        }
    }

    private void enviarParaFirebase(long id, String bc, String descr, String cat,
                                    double preco, double qnt, double total,
                                    String periodo, String obs, long updateAt){

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

        String key = String.valueOf(id);

        ref.child(key).child("id").setValue(id);
        ref.child(key).child("bc").setValue(bc);
        ref.child(key).child("descricao").setValue(descr);
        ref.child(key).child("categoria").setValue(cat);
        ref.child(key).child("preco").setValue(preco);
        ref.child(key).child("quantidade").setValue(qnt);
        ref.child(key).child("total").setValue(total);
        ref.child(key).child("periodo").setValue(periodo);
        ref.child(key).child("obs").setValue(obs);
        ref.child(key).child("updateAt").setValue(updateAt);
        ref.child(key).child("deleted").setValue(false);
    }

    private void clearFields() {
        bc_compras.setText("");
        descr_compras.setText("");
        cat_compras.setText("");
        preco_compras.setText("");
        qnt_compras.setText("");
        total_compras.setText("");
        scrollExpandableMain.setVisibility(View.GONE);
        isExpanded = false;
    }

    private void setupTextWatchers() {

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}

            public void afterTextChanged(Editable s) {
                try {
                    String precoStr = precoEditText.getText().toString();
                    String qntStr = qntEditText.getText().toString();

                    if (!precoStr.isEmpty() && !qntStr.isEmpty()) {
                        double preco = Double.parseDouble(precoStr);
                        double qnt = Double.parseDouble(qntStr);
                        totalEditText.setText(String.valueOf(preco * qnt));
                    } else {
                        totalEditText.setText("");
                    }

                } catch (Exception e) {
                    totalEditText.setText("");
                }
            }
        };
        precoEditText.addTextChangedListener(watcher);
        qntEditText.addTextChangedListener(watcher);
    }

}