package com.example.readapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class AdminActivity extends AppCompatActivity {
    private static final String ADMIN_FLAG = "Xieesizhu1120";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // 校验管理员权限
        if (!validateAdminAccess()) {
            finish();
            return;
        }

        setupClickListeners();
    }

    // 权限校验方法
    private boolean validateAdminAccess() {
        // 双重校验：1.检查传递的flag 2.检查实际管理员身份
        String receivedFlag = getIntent().getStringExtra("flag");
        if (!ADMIN_FLAG.equals(receivedFlag) || !isRealAdmin()) {
            Toast.makeText(this, "权限不足", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // 数据库校验管理员身份
    private boolean isRealAdmin() {
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        int userId = prefs.getInt("Uid", -1);
        return new MyDBUtils(this).isAdmin(userId);
    }

    private void setupClickListeners() {
        // 用户管理
        findViewById(R.id.card_user_management).setOnClickListener(v -> {
            startActivity(new Intent(this, UserManagementActivity.class));
        });

        // 书籍管理
        findViewById(R.id.card_book_management).setOnClickListener(v -> {
            startActivity(new Intent(this, BookManagementActivity.class));
        });

        // 评论管理
        findViewById(R.id.card_comment_management).setOnClickListener(v -> {
            startActivity(new Intent(this, CommentManagementActivity.class));
        });

        //阅读统计
        // 阅读统计
        findViewById(R.id.card_reading_stats).setOnClickListener(v -> {
            startActivity(new Intent(this, ReadingStatsActivity.class));
        });
    }
}
