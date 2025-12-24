package com.example.readapp;

import java.util.Date;

public class Comment {
    private int commentId;
    private int userId;
    private int bookId;
    private String content;
    private int status; // -2:屏蔽 -1:待审 0:举报 1:通过
    private String reportReason;
    private Date time;


    // 需要生成getter/setter方法
    // 需要生成构造方法
    public Comment(int commentId, int userId, int bookId, String content, int status, String reportReason, Date time) {
        this.commentId = commentId;
        this.userId = userId;
        this.bookId = bookId;
        this.content = content;
        this.status = status;
        this.reportReason = reportReason;
        this.time = time;
    }

    public Comment() {

    }

    public int getCommentId() {
        return commentId;
    }

    public int getUserId() {
        return userId;
    }

    public int getBookId() {
        return bookId;
    }

    public String getContent() {
        return content;
    }

    public int getStatus() {
        return status;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }
    public Date getTime() {
        return time;
    }
    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId=" + commentId +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", reportReason='" + reportReason + '\'' +
                '}';
    }
}