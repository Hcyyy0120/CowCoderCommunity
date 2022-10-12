package com.hcy.community.event;

import com.alibaba.fastjson.JSONObject;
import com.hcy.community.entity.DiscussPost;
import com.hcy.community.entity.Event;
import com.hcy.community.entity.Message;
import com.hcy.community.service.DiscussPostService;
import com.hcy.community.service.ElasticsearchService;
import com.hcy.community.service.MessageService;
import com.hcy.community.util.CommunityConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EventConsumer implements CommunityConstant {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private ElasticsearchService elasticsearchService;
    
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空");
            return;
        }
        
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误");
            return;
        }
        
        //构造并发送站内通知消息
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());
        
        //系统通知页的消息链接(xxx评论了你的帖子,‘点击查看’),需要帖子ID
        Map<Object, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String,Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(),entry.getValue());
            }
        }
        
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }
    
    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublicMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空");
            return;
        }
    
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误");
            return;
        }
    
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);
    }
    
    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息的内容为空");
            return;
        }
        
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息格式错误");
            return;
        }
        
        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
