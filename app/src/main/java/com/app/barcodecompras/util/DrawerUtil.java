package com.app.barcodecompras.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.*;
import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.firebase.FirebaseComprasHelper;
import com.app.barcodecompras.firebase.FirebaseBancoDadosHelper;
import com.google.android.material.navigation.NavigationView;

public class DrawerUtil {
    public static void setupDrawer(
            Activity activity,
            DrawerLayout drawer,
            NavigationView navigationView,
            FirebaseComprasHelper firebaseComprasHelper,
            FirebaseBancoDadosHelper firebaseBancoDadosHelper,
            BancoDadosBkp bancoDadosBkp
    ) {

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {

                if (id == R.id.nav_home) {

                    activity.startActivity(new Intent(activity, MainActivity.class));

                } else if (id == R.id.nav_add_bancodados) {

                    activity.startActivity(new Intent(activity, AddItemBancoDados.class));

                } else if (id == R.id.nav_busca_bancodados) {

                    activity.startActivity(new Intent(activity, BuscarBancoDadosActivity.class));

                } else if (id == R.id.nav_busca_compras) {

                    activity.startActivity(new Intent(activity, BuscarComprasActivity.class));

                } else if (id == R.id.nav_listar_categorias) {

                    activity.startActivity(new Intent(activity, ConfigCategoriasActivity.class));

                } else if (id == R.id.nav_syncFirebase) {

                    // sincroniza compras
                    if (firebaseComprasHelper != null) {
                        firebaseComprasHelper.syncCompleta();
                    }

                    // sincroniza bancodados
                    if (firebaseBancoDadosHelper != null) {
                        firebaseBancoDadosHelper.syncCompleta();
                    }

                    Toast.makeText(activity, "Sync Compras e Banco dados...", Toast.LENGTH_SHORT).show();

                } else if (id == R.id.nav_backup) {

                    if (bancoDadosBkp != null) {
                        bancoDadosBkp.showBackupConfirmationDialog();
                    }

                } else if (id == R.id.nav_restore) {

                    if (bancoDadosBkp != null) {
                        bancoDadosBkp.restaurarBackup();
                    }
                }

            }, 200);

            return true;
        });
    }
}