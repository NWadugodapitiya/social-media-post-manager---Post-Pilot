package com.postpilot.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity implements PostAdapter.OnPostListener {

    private RecyclerView rvPosts;
    private PostAdapter adapter;
    private List<Post> postList;
    private ImageView ivDelete, ivMenu;
    private TextView tvTitle;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        rvPosts = findViewById(R.id.rv_posts);
        ivDelete = findViewById(R.id.iv_delete);
        ivMenu = findViewById(R.id.iv_menu);
        tvTitle = findViewById(R.id.tv_title_main);

        // Initialize Demo Data
        postList = new ArrayList<>();
        postList.add(new Post("OCT 23, 2023", "Trip to the Mountains", "Exploring the hidden gems of the Rockies this summer with a...", "Instagram", "Draft"));
        postList.add(new Post("OCT 22, 2023", "Coffee Shop Vibes", "The best espresso in the city and why productivity spikes in...", "Twitter", "Scheduled"));
        postList.add(new Post("OCT 19, 2023", "Product Launch Teaser", "Something big is coming next week. Can you guess what it is...", "LinkedIn", "Published"));

        adapter = new PostAdapter(postList, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        FloatingActionButton fabCreatePost = findViewById(R.id.fab_create_post);
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        ivDelete.setOnClickListener(v -> {
            List<Post> selected = adapter.getSelectedPosts();
            if (!selected.isEmpty()) {
                adapter.removePosts(selected);
                Toast.makeText(this, selected.size() + " posts deleted", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            }
        });
    }

    @Override
    public void onPostLongClick(int position) {
        if (!isSelectionMode) {
            enterSelectionMode();
        }
        toggleSelection(position);
    }

    @Override
    public void onPostClick(int position) {
        if (isSelectionMode) {
            toggleSelection(position);
        } else {
            // Normal click - maybe open details
            Toast.makeText(this, "Opening " + postList.get(position).getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        adapter.setSelectionMode(true);
        ivDelete.setVisibility(View.VISIBLE);
        ivMenu.setVisibility(View.GONE);
        updateTitle();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        adapter.setSelectionMode(false);
        ivDelete.setVisibility(View.GONE);
        ivMenu.setVisibility(View.VISIBLE);
        tvTitle.setText(getString(R.string.my_posts));
        
        for (Post p : postList) p.setSelected(false);
        adapter.notifyDataSetChanged();
    }

    private void toggleSelection(int position) {
        Post post = postList.get(position);
        post.setSelected(!post.isSelected());
        adapter.notifyItemChanged(position);
        
        List<Post> selected = adapter.getSelectedPosts();
        if (selected.isEmpty()) {
            exitSelectionMode();
        } else {
            updateTitle();
        }
    }

    private void updateTitle() {
        int count = adapter.getSelectedPosts().size();
        tvTitle.setText(count + " Selected");
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}