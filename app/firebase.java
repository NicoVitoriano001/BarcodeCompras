package com.app.barcodecompras.firebase;

import com.app.barcodecompras.Compra;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class FirebaseManager {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void salvarCompra(Compra compra) {
        long now = System.currentTimeMillis();
        compra.setUpdateAt(now);

        db.collection("compras")
                .document(compra.getBcCompras())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long remoteTime = document.getLong("updateAt");

                        if (remoteTime != null && remoteTime > compra.getUpdateAt()) {
                            return; // REMOTO É MAIS NOVO → NÃO SOBRESCREVE
                        }
                    }

                    db.collection("compras")
                            .document(compra.getBcCompras())
                            .set(compra);
                });
    }
}