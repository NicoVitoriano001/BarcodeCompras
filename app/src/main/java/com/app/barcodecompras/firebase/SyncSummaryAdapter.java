package com.app.barcodecompras.firebase;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.barcodecompras.R;

import java.util.List;

public class SyncSummaryAdapter extends RecyclerView.Adapter<SyncSummaryAdapter.SyncViewHolder> {

    private List<SyncSummary> syncList;

    public SyncSummaryAdapter(List<SyncSummary> syncList) {
        this.syncList = syncList;
    }

    @NonNull
    @Override
    public SyncViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sync_summary, parent, false);
        return new SyncViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SyncViewHolder holder, int position) {
        SyncSummary item = syncList.get(position);

        holder.tvActionIcon.setText(item.getActionIcon());
        holder.tvActionTable.setText(item.getTableDisplayName());
        holder.tvActionDesc.setText(item.getActionDisplayName() + " - " + item.getDescription());
        holder.tvActionQuantity.setText(String.valueOf(item.getQuantity()));

        // Mostrar itens específicos se houver
        String formattedItems = item.getFormattedItems();
        if (!formattedItems.isEmpty()) {
            holder.tvSyncItems.setText(formattedItems);
            holder.tvSyncItems.setVisibility(View.VISIBLE);
        } else {
            holder.tvSyncItems.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return syncList != null ? syncList.size() : 0;
    }

    static class SyncViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionIcon, tvActionTable, tvActionDesc, tvActionQuantity, tvSyncItems;

        public SyncViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionIcon = itemView.findViewById(R.id.tvActionIcon);
            tvActionTable = itemView.findViewById(R.id.tvActionTable);
            tvActionDesc = itemView.findViewById(R.id.tvActionDesc);
            tvActionQuantity = itemView.findViewById(R.id.tvActionQuantity);
            tvSyncItems = itemView.findViewById(R.id.tvSyncItems);
        }
    }
}
