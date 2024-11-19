package com.gongsi.community.dao;

import com.gongsi.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    //为了可以查看某个用户发布的所有帖子,当user_id不为零时可以加上user_id的查询条件，当为零时不加上，是一个动态sql
    List<DiscussPost> selectDiscussPost(int user_id,int offset,int limit);
    //考虑用户帖子需要分页的情形：offset用于指定从第几行数据开始读取，limit每页最多显示多少条数据

    //查询用户帖子一共可能有几页，需要两个参数：一个是一共有多少条数据，另一个是limit
    //@Param（""）用于给参数起别名,动态sql拼条件时，如果方法只有一个参数，那么这个参数必须起别名，如果有多个就可以不用
    int selectDiscussPostRows(@Param("user_id") int user_id);
}
