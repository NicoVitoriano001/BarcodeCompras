package com.app.barcodecompras;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CollectedAdapter extends RecyclerView.Adapter<CollectedAdapter.CollectedViewHolder> {
    private final List<BuscarCollectedActivity.CollectedItem> collectedItems;

    public CollectedAdapter(List<BuscarCollectedActivity.CollectedItem> collectedItems) {
        this.collectedItems = collectedItems;
    }

    @NonNull
    @Override
    public CollectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collected, parent, false);
        return new CollectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectedViewHolder holder, int position) {
        BuscarCollectedActivity.CollectedItem item = collectedItems.get(position);
        holder.tvBarcode.setText(item.getBarcode());
        holder.tvDescription.setText(item.getDescription());
        holder.tvCategory.setText(item.getCategory());
    }

    @Override
    public int getItemCount() {
        return collectedItems.size();
    }

    static class CollectedViewHolder extends RecyclerView.ViewHolder {
        TextView tvBarcode, tvDescription, tvCategory;

        public CollectedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBarcode = itemView.findViewById(R.id.tvBarcode);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}