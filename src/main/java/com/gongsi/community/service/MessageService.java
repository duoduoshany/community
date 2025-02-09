package com.gongsi.community.service;

import com.gongsi.community.dao.MessageMapper;
import com.gongsi.community.entity.Message;
import com.gongsi.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    //查询所有会话最新一条消息
    public List<Message> findConversations(int userId,int offset,int limit){
        return messageMapper.selectConversations(userId,offset,limit);
    }
    //查询所有会话数量
    public int findConversationCount(int userId){
        return messageMapper.selectConversationsCount(userId);
    }
    //查询某个会话的消息列表
    public List<Message> findLetters(String conversationId,int offset,int limit){
        return messageMapper.selectLetters(conversationId,offset,limit);
    }
    //查询某个会话的消息数量
    public int findLetterCount(String conversationId){
        return messageMapper.selectLettersCount(conversationId);
    }
    //查询未读消息数量(针对于所有会话的总未读和单个会话的未读
    public int findLetterUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId,conversationId);
    }
    //处理数据然后直接插入
    public int addMessage(Message message){
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        //不理解竟然还要过滤
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }
    public int readMessage(List<Integer> list){
        return messageMapper.updateMessage(list,1);
    }
    public Message findLatestNotice(int userId,String topic){
        return messageMapper.selectLatestNotice(userId,topic);
    }
    public int findNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeCount(userId,topic);
    }
    public int findNoticeUnreadCount(int userId,String topic){
        return messageMapper.selectNoticeUnreadCount(userId,topic);
    }
    public List<Message> findNotices(int userId,String topic,int offset,int limit){
        return messageMapper.selectNotices(userId,topic,offset,limit);
    }
}
