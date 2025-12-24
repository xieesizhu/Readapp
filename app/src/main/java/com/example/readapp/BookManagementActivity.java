package com.example.readapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 书籍管理主界面
 * 功能：
 * 1. 展示书籍列表
 * 2. 支持搜索书籍
 * 3. 跳转添加/编辑界面
 * 4. 删除书籍
 * 5. 切换推荐状态
 */
public class BookManagementActivity extends AppCompatActivity {

    // 请求码定义
    private static final int REQUEST_ADD_BOOK = 1001;
    private static final int REQUEST_EDIT_BOOK = 1002;

    // 视图组件
    private RecyclerView recyclerView;
    private EditText etSearch;
    private BookAdapter adapter;
    private MyDBUtils dbHelper;

    private static final int SEARCH_DELAY = 500;
    private Handler searchHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_management);

        // 初始化组件
        initViews();

        // 初始化数据库
        dbHelper = new MyDBUtils(this);

        // 首次加载数据
        loadBooks();
    }

    // region 初始化视图
    private void initViews() {
        recyclerView = findViewById(R.id.rv_books);
        etSearch = findViewById(R.id.et_search);

        // 设置列表布局
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 配置搜索功能
        setupSearchListener();

        // 添加书籍按钮
        findViewById(R.id.btn_add_book).setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, AddEditBookActivity.class),
                        REQUEST_ADD_BOOK
                )
        );
    }
    // endregion

    /** 设置实时搜索监听（带防抖） */
    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 移除之前的搜索任务（防抖）
                searchHandler.removeCallbacksAndMessages(null);

                // 延迟执行搜索
                searchHandler.postDelayed(() -> {
                    String keyword = s.toString().trim();
                    if (keyword.isEmpty()) {
                        loadBooks(); // 空输入时加载全部
                    } else {
                        performSearch(keyword);
                    }
                }, SEARCH_DELAY);
            }
        });
    }

    /** 执行搜索（优化版） */
    private void performSearch(String keyword) {
        new Thread(() -> {
            try {
                List<BookBean> result = dbHelper.searchBooks(keyword);
                runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.updateBooks(result);
                        if (result.isEmpty()) {
                            Toast.makeText(BookManagementActivity.this,
                                    "未找到相关书籍", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("Search", "搜索失败", e);
                runOnUiThread(() ->
                        Toast.makeText(BookManagementActivity.this,
                                "搜索出错，请稍后重试", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // region 数据加载
    /**
     * 加载书籍数据
     */
    private void loadBooks() {
        new Thread(() -> {
            List<BookBean> books = dbHelper.getAllBooks();
            runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new BookAdapter(books);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateBooks(books);
                }
            });
        }).start();
    }

    // endregion

    /**
     * 处理返回结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // 无论添加还是编辑成功都刷新列表
            loadBooks();
        }
    }
    // endregion

    // region 列表适配器
    /**
     * 书籍列表适配器
     */
    private class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
        private List<BookBean> books;

        public BookAdapter(List<BookBean> books) {
            this.books = books;
        }

        /**
         * 更新数据
         */
        public void updateBooks(List<BookBean> newBooks) {
            this.books = newBooks;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookBean book = books.get(position);

            // 绑定数据
            holder.bindData(book);

            // 设置点击事件
            setupClickListeners(holder, book, position);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        /**
         * 设置点击事件
         */
        private void setupClickListeners(ViewHolder holder, BookBean book, int position) {
            // 点击卡片进入章节管理
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(BookManagementActivity.this, ChapterActivity.class);
                intent.putExtra("book_id", book.getBookId());
                startActivity(intent);
            });

            // 点击编辑按钮进入编辑界面
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(BookManagementActivity.this, AddEditBookActivity.class);
                intent.putExtra("book_id", book.getBookId());
                startActivityForResult(intent, REQUEST_EDIT_BOOK);
            });

            // 删除书籍
            holder.btnDelete.setOnClickListener(v -> deleteBook(book, position));

            // 切换推荐状态
            holder.swRecommend.setOnCheckedChangeListener((buttonView, isChecked) ->
                    updateRecommendStatus(book, isChecked)
            );
        }

        /**
         * 删除书籍
         */
        private void deleteBook(BookBean book, int position) {
            new Thread(() -> {
                dbHelper.deleteBook(book.getBookId());
                runOnUiThread(() -> {
                    books.remove(position);
                    notifyItemRemoved(position);
                });
            }).start();
        }

        /**
         * 更新推荐状态
         */
        private void updateRecommendStatus(BookBean book, boolean isRecommended) {
            new Thread(() ->
                    dbHelper.updateRecommendStatus(book.getBookId(), isRecommended ? 1 : 0)
            ).start();
        }

        /**
         * 列表项视图持有者
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle, tvAuthor, tvCategory;
            Switch swRecommend;
            TextView btnDelete;
            TextView btnEdit;

            public ViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvAuthor = itemView.findViewById(R.id.tv_author);
                tvCategory = itemView.findViewById(R.id.tv_category);
                swRecommend = itemView.findViewById(R.id.sw_recommend);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                btnEdit = itemView.findViewById(R.id.btn_edit);
            }

            /**
             * 绑定数据到视图
             */
            public void bindData(BookBean book) {
                tvTitle.setText(book.getTitle());
                tvAuthor.setText("作者：" + book.getAuthor());
                tvCategory.setText("分类：" + dbHelper.getCategoryName(book.getCategoryId()));
                swRecommend.setChecked(book.getIs_Recommendation() == 1);

                // 加载封面图片
                if (book.getCoverPath() != null && !book.getCoverPath().isEmpty()) {
                    Glide.with(BookManagementActivity.this)
                            .load(new File(book.getCoverPath()))
                            .placeholder(R.drawable.ic_books)
                            .into(ivCover);
                }
            }
        }
    }
    // endregion
}