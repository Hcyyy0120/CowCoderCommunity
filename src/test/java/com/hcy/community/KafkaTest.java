package com.hcy.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@SpringBootTest
public class KafkaTest {
    
    @Autowired
    private KafkaProducer kafkaProducer;
    
    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test","哈哈哈");
        kafkaProducer.sendMessage("test","哈哈哈哈哈哈");
    
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@Component
class KafkaProducer {
    
    @Autowired
    private KafkaTemplate kafkaTemplate;
    
    /**
     *
     * @param topic 消息主体
     * @param content 消息内容
     */
    public void sendMessage(String topic,String content) {
        kafkaTemplate.send(topic,content);
    }
    
}

@Component
class KafkaConsumer {
    @KafkaListener(topics = {"test"})
    public void handMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }

}

