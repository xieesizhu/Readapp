package com.example.readapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class RegActivity extends AppCompatActivity {
    private ImageView mSymbol;
    private TextView mQq;
    private EditText mEtUserName;
    private EditText mEtPsw;
    private EditText mEtPsw2;
    private EditText mEtQues;
    private EditText mEtAns;
    private EditText mEtAge;
    private Button mBtnReg;
    private TextView mTvLogin;
    private MyDBUtils utils;
    private List<UserBean> all;
    private int pos=0;
    private ImageView mIvAvatar;
    private String currentUserAvatarPath;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_IMAGE_REQUEST = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);
        initViews();
        utils = new MyDBUtils(RegActivity.this);
        setListeners();

    }

    private void setListeners() {
        mBtnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = mEtUserName.getText().toString();
                String psw = mEtPsw.getText().toString();
                String psw2 = mEtPsw2.getText().toString();
                String question = mEtQues.getText().toString();
                String answer = mEtAns.getText().toString();
                all = utils.finduser(userName);
                if (all.size() > 0) {
                    Toast.makeText(RegActivity.this,"用户名已存在",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(userName.equals("")||psw.equals("")||psw2.equals("")||question.equals("")||answer.equals("")||mEtAge.getText().toString().equals(""))
                {
                    Toast.makeText(RegActivity.this,"请填写完整信息",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (psw.equals(psw2)) {
                    UserBean userBean = new UserBean();
                    userBean.setUname(userName);
                    userBean.setPassword(psw);
                    userBean.setQuestion(question);
                    userBean.setAnswer(answer);
                    userBean.setAge(mEtAge.getText().toString());
                    userBean.setHeadimg(currentUserAvatarPath != null ? currentUserAvatarPath : "default");

                    utils.insertuser(userBean);
                    Toast.makeText(RegActivity.this,"注册成功，请再次登录",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegActivity.this, LoginActivity.class);
                    intent.putExtra("usern",userName);
                    intent.putExtra("psw",psw);
                    startActivity(intent);
                }else {
                    Toast.makeText(RegActivity.this,"两次密码不一致",Toast.LENGTH_SHORT).show();
                }
            }
        });
        mTvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // 添加头像点击监听
        mIvAvatar.setOnClickListener(v -> {
            if (checkPermission()) {
                openImagePicker();
            } else {
                ActivityCompat.requestPermissions(RegActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        });
    }

    // 权限检查方法
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "需要存储权限以选择头像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 打开图片选择器
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 处理选择的图片
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            // 使用Glide加载并显示图片
            Glide.with(this)
                    .load(selectedImageUri)
                    .circleCrop()
                    .into(mIvAvatar);

            // 保存图片到本地存储
            currentUserAvatarPath = FileUtils.saveAvatarToPrivateStorage(RegActivity.this, selectedImageUri);
        }
    }



    private void initViews() {
        mSymbol = findViewById(R.id.symbol);
        mQq = findViewById(R.id.qq);
        mEtUserName = findViewById(R.id.et_user_name);
        mEtPsw = findViewById(R.id.et_psw);
        mEtPsw2 = findViewById(R.id.et_psw2);
        mEtQues = findViewById(R.id.et_question);
        mEtAns = findViewById(R.id.et_answer);
        mEtAge = findViewById(R.id.et_age);
        mBtnReg = findViewById(R.id.btn_reg);
        mTvLogin = findViewById(R.id.tv_login);
        mIvAvatar = findViewById(R.id.iv_avatar);


    }
}
