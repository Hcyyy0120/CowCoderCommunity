package com.hcy.community.controller;

import com.hcy.community.annotation.LoginRequired;
import com.hcy.community.entity.Comment;
import com.hcy.community.entity.DiscussPost;
import com.hcy.community.entity.Event;
import com.hcy.community.event.EventProducer;
import com.hcy.community.service.CommentService;
import com.hcy.community.service.DiscussPostService;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.HostHolder;
import com.hcy.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;
import java.util.Random;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    HostHolder hostHolder;
    
    @Autowired
    private EventProducer eventProducer;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @LoginRequired//未登录不可评论
    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        if (StringUtils.isBlank(comment.getContent())) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        
        //触发评论事件
        Event event = new Event();
        event.setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityId())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        
        //判断评论是帖子的还是评论的
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
    
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            //触发发帖事件
            event = new Event();
            event.setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
    
            //计算帖子分数
            //只在对帖子评论的时候进行计算
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }
        return "redirect:/discuss/detail/" + discussPostId;
    }
}
