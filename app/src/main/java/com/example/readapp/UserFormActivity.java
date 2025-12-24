package com.example.readapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class UserFormActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;

    private ImageView ivAvatar;
    private EditText etUsername, etPassword, etQuestion, etAnswer,etAge;
    private String avatarPath = "";
    private boolean isEditMode = false;
    private UserBean currentUser;
    private MyDBUtils dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_form);
        dbHelper = new MyDBUtils(this);

        // 初始化视图
        initViews();

        // 检查编辑模式
        checkEditMode();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etQuestion = findViewById(R.id.et_question);
        etAnswer = findViewById(R.id.et_answer);
        etAge = findViewById(R.id.et_age);

        // 头像上传点击事件
        findViewById(R.id.btn_upload).setOnClickListener(v -> pickImage());

        // 提交按钮点击事件
        findViewById(R.id.btn_submit).setOnClickListener(v -> validateAndSubmit());
    }

    /**
     * 检查是否为编辑模式
     */
    private void checkEditMode() {
        int userId = getIntent().getIntExtra("USER_ID", -1);
        if (userId != -1) {
            isEditMode = true;
            loadUserData(userId);
        }
    }

    /**
     * 加载用户数据
     */
    private void loadUserData(int userId) {
        new Thread(() -> {
            currentUser = dbHelper.getUserById(userId);
            runOnUiThread(() -> {
                if (currentUser != null) {
                    // 显示现有数据
                    etUsername.setText(currentUser.getUname());
                    etPassword.setText(currentUser.getPassword());
                    etQuestion.setText(currentUser.getQuestion());
                    etAnswer.setText(currentUser.getAnswer());
                    etAge.setText(currentUser.getAge());
                    avatarPath = currentUser.getHeadimg();

                    // 加载头像
                    if (!avatarPath.isEmpty()) {
                        Glide.with(this)
                                .load(new File(getFilesDir(), avatarPath))
                                .into(ivAvatar);
                    }
                }
            });
        }).start();
    }

    /**
     * 选择图片
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            avatarPath = FileUtils.saveAvatarToPrivateStorage(this, uri);
            Glide.with(this).load(new File(getFilesDir(), avatarPath)).into(ivAvatar);
        }
    }

    /**
     * 验证并提交表单
     */
    private void validateAndSubmit() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String question = etQuestion.getText().toString().trim();
        String answer = etAnswer.getText().toString().trim();
        String age = etAge.getText().toString().trim();

        // 输入验证
        if (username.isEmpty()) {
            etUsername.setError("用户名不能为空");
            return;
        }
        if (!isEditMode && password.isEmpty()) {
            etPassword.setError("密码不能为空");
            return;
        }
        if (question.isEmpty()) {
            etQuestion.setError("密保问题不能为空");
            return;
        }
        if (answer.isEmpty()) {
            etAnswer.setError("密保答案不能为空");
            return;
        }
        if (age.isEmpty()) {
            etAge.setError("年龄不能为空");
            return;
        }

        // 创建用户对象
        UserBean user = new UserBean();
        user.setUname(username);
        user.setPassword(password);
        user.setQuestion(question);
        user.setAnswer(answer);
        user.setHeadimg(avatarPath);
        user.setAge(age);

        if (isEditMode) {
            user.setUid(currentUser.getUid());
            if(dbHelper.isUsernameExists(username)&&!username.equals(currentUser.getUname()))
            {
                Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
            }else
            {
                updateUser(user);
            }
        } else {
            createUser(user);
        }
    }

    /**
     * 创建用户
     */
    private void createUser(UserBean user) {
        new Thread(() -> {
            boolean success = dbHelper.insertUser(user);
            runOnUiThread(() -> {
                if (success) {
                    // 设置成功结果
                    setResult(RESULT_OK);
                    Toast.makeText(this, "用户创建成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "用户名已存在", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 更新用户
     */
    private void updateUser(UserBean user) {
        new Thread(() -> {
            boolean success = dbHelper.updateUser(user);
            runOnUiThread(() -> {
                if (success) {
                    // 设置成功结果
                    setResult(RESULT_OK);
                    Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "更新失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}