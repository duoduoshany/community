package com.gongsi.community.controller;

import com.gongsi.community.annotation.LoginRequired;
import com.gongsi.community.entity.Event;
import com.gongsi.community.entity.Page;
import com.gongsi.community.entity.User;
import com.gongsi.community.event.EventProducer;
import com.gongsi.community.service.FollowService;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path="/follow",method= RequestMethod.POST)
    @ResponseBody
    @LoginRequired//没有登录的不能使用关注功能
    //用户id从hostHolder获取，其余数据从前端获取,作为参数传入
    public String follow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);
        Event event=new Event();
        event.setTopic(TOPIC_FOLLOW)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setUserId(hostHolder.getUser().getId())
                .setEntityUserId(entityId);//暂时设置业务关注的只有用户这个实体，所以作者id就是实体id
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0,"已关注！");
    }

    @RequestMapping(path="/unfollow",method= RequestMethod.POST)
    @ResponseBody
    @LoginRequired//没有登录的不能使用关注功能
    //用户id从hostHolder获取，其余数据从前端获取,作为参数传入
    public String unfollow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.unfollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"取消关注成功！");
    }

    @RequestMapping(path="/followees/{userId}",method=RequestMethod.GET)
    //方法的参数要从路径中获取
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model){
        User user=userService.findUserById(userId);
        //用户可能没登陆，先判断user是不是空的，如果不为空就把user传给页面，页面需要显示xxx关注的人
        if(user==null){
            //controller要求返回的是网页，user为空的时候抛出异常，而不是return
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute(user);

        page.setLimit(5);//page在前端不存在，需要在controller层设置page的属性，才能传递相关参数给service
        //service才能按照分页的参数查询目标范围的一条条关注人的信息
        page.setPath("/followees/"+userId);
        //setRows要求传递整数类型
        page.setRows((int)followService.findFolloweeCount(userId,ENTITY_TYPE_USER));

        //接收service整合后的数据
        List<Map<String,Object>> userList=followService.findFollowees(userId,page.getOffset(),page.getLimit());
        //还得整合数据，判断当前用户对其他用户所关注的人是否有关注
        //遍历前还得判断是否为空，因为followService中针对列表为空的情况return null，这个返回值也会返回给controller
        if(userList!=null){
            for(Map<String,Object> map:userList){
                User u=(User)map.get("user");//因为值是Object类型
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        //数据完全，装入模板
        model.addAttribute("users",userList);
        return "/site/followee";
    }
    @RequestMapping(path="/followers/{userId}",method=RequestMethod.GET)
    //方法的参数要从路径中获取
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model){
        User user=userService.findUserById(userId);
        //用户可能没登陆，先判断user是不是空的，如果不为空就把user传给页面，页面需要显示xxx关注的人
        if(user==null){
            //controller要求返回的是网页，user为空的时候抛出异常，而不是return
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute(user);

        page.setLimit(5);//page在前端不存在，需要在controller层设置page的属性，才能传递相关参数给service
        //service才能按照分页的参数查询目标范围的一条条关注人的信息
        page.setPath("/followers/"+userId);
        //setRows要求传递整数类型
        page.setRows((int)followService.findFollowerCount(ENTITY_TYPE_USER,userId));

        //接收service整合后的数据
        List<Map<String,Object>> userList=followService.findFollowers(userId,page.getOffset(),page.getLimit());
        //还得整合数据，判断当前用户对其他用户所关注的人是否有关注
        //遍历前还得判断是否为空，因为followService中针对列表为空的情况return null，这个返回值也会返回给controller
        if(userList!=null){
            for(Map<String,Object> map:userList){
                User u=(User)map.get("user");//因为值是Object类型
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        //数据完全，装入模板
        model.addAttribute("users",userList);
        return "/site/follower";
    }
    //不登录也能访问关注列表和粉丝列表
    //因为粉丝列表的页面也需要用到登录状态的判断，直接弄成一个方法进行复用
    private boolean hasFollowed(int userId){
        if(hostHolder.getUser()==null){
            return false;//用户没有登录，直接返回无关注
        }
        return followService.isFollower(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
    }

}
