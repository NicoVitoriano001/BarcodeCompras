package com.app.barcodecompras;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.text.Editable;

import androidx.appcompat.app.AppCompatActivity;

import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.util.CategoriaItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigCategoriasActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private FirebaseComprasHelper firebaseComprasHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_listar_categorias);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);
        firebaseComprasHelper = new FirebaseComprasHelper(this, db);

        listarCategorias();
    }

    private void listarCategorias() {

        Cursor cursor = db.rawQuery(
                "SELECT cat_DB, COUNT(*) AS quantidade " +
                        "FROM bancodados_tab " +
                        "WHERE cat_DB IS NOT NULL AND cat_DB != '' " +
                        "GROUP BY cat_DB " +
                        "ORDER BY cat_DB ASC",
                null
        );

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "Nenhuma categoria encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== CONTAGEM DE ITENS DO BANCODADOS QUE TÊM COMPRAS =====
        Map<String, Integer> comprasCountMap = new HashMap<>();
        Cursor comprasCursor = db.rawQuery(
                "SELECT b.cat_DB, COUNT(DISTINCT b.bc_DB) FROM bancodados_tab b " +
                "INNER JOIN compras_tab c ON b.bc_DB = c.bc_compras " +
                "WHERE b.cat_DB IS NOT NULL AND b.cat_DB != '' " +
                "GROUP BY b.cat_DB",
                null
        );
        if (comprasCursor != null) {
            while (comprasCursor.moveToNext()) {
                comprasCountMap.put(comprasCursor.getString(0), comprasCursor.getInt(1));
            }
            comprasCursor.close();
        }
        // ===========================================================

        List<CategoriaItem> lista = new ArrayList<>();

        while (cursor.moveToNext()) {
            String categoria = cursor.getString(0);
            int quantidade = cursor.getInt(1);
            int comprasCount = comprasCountMap.getOrDefault(categoria, 0);

            lista.add(new CategoriaItem(categoria, quantidade, comprasCount));
        }

        cursor.close();

        // COMPONENTES
        EditText searchInput = new EditText(this);
        searchInput.setHint("Buscar categoria...");

        ListView listView = new ListView(this);

        ArrayAdapter<CategoriaItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>(lista)
        );
        toString();
        listView.setAdapter(adapter);

        // FILTRO
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // CLICK
        listView.setOnItemClickListener((parent, view, position, id) -> {

            CategoriaItem item = adapter.getItem(position);
            String categoriaSelecionada = item.nome;

            String[] opcoes = {"Editar Categoria", "Excluir Categoria", "Configurar Itens"};

            new AlertDialog.Builder(this)
                    .setTitle("Escolha uma ação")
                    .setItems(opcoes, (dialog, which) -> {

                        if (which == 0) {
                            abrirDialogEdicao(categoriaSelecionada);

                        } else if (which == 1) {
                            confirmarExclusao(categoriaSelecionada);

                        } else if (which == 2) {
                            abrirConfiguracao(categoriaSelecionada);
                        }

                    })
                    .show();
        });

        // LAYOUT
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);
        layout.addView(searchInput);
        layout.addView(listView);

        // ===== CONTAGEM TOTAL DE ITENS DO BANCODADOS QUE TÊM COMPRAS =====
        int totalComprasCount = 0;
        Cursor countComprasCursor = db.rawQuery(
                "SELECT COUNT(DISTINCT b.bc_DB) FROM bancodados_tab b " +
                "INNER JOIN compras_tab c ON b.bc_DB = c.bc_compras " +
                "WHERE b.cat_DB IS NOT NULL AND b.cat_DB != ''",
                null
        );
        if (countComprasCursor != null) {
            if (countComprasCursor.moveToFirst()) {
                totalComprasCount = countComprasCursor.getInt(0);
            }
            countComprasCursor.close();
        }
        // =================================================================

        new AlertDialog.Builder(this)
                .setTitle("Categorias (" + adapter.getCount() + ") [" + totalComprasCount + "]")
                .setView(layout)
                .setPositiveButton("Nova Categoria", (d, w) -> abrirDialogCriacao())
                .setNegativeButton("Fechar", (d, w) -> finish())
                .show();
    }


    private void abrirDialogCriacao() {
        EditText input = new EditText(this);
        input.setHint("Nome da categoria");

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Nova Categoria")
                .setView(input)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String novaCategoria = input.getText().toString().trim();

            if (novaCategoria.isEmpty()) {
                input.setError("Campo obrigatório");
                return;
            }

            inserirCategoria(novaCategoria);
            dialog.dismiss();
        });
    }

    private void abrirDialogEdicao(String categoriaAtual) {

        EditText input = new EditText(this);
        input.setHint("Nova categoria");

        // PREENCHE COM O VALOR ATUAL
        input.setText(categoriaAtual);

        // POSICIONA O CURSOR NO FINAL
        input.setSelection(categoriaAtual.length());

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Editar Categoria")
                .setMessage("Categoria atual:\n" + categoriaAtual)
                .setView(input)
                .setPositiveButton("Salvar", null) // IMPORTANTE
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String novaCategoria = input.getText().toString().trim();

            if (novaCategoria.isEmpty()) {
                input.setError("Campo obrigatório");
                return; // NÃO FECHA SE CATEGORIA VAZIA
            }

            // opcional: evitar atualização desnecessária
            if (novaCategoria.equals(categoriaAtual)) {
                dialog.dismiss();
                return;
            }

            atualizarCategoria(categoriaAtual, novaCategoria);

            dialog.dismiss(); // fecha só se estiver válido
        });
    }


    private void abrirConfiguracao(String categoria) {

        Intent intent = new Intent(this, ResultBancoDadosActivity.class);

        intent.putExtra("CODIGO", "");
        intent.putExtra("DESCRICAO", "");
        intent.putExtra("CATEGORIA", categoria);
        intent.putExtra("CATEGORIA_EXATA", true); // busca pelo nome exato da categoria

        startActivity(intent);
    }

    private void atualizarCategoria(String categoriaAntiga, String novaCategoria) {

        // 1. Coletar os itens afetados ANTES da atualização
        List<Long> ids = new ArrayList<>();
        List<String> barcodes = new ArrayList<>();
        List<String> descricoes = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT id, bc_DB, descr_DB FROM bancodados_tab WHERE cat_DB = ?",
                new String[]{categoriaAntiga}
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                barcodes.add(cursor.getString(1) != null ? cursor.getString(1) : "");
                descricoes.add(cursor.getString(2) != null ? cursor.getString(2) : "");
            }
            cursor.close();
        }

        long updateAt = System.currentTimeMillis();

        // 2. Renomear localmente em bancodados_tab (com timestamp para o sync)
        ContentValues values = new ContentValues();
        values.put("cat_DB", novaCategoria);
        values.put("updated_at", updateAt);

        int linhas = db.update(
                "bancodados_tab",
                values,
                "cat_DB = ?",
                new String[]{categoriaAntiga}
        );

        // 3. Enviar os itens renomeados ao Firebase (atualiza local + nó remoto)
        if (linhas > 0 && firebaseBancoHelper != null) {
            for (int i = 0; i < ids.size(); i++) {
                firebaseBancoHelper.atualizarItem(
                        ids.get(i),
                        barcodes.get(i),
                        descricoes.get(i),
                        novaCategoria
                );
            }
        }

        // 4. Renomear também no histórico de compras (cat_compras) e sincronizar
        ContentValues valuesCompras = new ContentValues();
        valuesCompras.put("cat_compras", novaCategoria);
        valuesCompras.put("updated_at", updateAt);

        int linhasCompras = db.update(
                "compras_tab",
                valuesCompras,
                "cat_compras = ?",
                new String[]{categoriaAntiga}
        );

        if (linhasCompras > 0 && firebaseComprasHelper != null) {
            firebaseComprasHelper.syncLocalParaFirebase();
        }

        if (linhas > 0) {
            Toast.makeText(this, "Atualizado com sucesso", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Erro ao atualizar", Toast.LENGTH_SHORT).show();
        }

        finish();
    }


    private void inserirCategoria(String categoria) {
        long result;

        if (firebaseBancoHelper != null) {
            // Insere localmente (com timestamp) e envia ao Firebase
            result = firebaseBancoHelper.inserirItem("", "", categoria);
        } else {
            ContentValues values = new ContentValues();
            values.put("cat_DB", categoria);
            values.put("updated_at", System.currentTimeMillis());
            result = db.insert("bancodados_tab", null, values);
        }

        if (result != -1) {
            Toast.makeText(this, "Categoria criada com sucesso", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Erro ao criar categoria", Toast.LENGTH_SHORT).show();
        }

        finish(); // mantém mesma lógica da Activity
    }

    private void confirmarExclusao(String categoria) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir")
                .setMessage("Deseja excluir a categoria?\n" + categoria)
                .setPositiveButton("Sim", (d, w) -> deletarCategoria(categoria))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarCategoria(String categoria) {

        // 1. Coletar os IDs dos itens da categoria
        List<Long> ids = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM bancodados_tab WHERE cat_DB = ?",
                new String[]{categoria}
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
            cursor.close();
        }

        if (ids.isEmpty()) {
            Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (firebaseBancoHelper != null) {
            // 2. Soft delete no Firebase + remoção local de cada item
            for (long id : ids) {
                firebaseBancoHelper.deletarItem(id);
            }
        } else {
            // Fallback: remoção local apenas
            db.delete("bancodados_tab", "cat_DB = ?", new String[]{categoria});
        }

        Toast.makeText(this, "Categoria excluída", Toast.LENGTH_SHORT).show();
        finish();
    }


    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }

}