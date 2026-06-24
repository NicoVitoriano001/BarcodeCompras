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

public class FirebaseBancoDadosHelper {
    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;
    private static final String TAG = "FirebaseBancoDados";
    private static final String FIELD_TIMESTAMP = "updateAt";  // camelCase, sem underline
    private static final String TABLE_NAME = "bancodados_tab";
    private static final String FIREBASE_NODE = "bancodados2"; // Novo nó no Firebase

    public FirebaseBancoDadosHelper(Context context, SQLiteDatabase db) {
        this.context = context;
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference(FIREBASE_NODE);
    }

    // 2026.06.24 FIREBASE → LOCAL (mesma lógica do FirebaseComprasHelper)
    public void syncFirebaseParaLocal(Runnable onComplete) {
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
                    long updateAt = 0;

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

                    // Verificar se existe no banco local pelo ID
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

                // Verificar itens que foram deletados no Firebase (hard delete)
                checkForDeletedItems();

                if (newestTimestamp > lastSyncTime) {
                    saveLastSyncTime(newestTimestamp);
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }


    // 2026.06.24 LOCAL → FIREBASE (mesma lógica do FirebaseComprasHelper)
        public void syncLocalParaFirebase() {
        long lastSyncTime = getLastSyncTime();

        Cursor cursor = db.rawQuery(
                "SELECT id, bc_DB, descr_DB, cat_DB, updated_at FROM " + TABLE_NAME +
                        " WHERE updated_at > ?",
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
            long updateAt = cursor.getLong(4);

            if (updateAt > newestTimestamp) {
                newestTimestamp = updateAt;
            }

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", id);
            itemMap.put("bc", bc);
            itemMap.put("descricao", descricao);
            itemMap.put("categoria", categoria);
            itemMap.put(FIELD_TIMESTAMP, updateAt);

            ref.child(String.valueOf(id)).setValue(itemMap);
        }
        cursor.close();

        if (newestTimestamp > lastSyncTime) {
            saveLastSyncTime(newestTimestamp);
        }
    }

    private void checkForDeletedItems() {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> firebaseIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String itemId = child.getKey();
                    if (itemId != null) {
                        firebaseIds.add(itemId);
                    }
                }

                Cursor localCursor = db.rawQuery("SELECT id FROM " + TABLE_NAME, null);
                int deletedCount = 0;

                while (localCursor.moveToNext()) {
                    String localId = String.valueOf(localCursor.getLong(0));
                    if (!firebaseIds.contains(localId)) {
                        db.delete(TABLE_NAME, "id = ?", new String[]{localId});
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

    // 2026.06.24 SYNC COMPLETA (BIDIRECIONAL)
    public void syncCompleta() {

        ref.get().addOnSuccessListener(snapshot -> {

            if (!snapshot.hasChildren()) {
                // FIREBASE VAZIO → SOBE TUDO DO LOCAL (SEM FILTRO)
                Cursor cursor = db.rawQuery(
                        "SELECT id, bc_DB, descr_DB, cat_DB, updated_at FROM " + TABLE_NAME,
                        null
                );

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);

                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", id);
                    itemMap.put("bc", cursor.getString(1));
                    itemMap.put("descricao", cursor.getString(2));
                    itemMap.put("categoria", cursor.getString(3));
                    itemMap.put(FIELD_TIMESTAMP, cursor.getLong(4));

                    ref.child(String.valueOf(id)).setValue(itemMap);
                }

                cursor.close();

                saveLastSyncTime(System.currentTimeMillis());

            } else {
                // FLUXO NORMAL
                syncFirebaseParaLocal(() -> syncLocalParaFirebase());
            }

        }).addOnFailureListener(e -> {
        });
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
            // Sincroniza com Firebase
            syncLocalParaFirebase();
            //Log.d(TAG, "Item inserido localmente e enviado para Firebase: ID=" + id);
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

        // Atualiza local
        int rows = db.update(TABLE_NAME, values, "id = ?", new String[]{String.valueOf(id)});

        if (rows > 0) {
            // Sincroniza com Firebase
            syncLocalParaFirebase();
        }
    }

    // 2026.06.24 DELETE (HARD DELETE - remove do Firebase e do Local)
    public void deletarItem(long id) {
        // Remove do Firebase (hard delete)
        ref.child(String.valueOf(id)).removeValue()
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });

        // Remove do SQLite
        db.delete(TABLE_NAME, "id = ?", new String[]{String.valueOf(id)});

        // Atualiza timestamp de sincronização
        saveLastSyncTime(System.currentTimeMillis());
    }


    private long getLastSyncTime() {
        return context.getSharedPreferences("sync_bancodados", Context.MODE_PRIVATE)
                .getLong("last_sync_time", 0);
    }

    private void saveLastSyncTime(long time) {
        context.getSharedPreferences("sync_bancodados", Context.MODE_PRIVATE)
                .edit()
                .putLong("last_sync_time", time)
                .apply();
    }

}