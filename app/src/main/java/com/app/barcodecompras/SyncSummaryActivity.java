package com.app.barcodecompras;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.database.DatabaseHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.firebase.SyncSummary;
import com.app.barcodecompras.firebase.SyncSummaryAdapter;

import java.util.ArrayList;
import java.util.List;

public class SyncSummaryActivity extends AppCompatActivity {

    private RecyclerView rvSyncSummary;
    private SyncSummaryAdapter adapter;
    private List<SyncSummary> syncList = new ArrayList<>();
    private TextView tvSyncTotal;
    private Button btnSyncConfirm, btnSyncCancel;

    private SQLiteDatabase db;
    private FirebaseComprasHelper firebaseComprasHelper;
    private FirebaseBancoDadosHelper firebaseBancoHelper;
    private int firebaseQueriesPending = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_summary);

        rvSyncSummary = findViewById(R.id.rvSyncSummary);
        tvSyncTotal = findViewById(R.id.tvSyncTotal);
        btnSyncConfirm = findViewById(R.id.btnSyncConfirm);
        btnSyncCancel = findViewById(R.id.btnSyncCancel);

        rvSyncSummary.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        DatabaseHelper.garantirTabelaSyncLog(db);

        firebaseComprasHelper = new FirebaseComprasHelper(this, db);
        firebaseBancoHelper = new FirebaseBancoDadosHelper(this, db);

        btnSyncCancel.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        btnSyncConfirm.setOnClickListener(v -> executarSincronizacao());

        // 1. Mostrar sumário Local → Firebase (imediato)
        carregarSumarioLocal();

        // 2. Buscar sumário Firebase → Local (assíncrono)
        buscarSumarioFirebase();
    }

    private void carregarSumarioLocal() {
        // Sumário Local → Firebase para Compras
        List<SyncSummary> comprasLocal = firebaseComprasHelper.getSyncSummaryLocalToFirebase();
        if (!comprasLocal.isEmpty()) {
            syncList.add(new SyncSummary("compras_tab", "LOCAL→FIREBASE", 0, "--- Compras enviar ---"));
            syncList.addAll(comprasLocal);
        }

        // Sumário Local → Firebase para BancoDados
        List<SyncSummary> bancodadosLocal = firebaseBancoHelper.getSyncSummaryLocalToFirebase();
        if (!bancodadosLocal.isEmpty()) {
            syncList.add(new SyncSummary("bancodados_tab", "LOCAL→FIREBASE", 0, "--- Banco Dados enviar ---"));
            syncList.addAll(bancodadosLocal);
        }

        atualizarAdapter();
    }

    private void buscarSumarioFirebase() {
        firebaseQueriesPending = 2; // compras + bancodados

        // Compras Firebase → Local
        firebaseComprasHelper.getSyncSummaryFirebaseToLocal(syncList, () -> {
            runOnUiThread(this::onFirebaseQueryComplete);
        });

        // Bancodados Firebase → Local
        firebaseBancoHelper.getSyncSummaryFirebaseToLocal(syncList, () -> {
            runOnUiThread(this::onFirebaseQueryComplete);
        });
    }

    private void onFirebaseQueryComplete() {
        firebaseQueriesPending--;
        if (firebaseQueriesPending <= 0) {
            atualizarAdapter();
        }
    }

    private void atualizarAdapter() {
        // Remove placeholder "Nenhuma alteração pendente" se houver itens reais
        boolean hasRealItems = false;
        for (SyncSummary s : syncList) {
            if (s.getQuantity() > 0) {
                hasRealItems = true;
                break;
            }
        }
        if (hasRealItems) {
            for (int i = syncList.size() - 1; i >= 0; i--) {
                SyncSummary s = syncList.get(i);
                if (s.getQuantity() == 0 && "Nenhuma alteração pendente".equals(s.getDescription())) {
                    syncList.remove(i);
                }
            }
        }

        if (syncList.isEmpty()) {
            syncList.add(new SyncSummary("-", "OK", 0, "Nenhuma alteração pendente"));
        }

        if (adapter == null) {
            adapter = new SyncSummaryAdapter(syncList);
            rvSyncSummary.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        int total = 0;
        for (SyncSummary s : syncList) {
            total += s.getQuantity();
        }
        tvSyncTotal.setText(String.format("Total: %d operações pendentes", total));

        if (total == 0) {
            btnSyncConfirm.setEnabled(false);
            btnSyncConfirm.setAlpha(0.5f);
            btnSyncConfirm.setText("Nada a sincronizar");
        } else {
            btnSyncConfirm.setEnabled(true);
            btnSyncConfirm.setAlpha(1.0f);
            btnSyncConfirm.setText("🔄 Sincronizar");
        }
    }

    private void executarSincronizacao() {
        btnSyncConfirm.setEnabled(false);
        btnSyncConfirm.setText("Sincronizando...");

        Toast.makeText(this, "Enviando alterações locais para Firebase...", Toast.LENGTH_SHORT).show();

        // 1. Local → Firebase: compras
        firebaseComprasHelper.syncLocalParaFirebase(() -> {
            runOnUiThread(() -> {
                // 2. Local → Firebase: banco de dados
                firebaseBancoHelper.syncLocalParaFirebase(() -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Buscando alterações do Firebase...", Toast.LENGTH_SHORT).show();

                        // 3. Firebase → Local: compras
                        firebaseComprasHelper.syncFirebaseParaLocal(() -> {
                            runOnUiThread(() -> {
                                // 4. Firebase → Local: banco de dados
                                firebaseBancoHelper.syncFirebaseParaLocal(() -> {
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Sincronização concluída com sucesso!", Toast.LENGTH_LONG).show();
                                        setResult(Activity.RESULT_OK);
                                        finish();
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        super.onDestroy();
    }
}
