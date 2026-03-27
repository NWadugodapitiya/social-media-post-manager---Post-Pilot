package com.postpilot.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardActivity extends AppCompatActivity implements PostAdapter.OnPostListener {

    private RecyclerView rvPosts;
    private PostAdapter adapter;
    private List<Post> postList;
    private List<Post> filteredList;
    private ImageView ivDelete, ivMenu;
    private TextView tvTitle;
    private EditText etSearch;
    private TextView tvFilterAll, tvFilterDrafts, tvFilterScheduled, tvFilterPublished;
    private boolean isSelectionMode = false;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvPosts = findViewById(R.id.rv_posts);
        ivDelete = findViewById(R.id.iv_delete);
        ivMenu = findViewById(R.id.iv_menu);
        tvTitle = findViewById(R.id.tv_title_main);
        etSearch = findViewById(R.id.et_search);

        tvFilterAll = findViewById(R.id.tv_filter_all);
        tvFilterDrafts = findViewById(R.id.tv_filter_drafts);
        tvFilterScheduled = findViewById(R.id.tv_filter_scheduled);
        tvFilterPublished = findViewById(R.id.tv_filter_published);

        // Initialize Demo Data
        postList = new ArrayList<>();
        postList.add(new Post("OCT 23, 2023", "Trip to the Mountains", "Exploring the hidden gems of the Rockies this summer with amazing views and adventures.", "Instagram", "Draft"));
        postList.add(new Post("OCT 22, 2023", "Coffee Shop Vibes", "The best espresso in the city and why productivity spikes in cozy cafes.", "Twitter", "Published"));
        postList.add(new Post("OCT 19, 2023", "Product Launch Teaser", "Something big is coming next week. Stay tuned for the official reveal!", "LinkedIn", "Scheduled"));
        postList.add(new Post("OCT 18, 2023", "Workout Motivation", "Push your limits today so you can be stronger tomorrow. No excuses!", "Instagram", "Published"));
        postList.add(new Post("OCT 17, 2023", "Travel Essentials", "Top 10 must-have items for your next adventure trip.", "Facebook", "Draft"));
        
        filteredList = new ArrayList<>(postList);
        adapter = new PostAdapter(filteredList, this);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        setupSearch();
        setupFilters();

        FloatingActionButton fabCreatePost = findViewById(R.id.fab_create_post);
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        ivDelete.setOnClickListener(v -> {
            List<Post> selected = adapter.getSelectedPosts();
            if (!selected.isEmpty()) {
                postList.removeAll(selected);
                applyFilterAndSearch();
                Toast.makeText(this, selected.size() + " posts deleted", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        tvFilterAll.setOnClickListener(v -> updateFilter("All"));
        tvFilterDrafts.setOnClickListener(v -> updateFilter("Draft"));
        tvFilterScheduled.setOnClickListener(v -> updateFilter("Scheduled"));
        tvFilterPublished.setOnClickListener(v -> updateFilter("Published"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        
        // Update UI
        tvFilterAll.setBackgroundResource(filter.equals("All") ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tvFilterAll.setTextColor(filter.equals("All") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_desc));
        
        tvFilterDrafts.setBackgroundResource(filter.equals("Draft") ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tvFilterDrafts.setTextColor(filter.equals("Draft") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_desc));

        tvFilterScheduled.setBackgroundResource(filter.equals("Scheduled") ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tvFilterScheduled.setTextColor(filter.equals("Scheduled") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_desc));

        tvFilterPublished.setBackgroundResource(filter.equals("Published") ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tvFilterPublished.setTextColor(filter.equals("Published") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_desc));

        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        String query = etSearch.getText().toString().toLowerCase();
        
        filteredList.clear();
        for (Post post : postList) {
            boolean matchesFilter = currentFilter.equals("All") || post.getStatus().equals(currentFilter);
            boolean matchesSearch = post.getTitle().toLowerCase().contains(query) || post.getDescription().toLowerCase().contains(query);
            
            if (matchesFilter && matchesSearch) {
                filteredList.add(post);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPostLongClick(int position) {
        if (!isSelectionMode) enterSelectionMode();
        toggleSelection(position);
    }

    @Override
    public void onPostClick(int position) {
        if (isSelectionMode) toggleSelection(position);
        else Toast.makeText(this, "Opening " + filteredList.get(position).getTitle(), Toast.LENGTH_SHORT).show();
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
        Post post = filteredList.get(position);
        post.setSelected(!post.isSelected());
        adapter.notifyItemChanged(position);
        List<Post> selected = adapter.getSelectedPosts();
        if (selected.isEmpty()) exitSelectionMode();
        else updateTitle();
    }

    private void updateTitle() {
        int count = adapter.getSelectedPosts().size();
        tvTitle.setText(count + " Selected");
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) exitSelectionMode();
        else super.onBackPressed();
    }
}