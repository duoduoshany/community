package com.gongsi.community.service;

import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    public List<DiscussPost> getDiscussPost(int user_id,int offset,int limit) {
        return discussPostMapper.selectDiscussPost(user_id,offset,limit);
    }
    public int getDiscussPostCount(int user_id) {
        return discussPostMapper.selectDiscussPostRows(user_id);
    }
    public int addDiscussPost(DiscussPost post) {
        //先判断用户传入的帖子是不是空的
        if(post==null)
        {
            throw new IllegalArgumentException("参数不能为空！");
        }
        //1.转义HTML标记，有可能用户写的文本包含类似于<script></script>的字符会被自动识别为网页
        //将这些特殊标记转化为文本
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //2.过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        //调用数据访问层的方法插入处理好的数据。
        return discussPostMapper.insertDiscussPost(post);
    }
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }
    public int updateCommentCount(int id,int commentCount)
    {
        return discussPostMapper.updateCommentCount(id,commentCount);
    }
}
