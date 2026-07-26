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

public class FirebaseComprasHelper {
    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;
    private static final String TAG = "FirebaseHelper";
    private static final String FIELD_TIMESTAMP = "updateAt";  // camelCase, sem underline
    private static final String TABLE_NAME = "compras_tab";
    private static final String FIREBASE_NODE = "compras";
    private static final String PREF_NAME = "sync";
    private static final String KEY_LAST_SYNC = "last_sync_time";

    public FirebaseComprasHelper(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference(FIREBASE_NODE);
        DatabaseHelper.garantirTabelaSyncLog(db);
    }

    // ==================== SUMÁRIO LOCAL → FIREBASE ====================
    public List<SyncSummary> getSyncSummaryLocalToFirebase() {
        List<SyncSummary> summary = new ArrayList<>();
        long lastSyncTime = getLastSyncTime();

        // Itens criados/editados localmente (com detalhes)
        Cursor cursor = db.rawQuery(
                "SELECT bc_compras, descr_compras, periodo_compras, obs_compras FROM " + TABLE_NAME +
                " WHERE updated_at > ?",
                new String[]{String.valueOf(lastSyncTime)}
        );
        if (cursor.moveToFirst()) {
            List<String> itens = new ArrayList<>();
            do {
                String bc = cursor.getString(0);
                String descr = cursor.getString(1);
                String periodo = cursor.getString(2);
                String obs = cursor.getString(3);
                itens.add(bc + " | " + descr + " | " + periodo + (obs.isEmpty() ? "" : " | " + obs));
            } while (cursor.moveToNext());
            int count = itens.size();
            if (count > 0) {
                String desc = count + " compra(s) para enviar ao Firebase";
                summary.add(new SyncSummary(TABLE_NAME, "CREATE", count, desc, itens));
            }
        }
        cursor.close();

        // Deleções pendentes (com detalhes)
        Cursor c = db.rawQuery(
                "SELECT sl.item_id, cp.bc_compras, cp.descr_compras, cp.periodo_compras " +
                "FROM sync_log sl " +
                "LEFT JOIN " + TABLE_NAME + " cp ON sl.item_id = cp.id " +
                "WHERE sl.table_name = ? AND sl.synced = 0",
                new String[]{TABLE_NAME}
        );
        if (c.moveToFirst()) {
            List<String> itens = new ArrayList<>();
            do {
                long itemId = c.getLong(0);
                String bc = c.getString(1);
                String descr = c.getString(2);
                String periodo = c.getString(3);
                if (bc != null) {
                    itens.add(bc + " | " + (descr != null ? descr : "") + " | " + (periodo != null ? periodo : ""));
                } else {
                    itens.add("ID: " + itemId + " (item já removido localmente)");
                }
            } while (c.moveToNext());
            int pendingDel = itens.size();
            if (pendingDel > 0) {
                summary.add(new SyncSummary(TABLE_NAME, "DELETE", pendingDel,
                        pendingDel + " compra(s) para excluir do Firebase", itens));
            }
        }
        c.close();

        return summary;
    }

    // ==================== SINCRONIZAR LOCAL → FIREBASE ====================
    public void syncLocalParaFirebase(Runnable onComplete) {
        long lastSyncTime = getLastSyncTime();

        // 1. Processar deleções pendentes
        processPendingDeletions(() -> {
            // 2. Enviar itens criados/editados
            Cursor cursor = db.rawQuery(
                    "SELECT id, bc_compras, descr_compras, cat_compras, preco_compras, " +
                            "qnt_compras, total_compras, periodo_compras, obs_compras, updated_at " +
                            "FROM " + TABLE_NAME + " WHERE updated_at > ?",
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
                double preco = cursor.getDouble(4);
                double quantidade = cursor.getDouble(5);
                double total = cursor.getDouble(6);
                String periodo = cursor.getString(7);
                String obs = cursor.getString(8);
                long updateAt = cursor.getLong(9);

                if (updateAt > newestTimestamp) {
                    newestTimestamp = updateAt;
                }

                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", id);
                itemMap.put("bc", bc);
                itemMap.put("descricao", descricao);
                itemMap.put("categoria", categoria);
                itemMap.put("preco", preco);
                itemMap.put("quantidade", quantidade);
                itemMap.put("total", total);
                itemMap.put("periodo", periodo);
                itemMap.put("obs", obs);
                itemMap.put(FIELD_TIMESTAMP, updateAt);
                itemMap.put("deleted", false);

                ref.child(String.valueOf(id)).setValue(itemMap);
            }
            cursor.close();

            if (newestTimestamp > lastSyncTime) {
                saveLastSyncTime(newestTimestamp);
            }

            if (onComplete != null) onComplete.run();
        });
    }

