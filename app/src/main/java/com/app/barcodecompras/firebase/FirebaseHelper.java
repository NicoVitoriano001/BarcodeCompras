package com.app.barcodecompras.firebase;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.app.barcodecompras.database.CompraFirebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.Context;


public class FirebaseHelper {

    private SQLiteDatabase db;
    private DatabaseReference ref;
    private Context context;


    public FirebaseHelper(Context context, SQLiteDatabase db) {
    this.context = context;
    this.db = db;
    //this.ref = FirebaseDatabase.getInstance().getReference("compras");
}

    public void syncLocalParaFirebase() {
//quando abre app, quando muda muita coisa, sincroniza tudo

    //DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

    // ✅ 1. recuperar último sync
    long lastSyncTime = getLastSyncTime();

    // ✅ 2. buscar só itens modificados
    Cursor cursor = db.rawQuery(
            "SELECT * FROM compras_tab WHERE updated_at > ?",
            new String[]{String.valueOf(lastSyncTime)}
    );

    // ✅ 3. buscar Firebase UMA vez
    ref.addListenerForSingleValueEvent(new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot snapshot) {

            long novoLastSync = lastSyncTime;

            while (cursor.moveToNext()) {

                String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
                if (bc == null || bc.isEmpty()) continue;

                String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));

                double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
                double qnt = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"));

                String periodo = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
                String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

                long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));

                // ✅ verifica no Firebase (sem nova chamada)
                DataSnapshot child = snapshot.child(bc);

                if (child.exists()) {

                    Long remoteTime = child.child("updatedAt").getValue(Long.class);

                    if (remoteTime != null && remoteTime >= updatedAt) {
                        continue; // já está sincronizado
                    }
                }

                // ✅ envia
                ref.child(bc).setValue(
                        new CompraFirebase(
                                bc, descr, cat, preco, qnt, total, periodo, obs, updatedAt
                        )
                );

                // ✅ atualiza maior timestamp
                if (updatedAt > novoLastSync) {
                    novoLastSync = updatedAt;
                }
            }

            cursor.close();

            // ✅ salva novo sync
            saveLastSyncTime(novoLastSync);
        }

        @Override
        public void onCancelled(DatabaseError error) {
            cursor.close();
        }
    });
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



    public void syncFirebaseParaLocal() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for (DataSnapshot child : snapshot.getChildren()) {

                    String bc = child.getKey();
                    if (bc == null) continue;

                    // 🔹 Dados do Firebase
                    String descr = child.child("descr").getValue(String.class);
                    String cat = child.child("cat").getValue(String.class);
                    Double preco = child.child("preco").getValue(Double.class);
                    Double qnt = child.child("qnt").getValue(Double.class);
                    Double total = child.child("total").getValue(Double.class);
                    String periodo = child.child("periodo").getValue(String.class);
                    String obs = child.child("obs").getValue(String.class);

                    Long remoteUpdated = child.child("updatedAt").getValue(Long.class);
                    if (remoteUpdated == null) remoteUpdated = 0L;

                    Cursor cursor = db.rawQuery(
                            "SELECT updated_at FROM compras_tab WHERE bc_compras = ?",
                            new String[]{bc}
                    );

                    if (cursor.moveToFirst()) {

                        // 🔹 Já existe no banco local
                        long localUpdated = cursor.getLong(0);

                        // ✅ Atualiza só se Firebase for mais novo
                        if (remoteUpdated > localUpdated) {

                            ContentValues values = new ContentValues();
                            values.put("descr_compras", descr);
                            values.put("cat_compras", cat);
                            values.put("preco_compras", preco);
                            values.put("qnt_compras", qnt);
                            values.put("total_compras", total);
                            values.put("periodo_compras", periodo);
                            values.put("obs_compras", obs);
                            values.put("updated_at", remoteUpdated);

                            db.update(
                                    "compras_tab",
                                    values,
                                    "bc_compras = ?",
                                    new String[]{bc}
                            );
                        }

                    } else {

                        // ✅ Não existe → INSERE
                        ContentValues values = new ContentValues();
                            values.put("bc_compras", bc);
                            values.put("descr_compras", descr);
                            values.put("cat_compras", cat);

                            values.put("preco_compras", preco);
                            values.put("qnt_compras", qnt);
                            values.put("total_compras", total);

                            values.put("periodo_compras", periodo);
                            values.put("obs_compras", obs);

                            values.put("updated_at", remoteUpdated);

                    }

                    cursor.close();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // opcional: tratar erro
            }
        });
    }
}
