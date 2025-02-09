package com.gongsi.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class kafkaTest {
    @Autowired
    private KafkaProducer kafkaProducer;
    //发完消息后程序不要立即结束，为了看到被动调用的消费者进程的处理结果
    //生产者发消息是我们主动调的，消费者处理消息是被动调的，调用生产者之后就会自动调用消费者，为了看到消费者的处理结果，所以线程停顿几秒
    //如果生产者在发送消息后，程序立刻结束，消费者可能还未来得及接收和处理消息，程序就已经退出了。
    @Test
    public void testKafka(){
        kafkaProducer.sendMessage("test","nihao ");
        kafkaProducer.sendMessage("test","zaima ");
        try {
            Thread.sleep(10*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
@Component//希望这个类由Spring容器来管理,这样这个就是生产者的Bean
class KafkaProducer{
    @Autowired
    private KafkaTemplate kafkaTemplate;
    //首先是传入消息的主题，另外要指定消息的内容
    public void sendMessage(String topic, String content){
        kafkaTemplate.send(topic,content);
    }
}
@Component//不需要template，因为消费者是被动的去处理消息
class KafkaConsumer{
    //没有监听到消息消费者进程就会阻塞，否则就立刻读取
    @KafkaListener(topics={"test"})
    public void handleMessage(ConsumerRecord record){
        System.out.println(record.value());
    }
}
