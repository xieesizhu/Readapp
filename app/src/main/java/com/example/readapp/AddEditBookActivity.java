package com.example.readapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.List;

// AddEditBookActivity.java
public class AddEditBookActivity extends AppCompatActivity {
    private static final int PICK_COVER_REQUEST = 1001;
    private static final int PICK_FILE_REQUEST = 1002;

    private ImageView ivCover;
    private EditText etTitle, etAuthor, etDescription;
    private Spinner spCategory;
    private String coverPath, filePath;
    private BookBean currentBook;
    private MyDBUtils dbHelper;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_book);

        dbHelper = new MyDBUtils(this);
        // 判断当前模式
        int bookId = getIntent().getIntExtra("book_id", -1);
        isEditMode = bookId != -1;

        initViews();
        setupUIForMode();
        setupCategorySpinner();
        loadBookData();
    }

    private void initViews() {
        ivCover = findViewById(R.id.iv_cover);
        etTitle = findViewById(R.id.et_title);
        etAuthor = findViewById(R.id.et_author);
        etDescription = findViewById(R.id.et_description);
        spCategory = findViewById(R.id.sp_category);

        // 封面选择
        findViewById(R.id.btn_select_cover).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_COVER_REQUEST);
        });

        // 文件选择
        findViewById(R.id.btn_select_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain|application/epub+zip");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        // 保存按钮
        findViewById(R.id.btn_save).setOnClickListener(v -> saveBook());
    }

    private void setupUIForMode() {
        Button btnSelectFile = findViewById(R.id.btn_select_file);
        TextView fileLabel = findViewById(R.id.tv_file_label);

        if (isEditMode) {
            btnSelectFile.setVisibility(View.GONE);
            fileLabel.setVisibility(View.GONE);
        }
    }


    private void setupCategorySpinner() {
        new Thread(() -> {
            List<CategoryBean> categories = dbHelper.getAllCategories();
            runOnUiThread(() -> {
                ArrayAdapter<CategoryBean> adapter = new ArrayAdapter<CategoryBean>(
                        this, android.R.layout.simple_spinner_item, categories) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        view.setText(getItem(position).getCategoryName());
                        return view;
                    }
                };
                spCategory.setAdapter(adapter);

                // 如果有编辑数据，设置默认选中
                if (currentBook != null) {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getCategoryId() == currentBook.getCategoryId()) {
                            spCategory.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void loadBookData() {
        // 获取传递过来的书籍ID
        int bookId = getIntent().getIntExtra("book_id", -1);
        if (bookId == -1) {
            return;
        }

        new Thread(() -> {
            currentBook = dbHelper.getBookById(bookId);
            if (currentBook != null) {
                runOnUiThread(() -> {
                    etTitle.setText(currentBook.getTitle());
                    etAuthor.setText(currentBook.getAuthor());
                    etDescription.setText(currentBook.getDescription());
                    if (currentBook.getCoverPath() != null) {
                        Glide.with(this)
                                .load(new File(currentBook.getCoverPath()))
                                .into(ivCover);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        try {
            Uri uri = data.getData();
            if (uri == null) {
                return;
            }

            if (requestCode == PICK_COVER_REQUEST) {
                handleCoverSelection(uri);
            } else if (requestCode == PICK_FILE_REQUEST) {
                handleFileSelection(uri);
            }
        } catch (Exception e) {
            Toast.makeText(this, "文件处理失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCoverSelection(Uri uri) throws IOException {
        coverPath = FileUtils.saveCoverToPrivateStorage(this, uri);
        Glide.with(this)
                .load(new File(coverPath))
                .into(ivCover);
    }

    private void handleFileSelection(Uri uri) throws IOException {
        filePath = FileUtils.saveBookToPrivateStorage(this, uri);
        Toast.makeText(this, "书籍文件已选择", Toast.LENGTH_SHORT).show();
    }

    private void saveBook() {
        // 验证输入
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        CategoryBean category = (CategoryBean) spCategory.getSelectedItem();

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "书名和作者不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (filePath == null && currentBook == null) {
            Toast.makeText(this, "请选择书籍文件", Toast.LENGTH_SHORT).show();
            return;
        }
        // 编辑模式保持原文件路径
        if (isEditMode && filePath == null) {
            filePath = currentBook.getFilePath();
        }

        // 创建/更新书籍对象
        BookBean book = currentBook != null ? currentBook : new BookBean();
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(etDescription.getText().toString());
        book.setCategoryId(category.getCategoryId());
        if(coverPath==null)
        {
            if(currentBook==null)
            {
                coverPath=null;
            }else
            {
                coverPath=currentBook.getCoverPath();
            }
        }
        book.setCoverPath(coverPath);
        book.setFilePath(filePath != null ? filePath : currentBook.getFilePath());

        new Thread(() -> {
            try {
                if (currentBook == null) {
                    // 新增书籍
                    long bookId = dbHelper.insertBook(book);
                    if (filePath != null) {
                        parseAndSaveChapters(bookId, filePath);
                    }
                } else {
                    // 更新书籍
                    dbHelper.updateBook(book);
                }

                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void parseAndSaveChapters(long bookId, String filePath) {
        try {
            // 正确调用 parseTxt 方法（返回 ParseResult）
            BookParser.ParseResult result = BookParser.parseTxt(new File(filePath));

            // 更新书籍统计信息
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("word_count", result.totalWords);
            values.put("chapter_count", result.chapterCount);
            db.update("books", values, "book_id = ?", new String[]{String.valueOf(bookId)});

            // 批量插入章节
            dbHelper.batchInsertChaptersWithStats(bookId, result.chapters);
        } catch (IOException e) {
            Log.e("ChapterParse", "章节解析失败: " + filePath, e);
        }
    }
}