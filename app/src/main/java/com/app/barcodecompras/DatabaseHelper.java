package com.app.barcodecompras;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "comprasDB.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS compras_tab");
        onCreate(db);
    }
}