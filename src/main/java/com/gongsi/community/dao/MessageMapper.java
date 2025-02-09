package com.gongsi.community.dao;
import com.gongsi.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    //查询当前用户的会话列表,针对每个会话只返回一条最新的私信:为了支持分页传入两个参数

    List<Message> selectConversations(int userId,int offset,int limit);

    //查询当前用户的会话数量

    int selectConversationsCount(int userId);

    //查询某个会话所包含的私信列表：支持分页

    List<Message> selectLetters(String ConversationId,int offset,int limit);

    //查询某个会话所包含的私信数量

    int selectLettersCount(String ConversationId);


    //查未读消息数量：针对某个用户的某个会话或所有会话
    //用动态sql，就能实现一个方法两个业务，比如传了会话Id就查会话Id，没有传的话就查用户Id的所有会话

    int selectLetterUnreadCount(int userId,String ConversationId);

    //增加私信
    int insertMessage(Message message);

    //未读更新为已读,修改状态的消息不止一个，所以传入的参数是消息列表
    int updateMessage(List<Integer> ids,int status);

    //查某主题下的最新消息
    Message selectLatestNotice(int userId,String topic);

    //查询某主题下的未读消息数量
    int selectNoticeUnreadCount(int userId,String topic);

    //查询某主题下的消息数量
    int selectNoticeCount(int userId,String topic);

    //查询某主题下的所有通知消息，这些消息还要支持分页
    List<Message> selectNotices(int userId,String topic,int offset,int limit);

    //



}