package com.app.barcodecompras.database;

public class BancoDados {

    private String bcIMDB;
    private String descrIMDB;
    private String catIMDB;
    private long updatedAt;

    // ✅ Construtor correto
    public BancoDados(String bcIMDB, String descrIMDB, String catIMDB, long updatedAt) {
        this.bcIMDB = bcIMDB;
        this.descrIMDB = descrIMDB;
        this.catIMDB = catIMDB;
        this.updatedAt = updatedAt;
    }

    // ✅ Getters
    public String getBcIMDB() {
        return bcIMDB;
    }

    public String getDescrIMDB() {
        return descrIMDB;
    }

    public String getCatIMDB() {
        return catIMDB;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
