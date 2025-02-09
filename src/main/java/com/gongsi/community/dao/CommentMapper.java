package com.gongsi.community.dao;

import com.gongsi.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> selectCommentsByEntity(int entity_type,int entity_id,int offset,int limit);
    //计算某类型某实体的评论的总的页数
    int selectCountByEntity(int entity_type,int entity_id);
    int insertComment(Comment comment);
    Comment selectCommentById(int id);
    Integer selectComment(int user_id, int entity_type, int entity_id, Date create_time);
}
