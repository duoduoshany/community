package com.gongsi.community.service;

import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussPostService {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    public List<DiscussPost> getDiscussPost(int userId,int offset,int limit) {
        return discussPostMapper.selectDiscussPost(userId,offset,limit);
    }
    public int getDiscussPostCount(int userId) {
        return discussPostMapper.selectDiscussPostRows(userId);
    }
}
