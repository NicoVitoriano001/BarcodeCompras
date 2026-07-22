package com.app.barcodecompras.database;
public class BancoDados {
    private long id;
    private String bc;
    private String descr;
    private String cat;
    private long updatedAt;
    private int contagemOcorrencias;

    public BancoDados(long id, String bc, String descr, String cat, long updatedAt) {
        this.id = id;
        this.bc = bc;
        this.descr = descr;
        this.cat = cat;
        this.updatedAt = updatedAt;
        this.contagemOcorrencias = 0;
    }

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

    public int getContagemOcorrencias() {
        return contagemOcorrencias;
    }

    public void setContagemOcorrencias(int contagemOcorrencias) {
        this.contagemOcorrencias = contagemOcorrencias;
    }
}