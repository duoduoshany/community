package com.gongsi.community;
import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.dao.UserMapper;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.entity.User;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
//测试代码为了引用CommunityApplication为配置类来自动创建Spring容器，标明以下注解
@ContextConfiguration(classes = CommunityApplication.class)

public class MapperTest {
    //实例化某类后将该Bean注入到某字段中，就不需要再写该类的具体实例化细节，还可以管理Bean的生命周期
    //要测的是UserMapper就把它注入进来
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Test
    public void testSelectUser()
    {
        //已经注入实例就可以调用方法
        User user=userMapper.getUserById(101);
        System.out.println(user);
        User user1=userMapper.getUserByName("liubei");
        System.out.println(user1);
        User user2=userMapper.getUserByEmail("nowcoder101@sina.com");
        System.out.println(user2);
    }
    @Test
    public void testInsertUser()
    {
        User user=new User();
        user.setUsername("刘亦菲");
        user.setPassword("123456");
        user.setEmail("liuyifei@126.com");
        user.setHeader_url("http://www.nowcoders.com/123.png");
        user.setSalt("123");
        user.setCreate_time(new Date());//当前时间
        //返回插入成功的值，1成功，0失败
        int result=userMapper.insertUser(user);
        System.out.println(result);
        System.out.println(user.getId());
    }
    //状态默认是0
    @Test
    public void updataUser()
    {
        int result=userMapper.updateStatus(151,1);
        System.out.println(result);
        int result1=userMapper.updateHeader(149,"http://www.nowcoders.com/123.png");
        System.out.println(result1);
        int result2= userMapper.updatePassword(148,"saxxdsasqs");
        System.out.println(result2);
    }
    @Test
    public void testSelectPost()
    {
        List<DiscussPost> list= discussPostMapper.selectDiscussPost(149,0,10);
        for(DiscussPost post:list)
        {
            System.out.println(post);
        }
        int result=discussPostMapper.selectDiscussPostRows(0);
        System.out.println(result);
    }
}
