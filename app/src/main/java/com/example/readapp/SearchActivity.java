package com.example.readapp;

import android.content.Intent;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText etSearch;
    private RecyclerView rvResults;
    private TextView tvEmpty;
    private MyDBUtils dbHelper;
    private Handler searchHandler = new Handler();

    // 防抖延迟时间（毫秒）
    private static final int SEARCH_DELAY = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 初始化视图
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);
        tvEmpty = findViewById(R.id.tv_empty);
        dbHelper = new MyDBUtils(this);

        // 设置列表布局
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // 设置搜索监听
        setupSearchListener();

        // 初始加载随机书籍
        performSearch("");
    }





    @Override
    protected void onResume() {
        super.onResume();
        // 当返回页面时如果搜索框为空，刷新随机列表
        if (etSearch.getText().toString().isEmpty()) {
            performSearch("");
        }
    }

    /**
     * 设置搜索监听（带防抖）
     */
    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 移除之前的搜索任务
                searchHandler.removeCallbacksAndMessages(null);
                // 延迟执行搜索
                searchHandler.postDelayed(() -> performSearch(s.toString()), SEARCH_DELAY);
            }
        });
    }

    /**
     * 执行搜索
     */
    private void performSearch(String keyword) {
        new Thread(() -> {
            List<BookBean> results;

            if (keyword.isEmpty()) {
                // 当关键词为空时获取随机书籍
                results = dbHelper.getRandomBooks(10);
            } else {
                // 否则执行正常搜索
                results = dbHelper.searchBooks(keyword);
            }

            runOnUiThread(() -> {
                if (results.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvResults.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvResults.setVisibility(View.VISIBLE);
                    SearchAdapter adapter = new SearchAdapter(results);
                    rvResults.setAdapter(adapter);
                }
            });
        }).start();
    }

    /**
     * 搜索结果适配器
     */
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<BookBean> books;

        SearchAdapter(List<BookBean> books) {
            this.books = books;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookBean book = books.get(position);

            holder.tvTitle.setText(book.getTitle());
            holder.tvAuthor.setText("作者：" + book.getAuthor());
            holder.tvDescription.setText(book.getDescription());

            // 加载封面图片
            Glide.with(SearchActivity.this)
                    .load(new File(book.getCoverPath()))
                    .placeholder(R.drawable.ic_books)
                    .into(holder.ivCover);

            // 点击跳转详情
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SearchActivity.this, BookDetailActivity.class);
                intent.putExtra("book_id", book.getBookId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle, tvAuthor, tvDescription;

            ViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvAuthor = itemView.findViewById(R.id.tv_author);
                tvDescription = itemView.findViewById(R.id.tv_description);
            }
        }
    }
}