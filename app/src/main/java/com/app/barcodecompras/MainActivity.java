package com.app.barcodecompras;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import android.os.Environment;
import androidx.core.view.GravityCompat;
import android.app.AlertDialog;


public class MainActivity extends AppCompatActivity {
private static final int REQUEST_CODE = 1;
private static final int REQUEST_CODE_ADD_ITEM = 1001;
private EditText bc_compras, descr_compras, cat_compras, preco_compras, qnt_compras, total_compras, periodo_compras, obs_compras;
private Button scanButton, saveButton, cancelButton;
private SQLiteDatabase db;
private DrawerLayout drawer;
private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar permissão ao iniciar o app. Usado no backup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissão Necessária")
                    .setMessage("Para salvar backups na pasta Downloads, por favor conceda a permissão 'Gerenciar todos os arquivos'")
                    .setPositiveButton("OK", (dialog, which) -> requestStoragePermission())
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        bc_compras = findViewById(R.id.bc_compras);
        descr_compras = findViewById(R.id.descr_compras);
        cat_compras = findViewById(R.id.cat_compras);
        preco_compras = findViewById(R.id.preco_compras);
        qnt_compras = findViewById(R.id.qnt_compras);
        total_compras = findViewById(R.id.total_compras);
        periodo_compras = findViewById(R.id.periodo_compras);
        obs_compras = findViewById(R.id.obs_compras);
        scanButton = findViewById(R.id.scanButton);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // No onCreate(), após inicializar os views:
        periodo_compras.setText(getDataHoraAtual()); // Define a data atual ao inicia

        // No onCreate() do MainActivity, após inicializar os views:
        FloatingActionButton fabSearch = findViewById(R.id.fab_searchITEM);
        fabSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BuscarComprasActivity.class);
            startActivity(intent);
        });

       // SQLiteDatabase: /data/user/0/com.app.barcodecompras/databases/comprasDB.db
       db = openOrCreateDatabase("comprasDB.db", MODE_PRIVATE, null);

       Button scanButton = findViewById(R.id.scanButton);

       scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
            integrator.setPrompt("Escaneie o código de barras");
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
       });

       periodo_compras.setOnClickListener(v -> showDatePickerDialog());
       saveButton.setOnClickListener(v -> saveData());
       cancelButton.setOnClickListener(v -> clearFields());

       if (checkStoragePermission()) {
           // Inicializar o banco de dados primeiro
           initializeDatabase();  // Agora usando o caminho padrão
       }

// Configurar Toolbar (usando a versão AppCompat)
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

// Configurar Navigation Drawer
        drawer = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

                if (id == R.id.nav_home) {
                    // Ação para home
                } else if (id == R.id.nav_gallery) {
                    // Ação para galeria
                } else if (id == R.id.nav_slideshow) {
                    // Ação para slideshow
                } else if (id == R.id.nav_busca_collected) {
                    Intent intent = new Intent(MainActivity.this, BuscarCollectedActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_backup) {
                    fazerBackup();
                } else if (id == R.id.nav_restore) {
                    restaurarBackup();
                }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    } // fim ONCREATE

//inicio data calendário
    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
                    periodo_compras.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day);
        datePickerDialog.show();
    }
    public String getDataHoraAtual() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
//fim data calendario
    private void initializeDatabase() {
    //data/data/com.app.barcodecompras/databases/comprasDB.db
    File dbFile = getDatabasePath("comprasDB.db");

    // Garantir que o diretório pai existe
    File parentDir = dbFile.getParentFile();
    if (!parentDir.exists()) {
        parentDir.mkdirs();
    }

    db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

    db.execSQL("CREATE TABLE IF NOT EXISTS compras_tab (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "bc_compras NUMERIC," +
            "descr_compras TEXT," +
            "cat_compras TEXT," +
            "preco_compras REAL," +
            "qnt_compras REAL," +
            "total_compras REAL," +
            "periodo_compras TEXT," +
            "obs_compras TEXT)");
    }

    // Resultado do scanner ZXing
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            bc_compras.setText(result.getContents());
            fetchItemDataCollectedTable(result.getContents());
        } else {
            Toast.makeText(this, "Nenhum código escaneado", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == REQUEST_CODE_ADD_ITEM && resultCode == RESULT_OK) {
            // Recarregar os dados após cadastro
            String barcode = bc_compras.getText().toString();
            fetchItemDataCollectedTable(barcode);
        }
    }

