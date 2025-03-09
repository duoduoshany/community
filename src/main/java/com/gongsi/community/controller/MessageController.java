package com.gongsi.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.gongsi.community.annotation.LoginRequired;
import com.gongsi.community.entity.Comment;
import com.gongsi.community.entity.Message;
import com.gongsi.community.entity.Page;
import com.gongsi.community.entity.User;
import com.gongsi.community.service.CommentService;
import com.gongsi.community.service.MessageService;
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
import org.springframework.web.util.HtmlUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;

    //私信列表,查询所有会话的最新消息构成的消息列表，请求方式是GET
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        //在这里，page已经自动装配给model了，无需再次装配。page要传递给前端，方便前端代码获取page的分页信息,如下方的page组件
        //分页信息，也是由controller层传递给模板，模板才能据此分页
        page.setRows(messageService.findConversationCount(hostHolder.getUser().getId()));
        page.setPath("/letter/list");
        page.setLimit(5);
        //会话列表,getoffset是前端传入当前页码，后端的实体类page就能计算出offset，并传递给controller方法
        List<Message> conversationList = messageService.findConversations
                (hostHolder.getUser().getId(), page.getOffset(), page.getLimit());
        //conversationList中的每个会话还得封装用户信息，未读消息数量，总的消息数量，因此封装成一个显示对象
        List<Map<String, Object>> conversations = new ArrayList<>();
        //遍历前一定要判断列表是否为空
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                //纠正：用户显示的是消息的发送方而不是当前用户,不能直接getTo_id，因为当前用户可能是from_id也可能是to_id
                //我要选的是与之相对的用户,如果当前用户是消息的发送方，那么显示的是消息的接收方，否则显示的是发送方。
                int targetId = hostHolder.getUser().getId() == message.getFrom_id() ? message.getTo_id() : message.getFrom_id();
                map.put("target", userService.findUserById(targetId));
                //message实体可以得到会话ID，先是会话中的消息数量
                map.put("letterCount", messageService.findLetterCount(message.getConversation_id()));
                //会话中未读的消息数量
                map.put("unreadCount", messageService.findLetterUnreadCount(hostHolder.getUser().getId(), message.getConversation_id()));
                //最终封装好的map对象传入结果集合中。
                conversations.add(map);
            }
        }
        //经过循环后得到每一条显示对象的集合数据传给model
        model.addAttribute("conversations", conversations);
        //未读私信数量
        int count = messageService.findLetterUnreadCount(hostHolder.getUser().getId(), null);
        model.addAttribute("letterUnreadCount", count);
        //未读通知数量
        int noticeUnreadCount=messageService.findNoticeUnreadCount(hostHolder.getUser().getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        //得到会话的所有消息列表，同样看需要显示什么就封装成一个显示对象，最后把显示对象的集合添加到model
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {//前提依然是列表不为空才遍历
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFrom_id()));
                //弄好一个显示对象，发送给结果集合
                letters.add(map);
            }
        }
        //循环之外把结果集合发送给模板
        model.addAttribute("letters", letters);
        //除此之外，页面还显示来自XXX的私信，XXX就是私信的对方，与上面一样，显示与当前用户相对的用户
        //因为会话中包含多条消息，可以取一条消息来做判断，也可以拆分conversationId
        model.addAttribute("target",getLetterTarget(conversationId));

        //设置已读
        List<Integer> ids=getLetterIds(letterList);
        if(!ids.isEmpty()) {
            messageService.readMessage(ids);//当消息的id集合不为空才调用方法读取集合
        }
        return "site/letter-detail";
    }
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> letterIds = new ArrayList<>();
        //集合不为空，才遍历集合
        if(letterList != null) {
            for(Message message : letterList) {
                //只有当会话的消息列表中的消息是接收方且消息的状态是未读的时候才需要传递对应的消息id集合给业务层
                if(hostHolder.getUser().getId()==message.getTo_id() && message.getStatus()==0){
                    letterIds.add(message.getId());
                }
            }
        }
        return letterIds;
    }
    private User getLetterTarget(String conversationId){
        String[] sds=conversationId.split("_");//用下划线做分割，分割成两个字符串用一个数组接收
        int sds0=Integer.parseInt(sds[0]);
        int sds1=Integer.parseInt(sds[1]);
        if(hostHolder.getUser().getId()==sds0){
            return userService.findUserById(sds1);
        }
        else{
            return userService.findUserById(sds0);
        }
    }
    @RequestMapping(path="/letter/send",method=RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName,String content)
    {
        //根据用户名来查找用户
        User user=userService.findUserByName(toName);
        //纠正，查找的用户可能为空
        if(user==null)
        {
            return CommunityUtil.getJSONString(1,"目标用户不存在");
        }
        //创建新消息，设置消息属性，再把消息传递给业务层
        Message message=new Message();
        message.setContent(content);
        message.setFrom_id(hostHolder.getUser().getId());
        message.setTo_id(user.getId());
        //拼接conversationId，这里我们设置小的id在前，大的id在后
        if(message.getFrom_id()<message.getTo_id())
        {
            message.setConversation_id(message.getFrom_id()+"_"+message.getTo_id());
        }
        else{
            message.setConversation_id(message.getTo_id()+"_"+message.getFrom_id());
        }
        message.setStatus(0);
        message.setCreate_time(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }
    @RequestMapping(path="/notice/list",method=RequestMethod.GET)
    //显示通知列表的方法同时包含查询评论类、点赞类、通知类的消息的代码
    public String getNoticeList(Model model) {
        //查询评论类通知：这一类通知会话数量为1，所以调用service查找最新的message只有一条
        User user=hostHolder.getUser();
        Message message=messageService.findLatestNotice(user.getId(),TOPIC_COMMENT);
        if(message!=null){
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);
            //有了message的所有字段，其中content字段的json字符串应该拆解成多个键值对放进整合map，先转义
            String content= HtmlUtils.htmlUnescape(message.getContent());
            //将json字符串转成hashMap对象，便于.获取键值对
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);
            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));//触发事件的对象,点击的时候为了可以得到用户的具体信息：用户名，所以传的不是用户id，而是整个用户对象
            messageVo.put("entityType",data.get("entityType"));//触发事件发生在哪个实体
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));
            int count=messageService.findNoticeCount(user.getId(),TOPIC_COMMENT);
            messageVo.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),TOPIC_COMMENT);
            messageVo.put("unread",unread);
            model.addAttribute("commentNotice",messageVo);
        }

        //查完评论类，查询点赞类通知
        message=messageService.findLatestNotice(user.getId(),TOPIC_LIKE);
        if(message!=null){
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);
            //有了message的所有字段，其中content字段的json字符串应该拆解成多个键值对放进整合map，先转义
            String content= HtmlUtils.htmlUnescape(message.getContent());
            //将json字符串转成hashMap对象，便于.获取键值对
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);
            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));//触发事件的对象
            messageVo.put("entityType",data.get("entityType"));//触发事件发生在哪个实体
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));
            int count=messageService.findNoticeCount(user.getId(),TOPIC_LIKE);
            messageVo.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),TOPIC_LIKE);
            messageVo.put("unread",unread);
            model.addAttribute("likeNotice",messageVo);
        }
        //查询关注类通知
        message=messageService.findLatestNotice(user.getId(),TOPIC_FOLLOW);
        if(message!=null){
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);
            //有了message的所有字段，其中content字段的json字符串应该拆解成多个键值对放进整合map，先转义
            String content= HtmlUtils.htmlUnescape(message.getContent());
            //将json字符串转成hashMap对象，便于.获取键值对
            Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);
            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));//触发事件的对象
            messageVo.put("entityType",data.get("entityType"));//触发事件发生在哪个实体
            messageVo.put("entityId",data.get("entityId"));
            int count=messageService.findNoticeCount(user.getId(),TOPIC_FOLLOW);
            messageVo.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),TOPIC_FOLLOW);
            messageVo.put("unread",unread);
            model.addAttribute("followNotice",messageVo);
        }

        //跟私信列表一样，通知列表要查询未读私信数量,跟这个用户有关的所有会话的未读消息数量
        int letterUnreadCount=messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询未读通知数量，也是所有topic
        int noticeUnreadCount=messageService.findNoticeUnreadCount(user.getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "site/notice";
    }
    //查哪个主题下的通知，这是前端决定的，前端通过在路径设置主题参数，让后端通过路径获取是哪个主题
    //只是做查询，没有涉及往redis或mysql中存入数据，所以不是POST请求
    @RequestMapping(path="/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic,Model model,Page page) throws ParseException {
        User user=hostHolder.getUser();
        page.setPath("/notice/detail/"+topic);
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));
        List<Message> messages=messageService.findNotices(user.getId(),topic,page.getOffset(),page.getLimit());
        //查询的系统通知不为空，就可以整合每一条消息为一个显示对象了,联想通知列表的详情页面
        //存储多个显示对象的集合
        List<Map<String,Object>> noticeVo=new ArrayList<>();
        if(messages!=null){
            for(Message message:messages) {
                Map<String, Object> messageVo = new HashMap<>();
                messageVo.put("notice", message);
                //内容中的键值对提取，才能得到用户xxx关注了xxx实体
                String content= HtmlUtils.htmlUnescape(message.getContent());
                Map<String,Object> data= JSONObject.parseObject(content,HashMap.class);
                messageVo.put("user",userService.findUserById((Integer)data.get("userId")));
                messageVo.put("entityType",data.get("entityType"));
                messageVo.put("entityId",data.get("entityId"));
                messageVo.put("postId",data.get("postId"));
                //注意还有消息的发送人是谁
                messageVo.put("fromUser",userService.findUserById(message.getFrom_id()));
                // 解析时间戳
                String createTimeString = (String) data.get("createTime");
                Date createTime = createTimeString != null ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(createTimeString) : null;
                // 查找 commentId
                Integer userId = (Integer) data.get("userId");
                Integer entityType = (Integer) data.get("entityType");
                Integer entityId = (Integer) data.get("entityId");
                messageVo.put("commentId",null);
                    Integer commentId = commentService.findComment(userId, entityType, entityId, createTime);
                    if (commentId != null) {
                        messageVo.put("commentId", commentId);
                    }
                //整合完成，放进显示对象的集合中
                noticeVo.add(messageVo);
            }
        }
        model.addAttribute("notices",noticeVo);
        //还有我访问了某类通知，应该把里面的消息都设成已读
        //只有当我是消息的接收方且消息是未读状态的时候才会获取id集合，并把id集合对应的每条消息都设成已读
        //为什么是id集合，mysql查询语句无法直接查询哪些消息在消息集合中，只能判断id在消息id的集合中从而判断出来
        List<Integer> ids=getLetterIds(messages);
        if(!ids.isEmpty()) {//还要判断集合非空，说明里边有数据还未处理
            messageService.readMessage(ids);
        }
        return "site/notice-detail";

    }
}
