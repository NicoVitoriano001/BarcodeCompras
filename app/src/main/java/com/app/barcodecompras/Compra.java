package com.app.barcodecompras;

public class Compra {
    private long id;
    private String bcCompras, descrCompras, catCompras, periodoCompras, obsCompras;
    private double precoCompras, totalCompras, qntCompras;
    private long updateAt;
    private int contagemOcorrencias;

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
        this.contagemOcorrencias = 0; // Inicializa com 0
    }

    public long getId() { return id; }
    public String getBcCompras() { return bcCompras; }
    public String getDescrCompras() { return descrCompras; }
    public String getCatCompras() { return catCompras; }
    public double getPrecoCompras() { return precoCompras; }
    public double getQntCompras() { return qntCompras; }
    public double getTotalCompras() { return totalCompras; }
    public String getPeriodoCompras() { return periodoCompras; }
    public String getObsCompras() { return obsCompras; }
    public long getUpdateAt() { return updateAt; }
    public void setUpdateAt(long updateAt) { this.updateAt = updateAt; }

//    public String getBc() { return bcCompras; }
    public String getDescricao() { return descrCompras; }
    public String getCategoria() { return catCompras; }
 //   public double getPreco() { return precoCompras; }
    public double getQuantidade() { return qntCompras; }
 //   public double getTotal() { return totalCompras; }
    public String getPeriodo() { return periodoCompras; }
//    public String getObs() { return obsCompras; }

    // NOVO GETTER E SETTER
    public int getContagemOcorrencias() {return contagemOcorrencias; }

    public void setContagemOcorrencias(int contagemOcorrencias) {
        this.contagemOcorrencias = contagemOcorrencias;
    }
}