package com.gongsi.community.event;

import com.alibaba.fastjson.JSONObject;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.entity.Event;
import com.gongsi.community.entity.Message;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.service.ElasticsearchService;
import com.gongsi.community.service.MessageService;
import com.gongsi.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {
    //发布消息可能会有隐含的问题出现，需要记录日志
    private static final Logger logger=LoggerFactory.getLogger(EventConsumer.class);
    //需要往message表中插入数据
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;


    //三个主题的处理形式非常接近，比如xxx关注了你，xxx评论了你，xxx点赞了你
    //可以写一个方法同时处理掉三个主题
    @KafkaListener(topics={TOPIC_LIKE,TOPIC_COMMENT,TOPIC_FOLLOW})
    public void handleMultiMessage(ConsumerRecord record) {
        //record.value()获取json字符串：为空：转化为json失败导致为null或json字符串因网络问题/序列化丢失
        if(record==null||record.value()==null){
            logger.error("消息内容为空!");
            return;
        }
        //json字符串不是空的，可以将其解析为event对象
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误");
            return;
        }
        //消费者的处理：根据生产者发送的原始的event数据来拼出一个消息实体（作为系统通知）发送给客户端
        //我们这里先探讨的是只有一个消费者，也就只对某topic的一个分区消费，所以暂时没有体现分区
        Message message=new Message();
        message.setFrom_id(SYSTEM_USER_ID);
        message.setTo_id(event.getEntityUserId());//通知的是作者
        message.setConversation_id(event.getTopic());
        message.setCreate_time(new Date());

        //消息的内容类似于XXX点赞了帖子，需要从event中提取与显示内容相关的数据封装成一个map，转化成json字符串放到content
        Map<String,Object> content=new HashMap<>();
        content.put("userId",event.getUserId());//1.触发事件的用户id
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());//2.事件操作的目标
        //event的其它数据在data这个map中，一股脑都放进content中即可
        //遍历map前先判断是否为空
        if(!event.getData().isEmpty()){
            //遍历map中的每对键值对
            for(Map.Entry<String,Object> entry:event.getData().entrySet()){
                content.put(entry.getKey(),entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }
    @KafkaListener(topics={TOPIC_PUBLISH})
    //ConsumerRecord封装了从Kafka消息队列中获取的消息及其相关的分区偏移量信息等。
    //接收消息时封装成ConsumerRecord再传递给消费者的监听方法，此时方法获取消息的全部数据
    public void handlePublishMessage(ConsumerRecord record) {
        //record.value()先判断record！=null，该方法获取json字符串：为空：本身没有传递消息值或者消息值序列化失败导致消息值丢失
        if(record==null||record.value()==null){
            logger.error("消息内容为空!");
            return;
        }
        //json字符串不是空的，可以将其解析为event对象
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误");
            return;
        }
        DiscussPost discussPost=discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }
    @KafkaListener(topics={TOPIC_DELETE})
    //ConsumerRecord封装了从Kafka消息队列中获取的消息及其相关的分区偏移量信息等。
    //接收消息时封装成ConsumerRecord再传递给消费者的监听方法，此时方法获取消息的全部数据
    public void handleDeleteMessage(ConsumerRecord record) {
        //record.value()先判断record！=null，该方法获取json字符串：为空：本身没有传递消息值或者消息值序列化失败导致消息值丢失
        if(record==null||record.value()==null){
            logger.error("消息内容为空!");
            return;
        }
        //json字符串不是空的，可以将其解析为event对象
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误");
            return;
        }
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    @KafkaListener(topics={TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record){
        if(record==null||record.value()==null){
            logger.error("消息内容为空!");
            return;
        }
        //json字符串不是空的，可以将其解析为event对象
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if(event==null){
            logger.error("消息格式错误");
            return;
        }
        //得到事件的data属性值，我们定义了data的键是String，值是Object
        String htmlUrl=(String)event.getData().get("htmlUrl");
        String filename=(String)event.getData().get("filename");
        String suffix=(String)event.getData().get("suffix");

        //拼命令，注意命令有些地方有空格
        String cmd=wkImageCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"
                +filename+suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功:"+cmd);
        } catch (IOException e) {
            logger.error("生成长图失败："+e.getMessage());
        }
    }

}
