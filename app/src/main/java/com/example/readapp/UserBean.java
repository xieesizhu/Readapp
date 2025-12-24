package com.example.readapp;

import java.io.Serializable;

public class UserBean implements Serializable {
    private int uid;        // 用户ID
    private String uname;   // 用户名
    private String password; // 密码
    private String headimg; // 头像路径
    private String question; // 安全问题
    private String answer; //安全答案
    private String age;

    // 无参构造方法
    public UserBean() {
    }

    // 带参构造方法
    public UserBean(int uid, String uname, String password, String headimg, String question, String answer ,String age) {
        this.uid = uid;
        this.uname = uname;
        this.password = password;
        this.headimg = headimg;
        this.question = question;
        this.answer = answer;
        this.age = age;
    }

    // Getter 和 Setter 方法
    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHeadimg() {
        return headimg;
    }

    public void setHeadimg(String headimg) {
        this.headimg = headimg;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
    public String getAge() {
        return age;
    }
    public void setAge(String age) {
        this.age = age;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
    // toString 方法，方便打印对象信息
    @Override
    public String toString() {
        return "UserBean{" +
                "uid=" + uid +
                ", uname='" + uname + '\'' +
                ", password='" + password + '\'' +
                ", headimg='" + headimg + '\'' +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}