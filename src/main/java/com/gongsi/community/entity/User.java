package com.gongsi.community.entity;

import java.util.Date;

public class User {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getActivation_code() {
        return activation_code;
    }

    public void setActivation_code(String activation_code) {
        this.activation_code = activation_code;
    }

    public String getHeader_url() {
        return header_url;
    }

    public void setHeader_url(String header_url) {
        this.header_url = header_url;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    private int id;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", salt='" + salt + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", activation_code='" + activation_code + '\'' +
                ", header_url='" + header_url + '\'' +
                ", create_time=" + create_time +
                '}';
    }

    private String username;
    private String password;
    private String email;
    private String salt;
    private int type;
    private int status;
    private String activation_code;
    private String header_url;
    private Date create_time;



}
