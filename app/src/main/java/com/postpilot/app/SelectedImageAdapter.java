package com.postpilot.app;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.ViewHolder> {

    private List<Uri> imageList;
    private OnImageRemoveListener listener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public SelectedImageAdapter(List<Uri> imageList, OnImageRemoveListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivSelected.setImageURI(imageList.get(position));
        holder.ivRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(position);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSelected, ivRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSelected = itemView.findViewById(R.id.iv_selected);
            ivRemove = itemView.findViewById(R.id.iv_remove);
        }
    }
}