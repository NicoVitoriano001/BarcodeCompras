package com.app.barcodecompras.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.app.barcodecompras.AddItemBancoDados;
import com.app.barcodecompras.BuscarBancoDadosActivity;
import com.app.barcodecompras.BuscarComprasActivity;
import com.app.barcodecompras.MainActivity;
import com.app.barcodecompras.R;
import com.app.barcodecompras.ResultComprasActivity;
import com.app.barcodecompras.database.BancoDadosBkp;
import com.app.barcodecompras.firebase.FirebaseHelper;
import com.google.android.material.navigation.NavigationView;

public class DrawerUtil {

    public static void setupDrawer(Activity activity,
                                  DrawerLayout drawer,
                                  NavigationView navigationView,
                                  FirebaseHelper firebaseHelper,
                                  BancoDadosBkp bancoDadosBkp){


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

                } else if (id == R.id.nav_syncFirebase) {
                    if (firebaseHelper != null) {
                        firebaseHelper.syncCompleta();
                        Toast.makeText(activity, "Sincronizando...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Erro: FirebaseHelper não inicializado", Toast.LENGTH_SHORT).show();
                    }

                } else if (id == R.id.nav_backup) {

                    if (bancoDadosBkp != null) {
                        bancoDadosBkp.showBackupConfirmationDialog();
                    } else {
                        Toast.makeText(activity, "Erro no backup", Toast.LENGTH_SHORT).show();
                    }

                } else if (id == R.id.nav_restore) {

                    if (bancoDadosBkp != null) {
                        bancoDadosBkp.restaurarBackup();
                    } else {
                        Toast.makeText(activity, "Erro no restore", Toast.LENGTH_SHORT).show();
                    }
                }

            }, 200);

            return true;
        });
    }
}
