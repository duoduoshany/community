package com.gongsi.community.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "discusspost",type="doc_",shards=6,replicas=3)
public class DiscussPost {
    @Id//属性与索引的_id字段映射，_id是文档的唯一标识符
    private int id;
    //Text 类型，用于 全文检索。
    //扩大文档被搜索到的范围，用拆分出最多的单词的分词器
    //搜索的时候用更精准的分词器：ik_smart
    @Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text,analyzer = "ik_max_word",searchAnalyzer = "ik_smart")
    private String content;
    @Field(type= FieldType.Integer)//属性与索引的普通字段映射，并指定字段类型为整型
    private int user_id;
    @Field(type= FieldType.Integer)
    private int type;
    @Field(type= FieldType.Integer)
    private int status;
    @Field(type= FieldType.Date)
    private Date create_time;
    @Field(type= FieldType.Integer)
    private int comment_count;
    @Field(type= FieldType.Double)
    private double score;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
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

    public Date getCreate_time() {
        return create_time;
    }

    public void setCreate_time(Date create_time) {
        this.create_time = create_time;
    }

    public int getComment_count() {
        return comment_count;
    }

    public void setComment_count(int comment_count) {
        this.comment_count = comment_count;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "DiscussPost{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", user_id=" + user_id +
                ", type=" + type +
                ", status=" + status +
                ", create_time=" + create_time +
                ", comment_count=" + comment_count +
                ", score=" + score +
                '}';
    }
}
