package com.example.readapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BookDetailActivity extends AppCompatActivity {
    private ImageView ivCover;
    private TextView tvTitle, tvAuthor, tvCategory, tvDescription;
    private RatingBar ratingBar;
    private Button btnAddShelf;
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private TextView tvChapterCount;
    private Button btnContinue;
    private int totalChapters = 0;
    private int lastReadChapterId = -1;
    private Button btnPostComment;
    private MyDBUtils dbHelper;
    private int bookId;
    private boolean isInShelf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // 初始化视图
        initViews();
        dbHelper = new MyDBUtils(this);

        // 获取书籍ID
        bookId = getIntent().getIntExtra("book_id", -1);

        // 加载数据
        loadBookDetail();
        checkBookshelfStatus();
        // 检查登录状态
        checkLoginStatus();
        // 检查阅读
        checkReadingProgress();
        loadComments();

        if (bookId != -1) {
            dbHelper.incrementViewCount(bookId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新检查阅读记录
        checkReadingProgress();
        // 刷新章节总数
        new Thread(() -> {
            int newChapterCount = dbHelper.getChapterCount(bookId);
            runOnUiThread(() ->
                    tvChapterCount.setText("全书共" + newChapterCount + "章")
            );
        }).start();
    }

    private void initViews() {
        ivCover = findViewById(R.id.iv_cover);
        tvTitle = findViewById(R.id.tv_title);
        tvAuthor = findViewById(R.id.tv_author);
        tvCategory = findViewById(R.id.tv_category);
        tvDescription = findViewById(R.id.tv_description);
        btnAddShelf = findViewById(R.id.btn_add_shelf);
        // 初始化评论组件
        rvComments = findViewById(R.id.rv_comments);
        btnPostComment = findViewById(R.id.btn_post_comment);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        tvChapterCount = findViewById(R.id.tv_chapter_count);
        btnContinue = findViewById(R.id.btn_continue);

        // 检查登录状态
        checkLoginStatus();
        loadComments();

        // 设置按钮点击事件
        btnAddShelf.setOnClickListener(v -> toggleBookshelfStatus());
        findViewById(R.id.btn_read).setOnClickListener(v -> startReading(false));
        btnContinue.setOnClickListener(v -> startReading(true));
        // 设置章节数点击事件
        tvChapterCount.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChapterActivity.class);
            intent.putExtra("book_id", bookId);
            startActivity(intent);
        });
    }

    /**
     * 加载书籍详情
     */
    private void loadBookDetail() {
        new Thread(() -> {
            BookBean book = dbHelper.getBookById(bookId);
            totalChapters = dbHelper.getChapterCount(bookId);
            runOnUiThread(() -> {
                Glide.with(this)
                        .load(new File(book.getCoverPath()))
                        .into(ivCover);
                tvTitle.setText(book.getTitle());
                tvAuthor.setText("作者：" + book.getAuthor());
                tvCategory.setText("分类：" + dbHelper.getCategoryName(book.getCategoryId()));
                tvChapterCount.setText("全书共" + totalChapters + "章");
                tvDescription.setText(book.getDescription());
            });
        }).start();
    }

    /**
     * 检查是否已在书架
     */
    private void checkBookshelfStatus() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        int userId = prefs.getInt("Uid", -1);

        new Thread(() -> {
            isInShelf = dbHelper.isBookInShelf(userId, bookId);
            runOnUiThread(() ->
                    btnAddShelf.setText(isInShelf ? "移除书架" : "加入书架")
            );
        }).start();
    }

    //阅读记录检查
    private void checkReadingProgress() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        int userId = prefs.getInt("Uid", -1);

        if (userId == -1) {
            return;
        }

        new Thread(() -> {
            try {
                lastReadChapterId = dbHelper.getLastReadChapter(userId, bookId);
                runOnUiThread(() -> {
                    if (lastReadChapterId != -1) {
                        findViewById(R.id.btn_read).setVisibility(View.GONE);
                        btnContinue.setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.btn_read).setVisibility(View.VISIBLE);
                        btnContinue.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e("ReadingProgress", "检查阅读进度失败", e);
            }
        }).start();
    }

    /**
     * 切换书架状态
     */
    private void toggleBookshelfStatus() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        int userId = prefs.getInt("Uid", -1);

        new Thread(() -> {
            if (isInShelf) {
                dbHelper.removeFromShelf(userId, bookId);
            } else {
                dbHelper.addToShelf(userId, bookId);
            }
            runOnUiThread(() -> {
                isInShelf = !isInShelf;
                btnAddShelf.setText(isInShelf ? "移除书架" : "加入书架");
                Toast.makeText(this,
                        isInShelf ? "已加入书架" : "已移除书架",
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * 开始阅读
     */
    private void startReading(boolean continueReading) {
        new Thread(() -> {
            int targetChapterId = -1;

            if (continueReading) {
                targetChapterId = lastReadChapterId;
            } else {
                List<ChapterBean> chapters = dbHelper.getChaptersByBook(bookId);
                if (!chapters.isEmpty()) {
                    targetChapterId = chapters.get(0).getChapterId();
                }
            }

            if (targetChapterId != -1) {
                Intent intent = new Intent(this, ReadActivity.class);
                intent.putExtra("chapter_id", targetChapterId);
                startActivity(intent);
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "获取章节失败", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * 检查登录状态控制评论按钮
     */
    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        boolean isLoggedIn = prefs.contains("Uid");
        btnPostComment.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        btnPostComment.setOnClickListener(v -> showCommentDialog());
    }

    /**
     * 加载评论数据
     */
    private void loadComments() {
        new Thread(() -> {
            if (dbHelper != null) {
                List<Comment> comments = dbHelper.getHotComments(bookId, 10);
            runOnUiThread(() -> {
                commentAdapter = new CommentAdapter(comments);
                rvComments.setAdapter(commentAdapter);
            });
            }
        }).start();
    }

    /**
     * 显示评论对话框
     */
    private void showCommentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_comment, null);
        EditText etContent = view.findViewById(R.id.et_content);

        builder.setTitle("发表评论")
                .setView(view)
                .setPositiveButton("发布", (dialog, which) -> {
                    String content = etContent.getText().toString();
                    postComment(content);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 提交评论到数据库
     */
    private void postComment(String content) {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
            int userId = prefs.getInt("Uid", -1);

            Comment comment = new Comment();
            comment.setUserId(userId);
            comment.setBookId(bookId);
            comment.setContent(content);
            comment.setStatus(-1); // 初始状态为待审核
            dbHelper.insertComment(comment);
            runOnUiThread(() -> {
                Toast.makeText(this, "评论已提交审核", Toast.LENGTH_SHORT).show();
                loadComments();
            });
        }).start();
    }

    /**
     * 评论适配器
     */
    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        private List<Comment> comments;

        CommentAdapter(List<Comment> comments) {
            this.comments = comments;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.tvContent.setText(comment.getContent());
            holder.tvUser.setText("用户：" + dbHelper.getUserName(comment.getUserId()));
            // 格式化评论时间
            if (comment.getTime() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String formattedTime = dateFormat.format(comment.getTime());
                holder.tvTime.setText("发表时间：" + formattedTime);
            } else {
                // 如果评论时间为null，设置默认显示
                holder.tvTime.setText("发表时间：未知");
            }

            // 举报功能
            holder.btnReport.setOnClickListener(v -> {
                new AlertDialog.Builder(BookDetailActivity.this)
                        .setTitle("确认举报")
                        .setMessage("确定举报此评论吗？")
                        .setPositiveButton("确认", (dialog, which) -> {
                            // 提交举报
                            dbHelper.reportComment(comment.getCommentId(), "用户举报"); // 可以直接写死一个简单的举报理由
                            Toast.makeText(BookDetailActivity.this, "举报已提交", Toast.LENGTH_SHORT).show();
                            // 刷新评论列表
                            loadComments();
                        })
                        .setNegativeButton("取消", null) // 点击取消不做任何操作
                        .show();
            });

        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent, tvUser, tvTime;
            Button btnReport;

            ViewHolder(View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvUser = itemView.findViewById(R.id.tv_user);
                btnReport = itemView.findViewById(R.id.btn_report);
                tvTime = itemView.findViewById(R.id.tv_time);
            }
        }
    }
}