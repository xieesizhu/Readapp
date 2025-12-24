package com.example.readapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.readapp.MyDBUtils;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPsw, etNewPsw, etNewPsw2;
    private TextView tvUserName;
    private MyDBUtils dbUtils;
    private String username;
    private int currentUserId;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // 初始化组件
        tvUserName = findViewById(R.id.tv_user_name);
        etOldPsw = findViewById(R.id.et_oldpsw);
        etNewPsw = findViewById(R.id.et_newpsw);
        etNewPsw2 = findViewById(R.id.et_newpsw2);
        Button btnChange = findViewById(R.id.btn_changepsw);
        dbUtils = new MyDBUtils(this);

        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences("user_info", MODE_PRIVATE);
        currentUserId = prefs.getInt("Uid", -1);
        username = prefs.getString("username", "");
        // 设置当前用户名
        tvUserName.setText("当前用户：" + username);
        // 设置修改按钮点击事件
        btnChange.setOnClickListener(v -> attemptPasswordChange());
    }

    //修改密码
    private void attemptPasswordChange() {
        String oldPsw = etOldPsw.getText().toString().trim();
        String newPsw = etNewPsw.getText().toString().trim();
        String confirmPsw = etNewPsw2.getText().toString().trim();

        if (validateInput(oldPsw, newPsw, confirmPsw)) {
            new Thread(() -> {
                boolean success = dbUtils.updateUserPassword(
                        currentUserId,
                        oldPsw,
                        newPsw
                );
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();

                        //清除用户数据
                        clearUserData();
                        Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                        intent.putExtra("usern",username);
                        startActivity(intent);

                    } else {
                        Toast.makeText(this, "原密码错误或修改失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }
    }

    private boolean validateInput(String oldPsw, String newPsw, String confirmPsw) {
        if (oldPsw.isEmpty()) {
            showError("请输入原密码");
            return false;
        }
        if (newPsw.isEmpty()) {
            showError("新密码不能为空");
            return false;
        }
        if (!newPsw.equals(confirmPsw)) {
            showError("两次输入的密码不一致");
            return false;
        }
        return true;
    }

    private void clearUserData() {
        SharedPreferences preferences = getSharedPreferences("user_info",MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("username")
                .remove("Uid")
                .apply();

        SharedPreferences dataPrefs = getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = dataPrefs.edit();
        editor.remove("userName")
                .remove("is")
                .remove("psw")
                .remove("Uid")
                .apply();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}