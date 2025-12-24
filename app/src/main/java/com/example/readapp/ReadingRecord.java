package com.example.readapp;

public class ReadingRecord {
    private int recordId;
    private int bookId;
    private int chapterId;
    private String bookTitle;
    private String bookCover;
    private String author;
    private int chapterNumber;
    private float progress;
    private String lastReadTime;


    public ReadingRecord(int recordId, int bookId, int chapterId, String bookTitle, String bookCover, String author, int chapterNumber, float progress, String lastReadTime) {
        this.recordId = recordId;
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.bookTitle = bookTitle;
        this.bookCover = bookCover;

        this.author = author;
        this.chapterNumber = chapterNumber;
        this.progress = progress;
        this.lastReadTime = lastReadTime;
    }

    public ReadingRecord( )
    {

    }

    public int getRecordId() {
        return recordId;
    }


    public int getBookId() {
        return bookId;
    }


    public int getChapterId() {
        return chapterId;
    }


    public String getBookTitle() {
        return bookTitle;
    }


    public String getBookCover() {
        return bookCover;
    }


    public String getAuthor() {
        return author;
    }


    public int getChapterNumber() {
        return chapterNumber;
    }


    public float getProgress() {
        return progress;
    }


    public String getLastReadTime() {
        return lastReadTime;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }


    public void setBookId(int bookId) {
        this.bookId = bookId;
    }


    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }


    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }


    public void setBookCover(String bookCover) {
        this.bookCover = bookCover;
    }


    public void setAuthor(String author) {
        this.author = author;
    }


    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }


    public void setProgress(float progress) {
        this.progress = progress;
    }


    public void setLastReadTime(String lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

}
