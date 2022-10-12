package com.hcy.community.controller;

import com.hcy.community.annotation.LoginRequired;
import com.hcy.community.entity.Event;
import com.hcy.community.entity.User;
import com.hcy.community.event.EventProducer;
import com.hcy.community.service.LikeService;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.CommunityUtil;
import com.hcy.community.util.HostHolder;
import com.hcy.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private EventProducer eventProducer;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @LoginRequired//有bug(或是奇怪的机制),未登录点赞不会跳转到登录页
    @ResponseBody
    @PostMapping("/like")
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        //点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        // 获得点赞的数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 获取状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);
        
        //触发点赞事件,取消点赞不通知
        if (likeStatus == 1) {
            Event event = new Event();
            event.setTopic(TOPIC_LIKE)
                    .setUserId(user.getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);//需要跳转到的帖子页面
            eventProducer.fireEvent(event);
        }
        
        if (entityType == ENTITY_TYPE_POST) {
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }
        
        return CommunityUtil.getJSONString(0, null, map);
    }
}
