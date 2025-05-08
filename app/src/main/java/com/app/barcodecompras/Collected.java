package com.app.barcodecompras;

public class Collected {
    private String bcIMDB;
    private String descrIMDB;
    private String catIMDB;

    // Construtor
    public Collected(String bcIMDB, String descrIMDB, String catIMDB) {
        this.bcIMDB = bcIMDB;
        this.descrIMDB = descrIMDB;
        this.catIMDB = catIMDB;
    }

    // Getters
    public String getBcIMDB() { return bcIMDB; }
    public String getDescrIMDB() { return descrIMDB; }
    public String getCatIMDB() { return catIMDB; }
}