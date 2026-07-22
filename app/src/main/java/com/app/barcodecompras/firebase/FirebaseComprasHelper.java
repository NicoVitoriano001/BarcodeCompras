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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseComprasHelper {
    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;
    private static final String TAG = "FirebaseHelper";
    private static final String FIELD_TIMESTAMP = "updateAt";  // camelCase, sem underline

    public FirebaseComprasHelper(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference("compras");
    }

    // Sync Firebase → Local
    public void syncFirebaseParaLocal() {
        long lastSyncTime = getLastSyncTime();

        Query query = ref.orderByChild(FIELD_TIMESTAMP).startAt(lastSyncTime + 1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                long newestTimestamp = lastSyncTime;
                List<String> firebaseIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemId = child.getKey();
                    if (itemId == null) continue;

                    firebaseIds.add(itemId);

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

                    // Ler ID (pode ser o mesmo da chave ou um campo separado)
                    if (child.hasChild("id")) {
                        Long val = child.child("id").getValue(Long.class);
                        if (val != null) id = val;
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

                    // Usar FIELD_TIMESTAMP (updateAt)
                    if (child.hasChild(FIELD_TIMESTAMP)) {
                        Long val = child.child(FIELD_TIMESTAMP).getValue(Long.class);
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
                        db.delete("compras_tab", "id = ?", new String[]{String.valueOf(id)});
                        continue;
                    }

                    // Verificar se existe no banco local pelo ID
                    Cursor cursor = db.rawQuery(
                            "SELECT id, updated_at FROM compras_tab WHERE id = ?",
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
                            db.update("compras_tab", values, "id = ?", new String[]{String.valueOf(id)});
                        }
                    } else {
                        cursor.close();
                        db.insert("compras_tab", null, values);
                    }
                }

              // 2026.06.25 EDITEI, ENTÃO, NÃO DELETAR COMPRAS, MAS SIM, EDITAR.  checkForDeletedItems(newestTimestamp);

                if (newestTimestamp > lastSyncTime) {
                    saveLastSyncTime(newestTimestamp);
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    public void syncLocalParaFirebase() {
        long lastSyncTime = getLastSyncTime();

        Cursor cursor = db.rawQuery(
                "SELECT id, bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras, updated_at FROM compras_tab WHERE updated_at > ?",
                new String[]{String.valueOf(lastSyncTime)}
        );

        int count = cursor.getCount();
        if (count == 0) {
            cursor.close();
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

    }

    // Metodo auxiliar para detectar itens deletados
    private void checkForDeletedItems(long lastTimestamp) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> activeIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemId = child.getKey();
                    if (itemId != null) {
                        Boolean deleted = child.child("deleted").getValue(Boolean.class);
                        if (deleted == null || !deleted) {
                            activeIds.add(itemId);
                        }
                    }
                }

                Cursor localCursor = db.rawQuery("SELECT id FROM compras_tab", null);
                int deletedCount = 0;

                while (localCursor.moveToNext()) {
                    String localId = String.valueOf(localCursor.getLong(0));
                    if (!activeIds.contains(localId)) {
                        db.delete("compras_tab", "id = ?", new String[]{localId});
                        deletedCount++;
                    }
                }
                localCursor.close();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Erro ao verificar itens deletados: " + error.getMessage());
            }
        });
    }

    // Sincronização completa
    public void syncCompleta() {
        syncLocalParaFirebase();
        new android.os.Handler().postDelayed(() -> syncFirebaseParaLocal(), 2500);
    }

    // Deletar item
    public void deletarItem(long id) {
        long updateAt = System.currentTimeMillis();

        ref.child(String.valueOf(id)).child("deleted").setValue(true);
        ref.child(String.valueOf(id)).child(FIELD_TIMESTAMP).setValue(updateAt);

        db.delete("compras_tab", "id = ?", new String[]{String.valueOf(id)});
        saveLastSyncTime(updateAt);

    }

    public void deletarItem(String bc) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM compras_tab WHERE bc_compras = ? ORDER BY id DESC LIMIT 1",
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