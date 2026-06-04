package com.app.barcodecompras.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.app.barcodecompras.Compra;

public class FirebaseManager {
    private DatabaseReference dbRef;

    public FirebaseManager() {
        dbRef = FirebaseDatabase.getInstance().getReference("compras");
    }

    // SALVAR COM REGRA DE CONFLITO
    public void salvarCompra(Compra compra) {

        long now = System.currentTimeMillis();
        compra.setUpdatedAt(now);

        dbRef.child(compra.getBcCompras())
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {
                        Long remoteTime = snapshot.child("updatedAt").getValue(Long.class);

                        if (remoteTime != null && remoteTime > compra.getUpdatedAt()) {
                            return; // 🔥 remoto é mais novo
                        }
                    }

                    dbRef.child(compra.getBcCompras()).setValue(compra);
                });
    }
}