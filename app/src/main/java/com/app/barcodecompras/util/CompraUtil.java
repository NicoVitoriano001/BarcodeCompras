package com.app.barcodecompras.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.app.barcodecompras.Compra;

import java.util.ArrayList;
import java.util.List;

/**
 * Métodos utilitários compartilhados para consultas de compras.
 * Evita duplicação de código entre adapter e activity.
 */
public class CompraUtil {

    /**
     * Busca todas as compras associadas a um código de barras.
     */
    public static List<Compra> buscarComprasPorCodigo(SQLiteDatabase db, String codigo) {
        List<Compra> registros = new ArrayList<>();

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM compras_tab WHERE bc_compras = ? ORDER BY SUBSTR(periodo_compras, 5) DESC, periodo_compras ASC",
                    new String[]{codigo}
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
                    String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
                    String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));
                    double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
                    double quantidade = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"));
                    String periodoCompra = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
                    String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

                    registros.add(new Compra(id, bc, descr, cat, preco, quantidade, total, periodoCompra, obs));
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return registros;
    }

    /**
     * Busca a primeira compra associada a um código de barras.
     */
    public static Compra buscarPrimeiraCompraPorCodigo(SQLiteDatabase db, String codigo) {
        Compra compra = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM compras_tab WHERE bc_compras = ? LIMIT 1",
                    new String[]{codigo}
            );
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String bc = cursor.getString(cursor.getColumnIndexOrThrow("bc_compras"));
                String descr = cursor.getString(cursor.getColumnIndexOrThrow("descr_compras"));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow("cat_compras"));
                double preco = cursor.getDouble(cursor.getColumnIndexOrThrow("preco_compras"));
                double quantidade = cursor.getDouble(cursor.getColumnIndexOrThrow("qnt_compras"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_compras"));
                String periodo = cursor.getString(cursor.getColumnIndexOrThrow("periodo_compras"));
                String obs = cursor.getString(cursor.getColumnIndexOrThrow("obs_compras"));

                compra = new Compra(id, bc, descr, cat, preco, quantidade, total, periodo, obs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return compra;
    }
}
