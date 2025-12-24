package com.example.readapp;

import java.io.Serializable;

public class ChapterBean implements Serializable {
    private int chapter_id;
    private int book_id;
    private int chapter_number;
    private String chapter_title;
    private String content;
    private int word_count;


    // 无参构造方法
    public ChapterBean() {
    }

    // 带参构造方法

    public ChapterBean( int chapter_number, String chapter_title, String content) {
        this.chapter_id = chapter_id;
        this.book_id = book_id;
        this.chapter_number = chapter_number;
        this.chapter_title = chapter_title;
        this.content = content;
        this.word_count = word_count;
    }

    // Getter 和 Setter 方法

    public int getChapterId() {
        return chapter_id;
    }


    public void setChapterId(int chapter_id) {
        this.chapter_id = chapter_id;
    }


    public int getBookId() {
        return book_id;
    }


    public void setBookId(int book_id) {
        this.book_id = book_id;
    }


    public int getChapterNumber() {
        return chapter_number;
    }


    public void setChapterNumber(int chapter_number) {
        this.chapter_number = chapter_number;
    }


    public String getChapterTitle() {
        return chapter_title;
    }


    public void setChapterTitle(String chapter_title) {
        this.chapter_title = chapter_title;
    }


    public String getContent() {
        return content;
    }


    public void setContent(String content) {
        this.content = content;
    }

    public int getWordCount() {
        return word_count;
    }
    public void setWordCount(int word_count) {
        this.word_count = word_count;
    }
    @Override
    public String toString() {
        return "ChapterBean{" +
                "chapter_id=" + chapter_id +
                ", book_id=" + book_id +
                ", chapter_number=" + chapter_number +
                ", chapter_title='" + chapter_title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

}