package com.example.readapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class ReadActivity extends AppCompatActivity {
    private TextView tvChapterTitle, tvContent;
    private ScrollView svContent;
    private Button btnPrev, btnNext, btnEdit;
    private MyDBUtils dbHelper;
    private int currentChapterId, bookId, userId;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // 初始化组件
        initViews();

        // 获取用户信息
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        userId = prefs.getInt("Uid", -1);
        isAdmin = dbHelper.isAdmin(userId);
        // 编辑按钮（仅管理员可见）
        btnEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        // 获取章节ID
        currentChapterId = getIntent().getIntExtra("chapter_id", -1);

        // 加载数据
        loadChapterData();
        setupButtonStates();
    }

    private void initViews() {
        tvChapterTitle = findViewById(R.id.tv_chapter_title);
        tvContent = findViewById(R.id.tv_content);
        svContent = findViewById(R.id.sv_content);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnEdit = findViewById(R.id.btn_edit);
        dbHelper = new MyDBUtils(this);

        // 返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 上一章
        btnPrev.setOnClickListener(v -> navigateToAdjacentChapter(true));

        // 下一章
        btnNext.setOnClickListener(v -> navigateToAdjacentChapter(false));

        // 全部章节
        findViewById(R.id.btn_all_chapters).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChapterActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });

        // 编辑按钮
        btnEdit.setOnClickListener(v -> showEditDialog());
    }

    /** 加载章节数据 */
    private void loadChapterData() {
        new Thread(() -> {
            // 获取章节详情
            ChapterBean chapter = dbHelper.getChapterById(currentChapterId);
            bookId = chapter.getBookId();

            // 获取阅读进度
            float progress = dbHelper.getReadingProgress(userId, bookId, currentChapterId);

            runOnUiThread(() -> {
                // 更新UI
                tvChapterTitle.setText("第"+ chapter.getChapterNumber() + "章 " + chapter.getChapterTitle());
                tvContent.setText(chapter.getContent());

                // 恢复阅读位置
                svContent.post(() -> {
                    int scrollRange = svContent.getChildAt(0).getHeight() - svContent.getHeight();
                    svContent.scrollTo(0, (int) (scrollRange * progress));
                });
            });
        }).start();
    }

    /** 章节导航 */
    private void navigateToAdjacentChapter(boolean isPrevious) {
        new Thread(() -> {
            int targetChapterId = isPrevious ?
                    dbHelper.getPreviousChapterId(currentChapterId) :
                    dbHelper.getNextChapterId(currentChapterId);

            if (targetChapterId == -1) {
                runOnUiThread(() -> Toast.makeText(this,
                        isPrevious ? "已是第一章" : "已是最后一章",
                        Toast.LENGTH_SHORT).show());
                return;
            }

            if (targetChapterId != -1)
            {
                // 更新当前章节ID
                currentChapterId = targetChapterId;

                // 立即保存切换后的章节进度
                saveProgressImmediately();
                currentChapterId = targetChapterId;
                runOnUiThread(() -> {
                    loadChapterData();
                    setupButtonStates();
                });
            }

        }).start();
    }

    private void saveProgressImmediately() {
        int scrollY = svContent.getScrollY();
        int totalHeight = svContent.getChildAt(0).getHeight() - svContent.getHeight();
        float progress = totalHeight > 0 ? (float) scrollY / totalHeight : 0;

        new Thread(() ->
                dbHelper.updateReadingProgress(userId, bookId, currentChapterId, progress)
        ).start();
    }

    /** 更新按钮状态 */
    private void setupButtonStates() {
        new Thread(() -> {
            boolean hasPrev = dbHelper.getPreviousChapterId(currentChapterId) != -1;
            boolean hasNext = dbHelper.getNextChapterId(currentChapterId) != -1;

            runOnUiThread(() -> {
                btnPrev.setEnabled(hasPrev);
                btnNext.setEnabled(hasNext);
            });
        }).start();
    }

    /** 显示编辑对话框 */
    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_chapter, null);
        EditText etContent = view.findViewById(R.id.et_content);

        // 加载当前内容
        etContent.setText(tvContent.getText());

        builder.setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newContent = etContent.getText().toString();
                    updateChapterContent(newContent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 更新章节内容 */
    private void updateChapterContent(String newContent) {
        new Thread(() -> {
            try {
                // 1. 更新章节内容
                dbHelper.updateChapterContent(currentChapterId, newContent);

                // 2. 计算新字数并更新章节表
                int wordCount = BookParser.countWords(newContent);
                dbHelper.updateChapterWordCount(currentChapterId, wordCount);

                // 3. 更新书籍总字数
                dbHelper.updateBookWordCount(bookId);

                runOnUiThread(() -> {
                    tvContent.setText(newContent);
                    Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();

                    // 4. 刷新阅读位置
                    svContent.post(() -> svContent.scrollTo(0, 0));
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "修改失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                Log.e("ChapterUpdate", "更新失败", e);
            }
        }).start();
    }

    /** 保存阅读进度 */
    @Override
    protected void onPause() {
        super.onPause();
        new Thread(() -> {
            // 计算阅读进度
            int scrollY = svContent.getScrollY();
            int totalHeight = svContent.getChildAt(0).getHeight() - svContent.getHeight();
            float progress = totalHeight > 0 ? (float) scrollY / totalHeight : 0;

            // 更新数据库
            dbHelper.updateReadingProgress(userId, bookId, currentChapterId, progress);
        }).start();
    }
}