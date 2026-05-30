package com.app.barcodecompras.firebase;

public class CompraFirebase {

    public String bc;
    public String descricao;
    public String categoria;
    public String periodo;
    public String obs;

    public double preco;
    public double quantidade;
    public double total;

    public long updatedAt;

    public CompraFirebase() {}

    public CompraFirebase(String bc, String descricao, String categoria,
                          double preco, double quantidade, double total,
                          String periodo, String obs, long updatedAt) {

        this.bc = bc;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidade = quantidade;
        this.total = total;
        this.periodo = periodo;
        this.obs = obs;
        this.updatedAt = updatedAt;
    }
}