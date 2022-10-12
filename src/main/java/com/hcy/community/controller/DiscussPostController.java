package com.hcy.community.controller;

import com.hcy.community.entity.*;
import com.hcy.community.event.EventProducer;
import com.hcy.community.service.CommentService;
import com.hcy.community.service.DiscussPostService;
import com.hcy.community.service.LikeService;
import com.hcy.community.service.UserService;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.CommunityUtil;
import com.hcy.community.util.HostHolder;
import com.hcy.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private EventProducer eventProducer;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "用户还未登录！");
        }
        if (StringUtils.isBlank(title) || StringUtils.isBlank(content)) {
            return CommunityUtil.getJSONString(403, "标题和内容不能为空！");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);
        
        //触发发帖事件
        Event event = new Event();
        event.setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);
        
        //计算帖子分数
        //将新增的帖子放到redis中(新发布的帖子也有一个默认分数)
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());
        
        return CommunityUtil.getJSONString(0, "帖子发布成功！");
    }
    
    //帖子详情
    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        //帖子发布者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        
        //帖子点赞数
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //帖子点赞状态
        //仅在登录状态下可以查询状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);
        
        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());
        
        //评论：给帖子的评论
        //回复：给评论的评论
        //当前帖子的所有评论
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        
        //生成帖子评论详情
        //评论Vo列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                //评论Vo
                HashMap<String, Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment", comment);
                //作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                
                //帖子点赞数
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                //帖子点赞状态
                //仅在登录状态下可以查询状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);
                
                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);//不做分页，查询所有
                //回复Vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        HashMap<String, Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //回复目标
                        User target = reply.getTargetId() == 0 ? null :
                                userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);
                        
                        //帖子点赞数
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        //帖子点赞状态
                        //仅在登录状态下可以查询状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        
        return "/site/discuss-detail";
    }
    
    // 置顶、取消置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        DiscussPost discussPostById = discussPostService.findDiscussPostById(id);
        // 获取置顶状态，1为置顶，0为正常状态,1^1=0 0^1=1
        int type = discussPostById.getType() ^ 1;
        discussPostService.updateType(id, type);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        
        // 触发发帖事件(更改帖子状态)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        
        return CommunityUtil.getJSONString(0, null, map);
    }
    
    // 加精、取消加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        DiscussPost discussPostById = discussPostService.findDiscussPostById(id);
        int status = discussPostById.getStatus() ^ 1;
        // 1为加精，0为正常， 1^1=0, 0^1=1
        discussPostService.updateStatus(id, status);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        
        // 触发发帖事件(更改帖子类型)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        
        //计算帖子分数
        //加精/取消精都会影响分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);
        
        return CommunityUtil.getJSONString(0, null, map);
    }
    
    //删除
    @ResponseBody
    @PostMapping("/delete")
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);
        //触发删帖事件
        Event event = new Event();
        event.setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }
}
