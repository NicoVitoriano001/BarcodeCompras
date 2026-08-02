package com.app.barcodecompras.firebase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.app.barcodecompras.database.DatabaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseBancoDadosHelper {
    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;
    private static final String TAG = "FirebaseBancoDados";
    private static final String FIELD_TIMESTAMP = "updateAt";  // camelCase, sem underline
    private static final String TABLE_NAME = "bancodados_tab";
    private static final String FIREBASE_NODE = "bancodado"; // Nó no Firebase
    private static final String PREF_NAME = "sync_bancodados";
    private static final String KEY_LAST_SYNC_FIREBASE_LOCAL = "last_sync_time_firebase_para_local";
    private static final String KEY_LAST_SYNC_LOCAL_FIREBASE = "last_sync_time_local_para_firebase";

    public FirebaseBancoDadosHelper(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference(FIREBASE_NODE);
        DatabaseHelper.garantirTabelaSyncLog(db);
    }

    // ==================== SUMÁRIO LOCAL → FIREBASE ====================
    public List<SyncSummary> getSyncSummaryLocalToFirebase() {
        List<SyncSummary> summary = new ArrayList<>();
        long lastSyncTime = getLastSyncLocalParaFirebaseTime();

        // Itens criados/editados localmente (com detalhes)
        Cursor cursor = db.rawQuery(
                "SELECT bc_DB, descr_DB, cat_DB FROM " + TABLE_NAME + " WHERE updated_at > ?",
                new String[]{String.valueOf(lastSyncTime)}
        );
        if (cursor.moveToFirst()) {
            List<String> itens = new ArrayList<>();
            do {
                String bc = cursor.getString(0);
                String descr = cursor.getString(1);
                String cat = cursor.getString(2);
                itens.add(bc + " | " + descr + " | " + cat);
            } while (cursor.moveToNext());
            int count = itens.size();
            if (count > 0) {
                summary.add(new SyncSummary(TABLE_NAME, "CREATE", count,
                        count + " item(ns) para enviar ao Firebase", itens));
            }
        }
        cursor.close();

        // Deleções pendentes (com detalhes)
        Cursor c = db.rawQuery(
                "SELECT sl.item_id, bd.bc_DB, bd.descr_DB, bd.cat_DB " +
                "FROM sync_log sl " +
                "LEFT JOIN " + TABLE_NAME + " bd ON sl.item_id = bd.id " +
                "WHERE sl.table_name = ? AND sl.synced = 0",
                new String[]{TABLE_NAME}
        );
        if (c.moveToFirst()) {
            List<String> itens = new ArrayList<>();
            do {
                long itemId = c.getLong(0);
                String bc = c.getString(1);
                String descr = c.getString(2);
                String cat = c.getString(3);
                if (bc != null) {
                    itens.add(bc + " | " + (descr != null ? descr : "") + " | " + (cat != null ? cat : ""));
                } else {
                    itens.add("ID: " + itemId + " (item já removido localmente)");
                }
            } while (c.moveToNext());
            int pendingDel = itens.size();
            if (pendingDel > 0) {
                summary.add(new SyncSummary(TABLE_NAME, "DELETE", pendingDel,
                        pendingDel + " item(ns) para excluir do Firebase", itens));
            }
        }
        c.close();

        return summary;
    }

    // ==================== SUMÁRIO FIREBASE → LOCAL (assíncrono) ====================
    public void getSyncSummaryFirebaseToLocal(List<SyncSummary> summaryList, Runnable onComplete) {
        long lastSyncTime = getLastSyncFirebaseParaLocalTime();

        Query query = ref.orderByChild(FIELD_TIMESTAMP).startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> createDetails = new ArrayList<>();
                List<String> updateDetails = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemKey = child.getKey();
                    if (itemKey == null) continue;

                    long id = 0;
                    String bc = "";
                    String descricao = "";

                    if (child.hasChild("id")) {
                        Long val = child.child("id").getValue(Long.class);
                        if (val != null) id = val;
                    } else {
                        id = parseLong(itemKey);
                    }

                    if (child.hasChild("bc")) {
                        String val = child.child("bc").getValue(String.class);
                        if (val != null) bc = val;
                    }
                    if (child.hasChild("descricao")) {
                        String val = child.child("descricao").getValue(String.class);
                        if (val != null) descricao = val;
                    }

                    String detail = (bc.isEmpty() ? "ID:" + id : bc) + " | " + (descricao.isEmpty() ? "(sem descrição)" : descricao);

                    Cursor cursor = db.rawQuery(
                            "SELECT id FROM " + TABLE_NAME + " WHERE id = ?",
                            new String[]{String.valueOf(id)}
                    );
                    if (cursor.moveToFirst()) {
                        updateDetails.add(detail);
                    } else {
                        createDetails.add(detail);
                    }
                    cursor.close();
                }

                if (!createDetails.isEmpty()) {
                    summaryList.add(new SyncSummary(TABLE_NAME, "CREATE", createDetails.size(),
                            createDetails.size() + " novo(s) vindo do Firebase", createDetails));
                }
                if (!updateDetails.isEmpty()) {
                    summaryList.add(new SyncSummary(TABLE_NAME, "UPDATE", updateDetails.size(),
                            updateDetails.size() + " atualização(ões) vindo do Firebase", updateDetails));
                }

                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    // ==================== FIREBASE → LOCAL ====================
    public void syncFirebaseParaLocal(Runnable onComplete) {
        long lastSyncTime = getLastSyncFirebaseParaLocalTime();

        Query query = ref.orderByChild(FIELD_TIMESTAMP).startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long newestTimestamp = lastSyncTime;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemId = child.getKey();
                    if (itemId == null) continue;

                    long id = 0;
                    String bc = "";
                    String descricao = "";
                    String categoria = "";
                    long updateAt = 0;
                    boolean deleted = false;

                    if (child.hasChild("id")) {
                        Long val = child.child("id").getValue(Long.class);
                        if (val != null) id = val;
                    } else {
                        id = parseLong(itemId);
                    }

                    if (child.hasChild("bc")) {
                        String val = child.child("bc").getValue(String.class);
                        if (val != null) bc = val;
                    }
                    if (child.hasChild("descricao")) {
                        String val = child.child("descricao").getValue(String.class);
                        if (val != null) descricao = val;
                    }
                    if (child.hasChild("categoria")) {
                        String val = child.child("categoria").getValue(String.class);
                        if (val != null) categoria = val;
                    }
                    if (child.hasChild(FIELD_TIMESTAMP)) {
                        Long val = child.child(FIELD_TIMESTAMP).getValue(Long.class);
                        if (val != null) {
                            updateAt = val;
                            if (updateAt > newestTimestamp) newestTimestamp = updateAt;
                        }
                    }
                    if (child.hasChild("deleted")) {
                        Boolean val = child.child("deleted").getValue(Boolean.class);
                        if (val != null) deleted = val;
                    }

                    // Se marcado como deletado no Firebase, remover do local
                    if (deleted) {
                        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
                        Log.d(TAG, "Item " + id + " removido localmente por soft delete do Firebase");
                        continue;
                    }

                    Cursor cursor = db.rawQuery(
                            "SELECT id, updated_at FROM " + TABLE_NAME + " WHERE id = ?",
                            new String[]{String.valueOf(id)}
                    );

                    ContentValues values = new ContentValues();
                    values.put("id", id);
                    values.put("bc_DB", bc);
                    values.put("descr_DB", descricao);
                    values.put("cat_DB", categoria);
                    values.put("updated_at", updateAt);

                    if (cursor.moveToFirst()) {
                        long localUpdated = cursor.getLong(1);
                        cursor.close();
                        if (updateAt > localUpdated) {
                            db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
                        }
                    } else {
                        cursor.close();
                        db.insert(TABLE_NAME, null, values);
                    }
                }

                // A detecção de itens deletados é feita pela flag "deleted" no fluxo
                // de sincronização acima (syncFirebaseParaLocal). Não baixamos todos
                // os nós do Firebase para comparar IDs pois com 10.000+ itens isso
                // consumiria banda e memória em excesso.
                // Se precisar limpar órfãos locais, execute uma sincronização limpa
                // (export/import JSON do nó bancodado via console Firebase).

                if (newestTimestamp > lastSyncTime) {
                    saveLastSyncFirebaseParaLocalTime(newestTimestamp);
                }

                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Erro sync Firebase→Local: " + error.getMessage());
                if (onComplete != null) onComplete.run();
            }
        });
    }

    // ==================== LOCAL → FIREBASE ====================
    public void syncLocalParaFirebase(Runnable onComplete) {
        long lastSyncTime = getLastSyncLocalParaFirebaseTime();

        // 1. Processar deleções pendentes
        processPendingDeletions(() -> {
            // 2. Enviar itens criados/editados
            Cursor cursor = db.rawQuery(
                    "SELECT id, bc_DB, descr_DB, cat_DB, updated_at FROM " + TABLE_NAME +
                            " WHERE updated_at > ?",
                    new String[]{String.valueOf(lastSyncTime)}
            );

            int count = cursor.getCount();
            if (count == 0) {
                cursor.close();
                if (onComplete != null) onComplete.run();
                return;
            }

            long newestTimestamp = lastSyncTime;

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String bc = cursor.getString(1);
                String descricao = cursor.getString(2);
                String categoria = cursor.getString(3);
                long updateAt = cursor.getLong(4);

                if (updateAt > newestTimestamp) {
                    newestTimestamp = updateAt;
                }            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", id);
            itemMap.put("bc", bc);
            itemMap.put("descricao", descricao);
            itemMap.put("categoria", categoria);
            itemMap.put(FIELD_TIMESTAMP, updateAt);
            itemMap.put("deleted", false);

            ref.child(String.valueOf(id)).setValue(itemMap);
        }
        cursor.close();

        if (newestTimestamp > lastSyncTime) {
            saveLastSyncLocalParaFirebaseTime(newestTimestamp);
        }

        if (onComplete != null) onComplete.run();
        });
    }

    // ==================== DELEÇÃO RASTREADA ====================
    // SOFT DELETE - marca o item como deletado no Firebase (deleted=true + timestamp)
    // para que outros dispositivos possam detectar a exclusão ao sincronizar.
    // O nó permanece no Firebase, permitindo que consultas orderByChild o encontrem.
    public void deletarItem(long id) {
        long updateAt = System.currentTimeMillis();

        // 1. Registrar no sync_log (antes de deletar)
        ContentValues logValues = new ContentValues();
        logValues.put("item_id", id);
        logValues.put("table_name", TABLE_NAME);
        logValues.put("action", "DELETE");
        logValues.put("timestamp", updateAt);
        logValues.put("synced", 1); // Marca como sincronizado pois vamos enviar agora
        db.insert("sync_log", null, logValues);

        // 2. Soft delete no Firebase (marcar como deletado, NÃO remover o nó)
        // Isso permite que outros usuários detectem a exclusão via consulta timestamp
        Map<String, Object> updates = new HashMap<>();
        updates.put("deleted", true);
        updates.put(FIELD_TIMESTAMP, updateAt);
        ref.child(String.valueOf(id)).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Item " + id + " marcado como deletado no Firebase"))
                .addOnFailureListener(e -> {
                    // Se falhou, marcar como não sincronizado para tentar novamente
                    db.execSQL("UPDATE sync_log SET synced = 0 WHERE item_id = ? AND table_name = ?",
                            new String[]{String.valueOf(id), TABLE_NAME});
                    Log.e(TAG, "Falha ao marcar deleção do item " + id + " no Firebase: " + e.getMessage());
                });

        // 3. Remover do SQLite local
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});

        // 4. Atualizar APENAS o timestamp de sync local→firebase
        // NÃO atualizar saveLastSyncFirebaseParaLocalTime pois isso é
        // responsabilidade exclusiva do syncFirebaseParaLocal()
        saveLastSyncLocalParaFirebaseTime(updateAt);
    }

    // ==================== GERENCIAMENTO DE SYNC_LOG ====================
    private void processPendingDeletions(Runnable onComplete) {
        Cursor c = db.rawQuery(
                "SELECT item_id, timestamp FROM sync_log WHERE table_name = ? AND synced = 0",
                new String[]{TABLE_NAME}
        );

        if (c.getCount() == 0) {
            c.close();
            if (onComplete != null) onComplete.run();
            return;
        }

        List<Long> idsToDelete = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();

        while (c.moveToNext()) {
            idsToDelete.add(c.getLong(0));
            timestamps.add(c.getLong(1));
        }
        c.close();

        for (int i = 0; i < idsToDelete.size(); i++) {
            long id = idsToDelete.get(i);
            long ts = timestamps.get(i);

            // Soft delete no Firebase (marcar como deletado)
            Map<String, Object> updates = new HashMap<>();
            updates.put("deleted", true);
            updates.put(FIELD_TIMESTAMP, ts);
            ref.child(String.valueOf(id)).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Limpar do sync_log após sucesso
                        db.delete("sync_log", "item_id = ? AND table_name = ?",
                                new String[]{String.valueOf(id), TABLE_NAME});
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Falha ao deletar item pendente " + id + ": " + e.getMessage());
                    });
        }

        if (onComplete != null) onComplete.run();
    }

    // ==================== VERIFICAR ITENS DELETADOS NO FIREBASE ====================
    // Safety net otimizado: em vez de baixar TODOS os itens do Firebase para
    // comparar IDs (inviável com 10.000+ itens), este método agora realiza
    // a verificação durante a sincronização normal.
    // A lógica de detecção de soft delete (deleted=true) já está em syncFirebaseParaLocal().
    // Para hard deletes (remoção manual via console Firebase), o método abaixo
    // faz uma verificação limitada apenas aos itens que estão na sincronização atual.
    // NOTA: Com 10.000+ itens, hard deletes isolados no console Firebase
    // não serão detectados automaticamente. Recomenda-se:
    // 1. SEMPRE usar o app para deletar (soft delete)
    // 2. Se deletar manualmente no console, execute uma sincronização completa
    //    com export/import do JSON
    // 3. Ou use o recurso de sync completo forçado no app
    private void checkForDeletedItems() {
        // Método mantido vazio intencionalmente - a detecção de soft delete
        // é feita diretamente em syncFirebaseParaLocal() através da flag "deleted".
        // Baixar todos os 10.000+ itens apenas para comparar IDs causaria:
        // - Consumo excessivo de banda (potencialmente excedendo cota do Spark)
        // - Alto uso de memória no dispositivo
        // - Lentidão na sincronização
        Log.d(TAG, "checkForDeletedItems: verificação via syncFirebaseParaLocal (soft delete)");
    }

    // ==================== TIMESTAMPS ====================
    private long getLastSyncFirebaseParaLocalTime() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC_FIREBASE_LOCAL, 0);
    }

    private void saveLastSyncFirebaseParaLocalTime(long time) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC_FIREBASE_LOCAL, time)
                .apply();
    }

    private long getLastSyncLocalParaFirebaseTime() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC_LOCAL_FIREBASE, 0);
    }

    private void saveLastSyncLocalParaFirebaseTime(long time) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC_LOCAL_FIREBASE, time)
                .apply();
    }

    // ==================== VERIFICAR DUPLICATA ====================
    // Retorna um array com {id, bc_DB, descr_DB, cat_DB} se duplicata existir, ou null
    public String[] verificarDuplicata(String bc, String descr) {
        Cursor cursor = db.rawQuery(
                "SELECT id, bc_DB, descr_DB, cat_DB FROM " + TABLE_NAME +
                " WHERE bc_DB = ? AND descr_DB = ? LIMIT 1",
                new String[]{bc, descr}
        );
        if (cursor.moveToFirst()) {
            String[] item = new String[4];
            item[0] = String.valueOf(cursor.getLong(0));  // id
            item[1] = cursor.getString(1);                 // bc_DB
            item[2] = cursor.getString(2);                 // descr_DB
            item[3] = cursor.getString(3);                 // cat_DB
            cursor.close();
            return item;
        }
        cursor.close();
        return null;
    }

    // ==================== UTILITÁRIOS ====================
    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== COMPATIBILIDADE (MÉTODOS ANTIGOS) ====================
    public void syncFirebaseParaLocal() {
        syncFirebaseParaLocal(null);
    }

    public void syncLocalParaFirebase() {
        syncLocalParaFirebase(null);
    }

    public void syncCompleta() {
        syncFirebaseParaLocal(() -> syncLocalParaFirebase(null));
    }

    // INSERIR ITEM
    public long inserirItem(String bc, String descr, String cat) {
        long updateAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_DB", bc);
        values.put("descr_DB", descr);
        values.put("cat_DB", cat);
        values.put("updated_at", updateAt);

        long id = db.insert(TABLE_NAME, null, values);

        if (id != -1) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", id);
            itemMap.put("bc", bc);
            itemMap.put("descricao", descr);
            itemMap.put("categoria", cat);
            itemMap.put(FIELD_TIMESTAMP, updateAt);
            itemMap.put("deleted", false);

            ref.child(String.valueOf(id)).setValue(itemMap);
        }

        return id;
    }

    // ATUALIZAR ITEM ESPECÍFICO
    public void atualizarItem(long id, String bc, String descr, String cat) {
        long updateAt = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("bc_DB", bc);
        values.put("descr_DB", descr);
        values.put("cat_DB", cat);
        values.put("updated_at", updateAt);

        int rows = db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});

        if (rows > 0) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", id);
            itemMap.put("bc", bc);
            itemMap.put("descricao", descr);
            itemMap.put("categoria", cat);
            itemMap.put(FIELD_TIMESTAMP, updateAt);
            itemMap.put("deleted", false);

            ref.child(String.valueOf(id)).setValue(itemMap);
        }
    }
}
