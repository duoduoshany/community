package com.gongsi.community.controller;

import com.gongsi.community.entity.*;
import com.gongsi.community.event.EventProducer;
import com.gongsi.community.service.CommentService;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.service.LikeService;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.HostHolder;
import com.gongsi.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    //需要往redis中存值
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    //对于客户端而言只需要传入标题和内容即可，评论数和点赞数一开始都是0
    public String addDiscussPost(String title, String content) {
        //先从hostholder获取当前用户
        User user = hostHolder.getUser();
        //如果user为空，就有问题，必须得登录才能发帖子,给页面返回一个必须登录的提示，403代表用户没有权限访问服务器的功能
        if (user == null) {
            return CommunityUtil.getJSONString(403, "您还没有登录");
        }
        //登录的话就得调用Service方法处理Controller传入的数据并保存帖子到数据库
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUser_id(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setStatus(0);
        discussPost.setCreate_time(new Date());
        discussPostService.addDiscussPost(discussPost);
        //触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        //计算新帖子分值，参与热度排行
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,discussPost.getId());

        //程序在执行中报错的情况，将来统一处理
        //Controller返回给前端对应的结果：json数据，本来是返回网页，现在返回的是字符串
        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", discussPost);
        //不要遗漏了装载用户对象到model便于查询出某个帖子后根据上面的用户id显示出用户头像和用户名
        User user = userService.findUserById(discussPost.getUser_id());
        model.addAttribute("user", user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);

        //点赞状态:登录的用户才会查询点赞的状态，否则不会查询，只会显示赞，没有已赞
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //评论信息：到时候根据page实体的属性来分页显示评论
        page.setLimit(5);
        //在页码上设置链接点击,点击下一页的时候刷新页面，页面依然需要帖子和作者详情的展示，所以分页路径跟方法路径一致
        page.setPath("/discuss/detail/" + discussPostId);
        //本来想用查询评论数的方法查询出来，但帖子这个实体本身包含评论数量的字段，查询更快
        page.setRows(discussPost.getComment_count());
        //得到当前帖子的所有评论
        List<Comment> commentList = commentService.findCommentsByEntity
                (ENTITY_TYPE_POST, discussPost.getId(), page.getOffset(), page.getLimit());
        //整合评论信息和评论的用户信息为新的显示对象，有多个这样的显示对象所以是一个列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();

        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> commentVo = new HashMap<>();
                //注意，我这里传入的是整个评论对象而不是评论内容，所以它包含评论时间
                commentVo.put("comment", comment);
                //对象包含头像和用户名属性
                commentVo.put("user", userService.findUserById(comment.getUser_id()));
                //点赞
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);

                //点赞状态:登录的用户才会查询点赞的状态，否则不会查询，只会显示赞，没有已赞
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                //回复列表,纠正：因为要得到每条评论的回复，所以entity_id是评论的id而不是帖子的id
                //entity_id就是目标评论的id，当然不可能是getEntity_id,这样得到的是评论的目标id，是帖子id。
                // 而当前遍历的评论的id就是回复的目标id（entity_id）！比如回复的目标id（entity_id)是2，那么就找id为2的评论。
                List<Comment> replyList = commentService.findCommentsByEntity
                        (ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //注意得到的仍然只有回复信息，没有相关的用户信息，依然需要将回复和用户信息整合起来
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply", reply);
                        //这里也一样，也是根据谁写的回复，getUserid就得到谁的用户id
                        System.out.println("user1 " + reply.getUser_id());
                        replyVo.put("user", userService.findUserById(reply.getUser_id()));
                        //整合后，判断回复还有没有回复
                        //有回复的话就需要在整合后的对象显示出来,也是往整合的回复对象中put键值对
                        //需要得到回复的目标用户名：形如 寒江雪 回复 Susan
                        User target = reply.getTarget_id() == 0 ?
                                null : userService.findUserById(reply.getTarget_id());
                        replyVo.put("target", target);//这里的target根据上一行的条件决定要不要显示
                        //容易忽略把集合完键值对的replyVo对象放进存储这个对象的列表中

                        //点赞
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);

                        //点赞状态:登录的用户才会查询点赞的状态，否则不会查询，只会显示赞，没有已赞
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                        replyVo.put("likeStatus", likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                //注意帖子评论还要回复数量
                //具体到某条评论,查询的是数量，计算方式是count(id)，查询的不是具体的数据，不用传入offset和limit的参数
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                //往集合中添加整合的对象
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "site/discuss-detail";
    }
    //置顶加精删除一共三个请求，一个一个处理，因为置顶需要提交帖子id的数据进来，所以是Post
    //置顶的请求我们要求是异步请求，点击按钮后不整体刷新，也可以进行上传数据的操作，并返回json字符串
    @RequestMapping(value = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);//帖子的状态发生变化，需要更改es中的帖子
        //触发发帖事件
        Event event=new Event();
        event.setTopic(TOPIC_PUBLISH)
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id)
                .setUserId(hostHolder.getUser().getId());
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }
    //加精
    @RequestMapping(value = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);//帖子的状态发生变化，需要更改es中的帖子
        //触发发帖事件
        Event event=new Event();
        event.setTopic(TOPIC_PUBLISH)//该主题不是生成系统消息，而是发布帖子到es服务器
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id)
                .setUserId(hostHolder.getUser().getId());
        eventProducer.fireEvent(event);
        String redisKey=RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);
        return CommunityUtil.getJSONString(0);
    }
    //删除
    @RequestMapping(value = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);//帖子的状态发生变化，需要更改es中的帖子
        //触发删帖事件
        Event event=new Event();
        event.setTopic(TOPIC_DELETE)//该主题不是生成系统消息，而是发布帖子到es服务器
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id)
                .setUserId(hostHolder.getUser().getId());
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }


}
