package com.gongsi.community.controller;

import com.gongsi.community.annotation.LoginRequired;
import com.gongsi.community.entity.Event;
import com.gongsi.community.entity.User;
import com.gongsi.community.event.EventProducer;
import com.gongsi.community.service.LikeService;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.HostHolder;
import com.gongsi.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
//点赞的时候也是异步请求，整个页面不刷新，点完之后就改变一下赞的标识即可
public class LikeController implements CommunityConstant {
    @Autowired
    private LikeService likeService;
    @Autowired
    private HostHolder hostHolder;//谁点赞，当前用户点赞，获取当前用户
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path="/like",method= RequestMethod.POST)
    @ResponseBody//异步请求统一加
    @LoginRequired//点赞的方法只有登录了才能点赞
    //给评论还是帖子点赞，这个参数是前端页面告知我们的
    public String Like(int entityType,int entityId,int entityUserId,int postId)
    {
        //先获取当前用户，不用判断用户是否为空是因为我已经拦截了没有登录的情况
        User user= hostHolder.getUser();
        //点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        //赞的数量要返回前端显示
        long likeCount=likeService.findEntityLikeCount(entityType,entityId);
        //赞的状态要返回前端显示
        int likeStatus= likeService.findEntityLikeStatus(user.getId(), entityType,entityId);

        //封装一下赞的数量和状态方便返回前端显示,封装一般用map封装，键自定义，值是所求结果
        Map<String,Object> map=new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);
        //传递赞的状态和数量即可（点赞这件事情会影响赞的状态）
        //要的是json，调用自定义工具类方法把map转成json格式并返回
        if(likeStatus==1){
            //触发事件也就是构造event
            Event event=new Event();
            event.setTopic(TOPIC_LIKE)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityUserId(entityUserId)//likeService既加入用户id到点赞实体对应的集合，又要将该点赞实体的作者的点赞数+1
                    .setData("postId",postId);//有可能点赞的是帖子，所以要传帖子id方便查帖子详情
            eventProducer.fireEvent(event);
        }
        if(entityType==ENTITY_TYPE_POST){
            String redisKey= RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }

       return CommunityUtil.getJSONString(0,null,map);
    }

}
