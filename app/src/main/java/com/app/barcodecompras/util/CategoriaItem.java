package com.app.barcodecompras.util;

public class CategoriaItem {
    public String nome;
    public int quantidade;
    public int quantidadeCompras;

    public CategoriaItem(String nome, int quantidade) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.quantidadeCompras = 0;
    }

    public CategoriaItem(String nome, int quantidade, int quantidadeCompras) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.quantidadeCompras = quantidadeCompras;
    }

    @Override
    public String toString() {
        return nome + " (" + quantidade + ") [" + quantidadeCompras + "]";
    }

}