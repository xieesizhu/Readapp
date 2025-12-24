package com.example.readapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class ReadingHistoryActivity extends AppCompatActivity {
    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private MyDBUtils dbHelper;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_history);

        // 初始化视图
        rvHistory = findViewById(R.id.rv_history);
        tvEmpty = findViewById(R.id.tv_empty);
        dbHelper = new MyDBUtils(this);

        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = prefs.getInt("Uid", -1);

        // 设置列表
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<ReadingRecord> records = dbHelper.getUserReadingRecords(currentUserId);

            runOnUiThread(() -> {
                if (records.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                    HistoryAdapter adapter = new HistoryAdapter(records);
                    rvHistory.setAdapter(adapter);
                }
            });
        }).start();
    }

    // 适配器
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<ReadingRecord> records;

        HistoryAdapter(List<ReadingRecord> records) {
            this.records = records;
        }

        // 在 HistoryAdapter 类中添加
        public void removeItem(int position) {
            records.remove(position);
            notifyItemRemoved(position);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reading_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReadingRecord record = records.get(position);

            // 加载封面
            Glide.with(ReadingHistoryActivity.this)
                    .load(new File(record.getBookCover()))
                    .placeholder(R.drawable.ic_books)
                    .into(holder.ivCover);

            holder.tvTitle.setText(record.getBookTitle());
            holder.tvAuthor.setText("作者：" + record.getAuthor());
            holder.tvChapter.setText("当前章节：第" + record.getChapterNumber() + "章");
            holder.tvProgress.setText("阅读进度：" + (int)(record.getProgress() * 100) + "%");

            // 查看详情
            holder.btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(ReadingHistoryActivity.this, BookDetailActivity.class);
                intent.putExtra("book_id", record.getBookId());
                startActivity(intent);
            });

            // 继续阅读
            holder.btnContinue.setOnClickListener(v -> {
                Intent intent = new Intent(ReadingHistoryActivity.this, ReadActivity.class);
                intent.putExtra("book_id", record.getBookId());
                intent.putExtra("chapter_id", record.getChapterId());
                intent.putExtra("progress", record.getProgress());
                startActivity(intent);
            });

            // 删除记录
            holder.btnDelete.setOnClickListener(v -> deleteRecord(record, position));
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvTitle, tvAuthor, tvChapter, tvProgress;
            Button btnDetail, btnContinue, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_cover);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvAuthor = itemView.findViewById(R.id.tv_author);
                tvChapter = itemView.findViewById(R.id.tv_chapter);
                tvProgress = itemView.findViewById(R.id.tv_progress);
                btnDetail = itemView.findViewById(R.id.btn_detail);
                btnContinue = itemView.findViewById(R.id.btn_continue);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    // 删除记录
    private void deleteRecord(ReadingRecord record, int position) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除这条阅读记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    new Thread(() -> {
                        boolean success = dbHelper.deleteReadingRecord(record.getRecordId());

                        runOnUiThread(() -> {
                            if (success) {
                                HistoryAdapter adapter = (HistoryAdapter) rvHistory.getAdapter();
                                adapter.removeItem(position);
                                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "删除失败，请重试", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

}