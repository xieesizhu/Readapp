package com.example.readapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

public class CommentManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private int currentFilter = -999; // 当前筛选状态
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_management);

        // 初始化视图组件
        initViews();

        // 设置筛选按钮监听
        setupFilterButtons();

        // 设置搜索框监听
        setupSearchListener();

        // 加载初始数据
        loadComments();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_comments);
        etSearch = findViewById(R.id.et_search);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    /** 设置筛选按钮点击事件 */
    private void setupFilterButtons() {
        int[] buttonIds = {R.id.btn_all, R.id.btn_pending, R.id.btn_reported, R.id.btn_blocked};
        int[] filters = {-999, -1, 0, -2};

        for (int i = 0; i < buttonIds.length; i++) {
            int finalI = i;
            findViewById(buttonIds[i]).setOnClickListener(v -> {
                currentFilter = filters[finalI];
                loadComments(); // 筛选状态改变时重新加载数据
            });
        }
    }

    /** 设置搜索框文本变化监听 */
    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                loadComments(); // 输入内容变化时重新加载数据
            }
        });
    }

    /** 加载评论数据 */
    private void loadComments() {
        new Thread(() -> {
            // 获取搜索关键词
            String searchKey = etSearch.getText().toString().trim();

            // 从数据库获取数据
            List<CommentDetail> comments = new MyDBUtils(this)
                    .getComments(currentFilter, searchKey);

            // 更新UI
            runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new CommentAdapter(comments);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateData(comments);
                }
            });
        }).start();
    }

    /** 显示删除确认对话框 */
    private void showDeleteConfirm(CommentDetail comment) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要永久删除这条评论吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 显示状态修改确认对话框 */
    private void showStatusConfirm(CommentDetail comment) {
        String message = comment.getStatus() == -2 ?
                "确定要恢复显示这条评论吗？" :
                "确定要屏蔽这条评论吗？";

        new AlertDialog.Builder(this)
                .setTitle("确认操作")
                .setMessage(message)
                .setPositiveButton("确认", (dialog, which) -> toggleCommentStatus(comment))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 执行删除操作 */
    private void deleteComment(CommentDetail comment) {
        new Thread(() -> {
            boolean success = new MyDBUtils(this).deleteComment(comment.getCommentId());
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                    loadComments(); // 刷新列表
                }
            });
        }).start();
    }

    /** 切换评论状态 */
    private void toggleCommentStatus(CommentDetail comment) {
        int newStatus = comment.getStatus() == -2 ? 1 : -2;

        new Thread(() -> {
            boolean success = new MyDBUtils(this)
                    .updateCommentStatus(comment.getCommentId(), newStatus);
            runOnUiThread(() -> {
                if (success) {
                    String msg = newStatus == -2 ? "已屏蔽" : "已恢复";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    loadComments(); // 刷新列表
                }
            });
        }).start();
    }

    /** 自定义适配器 */
    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
        private List<CommentDetail> comments;

        CommentAdapter(List<CommentDetail> comments) {
            this.comments = comments;
        }

        /** 更新数据集 */
        public void updateData(List<CommentDetail> newComments) {
            this.comments = newComments;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment_admin, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CommentDetail comment = comments.get(position);

            // 绑定数据
            holder.tvContent.setText(comment.getContent());
            if (comment.getStatus() == -2){
                holder.tvStatus.setText("已屏蔽");
            }
            else if (comment.getStatus() == -1){
                holder.tvStatus.setText("待审核");
                holder.btnPass.setVisibility(View.VISIBLE);  // 显示通过审核按钮
                holder.btnPass.setOnClickListener(v -> {
                    new Thread(() -> {
                        boolean success = new MyDBUtils(holder.itemView.getContext())
                                .updateCommentStatus(comment.getCommentId(), 1); // 更新状态为已通过
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(holder.itemView.getContext(), "已通过", Toast.LENGTH_SHORT).show();
                                holder.btnPass.setVisibility(View.GONE);
                                loadComments(); // 刷新列表
                            }
                        });
                    }).start();
                });
            }
            else if (comment.getStatus() == 0){
                holder.tvStatus.setText("被举报");
            }
            else if (comment.getStatus() == 1){
                holder.tvStatus.setText("已通过");
            }
            holder.tvUser.setText("用户：" + comment.getUserName());
            holder.tvBook.setText("小说：" + comment.getBookTitle());
            // 格式化评论时间
            if (comment.getCommentTime() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String formattedTime = dateFormat.format(comment.getCommentTime());
                holder.tvTime.setText("发表时间：" + formattedTime);
            } else {
                // 如果评论时间为null，设置默认显示
                holder.tvTime.setText("发表时间：未知");
            }

            // 设置状态显示
            updateStatusUI(holder, comment.getStatus());

            // 设置举报原因
            if ((comment.getStatus() == 0 || comment.getStatus() == -2) && comment.getReportReason() != null) {
                holder.tvReportReason.setVisibility(View.VISIBLE);
                if (comment.getStatus() == 0) {
                    holder.tvReportReason.setText("举报原因：" + comment.getReportReason());
                }
                if(comment.getStatus() == -2){
                    holder.tvReportReason.setText("已通过举报，举报原因：" + comment.getReportReason());
                }
            } else {
                holder.tvReportReason.setVisibility(View.GONE);
            }

            // 设置按钮状态
            holder.btnBlock.setText(comment.getStatus() == -2 ? "恢复显示" : "屏蔽");
            holder.btnBlock.setOnClickListener(v -> showStatusConfirm(comment));
            holder.btnDelete.setOnClickListener(v -> showDeleteConfirm(comment));
        }


        /** 更新状态显示 */
        private void updateStatusUI(ViewHolder holder, int status) {
            int colorRes = R.color.gray;
            String text = "未知状态";
            switch (status) {
                case -2:
                    text = "已屏蔽";
                    colorRes = R.color.gray_dark;
                    break;
                case -1:
                    text = "待审核";
                    colorRes = R.color.orange;
                    break;
                case 0:
                    text = "被举报";
                    colorRes = R.color.red;
                    break;
                case 1:
                    text = "已通过";
                    colorRes = R.color.green;
            }
            holder.tvStatus.setText(text);
            holder.tvStatus.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), colorRes))
            );
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        /** ViewHolder */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStatus, tvContent, tvUser, tvBook, tvTime, tvReportReason;
            Button btnDelete, btnBlock, btnPass;

            ViewHolder(View itemView) {
                super(itemView);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvContent = itemView.findViewById(R.id.tv_content);
                tvUser = itemView.findViewById(R.id.tv_user);
                tvBook = itemView.findViewById(R.id.tv_book);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvReportReason = itemView.findViewById(R.id.tv_report_reason);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                btnBlock = itemView.findViewById(R.id.btn_block);
                btnPass = itemView.findViewById(R.id.btn_pass);
            }
        }
    }
}