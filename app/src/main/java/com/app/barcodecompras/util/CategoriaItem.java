package com.app.barcodecompras.util;

public class CategoriaItem {
    public String nome;
    public int quantidade;

    public CategoriaItem(String nome, int quantidade) {
        this.nome = nome;
        this.quantidade = quantidade;
    }

    @Override
    public String toString() {

        return nome + " (" + quantidade + ")";
    }

}