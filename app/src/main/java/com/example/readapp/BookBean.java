package com.example.readapp;

import java.io.Serializable;

public class BookBean implements Serializable {
    private int bookId;        // 书籍ID
    private String title;      // 书籍标题
    private String author;     // 书籍作者
    private int categoryId;    // 分类ID
    private String coverPath;  // 封面路径
    private String filePath;   // 文件路径
    private String description; // 书籍描述
    private String uploadTime; // 上传时间
    private int is_Recommendation;  // 是否推荐
    private int viewCount;  // 新增阅读量字段
    private int wordCount;
    private int chapterCount;

    // 无参构造方法
    public BookBean() {
    }

    // 带参构造方法
    public BookBean(int bookId, String title, String author, int categoryId, String coverPath, String filePath, String description, String uploadTime, int is_Recommendation ,  int viewCount, int wordCount, int chapterCount) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.categoryId = categoryId;
        this.coverPath = coverPath;
        this.filePath = filePath;
        this.description = description;
        this.uploadTime = uploadTime;
        this.is_Recommendation = is_Recommendation;
        this.viewCount = viewCount;
        this.wordCount = wordCount;
        this.chapterCount = chapterCount;
    }

    // Getter 和 Setter 方法
    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getChapterCount() { return chapterCount; }
    public void setChapterCount(int chapterCount) { this.chapterCount = chapterCount; }


    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }
    public int getIs_Recommendation() {
        return is_Recommendation;
    }
    public void setIs_Recommendation(int is_Recommendation) {
        this.is_Recommendation = is_Recommendation;
    }

    @Override
    public String toString() {
        return "BookBean{" +
                "bookId=" + bookId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", categoryId=" + categoryId +
                ", coverPath='" + coverPath + '\'' +
                ", filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                ", uploadTime='" + uploadTime + '\'' +
                ", is_Recommendation=" + is_Recommendation +
                '}';
    }


}



