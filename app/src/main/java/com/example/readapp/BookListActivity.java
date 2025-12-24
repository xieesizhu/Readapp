package com.example.readapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BookListActivity extends AppCompatActivity {
    private RecyclerView rvBooks;
    private TextView tvTitle, tvEmpty;
    private BookAdapter adapter;
    private MyDBUtils dbHelper;

    // 接收参数常量
    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_LIST_TITLE = "list_title";
    public static final String EXTRA_IS_RECOMMEND = "is_recommend";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        // 初始化视图
        rvBooks = findViewById(R.id.rv_books);
        tvTitle = findViewById(R.id.tv_title);
        tvEmpty = findViewById(R.id.tv_empty);
        dbHelper = new MyDBUtils(this);

        // 设置布局
        rvBooks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(new ArrayList<>());
        rvBooks.setAdapter(adapter);

        // 处理传入参数
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // 设置标题
        String title = intent.getStringExtra(EXTRA_LIST_TITLE);
        tvTitle.setText(title != null ? title : "书籍列表");

        // 根据参数类型加载数据
        new Thread(() -> {
            List<BookBean> books = new ArrayList<>();

            if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
                int categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, 0);
                books = dbHelper.getBooksByCategory(categoryId);
                tvTitle.setText("书籍列表("+ dbHelper.getCategoryName(categoryId)+")");
            } else if (intent.getBooleanExtra(EXTRA_IS_RECOMMEND, false)) {
                books = dbHelper.getRecommendBooks();
            } else {
                books = dbHelper.getRandomBooks(10);
            }

            List<BookBean> finalBooks = books;
            runOnUiThread(() -> updateUI(finalBooks));
        }).start();
    }

    private void updateUI(List<BookBean> books) {
        if (books.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvBooks.setVisibility(View.VISIBLE);
            adapter.updateData(books);
        }
    }

    // 书籍列表适配器
    class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
        private List<BookBean> books;

        BookAdapter(List<BookBean> books) {
            this.books = books;
        }

        void updateData(List<BookBean> newBooks) {
            this.books = newBooks;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookBean book = books.get(position);

            holder.tvTitle.setText(book.getTitle());
            holder.tvAuthor.setText("作者：" + book.getAuthor());
            holder.tvDescription.setText(book.getDescription());

            // 加载封面（本地文件）
            Glide.with(BookListActivity.this)
                    .load(new File(book.getCoverPath()))
                    .placeholder(R.drawable.ic_books)
                    .into(holder.ivCover);

            // 点击事件
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BookListActivity.this, BookDetailActivity.class);
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