// Busca descr_compras e cat_compras baseado no código escaneado
// Busca descrição e categoria na tabela collected_tab
    private void fetchItemDataCollectedTable(String barcodeValue) {
    if (db == null || !db.isOpen()) {
        db = getDatabase(); // Metodo auxiliar para obter a instância correta
        Toast.makeText(this, "Banco de dados não disponível", Toast.LENGTH_SHORT).show();
        return;
    }

    Cursor cursor = db.rawQuery(
            "SELECT descr_imdb, cat_imdb FROM collected_tab WHERE bc_imdb = ?",
            new String[]{barcodeValue}
    );

    if (cursor != null) {
        try {
            if (cursor.moveToFirst()) {
                descr_compras.setText(cursor.getString(0)); // descr_imdb
                cat_compras.setText(cursor.getString(1));   // cat_imdb
            } else {
                // Item não encontrado - abrir activity de cadastro
                Intent intent = new Intent(MainActivity.this, AddItemIMDB.class);
                intent.putExtra("BARCODE_VALUE", barcodeValue);
                startActivityForResult(intent, REQUEST_CODE_ADD_ITEM);
            }
        } finally {
            cursor.close();
        }
    }
}

    // Metodo auxiliar para obter a instância do banco
    private SQLiteDatabase getDatabase() {
        if (db == null || !db.isOpen()) {
            File dbFile = getDatabasePath("comprasDB.db");
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        }
        return db;
    }

    private void saveData() {
        String bc_comprasVal = bc_compras.getText().toString().trim();
        String descr_comprasVal = descr_compras.getText().toString().trim();
        String cat_comprasVal = cat_compras.getText().toString().trim();
        String preco_comprasStr = preco_compras.getText().toString().trim();
        String qnt_comprasStr = qnt_compras.getText().toString().trim();
        String periodo_comprasVal = periodo_compras.getText().toString().trim();
        String obs_comprasVal = obs_compras.getText().toString().trim();

        if (bc_comprasVal.isEmpty() || preco_comprasStr.isEmpty() || qnt_comprasStr.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        double preco_comprasVal = Double.parseDouble(preco_comprasStr);
        double qnt_comprasVal = Double.parseDouble(qnt_comprasStr);
        double total_comprasVal = preco_comprasVal * qnt_comprasVal;

        total_compras.setText(String.valueOf(total_comprasVal));

        ContentValues values = new ContentValues();
        values.put("bc_compras", bc_comprasVal);
        values.put("descr_compras", descr_comprasVal);
        values.put("cat_compras", cat_comprasVal);
        values.put("preco_compras", preco_comprasVal);
        values.put("qnt_compras", qnt_comprasVal);
        values.put("total_compras", total_comprasVal);
        values.put("periodo_compras", periodo_comprasVal);
        values.put("obs_compras", obs_comprasVal);

        long result = db.insert("compras_tab", null, values);

        if (result != -1) {
            Toast.makeText(this, "Salvo com sucesso!", Toast.LENGTH_SHORT).show();
            clearFields();
        } else {
            Toast.makeText(this, "Erro ao salvar!", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
        }
    }
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ precisa de permissão especial
            return Environment.isExternalStorageManager();
        } else {
            // Android 10 e abaixo
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, pode continuar
            } else {
                Toast.makeText(this, "Permissão negada. Não é possível fazer backup/restore.", Toast.LENGTH_SHORT).show();
            }
        }
    }

   //data/data/com.app.barcodecompras/databases/comprasDB.db
    private void fazerBackup() {
        if (!checkStoragePermission()) {
            Toast.makeText(this, "Permissão necessária para fazer backup", Toast.LENGTH_SHORT).show();
            requestStoragePermission();
            return;
        }

        try {
            // Verificar se temos permissão mesmo após solicitar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Permissão ainda não concedida", Toast.LENGTH_SHORT).show();
                return;
            }
            File arquivoDB = getDatabasePath("comprasDB.db");
            String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String nomeArquivoBKP = "comprasDB_" + dataHora + ".db";

            // Usando a API recomendada para Android 10+
            File downloadsDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadsDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "COMPRAS");
            } else {
                downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            }

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File arquivoBackup = new File(downloadsDir, nomeArquivoBKP);
           // File arquivoDB = getDatabasePath("comprasDB.db");

            if (!arquivoDB.exists()) {
                Toast.makeText(this, "Banco de dados não encontrado!", Toast.LENGTH_SHORT).show();
                return;
            }

            copiarArquivo(arquivoDB, arquivoBackup);

            // Notificar o sistema sobre o novo arquivo (necessário no Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaScannerConnection.scanFile(this,
                        new String[]{arquivoBackup.getAbsolutePath()},
                        null,
                        null);
            }

            Toast.makeText(this,
                    "Backup criado com sucesso em: " + arquivoBackup.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao criar backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void restaurarBackup() {
        if (!checkStoragePermission()) {
            Toast.makeText(this, "Permissão necessária para restaurar backup", Toast.LENGTH_SHORT).show();
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

            //
            File arquivoDB = new File(getDatabasePath("comprasDB.db").getPath());
            File[] arquivos = downloadsDir.listFiles((dir, nome) ->
                    nome.startsWith("comprasDB_") && nome.endsWith(".db"));

            if (arquivos == null || arquivos.length == 0) {
                Toast.makeText(this, "Nenhum backup encontrado na pasta Downloads", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ordenar por data (do mais recente para o mais antigo)
            Arrays.sort(arquivos, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            String[] nomesArquivos = new String[arquivos.length];
            for (int i = 0; i < arquivos.length; i++) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                nomesArquivos[i] = arquivos[i].getName() + " - " + sdf.format(new Date(arquivos[i].lastModified()));
            }

            new AlertDialog.Builder(this)
                    .setTitle("Selecione o backup para restaurar")
                    .setItems(nomesArquivos, (dialog, which) -> {
                        confirmarRestauracao(arquivos[which]);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao acessar backups: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void confirmarRestauracao(File arquivoBackup) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar restauração")
                .setMessage("Deseja sobrescrever o banco de dados atual com o backup selecionado?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    try {
                        File arquivoDB = new File(getDatabasePath("comprasDB.db").getPath());
                        copiarArquivo(arquivoBackup, arquivoDB);
                        Toast.makeText(this, "Banco de dados restaurado com sucesso!", Toast.LENGTH_LONG).show();

                        // Recarregar a activity para aplicar as mudanças
                        recreate();
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao restaurar backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void clearFields() {
        bc_compras.setText("");
        descr_compras.setText("");
        cat_compras.setText("");
        preco_compras.setText("");
        qnt_compras.setText("");
        total_compras.setText("");
        periodo_compras.setText("");
        obs_compras.setText("");
    }

    @Override
    protected void onDestroy() {
        if (db != null) {
            if (db.isOpen()) {
                db.close();
            }
            db = null;
        }
        super.onDestroy();
    }
}