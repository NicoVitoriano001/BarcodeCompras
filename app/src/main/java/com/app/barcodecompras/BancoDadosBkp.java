package com.app.barcodecompras;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class BancoDadosBkp {
    private static final int REQUEST_CODE = 1;
    private static final File BACKUP_DIR = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "COMPRAS"
    );
    private final Context context;
    private final DatabaseHelper dbHelper;

    public BancoDadosBkp(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void showBackupConfirmationDialog() {
        String dataHora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeArquivoBKP = "comprasDB_" + dataHora + ".db";

        if (!BACKUP_DIR.exists()) {
            BACKUP_DIR.mkdirs();
        }

        new AlertDialog.Builder(context)
                .setTitle("Confirmar Backup")
                .setMessage("Deseja fazer backup do banco de dados?\n\n" +
                        "Local: " + BACKUP_DIR.getAbsolutePath() + "\n" +
                        "Nome: " + nomeArquivoBKP)
                .setPositiveButton("Sim", (dialog, which) -> fazerBackup())
                .setNegativeButton("Não", null)
                .show();
    }

    public void fazerBackup() {
        if (!checkStoragePermission()) {
            Toast.makeText(context, "Permissão necessária para fazer backup", Toast.LENGTH_SHORT).show();
            requestStoragePermission();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()) {
                Toast.makeText(context, "Permissão ainda não concedida", Toast.LENGTH_SHORT).show();
                return;
            }

            String dataHora = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nomeArquivoBKP = "comprasDB_" + dataHora + ".db";
            File arquivoBackup = new File(BACKUP_DIR, nomeArquivoBKP);
            File arquivoDB = context.getDatabasePath("comprasDB.db");

            if (!arquivoDB.exists()) {
                Toast.makeText(context, "Banco de dados não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }

            copiarArquivo(arquivoDB, arquivoBackup);

            Toast.makeText(context,
                    "Backup criado com sucesso em: " + arquivoBackup.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Erro ao criar backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void restaurarBackup() {
        if (!checkStoragePermission()) {
            Toast.makeText(context, "Permissão necessária para restaurar backup", Toast.LENGTH_SHORT).show();
            requestStoragePermission();
            return;
        }

        try {
            File downloadsDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadsDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "COMPRAS");
            } else {
                downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            }

            File arquivoDB = context.getDatabasePath("comprasDB.db");
            File[] arquivos = downloadsDir.listFiles((dir, nome) ->
                    nome.startsWith("comprasDB_") && nome.endsWith(".db"));

            if (arquivos == null || arquivos.length == 0) {
                Toast.makeText(context, "Nenhum backup encontrado na pasta Downloads", Toast.LENGTH_SHORT).show();
                return;
            }

            Arrays.sort(arquivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            String[] nomesArquivos = new String[arquivos.length];
            for (int i = 0; i < arquivos.length; i++) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                nomesArquivos[i] = arquivos[i].getName() + " - " + sdf.format(new Date(arquivos[i].lastModified()));
            }

            new AlertDialog.Builder(context)
                    .setTitle("Selecione o backup para restaurar")
                    .setItems(nomesArquivos, (dialog, which) -> confirmarRestauracao(arquivos[which]))
                    .setNegativeButton("Cancelar", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(context, "Erro ao acessar backups: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void confirmarRestauracao(File arquivoBackup) {
        new AlertDialog.Builder(context)
                .setTitle("Confirmar restauração")
                .setMessage("Deseja sobrescrever o banco de dados atual com o backup selecionado?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    try {
                        File arquivoDB = context.getDatabasePath("comprasDB.db");
                        copiarArquivo(arquivoBackup, arquivoDB);
                        Toast.makeText(context, "Banco de dados restaurado com sucesso!", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Erro ao restaurar backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void copiarArquivo(File origem, File destino) throws IOException {
        try (InputStream in = new FileInputStream(origem);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions((MainActivity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }
    }

    public boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
}