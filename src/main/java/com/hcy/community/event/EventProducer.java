package com.hcy.community.event;

import com.alibaba.fastjson.JSONObject;
import com.hcy.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    
    @Autowired
    private KafkaTemplate kafkaTemplate;
    
    //处理事件
    public void fireEvent(Event event) {
        //将事件发布到指定的主题topic
        //将event转为JSON字符串，由消费者将字符串还原为event
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
        
    }
}
