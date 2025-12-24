package com.example.readapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

public class ForgetActivity extends AppCompatActivity {
    private TextView tvQues, tvLogin , tvUname;
    private EditText etAnswer, etNewpsw, etNewpsw2;
    private Button btnChangepsw;
    private MyDBUtils dbHelper;
    private String username; // 从登录页面传递过来的用户名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget);
        tvQues = findViewById(R.id.tv_ques);
        etAnswer = findViewById(R.id.et_answer);
        etNewpsw = findViewById(R.id.et_newpsw);
        etNewpsw2 = findViewById(R.id.et_newpsw2);
        btnChangepsw = findViewById(R.id.btn_changepsw);
        tvLogin = findViewById(R.id.tv_login);
        tvUname = findViewById(R.id.et_user_name);
        dbHelper = new MyDBUtils(ForgetActivity.this);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        if (username == null) {
            Toast.makeText(this, "用户信息错误", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 显示安全问题
        showSecurityQuestion();

        // 修改密码按钮点击事件
        btnChangepsw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String answer = etAnswer.getText().toString().trim();
                String newPsw = etNewpsw.getText().toString().trim();
                String newPsw2 = etNewpsw2.getText().toString().trim();
                if (!validateInput(answer, newPsw, newPsw2)) {
                    return;
                }
                boolean success = dbHelper.updatePassword(username, answer, newPsw);
                if (success) {
                    Toast.makeText( ForgetActivity.this, "密码修改成功，请再次登录", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent( ForgetActivity.this, LoginActivity.class));
                    intent.putExtra("usern",username);
                    finish();
                } else {
                    Toast.makeText(ForgetActivity.this, "修改失败，请检查安全答案", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 跳转登录
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent( ForgetActivity.this, LoginActivity.class));
                finish();
            }
        }) ;
    }

    //显示安全问题
    private void showSecurityQuestion() {
            String question = dbHelper.getSecurityQuestion(username);
                if (question != null) {
                    tvUname.setText("用户名：" + username);
                    tvQues.setText("安全问题：" + question);
                } else {
                    Toast.makeText(this, "获取安全问题失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
    }


    private boolean validateInput(String answer, String newPsw, String newPsw2) {
        if (answer.isEmpty()) {
            Toast.makeText(this, "安全答案不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newPsw.isEmpty()) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPsw.equals(newPsw2)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
