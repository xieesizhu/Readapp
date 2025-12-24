package com.example.readapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private ImageView mSymbol;
    private TextView mQq;
    private EditText mEtUserName;
    private EditText mEtPsw;
    private Button mBtnLogin;
    private TextView mTvRegister;
    private CheckBox mCbRemember;
    private TextView mTvForget;
    private TextView mTvGuest;
    private MyDBUtils utils;
    private static final int REQUEST_CODE_PERMISSION = 1001;

    private List<UserBean> all;
    private int pos=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initViews();
        obtainPermissions();
        checkPermission();
        utils = new MyDBUtils(LoginActivity.this);
        rememberlog();
        Intent intent = getIntent();
        String userName1 = intent.getStringExtra("usern");
        String psw1 = intent.getStringExtra("psw");
        if (userName1 != null) {
            mEtUserName.setText(userName1);
            mEtPsw.setText(psw1);
        }
        try {
            saveDefaultCover();
        } catch (IOException e) {
            e.printStackTrace();
        }
        checkFirstRun();
        setListeners();
    }
    //记住密码
    private void rememberlog() {
        SharedPreferences preferences = getSharedPreferences("data",MODE_PRIVATE);
        String userName = preferences.getString("userName","");
        String psw = preferences.getString("psw","");
        String is = preferences.getString("is","");
        if("true".equals(is)){
            mEtUserName.setText(userName);
            mEtPsw.setText(psw);
            mCbRemember.setChecked(true);
        }
    }

    //申请读写权限
    private void obtainPermissions() {
        //判断目标设备的版本
        int version= Build.VERSION.SDK_INT;
        if(version>23){
            //判断是否有读写权限
            if(ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) //没授权
            {
                ActivityCompat.requestPermissions(LoginActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1
                );
            }
        }
    }


    // 检查首次运行
    private void checkFirstRun() {
        SharedPreferences prefs = getSharedPreferences("app_config", MODE_PRIVATE);
        if (!prefs.getBoolean("initialized", false)) {
            initializePresetBooks();
            prefs.edit().putBoolean("initialized", true).apply();
        }
    }

    //申请权限
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
            }
        }
    }

    //登录
    private void setListeners() {
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int flag=0;
                String userName = mEtUserName.getText().toString();
                String psw = mEtPsw.getText().toString();
                if(userName.equals("")||psw.equals("")){
                    Toast.makeText(LoginActivity.this,"用户名或密码不能为空",Toast.LENGTH_SHORT).show();
                }else{
                    all = utils.finduser(userName);
                    if(all.size()==0){
                        Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                        return;
                    }else {
                        if(all.get(0).getUname().equals(userName)&&all.get(0).getPassword().equals(psw))
                        {
                            flag=1;
                            if(mCbRemember.isChecked())
                            {
                                SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
                                editor.putString("userName",userName);
                                editor.putString("psw",psw);
                                editor.putString("is","true");
                                editor.putInt("Uid",all.get(pos).getUid());
                                editor.apply();
                            }else if(!mCbRemember.isChecked()){
                                SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
                                editor.putString("userName","");
                                editor.putString("psw","");
                                editor.putString("is","false");
                                editor.putInt("Uid",all.get(pos).getUid());
                                editor.apply();
                            }
                        }
                    }
                    if(flag==1){
                        SharedPreferences.Editor loginer = getSharedPreferences("user_info",MODE_PRIVATE).edit();
                        loginer.putString("username",userName);
                        loginer.putInt("Uid",all.get(0).getUid());
                        loginer.apply();
                        Toast.makeText(LoginActivity.this,"登录成功",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("username", mEtUserName.getText().toString()); // 传递当前用户名
                        startActivity(intent);
                    }else{
                        Toast.makeText(LoginActivity.this,"用户名或密码错误",Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
        //注册
        mTvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegActivity.class);
                startActivity(intent);
            }
        });
        //游客
        mTvGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 清除所有用户数据
                clearGuestData();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                Toast.makeText(LoginActivity.this,"游客身份无法使用书架功能，仅可阅读，且不保留阅读进度",Toast.LENGTH_SHORT).show();
            }
        });


        //忘记密码
        mTvForget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgetActivity.class);
                if(mEtUserName.getText().toString().equals(""))
                {
                    Toast.makeText(LoginActivity.this,"请先输入用户名",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(utils.finduser(mEtUserName.getText().toString()).size()==0)
                {
                    Toast.makeText(LoginActivity.this,"用户名不存在",Toast.LENGTH_SHORT).show();
                    return;
                }
                intent.putExtra("username", mEtUserName.getText().toString()); // 传递当前用户名
                startActivity(intent);
            }
        });


    }

    //读取页面内容
    private void initViews() {
        mSymbol = findViewById(R.id.symbol);
        mQq = findViewById(R.id.qq);
        mEtUserName = findViewById(R.id.et_user_name);
        mEtPsw = findViewById(R.id.et_psw);
        mBtnLogin = findViewById(R.id.btn_login);
        mTvRegister = findViewById(R.id.tv_register);
        mCbRemember = findViewById(R.id.checkBox);
        mTvGuest = findViewById(R.id.tv_guest);
        mTvForget = findViewById(R.id.tv_forget);
    }

    //除游客数据
    private void clearGuestData() {
        // 清除 data 文件
        SharedPreferences dataPrefs = getSharedPreferences("data", MODE_PRIVATE);
        dataPrefs.edit().clear().apply();

        // 清除 user_info 文件
        SharedPreferences userPrefs = getSharedPreferences("user_info", MODE_PRIVATE);
        userPrefs.edit().clear().apply();
    }

    // region 初始化预设数据
    public void initializePresetBooks() {
        new Thread(() -> {
            try {
                AssetManager assetManager = getAssets();
                String[] files = assetManager.list("books");

                for (String filename : files) {
                    // 解析文件名
                    BookMetaData meta = parseFilename(filename);
                    if (meta == null) {
                        continue;
                    }

                    // 复制文件到本地存储
                    File localFile = copyAssetToStorage(assetManager, filename);

                    // 创建书籍记录
                    createBookRecord(meta, localFile);
                }

            } catch (IOException e) {
                Log.e("PresetBooks", "初始化失败", e);
            }
        }).start();
    }

    // 解析文件名元数据
    private BookMetaData parseFilename(String filename) {
        String[] parts = filename.replace(".txt", "").split("_");
        if (parts.length != 3) {
            return null;
        }

        return new BookMetaData(
                parts[0],  // 书名
                parts[1],  // 作者
                parts[2]   // 分类
        );
    }

    // 复制资源文件到本地存储
    private File copyAssetToStorage(AssetManager assetManager, String filename)
            throws IOException {

        File outputFile = new File(getFilesDir(), filename);
        try (InputStream is = assetManager.open("books/" + filename);
             OutputStream os = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        return outputFile;
    }

    // 创建书籍数据库记录
    private void createBookRecord(BookMetaData meta, File file) throws IOException {
        BookBean book = new BookBean();
        book.setTitle(meta.title);
        book.setAuthor(meta.author);
        String coverPath = processCover(meta.title);
        book.setCoverPath(coverPath);
        book.setCategoryId(utils.getCategoryIdByName(meta.category));
        book.setFilePath(file.getAbsolutePath());

        long bookId = utils.insertBook(book);
        parseAndSaveChapters(bookId, file.getAbsolutePath());
    }

    /**
     * 处理封面文件逻辑
     * @param bookTitle 书籍标题
     * @return 封面文件路径
     */
    private String processCover(String bookTitle) throws IOException {
        // 尝试加载指定封面
        String assetCoverPath = "covers/" + bookTitle + ".jpg";
        try {
            return saveAssetToPrivate(assetCoverPath);
        } catch (IOException e) {
            Log.w("Preset", "未找到指定封面，使用默认封面: " + bookTitle);
            return saveDefaultCover();
        }
    }
    /**
     * 保存默认封面到私有目录
     */
    private String saveDefaultCover() throws IOException {
        // 从assets加载默认封面
        return saveAssetToPrivate("covers/ic_books.jpg");
    }

    /**
     * 通用方法：保存assets文件到私有目录
     */
    private String saveAssetToPrivate(String assetPath) throws IOException {
        String fileName = assetPath.contains("/") ?
                assetPath.substring(assetPath.lastIndexOf("/") + 1) : assetPath;
        File outputFile = new File(getFilesDir(), fileName);

        try (InputStream is = getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        return outputFile.getAbsolutePath();
    }

    // 元数据内部类
    private static class BookMetaData {
        String title;
        String author;
        String category;

        BookMetaData(String title, String author, String category) {
            this.title = title;
            this.author = author;
            this.category = category;
        }
    }
    // endregion

    // region 章节解析与保存
    private void parseAndSaveChapters(long bookId, String filePath) {
        try {
            // 正确调用 parseTxt 方法（返回 ParseResult）
            BookParser.ParseResult result = BookParser.parseTxt(new File(filePath));

            // 更新书籍统计信息
            SQLiteDatabase db = utils.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("word_count", result.totalWords);
            values.put("chapter_count", result.chapterCount);
            db.update("books", values, "book_id = ?", new String[]{String.valueOf(bookId)});

            // 批量插入章节
            utils.batchInsertChaptersWithStats(bookId, result.chapters);
        } catch (IOException e) {
            Log.e("ChapterParse", "章节解析失败: " + filePath, e);
        }
    }
}
