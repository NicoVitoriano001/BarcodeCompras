package com.app.barcodecompras.firebase;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma operação pendente de sincronização.
 * Agora inclui a lista de itens específicos para exibição detalhada.
 */
public class SyncSummary {
    private String tableName;        // "compras_tab" ou "bancodados_tab"
    private String actionType;       // "CREATE", "UPDATE", "DELETE"
    private int quantity;
    private String description;      // Descrição amigável
    private List<String> itemDetails; // Lista de itens específicos

    public SyncSummary(String tableName, String actionType, int quantity, String description) {
        this(tableName, actionType, quantity, description, new ArrayList<String>());
    }

    public SyncSummary(String tableName, String actionType, int quantity, String description, List<String> itemDetails) {
        this.tableName = tableName;
        this.actionType = actionType;
        this.quantity = quantity;
        this.description = description;
        this.itemDetails = itemDetails != null ? itemDetails : new ArrayList<String>();
    }

    public String getTableName() { return tableName; }
    public String getActionType() { return actionType; }
    public int getQuantity() { return quantity; }
    public String getDescription() { return description; }
    public List<String> getItemDetails() { return itemDetails; }

    /**
     * Retorna uma string formatada com os itens, um por linha.
     */
    public String getFormattedItems() {
        if (itemDetails == null || itemDetails.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < itemDetails.size(); i++) {
            sb.append("  • ").append(itemDetails.get(i));
            if (i < itemDetails.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public String getTableDisplayName() {
        if ("compras_tab".equals(tableName)) return "Compras";
        if ("bancodados_tab".equals(tableName)) return "Banco de Dados";
        return tableName;
    }

    public String getActionDisplayName() {
        switch (actionType) {
            case "CREATE": return "Criar";
            case "UPDATE": return "Editar";
            case "DELETE": return "Excluir";
            default: return actionType;
        }
    }

    public String getActionIcon() {
        switch (actionType) {
            case "CREATE": return "➕";
            case "UPDATE": return "✏️";
            case "DELETE": return "🗑️";
            default: return "🔄";
        }
    }
}
