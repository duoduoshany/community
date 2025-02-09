package com.gongsi.community.event;

import com.alibaba.fastjson.JSONObject;
import com.gongsi.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    @Autowired
    public KafkaTemplate kafkaTemplate;
    //处理对象
    public void fireEvent(Event event) {

        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
