package com.app.barcodecompras;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.google.android.material.navigation.NavigationView;

public class BuscarComprasActivity extends AppCompatActivity {
    private static final int EDIT_COMPRA_REQUEST = 1;
    private EditText etBuscaCodigo, etBuscaDescricao, etBuscaCategoria, etBuscaPeriodo , etBuscaOBS;
    private Button btnBuscar, btnCancelar;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_compras);

        // Inicializar views
        etBuscaCodigo = findViewById(R.id.etBuscaCodigo);
        etBuscaDescricao = findViewById(R.id.etBuscaDescricao);
        etBuscaCategoria = findViewById(R.id.etBuscaCategoria);
        etBuscaPeriodo = findViewById(R.id.etBuscaPeriodo);
        etBuscaOBS = findViewById(R.id.etBuscaOBS);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnCancelar = findViewById(R.id.btnCancelarBusca);

        // Configurar data atual no campo de período
        etBuscaPeriodo.setText(getDataHoraAtual());
        // Configurar listener para abrir o date picker
        etBuscaPeriodo.setOnClickListener(v -> showDatePickerDialog());

        // Configurar listeners
        btnBuscar.setOnClickListener(v -> realizarBusca());
        btnCancelar.setOnClickListener(v -> finish());

        //DRAWER -- INICIO
        drawer = findViewById(R.id.edit_drawer_layout);
        navigationView = findViewById(R.id.busca_compras_nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            // Adicione um pequeno delay para evitar travamentos
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_add_bancodados) {
                    startActivity(new Intent(this, AddItemIMDB.class));
                } else if (id == R.id.nav_busca_bancodados) {
                    startActivity(new Intent(this, BuscarBancoDadosActivity.class));
                } else if (id == R.id.nav_backup) {
                    startActivity(new Intent(this, MainActivity.class));
                } else if (id == R.id.nav_restore) {
                    startActivity(new Intent(this, MainActivity.class));
                }
                // Não chame finish() aqui - deixe o sistema gerenciar
            }, 200); // 250ms de delay
            return true;
        });//DRAWER -- FIM
    }// FIM ON CREATE


// INICIO PEGAR DATA
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
                    etBuscaPeriodo.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day);
        datePickerDialog.show();
    }
    public String getDataHoraAtual() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
    // FIM PEGAR DATA

    private void realizarBusca() {
        Intent intent = new Intent(this, ResultComprasActivity.class);

        // Passar parâmetros de busca para a tela de resultados
        intent.putExtra("CODIGO", etBuscaCodigo.getText().toString());
        intent.putExtra("DESCRICAO", etBuscaDescricao.getText().toString());
        intent.putExtra("CATEGORIA", etBuscaCategoria.getText().toString());
        intent.putExtra("PERIODO", etBuscaPeriodo.getText().toString());
        intent.putExtra("OBSERVACAO", etBuscaOBS.getText().toString());
        startActivity(intent);
    }

    // Adicione este metodo para tratar o retorno
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_COMPRA_REQUEST && resultCode == RESULT_OK) {
            finish();
            }
        }
    }
