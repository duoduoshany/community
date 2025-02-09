package com.gongsi.community.service;

import com.gongsi.community.dao.CommentMapper;
import com.gongsi.community.entity.Comment;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.HostHolder;
import com.gongsi.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.Date;
import java.util.List;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;

    public List<Comment> findCommentsByEntity(int entity_type,int entity_id,int offset,int limit){
        return commentMapper.selectCommentsByEntity(entity_type,entity_id,offset,limit);
    }
    public int findCommentCount(int entity_type,int entity_id){
        return commentMapper.selectCountByEntity(entity_type, entity_id);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        //注意先判断用户提交的评论是否为空
        if(comment==null)
        {
            throw new IllegalArgumentException("参数不能为空！");
        }//为空，封装起来提醒用户,抛出异常后程序后面的代码不会执行

        //2.对评论处理：依然是html标记转义和敏感词过滤
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));

        //3.调用方法插入评论
        int rows=commentMapper.insertComment(comment);

        //前面的插入针对所有目标实体类型的评论

        //4.当类型是帖子才调用discussPostService方法更新帖子表的评论数量字段值
        //假设插入的评论的目标类型是评论，那这时候肯定不调用discussPostService的方法去更新评论的数量
        //传递更新后的数量给调用的方法，所以用commentMapper的方法查询而不是直接discusspost.comment_count
        if(comment.getEntity_type()==ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(comment.getEntity_type(), comment.getEntity_id());
            discussPostService.updateCommentCount(comment.getEntity_id(), count);
            //对目标插入，就对目标的数量更新
        }

        return rows;//返回插入的数据量
    }
    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
    public Integer findComment(int user_id,int entity_type,int entity_id,Date create_time){
        return commentMapper.selectComment(user_id,entity_type,entity_id,create_time);
    }
}
