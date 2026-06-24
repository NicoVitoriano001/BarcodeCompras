package com.app.barcodecompras.database;

public class BancoDados {

    private long id;
    private String bc;
    private String descr;
    private String cat;
    private long updatedAt;

    public BancoDados(long id, String bc, String descr, String cat, long updatedAt) {
        this.id = id;
        this.bc = bc;
        this.descr = descr;
        this.cat = cat;
        this.updatedAt = updatedAt;
    }

    // Getters corretos
    public long getId() {
        return id;
    }

    public String getBcIMDB() {
        return bc;
    }

    public String getDescrIMDB() {
        return descr;
    }

    public String getCatIMDB() {
        return cat;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}