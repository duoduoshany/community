package com.gongsi.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private String topic;
    private int userId;//触发事件的人，比如触发点赞、评论、关注的人的id
    private int entityType;//事件发送在哪个主体上，比如哪条帖子、哪个用户、哪条评论
    private int entityId;
    private int entityUserId;//帖子/评论的作者id
    //事件对象应具有通用性，上面的属性只关联了点赞、评论、关注三种事件
    //将来可能还有别的事件也要处理，但我没办法预判还需要加什么属性
    //就加个map，把跟额外事件关联的属性都放到map中
    private Map<String,Object> data=new HashMap<>();

    public String getTopic() {
        return topic;
    }
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }//set完topic之后返回事件对象，又可以调用set方法继续set当前对象的其它属性
    public int getUserId() {
        return userId;
    }
    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }
    public int getEntityType() {
        return entityType;
    }
    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }
    public int getEntityId() {
        return entityId;
    }
    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }
    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }
    public Map<String, Object> getData() {
        return data;
    }
    //map传入键值对的数目是不确定的，有可能之后传3对，再之后传2对，只需要返回事件对象就可以多次调用setData传进多对键值对到map，
    //无限地往同一个map中传多个键值对
    public Event setData(String key,Object value) {
        this.data.put(key, value);//只传一对键值对放到map里
        return this;
    }
}
