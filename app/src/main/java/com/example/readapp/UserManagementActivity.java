package com.example.readapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private MyDBUtils dbHelper;
    private static final int REQUEST_EDIT_USER = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);
        dbHelper = new MyDBUtils(this);

        // 初始化视图
        initViews();

        // 加载数据
        loadUsers();

        findViewById(R.id.btn_add_user).setOnClickListener(v -> {
            // 使用startActivityForResult启动
            Intent intent = new Intent(this, UserFormActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_USER);
        });
    }

    private void initViews() {
        // 初始化列表
        recyclerView = findViewById(R.id.rv_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 添加用户按钮点击事件
        findViewById(R.id.btn_add_user).setOnClickListener(v -> {
            startActivity(new Intent(this, UserFormActivity.class));
        });

        // 搜索功能
        setupSearch();
    }

    /**
     * 配置搜索功能
     */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                searchUsers(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    /**
     * 加载用户数据
     */
    private void loadUsers() {
        new Thread(() -> {
            List<UserBean> users = dbHelper.getAllUsers();
            runOnUiThread(() -> {
                adapter = new UserAdapter(users);
                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    /**
     * 搜索用户
     */
    private void searchUsers(String keyword) {
        new Thread(() -> {
            List<UserBean> result = dbHelper.searchUsers(keyword);
            runOnUiThread(() -> adapter.updateData(result));
        }).start();
    }

    // 新增代码：处理返回结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_USER && resultCode == RESULT_OK) {
            // 当用户信息修改/新增成功时刷新列表
            loadUsers();
        }
    }

    /**
     * 用户列表适配器
     */
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private List<UserBean> users;

        UserAdapter(List<UserBean> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserBean user = users.get(position);
            holder.tvUsername.setText(user.getUname());
            holder.tvUserId.setText("ID: " + user.getUid());

            // 加载头像
            Glide.with(holder.itemView)
                    .load(new File(getFilesDir(), user.getHeadimg()))
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);

            // 修改按钮点击事件
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(UserManagementActivity.this, UserFormActivity.class);
                intent.putExtra("USER_ID", user.getUid());
                startActivity(intent);
            });

            // 删除按钮点击事件
            holder.btnDelete.setOnClickListener(v -> showDeleteDialog(user));

            // 修改按钮点击事件
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(UserManagementActivity.this, UserFormActivity.class);
                intent.putExtra("USER_ID", user.getUid());
                // 使用startActivityForResult启动
                startActivityForResult(intent, REQUEST_EDIT_USER);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public void updateData(List<UserBean> newUsers) {
            this.users = newUsers;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvUsername, tvUserId;
            Button btnEdit, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                tvUsername = itemView.findViewById(R.id.tv_username);
                tvUserId = itemView.findViewById(R.id.tv_user_id);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }

        /**
         * 显示删除确认对话框
         */
        private void showDeleteDialog(UserBean user) {
            new AlertDialog.Builder(UserManagementActivity.this)
                    .setTitle("删除用户")
                    .setMessage("确定删除用户 " + user.getUname() + " 吗？")
                    .setPositiveButton("删除", (dialog, which) -> deleteUser(user))
                    .setNegativeButton("取消", null)
                    .show();
        }

        /**
         * 执行删除操作
         */
        private void deleteUser(UserBean user) {
            new Thread(() -> {
                boolean success = dbHelper.deleteUser(user.getUid());
                runOnUiThread(() -> {
                    if (success) {
                        loadUsers();
                        Toast.makeText(UserManagementActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }


    }
}
