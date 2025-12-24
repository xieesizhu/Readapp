package com.example.readapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class MyDB extends SQLiteOpenHelper {
    public MyDB(@Nullable Context context) {
        super(context, "readApp.db", null, 1);
    }

    private void enableForeignKeys() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 用户表
        String createUserTable = "CREATE TABLE user (" +
                "uid INTEGER PRIMARY KEY AUTOINCREMENT, " + // 用户唯一标识，自增主键
                "uname TEXT NOT NULL, " + // 用户名，不能为空
                "password TEXT NOT NULL, " + // 用户密码，不能为空
                "headimg TEXT NOT NULL, " + // 用户头像路径，不能为空
                "question TEXT NOT NULL, " + // 安全问题，不能为空
                "answer TEXT NOT NULL, " + // 安全答案，不能为空
                "age TEXT NOT NULL " + // 用户年龄，不能为空
                ")";
        db.execSQL(createUserTable);

        // 书籍分类表
        String createCategoryTable = "CREATE TABLE categories (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 分类唯一标识，自增主键
                "category_name TEXT UNIQUE NOT NULL" + // 分类名称，唯一且不能为空
                ")";
        db.execSQL(createCategoryTable);

        // 书籍表
        String createBookTable = "CREATE TABLE books (" +
                "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 书籍唯一标识，自增主键
                "title TEXT NOT NULL, " + // 书籍标题，不能为空
                "author TEXT NOT NULL, " + // 书籍作者，不能为空
                "category_id INTEGER , " + // 书籍所属分类ID，外键，不能为空
                "cover_path TEXT , " + // 书籍封面图片路径，可以为空
                "file_path TEXT , " + // 书籍文件路径（如TXT或EPUB文件），不能为空
                "description TEXT, " + // 书籍描述，可以为空
                "word_count INTEGER DEFAULT 0, " + // 字数
                "chapter_count INTEGER DEFAULT 0, " + // 章节数
                "upload_time DATETIME DEFAULT CURRENT_TIMESTAMP, " + // 书籍上传时间，默认为当前时间
                "is_recommendation INTEGER DEFAULT 0, " + // 是否为推荐阅读，默认值为0"
                "view_count  INTEGER DEFAULT 0 " + // 阅读次数，默认值为0"
                ")";
        db.execSQL(createBookTable);

        // 章节表（带级联删除）
        String createChapterTable = "CREATE TABLE chapters (" +
                "chapter_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "book_id INTEGER NOT NULL," +
                "chapter_number INTEGER," +
                "chapter_title TEXT," +
                "content TEXT NOT NULL," +
                "word_count INTEGER," +
                "FOREIGN KEY(book_id) REFERENCES books(book_id) ON DELETE CASCADE" +
                ")";
        db.execSQL(createChapterTable);

        // 书架表
        String createBookshelfTable = "CREATE TABLE bookshelves (" +
                "shelf_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 书架记录唯一标识，自增主键
                "user_id INTEGER NOT NULL, " + // 用户ID，外键，不能为空
                "book_id INTEGER NOT NULL, " + // 书籍ID，外键，不能为空
                "add_time DATETIME DEFAULT CURRENT_TIMESTAMP, " + // 书籍添加到书架的时间，默认为当前时间
                "UNIQUE(user_id, book_id)" + // 用户ID和书籍ID组合唯一，避免重复添加
                ")";
        db.execSQL(createBookshelfTable);

        // 阅读记录表
        String createReadingHistoryTable = "CREATE TABLE reading_history (" +
                "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 阅读记录唯一标识，自增主键
                "user_id INTEGER NOT NULL, " + // 用户ID，外键，不能为空
                "book_id INTEGER NOT NULL, " + // 书籍ID，外键，不能为空
                "chapter_id INTEGER NOT NULL, " + // 章节ID，不能为空"
                "progress REAL DEFAULT 0, " + // 阅读进度（0.0-1.0），默认值为0
                "last_read DATETIME DEFAULT CURRENT_TIMESTAMP" + // 最后一次阅读时间，默认为当前时间
                ")";
        db.execSQL(createReadingHistoryTable);

        // 管理员表
        String createAdminTable = "CREATE TABLE admins (" +
                "admin_id INTEGER PRIMARY KEY, " + // 管理员ID，主键，与user表中的uid一致
                "permission_level INTEGER DEFAULT 1" + // 管理员权限等级，默认值为1
                ")";
        db.execSQL(createAdminTable);

        // 推荐权重表
        String createRecommendationWeightsTable = "CREATE TABLE recommendation_weights (" +
                "user_id INTEGER NOT NULL, " + // 用户ID，外键，不能为空
                "category_id INTEGER NOT NULL, " + // 分类ID，外键，不能为空
                "weight REAL DEFAULT 0, " + // 推荐权重值，默认值为0
                "word_count INTEGER DEFAULT 0, " +    // 字数
                "chapter_count INTEGER DEFAULT 0, " + // 章节数
                "PRIMARY KEY (user_id, category_id)" + // 用户ID和分类ID组合为主键
                ")";
        db.execSQL(createRecommendationWeightsTable);

        // 评论表
        String createCommentTable = "CREATE TABLE comments (" +
                "comment_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 评论唯一标识，自增主键
                "user_id INTEGER NOT NULL, " +            // 用户ID
                "book_id INTEGER NOT NULL, " +            // 书籍ID
                "content TEXT NOT NULL, " +               // 评论内容
                "status INTEGER DEFAULT -1, " +           // 状态（-2:已屏蔽 -1:待审核 0:被举报 1:已通过）
                "report_reason TEXT, " +                  // 举报原因（可选）
                "comment_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(user_id) REFERENCES user(uid), " +
                "FOREIGN KEY(book_id) REFERENCES books(book_id)" +
                ")";
        db.execSQL(createCommentTable);

        // 创建索引
        db.execSQL("CREATE INDEX idx_books_title ON books(title)");
        db.execSQL("CREATE INDEX idx_books_author ON books(author)");
        db.execSQL("CREATE INDEX idx_history_user ON reading_history(user_id)");
        db.execSQL("CREATE INDEX idx_comments_status ON comments(status)");
        db.execSQL("CREATE INDEX idx_comments_time ON comments(comment_time)");
        db.execSQL("CREATE INDEX idx_chapters_book ON chapters(book_id)");
        db.execSQL("CREATE INDEX idx_reading_history_user_book ON reading_history(user_id, book_id)");
        db.execSQL("CREATE INDEX idx_reading_history_time ON reading_history(last_read)");

        // 插入初始分类
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('玄幻','1')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('科幻','2')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('都市','3')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('历史','4')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('军事','5')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('奇幻','6')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('武侠','7')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('游戏','8')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('仙侠','9')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('体育','10')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('诸天无限','11')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('悬疑','12')");
        db.execSQL("INSERT INTO categories (category_name,category_id) VALUES ('轻小说','13')");
        // 插入初始用户，第一个用户uid为1，即admin管理员
        db.execSQL("INSERT INTO user (uname, password, headimg, question, answer,age) VALUES ('admin', 'admin', 'default.png' ,'你的安全问题是什么？','你的安全答案是什么？','19')");
        // 插入初始管理员
        db.execSQL("INSERT INTO admins (admin_id, permission_level) VALUES (1, 1)");
        // 插入初始书籍
        //db.execSQL("INSERT INTO books (title, author, category_id, cover_path, file_path, description) VALUES ('斗破苍穹', '天蚕土豆', 1, 'default.png', 'https://www.baidu.com', '这是一本很好看的小说')");
        //db.execSQL("INSERT INTO books (title, author, category_id, cover_path, file_path, description) VALUES ('斗罗大陆', '唐家三少', 2, 'default.png', 'https://www.baidu.com', '这是一本很好看的小说')");
        // 插入初始评论 状态（-2:已屏蔽 -1:待审核 0:被举报 1:已通过）
        db.execSQL("INSERT INTO comments (user_id, book_id, content, status) VALUES (1, 1, '这是一条已通过测试评论', 1)");
        db.execSQL("INSERT INTO comments (user_id, book_id, content, status) VALUES (1, 1, '这是一条被举报测试评论', 0)");
        db.execSQL("INSERT INTO comments (user_id, book_id, content, status) VALUES (1, 1, '这是一条待审核测试评论', -1)");
        db.execSQL("INSERT INTO comments (user_id, book_id, content, status) VALUES (1, 1, '这是一条已屏蔽测试评论', -2)");
        db.execSQL("INSERT INTO comments (user_id, book_id, content, status) VALUES (1, 2, '这是一条测试评论', 1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
