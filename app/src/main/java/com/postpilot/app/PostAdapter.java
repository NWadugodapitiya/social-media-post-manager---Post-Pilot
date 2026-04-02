package com.postpilot.app;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> posts;
    private OnPostListener listener;
    private boolean isSelectionMode = false;

    public interface OnPostListener {
        void onPostLongClick(int position);
        void onPostClick(int position);
    }

    public PostAdapter(List<Post> posts, OnPostListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.tvDate.setText(post.getDate());
        holder.tvTitle.setText(post.getTitle());
        holder.tvDesc.setText(post.getDescription());
        holder.tvPlatform.setText(post.getPlatform());
        holder.tvStatus.setText(post.getStatus());

        if ("Published".equals(post.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else if ("Scheduled".equals(post.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFA000"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#7A8599"));
        }

        // Load thumbnail if available
        String images = post.getImages();
        if (images != null && !images.isEmpty()) {
            String[] parts = images.split(",");
            if (parts.length > 0) {
                try {
                    Uri uri = Uri.parse(parts[0]);
                    if ("file".equals(uri.getScheme())) {
                        holder.ivThumbnail.setImageBitmap(BitmapFactory.decodeFile(uri.getPath()));
                    } else {
                        holder.ivThumbnail.setImageURI(uri);
                    }
                } catch (Exception e) {
                    holder.ivThumbnail.setImageResource(R.drawable.welcome_img);
                }
            }
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.welcome_img);
        }

        holder.checkBox.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(post.isSelected());
        holder.tvEdit.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);

        if (post.isSelected()) {
            holder.itemLayout.setBackgroundColor(Color.parseColor("#E0E9FF"));
        } else {
            holder.itemLayout.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.tvEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EditPostActivity.class);
            intent.putExtra("post_id", post.getId());
            intent.putExtra("post_title", post.getTitle());
            intent.putExtra("post_desc", post.getDescription());
            intent.putExtra("post_images", post.getImages());
            intent.putExtra("post_date", post.getDate());
            v.getContext().startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPostClick(position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPostLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void removePosts(List<Post> postsToRemove) {
        posts.removeAll(postsToRemove);
        notifyDataSetChanged();
    }

    public List<Post> getSelectedPosts() {
        List<Post> selected = new ArrayList<>();
        for (Post p : posts) {
            if (p.isSelected()) selected.add(p);
        }
        return selected;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvDesc, tvPlatform, tvStatus, tvEdit;
        CheckBox checkBox;
        LinearLayout itemLayout;
        ImageView ivThumbnail;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvPlatform = itemView.findViewById(R.id.tv_platform);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvEdit = itemView.findViewById(R.id.tv_edit);
            checkBox = itemView.findViewById(R.id.checkbox);
            itemLayout = itemView.findViewById(R.id.item_layout);
            ivThumbnail = itemView.findViewById(R.id.iv_post_thumbnail);
        }
    }
}