    // ==================== SINCRONIZAR FIREBASE → LOCAL ====================
    public void syncFirebaseParaLocal(Runnable onComplete) {
        long lastSyncTime = getLastSyncTime();

        Query query = ref.orderByChild(FIELD_TIMESTAMP).startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long newestTimestamp = lastSyncTime;
                List<Long> firebaseActiveIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemId = child.getKey();
                    if (itemId == null) continue;

                    long id = 0;
                    String bc = "";
                    String descricao = "";
                    String categoria = "";
                    double preco = 0;
                    double quantidade = 0;
                    double total = 0;
                    String periodo = "";
                    String obs = "";
                    long updateAt = 0;
                    boolean deleted = false;

                    if (child.hasChild("id")) {
                        Long val = child.child("id").getValue(Long.class);
                        if (val != null) id = val;
                    } else {
                        try { id = Long.parseLong(itemId); } catch (Exception ignored) {}
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
                        String val = child.child("periodo").getValue(String.class);
                        if (val != null) periodo = val;
                    }
                    if (child.hasChild("obs")) {
                        String val = child.child("obs").getValue(String.class);
                        if (val != null) obs = val;
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
                        continue;
                    }

                    firebaseActiveIds.add(id);

                    // Verificar se existe no banco local
                    Cursor cursor = db.rawQuery(
                            "SELECT id, updated_at FROM " + TABLE_NAME + " WHERE id = ?",
                            new String[]{String.valueOf(id)}
                    );

                    ContentValues values = new ContentValues();
                    values.put("id", id);
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
                            db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});
                        }
                    } else {
                        cursor.close();
                        db.insert(TABLE_NAME, null, values);
                    }
                }

                if (newestTimestamp > lastSyncTime) {
                    saveLastSyncTime(newestTimestamp);
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

    // ==================== SUMÁRIO FIREBASE → LOCAL (assíncrono) ====================
    public void getSyncSummaryFirebaseToLocal(List<SyncSummary> summaryList, Runnable onComplete) {
        long lastSyncTime = getLastSyncTime();

        Query query = ref.orderByChild(FIELD_TIMESTAMP).startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> createDetails = new ArrayList<>();
                List<String> updateDetails = new ArrayList<>();
                List<String> deleteDetails = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemKey = child.getKey();
                    if (itemKey == null) continue;

                    // Verificar se é uma exclusão
                    if (child.hasChild("deleted")) {
                        Boolean deleted = child.child("deleted").getValue(Boolean.class);
                        if (deleted != null && deleted) {
                            String bc = "";
                            if (child.hasChild("bc")) {
                                String val = child.child("bc").getValue(String.class);
                                if (val != null) bc = val;
                            }
                            String descr = "";
                            if (child.hasChild("descricao")) {
                                String val = child.child("descricao").getValue(String.class);
                                if (val != null) descr = val;
                            }
                            deleteDetails.add((bc.isEmpty() ? "ID:" + itemKey : bc) + " | " + (descr.isEmpty() ? "(sem descrição)" : descr));
                            continue;
                        }
                    }

                    long id = 0;
                    String bc = "";
                    String descricao = "";

                    if (child.hasChild("id")) {
                        Long val = child.child("id").getValue(Long.class);
                        if (val != null) id = val;
                    } else {
                        try { id = Long.parseLong(itemKey); } catch (Exception ignored) {}
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
                if (!deleteDetails.isEmpty()) {
                    summaryList.add(new SyncSummary(TABLE_NAME, "DELETE", deleteDetails.size(),
                            deleteDetails.size() + " exclusão(ões) vindo do Firebase", deleteDetails));
                }

                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    // ==================== DELEÇÃO RASTREADA ====================
    public void deletarItem(long id) {
        long updateAt = System.currentTimeMillis();

        // 1. Marcar como deletado no Firebase (soft delete)
        ref.child(String.valueOf(id)).child("deleted").setValue(true);
        ref.child(String.valueOf(id)).child(FIELD_TIMESTAMP).setValue(updateAt);

        // 2. Registrar no sync_log (para caso o Firebase falhe)
        ContentValues logValues = new ContentValues();
        logValues.put("item_id", id);
        logValues.put("table_name", TABLE_NAME);
        logValues.put("action", "DELETE");
        logValues.put("timestamp", updateAt);
        logValues.put("synced", 1); // Já consideramos sincronizado pois enviamos direto
        db.insert("sync_log", null, logValues);

        // 3. Deletar localmente
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});
        saveLastSyncTime(updateAt);
    }

    public void deletarItem(String bc) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_NAME + " WHERE bc_compras = ? ORDER BY id DESC LIMIT 1",
                new String[]{bc}
        );
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            cursor.close();
            deletarItem(id);
        } else {
            cursor.close();
        }
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

            // Soft delete no Firebase
            ref.child(String.valueOf(id)).child("deleted").setValue(true);
            ref.child(String.valueOf(id)).child(FIELD_TIMESTAMP).setValue(ts);

            // Marcar como sincronizado e limpar log
            db.delete("sync_log", "item_id = ? AND table_name = ?",
                    new String[]{String.valueOf(id), TABLE_NAME});
        }

        if (onComplete != null) onComplete.run();
    }

    // ==================== TIMESTAMP ====================
    private long getLastSyncTime() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC, 0);
    }

    private void saveLastSyncTime(long time) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC, time)
                .apply();
    }

    // ==================== VERIFICAR DUPLICATA (COMPRAS) ====================
    // Retorna um array com {id, bc, descr, periodo, obs} se duplicata existir, ou null
    // A chave única é: bc_compras + descr_compras + periodo_compras + obs_compras
    public String[] verificarDuplicataCompras(String bc, String descr, String periodo, String obs) {
        Cursor cursor = db.rawQuery(
                "SELECT id, bc_compras, descr_compras, periodo_compras, obs_compras FROM " + TABLE_NAME +
                " WHERE bc_compras = ? AND descr_compras = ? AND periodo_compras = ? AND obs_compras = ? LIMIT 1",
                new String[]{bc, descr, periodo, obs}
        );
        if (cursor.moveToFirst()) {
            String[] item = new String[5];
            item[0] = String.valueOf(cursor.getLong(0));  // id
            item[1] = cursor.getString(1);                 // bc_compras
            item[2] = cursor.getString(2);                 // descr_compras
            item[3] = cursor.getString(3);                 // periodo_compras
            item[4] = cursor.getString(4);                 // obs_compras
            cursor.close();
            return item;
        }
        cursor.close();
        return null;
    }

    // ==================== COMPATIBILIDADE (MÉTODOS ANTIGOS) ====================
    public void syncLocalParaFirebase() {
        syncLocalParaFirebase(null);
    }

    public void syncFirebaseParaLocal() {
        syncFirebaseParaLocal(null);
    }

    public void syncCompleta() {
        syncLocalParaFirebase(() -> new android.os.Handler().postDelayed(
                () -> syncFirebaseParaLocal(null), 2500));
    }
}
