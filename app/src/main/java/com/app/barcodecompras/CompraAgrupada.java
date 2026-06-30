package com.app.barcodecompras;

import java.util.ArrayList;
import java.util.List;

public class CompraAgrupada {
    private String bcCompras;
    private String descrCompras;
    private String catCompras;
    private String periodoCompras;
    private String obsCompras;
    private int contagemOcorrencias;
    private List<Compra> compras = new ArrayList<>();

    public CompraAgrupada(String bcCompras, String descrCompras, String catCompras,
                          String periodoCompras, String obsCompras, int contagemOcorrencias) {
        this.bcCompras = bcCompras;
        this.descrCompras = descrCompras;
        this.catCompras = catCompras;
        this.periodoCompras = periodoCompras;
        this.obsCompras = obsCompras;
        this.contagemOcorrencias = contagemOcorrencias;
    }

    public String getBcCompras() { return bcCompras; }
    public String getDescrCompras() { return descrCompras; }
    public String getCatCompras() { return catCompras; }
    public String getPeriodoCompras() { return periodoCompras; }
    public String getObsCompras() { return obsCompras; }
    public int getContagemOcorrencias() { return contagemOcorrencias; }

    public List<Compra> getCompras() { return compras; }

    public void setCompras(List<Compra> compras) { this.compras = compras; }
}