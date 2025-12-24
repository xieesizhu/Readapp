package com.example.readapp;

import java.util.Date;

public class CommentDetail extends Comment {
    private String userName;
    private String bookTitle;
    private Date commentTime;

    public CommentDetail() {}

    // Getters and Setters
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public Date getCommentTime() { return commentTime; }
    public void setCommentTime(Date commentTime) { this.commentTime = commentTime; }
}