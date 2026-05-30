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

public class FirebaseHelper {

    private SQLiteDatabase db;
    private DatabaseReference ref;

    public FirebaseHelper(SQLiteDatabase db) {
        this.db = db;
        this.ref = FirebaseDatabase.getInstance().getReference("compras");
    }

    private void syncLocalParaFirebase() {

        Cursor cursor = db.rawQuery("SELECT * FROM compras_tab", null);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("compras");

        while (cursor.moveToNext()) {

            String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
            //if (bc == null || bc.isEmpty()) continue;

            String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
            String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));

            double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
            double qnt = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
            double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"));

            String periodo = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
            String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

            long updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));

            ref.child(bc).get().addOnSuccessListener(snapshot -> {

                if (snapshot.exists()) {

                    Long remoteTime = snapshot.child("updatedAt").getValue(Long.class);

                    if (remoteTime != null && remoteTime >= updatedAt) {
                        return; //NÃO ENVIA (já está atualizado)
                    }
                }

                //ENVIA só se necessário
                ref.child(bc).setValue(
                        new CompraFirebase(
                                bc, descr, cat, preco, qnt, total, periodo, obs, updatedAt
                        )
                );
            });
        }

        cursor.close();
    }

    private void syncFirebaseParaLocal() {

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
                        values.put("updated_at", remoteUpdated);

                        db.insert("compras_tab", null, values);
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
