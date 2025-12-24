package com.example.readapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryActivity extends AppCompatActivity {
    private RecyclerView rvCategories;
    private TextView tvEmpty;
    private MyDBUtils dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // 初始化视图
        rvCategories = findViewById(R.id.rv_categories);
        tvEmpty = findViewById(R.id.tv_empty);
        dbHelper = new MyDBUtils(this);

        // 设置布局管理器
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        // 加载分类数据
        loadCategories();
    }

    /**
     * 加载分类数据
     */
    private void loadCategories() {
        new Thread(() -> {
            List<CategoryBean> categories = dbHelper.getAllCategoriesWithCount();

            runOnUiThread(() -> {
                if (categories.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvCategories.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvCategories.setVisibility(View.VISIBLE);
                    CategoryAdapter adapter = new CategoryAdapter(categories);
                    rvCategories.setAdapter(adapter);
                }
            });
        }).start();
    }

    /**
     * 分类适配器
     */
    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<CategoryBean> categories;

        CategoryAdapter(List<CategoryBean> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryBean category = categories.get(position);

            holder.tvCategoryName.setText(category.getCategoryName());
            holder.tvBookCount.setText(String.format("%d本", category.getBookCount()));

            // 点击跳转到分类书籍列表
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(CategoryActivity.this, BookListActivity.class);
                intent.putExtra("category_id", category.getCategoryId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName, tvBookCount;

            ViewHolder(View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tv_category_name);
                tvBookCount = itemView.findViewById(R.id.tv_book_count);
            }
        }
    }
}