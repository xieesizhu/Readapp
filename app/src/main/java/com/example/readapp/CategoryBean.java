package com.example.readapp;

import java.io.Serializable;

public class CategoryBean implements Serializable {
    private int categoryId;   // 分类ID
    private String categoryName; // 分类名称
    private int BookCount; // 分类下的书籍数量

    // 无参构造方法
    public CategoryBean() {
    }

    // 带参构造方法
    public CategoryBean(int categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    // Getter 和 Setter 方法
    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getBookCount() {
        return BookCount;
    }

    public void setBookCount(int categoryCount) {
        this.BookCount = categoryCount;
    }

    // toString 方法
    @Override
    public String toString() {
        return categoryName;
    }
}