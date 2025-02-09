package com.gongsi.community.entity;

import java.util.Date;

public class Comment {
    private int id;
    private int user_id;
    private int entity_type;
    private int entity_id;
    private int target_id;//评论哪条评论，要匹配到发表评论的用户，好消息通知
    private String content;
    private int status;//评论一定要有状态
    private Date create_time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getEntity_type() {
        return entity_type;
    }

    public void setEntity_type(int entity_type) {
        this.entity_type = entity_type;
    }

    public int getEntity_id() {
        return entity_id;
    }

    public void setEntity_id(int entity_id) {
        this.entity_id = entity_id;
    }

    public int getTarget_id() {
        return target_id;
    }

    public void setTarget_id(int target_id) {
        this.target_id = target_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", entity_type=" + entity_type +
                ", entity_id=" + entity_id +
                ", target_id=" + target_id +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", create_time=" + create_time +
                '}';
    }
}
