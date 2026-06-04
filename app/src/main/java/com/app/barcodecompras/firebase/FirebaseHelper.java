package com.app.barcodecompras.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;
    private static final String TAG = "FirebaseHelper";

    public FirebaseHelper(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference("compras");
    }

    // Sync Firebase → Local (baixa APENAS itens modificados após última sincronização)
    public void syncFirebaseParaLocal() {
        long lastSyncTime = getLastSyncTime();
        // Query otimizada: busca apenas itens com updateAt > lastSyncTime
        Query query = ref.orderByChild("updateAt").startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long newestTimestamp = lastSyncTime;
                List<String> modifiedIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String bc = child.getKey();
                    if (bc == null) continue;

                    modifiedIds.add(bc);

                    String descricao = "";
                    String categoria = "";
                    double preco = 0;
                    double quantidade = 0;
                    double total = 0;
                    String periodo = "";
                    String obs = "";
                    long updateAt = 0;
                    boolean deleted = false;

                    // Ler cada campo individualmente
                    if (child.hasChild("descricao")) {
                        descricao = child.child("descricao").getValue(String.class);
                        if (descricao == null) descricao = "";
                    }

                    if (child.hasChild("categoria")) {
                        categoria = child.child("categoria").getValue(String.class);
                        if (categoria == null) categoria = "";
                    }

                    if (child.hasChild("preco")) {
                        Double val = child.child("preco").getValue(Double.class);
                        if (val != null) preco = val;
                    }

                    if (child.hasChild("quantidade")) {
                        Double val = child.child("quantidade").getValue(Double.class);
                        if (val != null) quantidade = val;
                    }

                    if (child.hasChild("total")) {
                        Double val = child.child("total").getValue(Double.class);
                        if (val != null) total = val;
                    } else {
                        total = preco * quantidade;
                    }

                    if (child.hasChild("periodo")) {
                        periodo = child.child("periodo").getValue(String.class);
                        if (periodo == null) periodo = "";
                    }

                    if (child.hasChild("obs")) {
                        obs = child.child("obs").getValue(String.class);
                        if (obs == null) obs = "";
                    }

                    if (child.hasChild("updateAt")) {
                        Long val = child.child("updateAt").getValue(Long.class);
                        if (val != null) {
                            updateAt = val;
                            if (updateAt > newestTimestamp) {
                                newestTimestamp = updateAt;
                            }
                        }
                    }

                    if (child.hasChild("deleted")) {
                        Boolean val = child.child("deleted").getValue(Boolean.class);
                        if (val != null) deleted = val;
                    }

                    // Se marcado como deletado, remover do local
                    if (deleted) {
                        db.delete("compras_tab", "bc_compras = ?", new String[]{bc});
                        continue;
                    }

                    // Verificar se existe no banco local
                    Cursor cursor = db.rawQuery(
                            "SELECT id, updated_at FROM compras_tab WHERE bc_compras = ?",
                            new String[]{bc}
                    );

                    ContentValues values = new ContentValues();
                    values.put("bc_compras", bc);
                    values.put("descr_compras", descricao);
                    values.put("cat_compras", categoria);
                    values.put("preco_compras", preco);
                    values.put("qnt_compras", quantidade);
                    values.put("total_compras", total);
                    values.put("periodo_compras", periodo);
                    values.put("obs_compras", obs);
                    values.put("updated_at", updateAt);

                    if (cursor.moveToFirst()) {
                        long localUpdated = cursor.getLong(1);
                        cursor.close();

                        if (updateAt > localUpdated) {
                            db.update("compras_tab", values, "bc_compras = ?", new String[]{bc});
                            Log.d(TAG, "Atualizado item: " + bc);
                        }
                    } else {
                        cursor.close();
                        db.insert("compras_tab", null, values);
                        Log.d(TAG, "Inserido novo item: " + bc);
                    }
                }

                // Verificar se algum item foi deletado no Firebase (não retornou na query)
                // Mas como a query usa startAt, itens deletados antes do lastSync não são detectados
                // Para isso, mantemos uma lista de todos os IDs ativos no Firebase
                checkForDeletedItems(newestTimestamp);

                // Salvar novo timestamp se houver itens mais novos
                if (newestTimestamp > lastSyncTime) {
                    saveLastSyncTime(newestTimestamp);
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    // Metodo auxiliar para detectar itens deletados no Firebase
    private void checkForDeletedItems(long lastTimestamp) {
        // Buscar todos os IDs ativos no Firebase (sem filtro de data)
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> activeIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String bc = child.getKey();
                    if (bc != null) {
                        // Verificar se não está marcado como deletado
                        Boolean deleted = child.child("deleted").getValue(Boolean.class);
                        if (deleted == null || !deleted) {
                            activeIds.add(bc);
                        }
                    }
                }

                // Remover itens locais que não estão mais ativos no Firebase
                Cursor localCursor = db.rawQuery("SELECT bc_compras FROM compras_tab", null);
                int deletedCount = 0;

                while (localCursor.moveToNext()) {
                    String localBc = localCursor.getString(0);
                    if (!activeIds.contains(localBc)) {
                        db.delete("compras_tab", "bc_compras = ?", new String[]{localBc});
                        deletedCount++;
                    }
                }
                localCursor.close();

                if (deletedCount > 0) {
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    // Sync Local → Firebase (envia APENAS itens modificados)
    public void syncLocalParaFirebase() {
        long lastSyncTime = getLastSyncTime();

        Cursor cursor = db.rawQuery(
                "SELECT bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras, updated_at FROM compras_tab WHERE updated_at > ?",
                new String[]{String.valueOf(lastSyncTime)}
        );

        int count = cursor.getCount();
        if (count == 0) {
            cursor.close();
            return;
        }

        long newestTimestamp = lastSyncTime;

        while (cursor.moveToNext()) {
            String bc = cursor.getString(0);
            String descricao = cursor.getString(1);
            String categoria = cursor.getString(2);
            double preco = cursor.getDouble(3);
            double quantidade = cursor.getDouble(4);
            double total = cursor.getDouble(5);
            String periodo = cursor.getString(6);
            String obs = cursor.getString(7);
            long updateAt = cursor.getLong(8);

            if (updateAt > newestTimestamp) {
                newestTimestamp = updateAt;
            }

            // Enviar apenas campos que mudaram (usando updateChildren para eficiência)
            ref.child(bc).updateChildren(getItemMap(bc, descricao, categoria, preco, quantidade, total, periodo, obs, updateAt, false));
        }
        cursor.close();

        // Atualizar timestamp apenas se houve envio
        if (newestTimestamp > lastSyncTime) {
            saveLastSyncTime(newestTimestamp);
        }

    }

    // Metodo auxiliar para criar mapa de atualização (evita recriar objeto a cada chamada)
    private java.util.Map<String, Object> getItemMap(String bc, String descricao, String categoria,
                                                     double preco, double quantidade, double total,
                                                     String periodo, String obs, long updateAt, boolean deleted) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("bc", bc);
        map.put("descricao", descricao);
        map.put("categoria", categoria);
        map.put("preco", preco);
        map.put("quantidade", quantidade);
        map.put("total", total);
        map.put("periodo", periodo);
        map.put("obs", obs);
        map.put("updateAt", updateAt);
        map.put("deleted", deleted);
        return map;
    }

    // Sincronização completa (bidirecional otimizada)
    public void syncCompleta() {
        syncLocalParaFirebase();
        new android.os.Handler().postDelayed(() -> syncFirebaseParaLocal(), 1500);
    }

    // Deletar item com soft delete
    public void deletarItem(String bc) {
        long updateAt = System.currentTimeMillis();

        // Marcar como deletado no Firebase
        ref.child(bc).child("deleted").setValue(true);
        ref.child(bc).child("updateAt").setValue(updateAt);

        // Remover do banco local
        db.delete("compras_tab", "bc_compras = ?", new String[]{bc});

        // Atualizar timestamp local para próxima sincronização
        saveLastSyncTime(updateAt);
    }

    // Sincronizar um único item específico (útil após edição)
    public void syncSingleItem(String bc) {
        Cursor cursor = db.rawQuery(
                "SELECT bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras, updated_at FROM compras_tab WHERE bc_compras = ?",
                new String[]{bc}
        );

        if (cursor.moveToFirst()) {
            String descricao = cursor.getString(1);
            String categoria = cursor.getString(2);
            double preco = cursor.getDouble(3);
            double quantidade = cursor.getDouble(4);
            double total = cursor.getDouble(5);
            String periodo = cursor.getString(6);
            String obs = cursor.getString(7);
            long updateAt = cursor.getLong(8);

            ref.child(bc).updateChildren(getItemMap(bc, descricao, categoria, preco, quantidade, total, periodo, obs, updateAt, false));
        }
        cursor.close();
    }

    private long getLastSyncTime() {
        return context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .getLong("last_sync_time", 0);
    }

    private void saveLastSyncTime(long time) {
        context.getSharedPreferences("sync", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_sync_time", time)
                .apply();
    }
}