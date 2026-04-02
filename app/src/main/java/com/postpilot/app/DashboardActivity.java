package com.postpilot.app;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        TextView tvGreeting = findViewById(R.id.tv_greeting);
        SharedPreferences sharedPreferences = getSharedPreferences("PostPilotPrefs", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "Nirmal");
        tvGreeting.setText("Hello, " + userName);

        dbHelper = new DatabaseHelper(this);

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

        loadPostsFromDatabase();

        setupSearch();
        setupFilters();

        FloatingActionButton fabCreatePost = findViewById(R.id.fab_create_post);
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        ivDelete.setOnClickListener(v -> {
            List<Post> selected = adapter.getSelectedPosts();
            if (!selected.isEmpty()) {
                for (Post p : selected) {
                    dbHelper.deletePost(p.getId());
                }
                loadPostsFromDatabase();
                Toast.makeText(this, selected.size() + " posts deleted", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPostsFromDatabase();
        
        // Update greeting in case name was changed in Profile
        TextView tvGreeting = findViewById(R.id.tv_greeting);
        SharedPreferences sharedPreferences = getSharedPreferences("PostPilotPrefs", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "Nirmal");
        tvGreeting.setText("Hello, " + userName);
    }

    private void loadPostsFromDatabase() {
        postList = dbHelper.getAllPosts();
        applyFilterAndSearch();
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
        if (filteredList == null) filteredList = new ArrayList<>();
        filteredList.clear();
        for (Post post : postList) {
            boolean matchesFilter = currentFilter.equals("All") || post.getStatus().equals(currentFilter);
            boolean matchesSearch = post.getTitle().toLowerCase().contains(query) || post.getDescription().toLowerCase().contains(query);
            if (matchesFilter && matchesSearch) filteredList.add(post);
        }
        if (adapter == null) {
            adapter = new PostAdapter(filteredList, this);
            rvPosts.setLayoutManager(new LinearLayoutManager(this));
            rvPosts.setAdapter(adapter);
        } else adapter.notifyDataSetChanged();
    }

    @Override
    public void onPostLongClick(int position) {
        if (!isSelectionMode) enterSelectionMode();
        toggleSelection(position);
    }

    @Override
    public void onPostClick(int position) {
        if (isSelectionMode) toggleSelection(position);
        else {
            Post post = filteredList.get(position);
            Intent intent = new Intent(this, PostPreviewActivity.class);
            intent.putExtra("post_id", post.getId());
            intent.putExtra("post_title", post.getTitle());
            intent.putExtra("post_desc", post.getDescription());
            intent.putExtra("post_images", post.getImages());
            intent.putExtra("post_date", post.getDate());
            startActivity(intent);
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