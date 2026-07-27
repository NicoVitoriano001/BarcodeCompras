package com.app.barcodecompras;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe que agrupa itens do Banco de Dados (bancodados_tab),
 * similar à CompraAgrupada usada em ResultComprasActivity.
 * Cada grupo contém um item do banco de dados e suas compras relacionadas.
 */
public class BancoDadosAgrupado {
    private String bcDB;
    private String descrDB;
    private String catDB;
    private int contagemOcorrencias;
    private List<Compra> comprasRelacionadas = new ArrayList<>();

    public BancoDadosAgrupado(String bcDB, String descrDB, String catDB, int contagemOcorrencias) {
        this.bcDB = bcDB;
        this.descrDB = descrDB;
        this.catDB = catDB;
        this.contagemOcorrencias = contagemOcorrencias;
    }

    public String getBcDB() { return bcDB; }
    public String getDescrDB() { return descrDB; }
    public String getCatDB() { return catDB; }
    public int getContagemOcorrencias() { return contagemOcorrencias; }
    public List<Compra> getComprasRelacionadas() { return comprasRelacionadas; }

    public void setComprasRelacionadas(List<Compra> compras) { this.comprasRelacionadas = compras; }
}
