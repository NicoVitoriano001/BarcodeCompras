package com.app.barcodecompras.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "comprasDB.db";
    private static final int DATABASE_VERSION = 3; // aumente versão

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //Tabela compras
        db.execSQL("CREATE TABLE IF NOT EXISTS compras_tab (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bc_compras TEXT, " +
                "descr_compras TEXT, " +
                "cat_compras TEXT, " +
                "preco_compras REAL, " +
                "qnt_compras REAL, " +
                "total_compras REAL, " +
                "periodo_compras TEXT, " +
                "obs_compras TEXT, " +
                "updated_at INTEGER)");

        //Tabela banco de dados
        db.execSQL("CREATE TABLE IF NOT EXISTS bancodados_tab (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bc_DB NUMBER, " +
                "descr_DB TEXT, " +
                "cat_DB TEXT," +
                "updated_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //Atualização segura (sem apagar dados)
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE compras_tab ADD COLUMN updated_at INTEGER");
            } catch (Exception ignored) {}
        }
    }

}