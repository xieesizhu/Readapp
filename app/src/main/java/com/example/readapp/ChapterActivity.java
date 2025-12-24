package com.example.readapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChapterActivity extends AppCompatActivity {
    private RecyclerView rvChapters;
    private Button btnAddChapter;
    private List<ChapterBean> chapterList = new ArrayList<>();
    private MyDBUtils dbHelper;
    private int bookId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);

        // 初始化数据库帮助类
        dbHelper = new MyDBUtils(this);

        // 初始化视图组件
        rvChapters = findViewById(R.id.rv_chapters);
        btnAddChapter = findViewById(R.id.btn_add_chapter);
        rvChapters.setLayoutManager(new LinearLayoutManager(this));

        // 获取书籍ID
        bookId = getIntent().getIntExtra("book_id", -1);

        // 检查管理员状态
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        int userId = prefs.getInt("Uid", -1);
        isAdmin = dbHelper.isAdmin(userId);

        // 管理员显示添加按钮
        if (isAdmin) {
            btnAddChapter.setVisibility(View.VISIBLE);
            btnAddChapter.setOnClickListener(v -> showAddChapterDialog());
        }

        loadChapters();
    }

    /**
     * 加载章节数据
     */
    private void loadChapters() {
        new Thread(() -> {
            chapterList = dbHelper.getChaptersByBook(bookId);
            runOnUiThread(() -> rvChapters.setAdapter(new ChapterAdapter()));
        }).start();
    }

    /**
     * 显示添加章节对话框
     */
    private void showAddChapterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_chapter, null);

        EditText etNumber = dialogView.findViewById(R.id.et_chapter_num);
        EditText etTitle = dialogView.findViewById(R.id.et_chapter_title);
        EditText etContent = dialogView.findViewById(R.id.et_content);

        builder.setView(dialogView)
                .setPositiveButton("添加", (dialog, which) -> {
                    ChapterBean newChapter = new ChapterBean(
                            Integer.parseInt(etNumber.getText().toString()),
                            etTitle.getText().toString(),
                            etContent.getText().toString()
                    );
                    newChapter.setBookId(bookId);
                    new Thread(() -> {
                        dbHelper.insertChapter(newChapter);
                        loadChapters(); // 刷新列表
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 章节列表适配器
    class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvChapterNum, tvChapterTitle;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChapterNum = itemView.findViewById(R.id.tv_chapter_num);
                tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chapter, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChapterBean chapter = chapterList.get(position);
            holder.tvChapterNum.setText("第" + chapter.getChapterNumber() + "章");
            holder.tvChapterTitle.setText(chapter.getChapterTitle());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChapterActivity.this, ReadActivity.class);
                intent.putExtra("chapter_id", chapter.getChapterId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return chapterList.size();
        }
    }
}