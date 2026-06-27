package com.app.barcodecompras.database;
public class CompraFirebase {
    // Nomes EXATAMENTE iguais aos do Firebase
    public String bc;
    public String descricao;

    public String categoria;
    public double preco;

    public double quantidade;
    public double total;
    public String periodo;
    public String obs;
    public long updateAt;  // Atenção: é "updateAt" e não "updatedAt"
    public boolean deleted;

    // Construtor vazio necessário para Firebase
    public CompraFirebase() {
    }

    public CompraFirebase(String bc, String descricao, String categoria,
                          double preco, double quantidade, double total,
                          String periodo, String obs, long updateAt) {
        this.bc = bc;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidade = quantidade;
        this.total = total;
        this.periodo = periodo;
        this.obs = obs;
        this.updateAt = updateAt;
        this.deleted = false;
    }
}