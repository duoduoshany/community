package com.gongsi.community.entity;

import java.util.Date;

public class Message {
    private int id;
    private int from_id;//消息的发送方id
    private int to_id;//消息的接收方id
    private String content;//消息内容
    private int status;//消息的状态，0未读，1已读，2删除
    private Date create_time;//消息的创建时间
    //用户111发给112和用户112发给111的消息在同一个会话，111_112,这就是会话id
    //引入会话id方便将消息分组，如一个会话中有很多消息
    private String conversation_id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFrom_id() {
        return from_id;
    }

    public void setFrom_id(int from_id) {
        this.from_id = from_id;
    }

    public int getTo_id() {
        return to_id;
    }

    public void setTo_id(int to_id) {
        this.to_id = to_id;
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

    public String getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(String conversation_id) {
        this.conversation_id = conversation_id;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", from_id=" + from_id +
                ", to_id=" + to_id +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", create_time=" + create_time +
                ", conversation_id='" + conversation_id + '\'' +
                '}';
    }
}
