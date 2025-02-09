package com.gongsi.community.controller;

import com.gongsi.community.entity.Comment;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.entity.Event;
import com.gongsi.community.event.EventProducer;
import com.gongsi.community.service.CommentService;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.HostHolder;
import com.google.code.kaptcha.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.Serializable;
import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private CommentService commentService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private DiscussPostService discussPostService;

    @RequestMapping(path="/add/{discussPostId}",method= RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setCreate_time(new Date());
        comment.setStatus(0);
        comment.setUser_id(hostHolder.getUser().getId());//从当前线程获取用户id
        //有两个值都由用户指定，所以实体类型和实体id从浏览器隐式得到，不是服务器设置的
        //初始化完要传递给业务层的数据就可以调用业务层的插入方法了
        int rows=commentService.addComment(comment);

        //插入评论后触发评论事件发送系统消息，构造事件的属性
        Event event=new Event();
        event.setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())//当前登陆的用户触发事件
                .setEntityType(comment.getEntity_type())
                .setEntityId(comment.getEntity_id())
                //点击查看，还要链接到具体的帖子，要把帖子id放到额外的data中
                .setData("postId",discussPostId);
                //针对的是回复的目标用户的id是什么.setEntityUserId(comment.getTarget_id());
                //触发事件，要知道触发的这条评论的作者和这个帖子的作者
                if(comment.getEntity_type()==ENTITY_TYPE_POST) {
                    DiscussPost target=discussPostService.findDiscussPostById(comment.getEntity_id());//找到评论的目标帖子
                    //每条评论对象都会记录作者的id是user_id
                    event.setEntityUserId(target.getUser_id());
                }else if(comment.getEntity_type()==ENTITY_TYPE_COMMENT){
                    Comment target=commentService.findCommentById(comment.getEntity_id());
                    event.setEntityUserId(target.getUser_id());
                }
                //构造完事件对象，就可以调用生产者的service去发布消息
        eventProducer.fireEvent(event);
        //评论给帖子的时候才触发事件
        if(comment.getEntity_type()==ENTITY_TYPE_POST) {
            event=new Event().setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
        }
        return "redirect:/discuss/detail/"+discussPostId;
    }
}






















