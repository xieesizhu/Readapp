package com.example.readapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MyDBUtils {
    private Context context;
    private MyDB myDB;
    private SQLiteDatabase db;



    public MyDBUtils(Context context) {
        this.context = context;
        myDB = new MyDB(context);
        db=myDB.getReadableDatabase(); //打开数据库
    }
    public void close(){
        db.close();
    }

    //查询用户
    public List<UserBean> finduser(String name){
            String sql="select * from user where uname='"+name+"'";
            Cursor cursor = db.rawQuery(sql,null);
            List<UserBean> userlist=new ArrayList<>();
            while (cursor.moveToNext()){
                UserBean user=new UserBean();
                user.setUid(cursor.getInt(0));
                user.setUname(cursor.getString(1));
                user.setPassword(cursor.getString(2));
                user.setHeadimg(cursor.getString(3));
                userlist.add(user);
            }
            return userlist;
    }
    //插入用户
    public void insertuser(UserBean user) {
        String sql = "insert into user values(null,?,?,?,?,?,?)";
        db.execSQL(sql, new Object[]{
                user.getUname(),
                user.getPassword(),
                user.getHeadimg(),  // 使用用户选择的头像路径
                user.getQuestion(),
                user.getAnswer(),
                user.getAge()
        });
    }
    // 获取安全问题
    public String getSecurityQuestion(String username) {
        Cursor cursor = db.query("user",
                new String[]{"question"},
                "uname = ?",
                new String[]{username},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String question = cursor.getString(0);
            cursor.close();
            return question;
        }
        return null;
    }

    // 更新密码（带安全答案验证）
    public boolean updatePassword(String username, String answer, String newPassword) {
        Cursor cursor = db.query("user",
                new String[]{"answer"},
                "uname = ?",
                new String[]{username},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String answerFromDB = cursor.getString(0);
            cursor.close();
            if (answerFromDB.equals(answer)) {
                db.execSQL("update user set password = ? where uname = ?",
                        new String[]{newPassword, username});
                return true;
            }
        }
        return false;
    }

    // 获取头像
    public String getUserAvatar(int userId) {
        Cursor cursor = db.query("user",
                new String[]{"headimg"},
                "uid = ?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(0);
            cursor.close();
            return path;
        }
        return null;
    }

    //更新头像
    public boolean updateUserAvatar(int userId, String avatarPath) {
        ContentValues values = new ContentValues();
        values.put("headimg", avatarPath);
        int rows = db.update("user", values, "uid = ?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    //更新密码
    public boolean updateUserPassword(int userId, String oldPassword, String newPassword) {
        try {
            // 验证原密码
            Cursor cursor = db.query("user",
                    new String[]{"password"},
                    "uid = ? AND password = ?",
                    new String[]{String.valueOf(userId), oldPassword},
                    null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                // 更新密码
                ContentValues values = new ContentValues();
                values.put("password", newPassword);
                int rows = db.update("user", values, "uid = ?", new String[]{String.valueOf(userId)});
                cursor.close();
                return rows > 0;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 检查管理员身份
    public boolean isAdmin(int userId) {
        SQLiteDatabase db = new MyDB(context).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM admins WHERE admin_id=?",
                new String[]{String.valueOf(userId)});
        boolean isAdmin = cursor.getCount() > 0;
        cursor.close();
        return isAdmin;
    }

    /**
     * 获取评论列表（带关联信息）
     * @param statusFilter 状态筛选：-999(全部), -1(待审), 0(举报), -2(屏蔽)
     * @param searchKey 搜索关键词（用户/小说名）
     */
    public List<CommentDetail> getComments(int statusFilter, String searchKey) {
        List<CommentDetail> comments = new ArrayList<>();
        SQLiteDatabase db = new MyDB(context).getReadableDatabase();

        // 基础查询语句
        String query = "SELECT c.*, u.uname, b.title " +
                "FROM comments c " +
                "LEFT JOIN user u ON c.user_id = u.uid " +
                "LEFT JOIN books b ON c.book_id = b.book_id " +
                "WHERE 1=1 ";

        List<String> params = new ArrayList<>();

        // 状态筛选条件
        if (statusFilter != -999) {
            query += " AND c.status = ?";
            params.add(String.valueOf(statusFilter));
        }

        // 搜索条件
        if (!TextUtils.isEmpty(searchKey)) {
            query += " AND (u.uname LIKE ? OR b.title LIKE ?)";
            params.add("%" + searchKey + "%");
            params.add("%" + searchKey + "%");
        }

        // 排序规则
        query += " ORDER BY c.comment_time DESC";

        Cursor cursor = db.rawQuery(query, params.toArray(new String[0]));

        // SimpleDateFormat 用于解析 yyyy-MM-dd HH:mm:ss 格式的日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 遍历结果集
        while (cursor.moveToNext()) {
            CommentDetail detail = new CommentDetail();
            detail.setCommentId(cursor.getInt(0));
            detail.setUserId(cursor.getInt(1));
            detail.setBookId(cursor.getInt(2));
            detail.setContent(cursor.getString(3));
            detail.setStatus(cursor.getInt(4));
            detail.setReportReason(cursor.getString(5));

            // 获取时间字段（假设是字符串类型）
            String timeString = cursor.getString(6); // 读取时间字段
            Date commentDate = null;
            if (timeString != null) {
                try {
                    commentDate = dateFormat.parse(timeString); // 解析时间字符串
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 设置解析后的时间
            detail.setCommentTime(commentDate);

            detail.setUserName(cursor.getString(7));
            detail.setBookTitle(cursor.getString(8));

            comments.add(detail);
        }
        cursor.close();
        return comments;
    }



    /**
     * 删除评论
     * @param commentId 评论ID
     */
    public boolean deleteComment(int commentId) {
        SQLiteDatabase db = new MyDB(context).getWritableDatabase();
        return db.delete("comments", "comment_id=?",
                new String[]{String.valueOf(commentId)}) > 0;
    }

    /**
     * 更新评论状态
     * @param commentId 评论ID
     * @param newStatus 新状态值
     */
    public boolean updateCommentStatus(int commentId, int newStatus) {
        SQLiteDatabase db = new MyDB(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", newStatus);
        return db.update("comments", values,
                "comment_id=?", new String[]{String.valueOf(commentId)}) > 0;
    }

    /**
     * 添加新用户
     * @param user 用户对象
     * @return 是否成功
     */
    public boolean insertUser(UserBean user) {
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 检查用户名是否存在
        if (isUsernameExists(user.getUname())) {
            return false;
        }

        values.put("uname", user.getUname());
        values.put("password", user.getPassword());
        values.put("headimg", user.getHeadimg());
        values.put("question", user.getQuestion());
        values.put("answer", user.getAnswer());
        values.put("age", user.getAge());

        long result = db.insert("user", null, values);
        db.close();
        return result != -1;
    }

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 是否成功
     */
    public boolean updateUser(UserBean user) {
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("uname", user.getUname());
        values.put("password", user.getPassword());
        values.put("headimg", user.getHeadimg());
        values.put("question", user.getQuestion());
        values.put("answer", user.getAnswer());
        values.put("age", user.getAge());

        int result = db.update("user", values,
                "uid=?", new String[]{String.valueOf(user.getUid())});
        db.close();
        return result > 0;
    }

    /**
     * 检查用户名是否存在
     */
    boolean isUsernameExists(String username) {
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT uid FROM user WHERE uname=?",
                new String[]{username}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 获取所有用户
     */
    public List<UserBean> getAllUsers() {
        List<UserBean> users = new ArrayList<>();
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM user", null);

        while (cursor.moveToNext()) {
            UserBean user = new UserBean(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
            );
            users.add(user);
        }
        cursor.close();
        return users;
    }

    /**
     * 根据ID获取用户
     */
    public UserBean getUserById(int uid) {
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM user WHERE uid=?",
                new String[]{String.valueOf(uid)}
        );

        UserBean user = null;
        if (cursor.moveToFirst()) {
            user = new UserBean(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
            );
        }
        cursor.close();
        return user;
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(int uid) {
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("user", "uid=?",
                new String[]{String.valueOf(uid)});
        db.close();
        return result > 0;
    }

    /**
     * 搜索用户
     */
    public List<UserBean> searchUsers(String keyword) {
        List<UserBean> users = new ArrayList<>();
        MyDB dbHelper = new MyDB(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM user WHERE uname LIKE ? OR uid = ?",
                new String[]{"%" + keyword + "%", keyword}
        );

        while (cursor.moveToNext()) {
            UserBean user = new UserBean(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6)
            );
            users.add(user);
        }
        cursor.close();
        return users;
    }

    // 新增更新阅读量的方法
    public void incrementViewCount(int bookId) {
        String sql = "UPDATE books SET view_count = view_count + 1 WHERE book_id = ?";
        db.execSQL(sql, new Object[]{bookId});
    }

    // 书籍操作
    public long insertBook(BookBean book) {
        ContentValues values = new ContentValues();
        values.put("title", book.getTitle());
        values.put("author", book.getAuthor());
        values.put("category_id", book.getCategoryId());
        if(book.getCoverPath()==null||book.getCoverPath().equals(""))
        {
            book.setCoverPath("/data/user/0/com.example.readapp/files/ic_books.jpg");
            values.put("cover_path", book.getCoverPath());
        }else{
            values.put("cover_path", book.getCoverPath());
        }
        values.put("file_path", book.getFilePath());
        values.put("description", book.getDescription());
        values.put("is_recommendation", book.getIs_Recommendation());
        return db.insert("books", null, values);
    }

    public int updateBook(BookBean book) {
        ContentValues values = new ContentValues();
        values.put("title", book.getTitle());
        values.put("author", book.getAuthor());
        values.put("category_id", book.getCategoryId());
        if(book.getCoverPath()==null||book.getCoverPath().equals(""))
        {
            book.setCoverPath("/data/user/0/com.example.readapp/files/ic_books.jpg");
        }else{
            values.put("cover_path", book.getCoverPath());
        }
        values.put("file_path", book.getFilePath());
        values.put("description", book.getDescription());
        values.put("is_recommendation", book.getIs_Recommendation());
        return db.update("books", values, "book_id=?",
                new String[]{String.valueOf(book.getBookId())});
    }

    public void deleteBook(int bookId) {
        db.delete("books", "book_id=?", new String[]{String.valueOf(bookId)});
    }

    // 章节操作
    public long insertChapter(ChapterBean chapter) {
        ContentValues values = new ContentValues();
        values.put("book_id", chapter.getBookId());
        values.put("chapter_number", chapter.getChapterNumber());
        values.put("chapter_title", chapter.getChapterTitle());
        values.put("content", chapter.getContent());
        return db.insert("chapters", null, values);
    }

    public List<ChapterBean> getChaptersByBook(int bookId) {
        List<ChapterBean> chapters = new ArrayList<>();
        Cursor cursor = db.query("chapters", null, "book_id=?",
                new String[]{String.valueOf(bookId)}, null, null, "chapter_number ASC");

        while (cursor.moveToNext()) {
            ChapterBean chapter = new ChapterBean();
            chapter.setChapterId(cursor.getInt(0));
            chapter.setBookId(cursor.getInt(1));
            chapter.setChapterNumber(cursor.getInt(2));
            chapter.setChapterTitle(cursor.getString(3));
            chapter.setContent(cursor.getString(4));
            chapters.add(chapter);
        }
        cursor.close();
        return chapters;
    }

    //更新推荐
    public void updateRecommendStatus(int bookId, int recommend) {
        ContentValues values = new ContentValues();
        boolean recommend1;
        if(recommend == 1)
        {
            recommend1=true;
        }
        else
        {
            recommend1=false;
        }
        values.put("is_recommendation", recommend1 ? 1 : 0);
        db.update("books", values, "book_id=?", new String[]{String.valueOf(bookId)});
    }

    // 搜索书籍
    // 在 MyDBUtils.java 中添加以下方法
    public List<BookBean> searchBooks(String keyword) {
        List<BookBean> result = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM books WHERE title LIKE ? OR author LIKE ?",
                new String[]{"%" + keyword + "%", "%" + keyword + "%"}
        );

        while (cursor.moveToNext()) {
            BookBean book = new BookBean();
            book.setBookId(cursor.getInt(cursor.getColumnIndex("book_id")));
            book.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            book.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
            book.setCategoryId(cursor.getInt(cursor.getColumnIndex("category_id")));
            book.setCoverPath(cursor.getString(cursor.getColumnIndex("cover_path")));
            book.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
            book.setDescription(cursor.getString(cursor.getColumnIndex("description")));
            book.setUploadTime(cursor.getString(cursor.getColumnIndex("upload_time")));
            book.setIs_Recommendation(cursor.getInt(cursor.getColumnIndex("is_recommendation")));
            book.setChapterCount(cursor.getInt(cursor.getColumnIndex("chapter_count")));
            book.setWordCount(cursor.getInt(cursor.getColumnIndex("word_count")));
            result.add(book);
        }
        cursor.close();
        return result;
    }

    // 获取所有书籍
    public List<BookBean> getAllBooks() {
        List<BookBean> books = new ArrayList<>();
        Cursor cursor = db.query("books", null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            BookBean book = new BookBean();
            book.setBookId(cursor.getInt(cursor.getColumnIndex("book_id")));
            book.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            book.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
            book.setCategoryId(cursor.getInt(cursor.getColumnIndex("category_id")));
            book.setCoverPath(cursor.getString(cursor.getColumnIndex("cover_path")));
            book.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
            book.setDescription(cursor.getString(cursor.getColumnIndex("description")));
            book.setUploadTime(cursor.getString(cursor.getColumnIndex("upload_time")));
            book.setIs_Recommendation(cursor.getInt(cursor.getColumnIndex("is_recommendation")));


            books.add(book);
        }
        cursor.close();
        return books;
    }

    // 根据分类ID获取分类名称
    public String getCategoryName(int categoryId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("categories",
                new String[]{"category_name"},
                "category_id=?",
                new String[]{String.valueOf(categoryId)},
                null, null, null);

        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }

    /**
     * 获取所有分类
     */
    public List<CategoryBean> getAllCategories() {
        List<CategoryBean> categories = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("categories", null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            CategoryBean category = new CategoryBean();
            category.setCategoryId(cursor.getInt(cursor.getColumnIndex("category_id")));
            category.setCategoryName(cursor.getString(cursor.getColumnIndex("category_name")));
            categories.add(category);
        }
        cursor.close();
        return categories;
    }


    public SQLiteDatabase getWritableDatabase() {
        return db;
    }
    public SQLiteDatabase getReadableDatabase() {
        return db;
    }


    // 获取分类ID
    public int getCategoryIdByName(String categoryName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("categories",
                new String[]{"category_id"},
                "category_name=?",
                new String[]{categoryName},
                null, null, null);

        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    //章节号校验
    public boolean isChapterExist(int bookId, int chapterNumber) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("chapters",
                new String[]{"chapter_id"},
                "book_id=? AND chapter_number=?",
                new String[]{String.valueOf(bookId), String.valueOf(chapterNumber)},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    public void batchInsertChaptersWithStats(long bookId, List<ChapterBean> chapters) {
        SQLiteDatabase db = getWritableDatabase();

        // 使用事务提升性能
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            for (ChapterBean chapter : chapters) {
                values.clear();
                values.put("book_id", bookId);
                values.put("chapter_number", chapter.getChapterNumber());
                values.put("chapter_title",
                        TextUtils.isEmpty(chapter.getChapterTitle()) ?
                                "第"+chapter.getChapterNumber()+"章" :
                                chapter.getChapterTitle());
                values.put("content", chapter.getContent());
                values.put("word_count", chapter.getWordCount()); // 存储章节字数

                db.insert("chapters", null, values);

                // 每100章提交一次事务防止内存过大
                if (chapter.getChapterNumber() % 100 == 0) {
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    db.beginTransaction();
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // 更新书籍统计信息
        updateBookStatistics(bookId);
    }

    /**
     * 更新书籍总字数和章节数
     */
    private void updateBookStatistics(long bookId) {
        SQLiteDatabase db = getWritableDatabase();

        // 统计章节总数和总字数
        String query = "SELECT COUNT(*) AS total_chapters, " +
                "SUM(word_count) AS total_words " +
                "FROM chapters WHERE book_id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(bookId)})) {
            if (cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put("chapter_count", cursor.getInt(0));
                values.put("word_count", cursor.getInt(1));

                db.update("books",
                        values,
                        "book_id = ?",
                        new String[]{String.valueOf(bookId)});
            }
        }
    }

    public BookBean getBookById(int bookId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("books", null,
                "book_id=?", new String[]{String.valueOf(bookId)}, null, null, null);

        if (cursor.moveToFirst()) {
            BookBean book = new BookBean();
            book.setBookId(cursor.getInt(0));
            book.setTitle(cursor.getString(1));
            book.setAuthor(cursor.getString(2));
            book.setCategoryId(cursor.getInt(3));
            book.setCoverPath(cursor.getString(4));
            book.setFilePath(cursor.getString(5));
            book.setDescription(cursor.getString(6));
            return book;
        }
        return null;
    }

    /** 根据ID获取章节 */
    public ChapterBean getChapterById(int chapterId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM chapters WHERE chapter_id = ?",
                new String[]{String.valueOf(chapterId)}
        );

        ChapterBean chapter = null;
        if (cursor.moveToFirst()) {
            chapter = new ChapterBean();
            chapter.setChapterId(cursor.getInt(0));
            chapter.setBookId(cursor.getInt(1));
            chapter.setChapterNumber(cursor.getInt(2));
            chapter.setChapterTitle(cursor.getString(3));
            chapter.setContent(cursor.getString(4));
        }
        cursor.close();
        return chapter;
    }

    /** 获取前一章ID */
    public int getPreviousChapterId(int currentChapterId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT chapter_id FROM chapters WHERE book_id = " +
                        "(SELECT book_id FROM chapters WHERE chapter_id = ?) " +
                        "AND chapter_number < (SELECT chapter_number FROM chapters WHERE chapter_id = ?) " +
                        "ORDER BY chapter_number DESC LIMIT 1",
                new String[]{String.valueOf(currentChapterId), String.valueOf(currentChapterId)}
        );

        int result = cursor.moveToFirst() ? cursor.getInt(0) : -1;
        cursor.close();
        return result;
    }

    /** 获取下一章ID */
    public int getNextChapterId(int currentChapterId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT chapter_id FROM chapters WHERE book_id = " +
                        "(SELECT book_id FROM chapters WHERE chapter_id = ?) " +
                        "AND chapter_number > (SELECT chapter_number FROM chapters WHERE chapter_id = ?) " +
                        "ORDER BY chapter_number ASC LIMIT 1",
                new String[]{String.valueOf(currentChapterId), String.valueOf(currentChapterId)}
        );

        int result = cursor.moveToFirst() ? cursor.getInt(0) : -1;
        cursor.close();
        return result;
    }

    /** 更新章节内容 */
    public void updateChapterContent(int chapterId, String newContent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("content", newContent);
        db.update("chapters", values, "chapter_id = ?", new String[]{String.valueOf(chapterId)});
    }

    /** 获取阅读进度 */
    public float getReadingProgress(int userId, int bookId, int chapterId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT progress FROM reading_history " +
                        "WHERE user_id = ? AND book_id = ? AND chapter_id = ? " +
                        "ORDER BY last_read DESC LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(bookId), String.valueOf(chapterId)}
        );

        float progress = cursor.moveToFirst() ? cursor.getFloat(0) : 0;
        cursor.close();
        return progress;
    }


    /**
     * 通用书籍查询方法
     * @param sql 查询语句
     * @param selectionArgs 参数数组
     * @return 书籍列表
     */
    private List<BookBean> queryBooks(String sql, String[] selectionArgs) {
        List<BookBean> books = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(sql, selectionArgs);
            while (cursor.moveToNext()) {
                BookBean book = new BookBean();
                book.setBookId(cursor.getInt(cursor.getColumnIndex("book_id")));
                book.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                book.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
                book.setCategoryId(cursor.getInt(cursor.getColumnIndex("category_id")));
                book.setCoverPath(cursor.getString(cursor.getColumnIndex("cover_path")));
                book.setFilePath(cursor.getString(cursor.getColumnIndex("file_path")));
                book.setDescription(cursor.getString(cursor.getColumnIndex("description")));
                book.setViewCount(cursor.getInt(cursor.getColumnIndex("view_count")));
                books.add(book);
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "queryBooks error", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return books;
    }

    /**
     * 获取推荐书籍
     */
    public List<BookBean> getRecommendedBooks(int limit) {
        String sql = "SELECT * FROM books WHERE is_recommendation = 1 ORDER BY RANDOM() LIMIT ?";
        return queryBooks(sql, new String[]{String.valueOf(limit)});
    }

    /**
     * 获取热门书籍
     */
    public List<BookBean> getPopularBooks(int limit) {
        String sql = "SELECT b.*, COUNT(r.book_id) as read_count " +
                "FROM books b LEFT JOIN reading_history r ON b.book_id = r.book_id " +
                "GROUP BY b.book_id ORDER BY read_count DESC LIMIT ?";
        return queryBooks(sql, new String[]{String.valueOf(limit)});
    }


    /**
     * 检查是否在书架
     */
    public boolean isBookInShelf(int userId, int bookId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT shelf_id FROM bookshelves WHERE user_id = ? AND book_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(bookId)}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 添加到书架
     */
    public void addToShelf(int userId, int bookId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("book_id", bookId);
        db.insertWithOnConflict(
                "bookshelves",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    /**
     * 删除书架
     */
    public void deleteFromShelf(int userId, int bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(
                "bookshelves",
                "user_id = ? AND book_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(bookId)}
        );
    }

    /**
     * 从书架移除书籍
     * @param userId 用户ID
     * @param bookId 书籍ID
     */
    public void removeFromShelf(int userId, int bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("bookshelves",
                "user_id = ? AND book_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(bookId)});
    }
    //通过用户id回去用户名字
    public String getUserName(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT uname FROM user WHERE uid = ?",
                new String[]{String.valueOf(userId)}
        );

        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }


    /**
     * 获取热门评论（添加空结果处理）
     */
    public List<Comment> getHotComments(int bookId, int limit) {
        List<Comment> comments = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM comments WHERE book_id = ? AND (status = 1 OR status = 0) ORDER BY comment_time DESC LIMIT ?",
                    new String[]{String.valueOf(bookId), String.valueOf(limit)}
            );

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            while (cursor.moveToNext()) {
                Comment comment = new Comment();
                comment.setCommentId(cursor.getInt(0));
                comment.setUserId(cursor.getInt(1));
                comment.setBookId(cursor.getInt(2));
                comment.setContent(cursor.getString(3));
                comment.setStatus(cursor.getInt(4));

                // 获取时间字符串并转换为 Date 对象
                String timeString = cursor.getString(6);  // 获取存储的时间字符串
                if (timeString != null && !timeString.isEmpty()) {
                    try {
                        Date date = dateFormat.parse(timeString);  // 解析时间字符串为 Date 对象
                        if (date != null) {
                            comment.setTime(date);  // 设置评论时间
                        } else {
                            Log.e("DB", "解析时间失败，返回 null 日期：" + timeString);
                        }
                    } catch (ParseException e) {
                        Log.e("DB", "时间解析失败，异常信息：", e);
                    }
                } else {
                    Log.e("DB", "时间字符串为空或无效：" + timeString);
                }

                comments.add(comment);
            }
        } catch (Exception e) {
            Log.e("DB", "获取评论失败", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return comments;
    }



    /**
     * 获取随机书籍
     */
    public List<BookBean> getRandomBooks(int limit) {
        List<BookBean> books = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM books ORDER BY RANDOM() LIMIT ?",
                new String[]{String.valueOf(limit)}
        );

        while (cursor.moveToNext()) {
            BookBean book = parseBookFromCursor(cursor);
            books.add(book);
        }
        cursor.close();
        return books;
    }

    /**
     * 举报评论
     */
    public void reportComment(int commentId, String reason) {
        ContentValues values = new ContentValues();
        values.put("status", 0); // 状态改为被举报
        values.put("report_reason", reason);
        db.update("comments", values, "comment_id = ?",
                new String[]{String.valueOf(commentId)});
    }

    /**
     * 解析书籍数据（公共方法）
     */
    private BookBean parseBookFromCursor(Cursor cursor) {
        BookBean book = new BookBean();
        book.setBookId(cursor.getInt(0));
        book.setTitle(cursor.getString(1));
        book.setAuthor(cursor.getString(2));
        book.setCategoryId(cursor.getInt(3));
        book.setCoverPath(cursor.getString(4));
        book.setFilePath(cursor.getString(5));
        book.setDescription(cursor.getString(6));
        book.setViewCount(cursor.getInt(11));
        return book;
    }

    /**
     * 插入新评论
     * @param comment 评论对象
     */
    public void insertComment(Comment comment) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", comment.getUserId());
        values.put("book_id", comment.getBookId());
        values.put("content", comment.getContent());
        values.put("status", comment.getStatus());
        // 获取当前时间并格式化为字符串
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedTime = dateFormat.format(new Date());  // 获取当前时间并格式化

        values.put("comment_time", formattedTime);  // 插入格式化后的时间字符串

        db.insert("comments", null, values);
    }

    /**
     * 获取书架书籍（带分页）
     */
    public List<BookBean> getBookshelfBooks(int userId) {
        String sql = "SELECT b.* FROM books b " +
                "INNER JOIN bookshelves s ON b.book_id = s.book_id " +
                "WHERE s.user_id = ? " +
                "ORDER BY s.add_time DESC";
        return queryBooks(sql, new String[]{String.valueOf(userId)});
    }

    //获得章节数
    public int getChapterCount(int bookId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM chapters WHERE book_id = ?",
                new String[]{String.valueOf(bookId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    //获得最近历史
    public int getLastReadChapter(int userId, int bookId) {
        SQLiteDatabase db = getReadableDatabase();
        String query =
                "SELECT chapter_id FROM reading_history " +
                        "WHERE user_id = ? AND book_id = ? " +
                        "ORDER BY last_read DESC LIMIT 1";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(bookId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        }
    }

    //更新阅读历史
    public void updateReadingProgress(int userId, int bookId, int chapterId, float progress) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("book_id", bookId);
        values.put("chapter_id", chapterId);
        values.put("progress", progress);
        values.put("last_read", System.currentTimeMillis());

        // 使用 REPLACE 策略确保唯一记录
        db.insertWithOnConflict(
                "reading_history",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    /**
     * 更新单个章节字数
     * @param chapterId 章节ID
     * @param wordCount 新的字数
     */
    public void updateChapterWordCount(int chapterId, int wordCount) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("word_count", wordCount);
        db.update("chapters",
                values,
                "chapter_id = ?",
                new String[]{String.valueOf(chapterId)});
    }

    /**
     * 更新书籍总字数（包含事务处理）
     * @param bookId 书籍ID
     */
    public void updateBookWordCount(int bookId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // 计算总字数
            String sumQuery = "SELECT SUM(word_count) FROM chapters WHERE book_id = ?";
            Cursor cursor = db.rawQuery(sumQuery, new String[]{String.valueOf(bookId)});

            int totalWords = 0;
            if (cursor.moveToFirst()) {
                totalWords = cursor.getInt(0);
            }
            cursor.close();

            // 更新书籍表
            ContentValues values = new ContentValues();
            values.put("word_count", totalWords);
            db.update("books",
                    values,
                    "book_id = ?",
                    new String[]{String.valueOf(bookId)});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 获取带书籍数量的分类列表
     */
    public List<CategoryBean> getAllCategoriesWithCount() {
        List<CategoryBean> categories = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT c.category_id, c.category_name, COUNT(b.book_id) as book_count " +
                "FROM categories c LEFT JOIN books b ON c.category_id = b.category_id " +
                "GROUP BY c.category_id";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                CategoryBean category = new CategoryBean();
                category.setCategoryId(cursor.getInt(0));
                category.setCategoryName(cursor.getString(1));
                category.setBookCount(cursor.getInt(2));
                categories.add(category);
            }
        }
        return categories;
    }

    // 根据分类获取书籍
    public List<BookBean> getBooksByCategory(int categoryId) {
        List<BookBean> books = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query("books",
                null,
                "category_id = ?",
                new String[]{String.valueOf(categoryId)},
                null, null, null);

        while (cursor.moveToNext()) {
            BookBean book = newparseBookFromCursor(cursor);
            books.add(book);
        }
        cursor.close();
        return books;
    }

    // 获取推荐书籍（示例逻辑）
    public List<BookBean> getRecommendBooks() {
        List<BookBean> books = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // 示例：获取推荐标志为1的书籍
        Cursor cursor = db.query("books",
                null,
                "is_recommendation = 1",
                null, null, null,
                "RANDOM() LIMIT 10"); // 随机取10本

        while (cursor.moveToNext()) {
            BookBean book = newparseBookFromCursor(cursor);
            books.add(book);
        }
        cursor.close();
        return books;
    }

    // 解析书籍数据的公共方法
    private BookBean newparseBookFromCursor(Cursor cursor) {
        BookBean book = new BookBean();
        book.setBookId(cursor.getInt(cursor.getColumnIndex("book_id")));
        book.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        book.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
        book.setCoverPath(cursor.getString(cursor.getColumnIndex("cover_path")));
        book.setDescription(cursor.getString(cursor.getColumnIndex("description")));
        return book;
    }

    public List<ReadingRecord> getUserReadingRecords(int userId) {
        List<ReadingRecord> records = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // 获取每个书籍的最新记录
        String query = "SELECT rh.*, " +
                "b.title AS book_title, " +
                "b.cover_path AS book_cover, " +
                "b.author, " +
                "c.chapter_number " +
                "FROM reading_history rh " +
                "INNER JOIN (" +
                "   SELECT book_id, MAX(last_read) AS max_time " +
                "   FROM reading_history " +
                "   WHERE user_id = ? " +
                "   GROUP BY book_id" +
                ") latest ON rh.book_id = latest.book_id AND rh.last_read = latest.max_time " +
                "JOIN books b ON rh.book_id = b.book_id " +
                "JOIN chapters c ON rh.chapter_id = c.chapter_id " +
                "WHERE rh.user_id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(userId)});

        while (cursor.moveToNext()) {
            ReadingRecord record = new ReadingRecord();
            record.setRecordId(cursor.getInt(cursor.getColumnIndex("record_id")));
            record.setBookId(cursor.getInt(cursor.getColumnIndex("book_id")));
            record.setChapterId(cursor.getInt(cursor.getColumnIndex("chapter_id")));
            record.setBookTitle(cursor.getString(cursor.getColumnIndex("book_title")));
            record.setBookCover(cursor.getString(cursor.getColumnIndex("book_cover")));
            record.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
            record.setChapterNumber(cursor.getInt(cursor.getColumnIndex("chapter_number")));
            record.setProgress(cursor.getFloat(cursor.getColumnIndex("progress")));
            record.setLastReadTime(cursor.getString(cursor.getColumnIndex("last_read")));
            records.add(record);
        }
        cursor.close();
        return records;
    }

    public boolean deleteReadingRecord(int recordId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            int rows = db.delete("reading_history",
                    "record_id = ?",
                    new String[]{String.valueOf(recordId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e("DB_ERROR", "删除记录失败", e);
            return false;
        }
    }

    // 智能推荐功能
    public List<BookBean> getSmartRecommendations(int userId, int recommendCount) {
        List<BookBean> finalRecommend = new ArrayList<>();
        Set<Integer> usedBookIds = new HashSet<>();

        try {
            // 第一阶段：基于用户偏好的推荐
            List<BookBean> baseRecommend = getBaseRecommendations(userId, recommendCount);
            addToRecommendation(baseRecommend, finalRecommend, usedBookIds);

            // 第二阶段：补足推荐
            if (finalRecommend.size() < recommendCount) {
                int remaining = recommendCount - finalRecommend.size();
                List<BookBean> supplement = getRandomBooksExclude(remaining, usedBookIds);
                addToRecommendation(supplement, finalRecommend, usedBookIds);
            }

            // 第三阶段：最终兜底
            if (finalRecommend.size() < recommendCount) {
                int remaining = recommendCount - finalRecommend.size();
                List<BookBean> lastResort = getRandomBooks(remaining);
                addLastResort(lastResort, finalRecommend, usedBookIds, remaining);
            }

            // 随机打乱顺序
            Collections.shuffle(finalRecommend);
            return finalRecommend.subList(0, Math.min(recommendCount, finalRecommend.size()));
        } finally {
            // 清理资源（保持连接不关闭）
        }
    }

    private void addToRecommendation(List<BookBean> source, List<BookBean> target, Set<Integer> usedIds) {
        for (BookBean book : source) {
            if (!usedIds.contains(book.getBookId())) {
                target.add(book);
                usedIds.add(book.getBookId());
            }
        }
    }

    private void addLastResort(List<BookBean> source, List<BookBean> target,
                               Set<Integer> usedIds, int remaining) {
        int added = 0;
        for (BookBean book : source) {
            if (added >= remaining) {
                break;
            }
            if (!usedIds.contains(book.getBookId())) {
                target.add(book);
                usedIds.add(book.getBookId());
                added++;
            }
        }
    }

    /**
     * 基础推荐查询（完整实现）
     */
    private List<BookBean> getBaseRecommendations(int userId, int limit) {
        String query =
                "SELECT main.* FROM books main " +
                        "JOIN (" +
                        "  SELECT " +
                        "    b.category_id, " +
                        "    COUNT(*) * 1.0 / total.total_count AS weight, " +
                        "    b.book_id " +
                        "  FROM bookshelves s " +
                        "  JOIN books b ON s.book_id = b.book_id " +
                        "  JOIN (" +
                        "    SELECT COUNT(*) AS total_count " +
                        "    FROM bookshelves " +
                        "    WHERE user_id = ?" +
                        "  ) total " +
                        "  WHERE s.user_id = ? " +
                        "  GROUP BY b.category_id " +
                        ") cw ON main.category_id = cw.category_id " +
                        "WHERE main.book_id IN (" +
                        "  SELECT sub.book_id FROM (" +
                        "    SELECT " +
                        "      book_id, " +
                        buildWordCountCase("word_count") + " AS wc_group, " +
                        buildChapterCase("chapter_count") + " AS ch_group " +
                        "    FROM books" +
                        "    WHERE category_id = cw.category_id" +
                        "    GROUP BY wc_group, ch_group" +
                        "    ORDER BY RANDOM()" +
                        "  ) sub" +
                        ") " +
                        "ORDER BY cw.weight DESC " +
                        "LIMIT ?";

        return executeBookQuery(query,
                new String[]{String.valueOf(userId), String.valueOf(userId), String.valueOf(limit)});
    }

    // 构建带列名的条件语句
    private String buildWordCountCase(String column) {
        return "CASE " +
                "WHEN " + column + " < 100000 THEN 1 " +
                "WHEN " + column + " BETWEEN 100000 AND 500000 THEN 2 " +
                "WHEN " + column + " BETWEEN 500001 AND 1000000 THEN 3 " +
                "WHEN " + column + " BETWEEN 1000001 AND 2000000 THEN 4 " +
                "ELSE 5 END";
    }

    private String buildChapterCase(String column) {
        return "CASE " +
                "WHEN " + column + " <= 50 THEN 1 " +
                "WHEN " + column + " BETWEEN 51 AND 100 THEN 2 " +
                "WHEN " + column + " BETWEEN 101 AND 300 THEN 3 " +
                "WHEN " + column + " BETWEEN 301 AND 700 THEN 4 " +
                "WHEN " + column + " BETWEEN 701 AND 1200 THEN 5 " +
                "ELSE 6 END";
    }

    /**
     * 获取随机书籍（排除已选）
     */
    private List<BookBean> getRandomBooksExclude(int limit, Set<Integer> excludeIds) {
        if (excludeIds.isEmpty()) {
            return getRandomBooks(limit);
        }

        String[] params = new String[excludeIds.size() + 1];
        int i = 0;
        for (Integer id : excludeIds) {
            params[i++] = String.valueOf(id);
        }
        params[i] = String.valueOf(limit);

        String query =
                "SELECT book_id, title, author, view_count, category_id, " +
                        "cover_path, file_path, description, " +
                        "word_count, chapter_count " +
                        "FROM books " +
                        "WHERE book_id NOT IN (" + getPlaceholders(excludeIds.size()) + ") " +
                        "ORDER BY RANDOM() LIMIT ?";

        return executeBookQuery(query, params);
    }

    /**
     * 通用查询执行方法
     */
    private List<BookBean> executeBookQuery(String query, String[] params) {
        List<BookBean> books = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(query, params);
            while (cursor.moveToNext()) {
                BookBean book = new BookBean();
                // 核心字段
                book.setBookId(getInt(cursor, "book_id"));
                book.setTitle(getString(cursor, "title"));
                book.setAuthor(getString(cursor, "author"));
                book.setCategoryId(getInt(cursor, "category_id"));

                // 文件路径
                book.setCoverPath(getString(cursor, "cover_path"));
                book.setFilePath(getString(cursor, "file_path"));

                // 描述信息
                book.setDescription(getString(cursor, "description"));

                // 统计信息
                book.setWordCount(getInt(cursor, "word_count"));
                book.setChapterCount(getInt(cursor, "chapter_count"));
                book.setViewCount(getInt(cursor, "view_count"));

                books.add(book);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return books;
    }

    // 辅助方法
    private String buildWordCountCase() {
        return "CASE " +
                "WHEN word_count < 100000 THEN 1 " +
                "WHEN word_count BETWEEN 100000 AND 500000 THEN 2 " +
                "WHEN word_count BETWEEN 500001 AND 1000000 THEN 3 " +
                "WHEN word_count BETWEEN 1000001 AND 2000000 THEN 4 " +
                "ELSE 5 END";
    }

    private String buildChapterCase() {
        return "CASE " +
                "WHEN chapter_count <= 50 THEN 1 " +
                "WHEN chapter_count BETWEEN 51 AND 100 THEN 2 " +
                "WHEN chapter_count BETWEEN 101 AND 300 THEN 3 " +
                "WHEN chapter_count BETWEEN 301 AND 700 THEN 4 " +
                "WHEN chapter_count BETWEEN 701 AND 1200 THEN 5 " +
                "ELSE 6 END";
    }

    private String getPlaceholders(int count) {
        return TextUtils.join(",", Collections.nCopies(count, "?"));
    }

    private int getInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndex(column));
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndex(column));
    }

    /**
     * 获取书籍总数（兜底判断）
     */
    public int getTotalBooksCount() {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM books", null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean updateUserName(int userId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uname", newName);

        int rows = db.update("user", values, "uid = ?", new String[]{String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    public boolean updateUserAge(int userId, String newAge) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("age", newAge);

        int rows = db.update("user", values, "uid = ?", new String[]{String.valueOf(userId)});
        db.close();
        return rows > 0;
    }

    //书籍统计

    public List<CategoryStat> getCategoryStats() {
        List<CategoryStat> stats = new ArrayList<>();
        String query = "SELECT c.category_name, SUM(b.view_count) AS total " +
                "FROM books b " +
                "JOIN categories c ON b.category_id = c.category_id " +
                "GROUP BY b.category_id " +
                "ORDER BY total DESC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            stats.add(new CategoryStat(
                    cursor.getString(0),
                    cursor.getInt(1)
            ));
        }
        cursor.close();
        db.close();
        return stats;
    }

    public List<BookStat> getTopBookStats(int limit) {
        List<BookStat> stats = new ArrayList<>();
        String query = "SELECT title, view_count FROM books ORDER BY view_count DESC LIMIT " + limit;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            stats.add(new BookStat(
                    cursor.getString(0),
                    cursor.getInt(1)
            ));
        }
        cursor.close();
        db.close();
        return stats;
    }


    /**
     * 根据分类获取书架书籍（使用通用查询方法）
     * @param userId 用户ID
     * @param categoryId 分类ID（-1表示全部）
     * @return 书籍列表
     */
    public List<BookBean> getBookshelfBooksByCategory(int userId, int categoryId) {
        // 基础查询语句
        String baseQuery = "SELECT b.* FROM bookshelves bs " +
                "JOIN books b ON bs.book_id = b.book_id " +
                "WHERE bs.user_id = ?";

        // 根据分类ID动态构建查询
        String sql;
        String[] selectionArgs;

        if (categoryId == -1) {
            // 查询全部分类
            sql = baseQuery;
            selectionArgs = new String[]{String.valueOf(userId)};
        } else {
            // 查询指定分类
            sql = baseQuery + " AND b.category_id = ?";
            selectionArgs = new String[]{String.valueOf(userId), String.valueOf(categoryId)};
        }

        // 使用通用查询方法
        return queryBooks(sql, selectionArgs);
    }

}
