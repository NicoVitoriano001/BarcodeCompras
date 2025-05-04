package com.app.barcodecompras;
//modelo Compra.java
public class Compra {
    private long id;
    private String bcCompras, descrCompras, catCompras, periodoCompras, obsCompras;
    private double precoCompras, totalCompras, qntCompras;

    // Construtor
    public Compra(long id, String bcCompras, String descrCompras, String catCompras,
                  double precoCompras, double qntCompras, double totalCompras,
                  String periodoCompras, String obsCompras) {
        this.id = id;
        this.bcCompras = bcCompras;
        this.descrCompras = descrCompras;
        this.catCompras = catCompras;
        this.precoCompras = precoCompras;
        this.qntCompras = qntCompras;
        this.totalCompras = totalCompras;
        this.periodoCompras = periodoCompras;
        this.obsCompras = obsCompras;
    }

    // Getters
    public long getId() { return id; }
    public String getBcCompras() { return bcCompras; }
    public String getDescrCompras() { return descrCompras; }
    public String getCatCompras() { return catCompras; }
    public double getPrecoCompras() { return precoCompras; }
    public double getQntCompras() { return qntCompras; }
    public double getTotalCompras() { return totalCompras; }
    public String getPeriodoCompras() { return periodoCompras; }
    public String getObsCompras() { return obsCompras; }
}