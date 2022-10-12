package com.hcy.community.controller;

import com.hcy.community.annotation.LoginRequired;
import com.hcy.community.entity.Event;
import com.hcy.community.entity.Page;
import com.hcy.community.entity.User;
import com.hcy.community.event.EventProducer;
import com.hcy.community.service.FollowService;
import com.hcy.community.service.UserService;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.CommunityUtil;
import com.hcy.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    
    @Autowired
    private FollowService followService;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EventProducer eventProducer;
    
    @LoginRequired
    @ResponseBody
    @PostMapping("/follow")
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);
        //触发关注时间
        Event event = new Event();
        event.setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0, "已关注");
    }
    
    @LoginRequired
    @ResponseBody
    @PostMapping("/unfollow")
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注");
    }
    
    //查询某用户的关注列表
    @GetMapping("/followees/{userId}")
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));
        
        List<Map<String, Object>> followeeList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (followeeList != null) {
            for (Map<String, Object> map : followeeList) {
                User followee = (User) map.get("user");
                map.put("hasFollowed", followService.hasFollowed(hostHolder.getUser(), followee.getId()));
            }
        }
        model.addAttribute("users",followeeList);
        return "/site/followee";
    }
    
    //查询某用户的粉丝列表
    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER,userId));
        
        List<Map<String, Object>> followerList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (followerList != null) {
            for (Map<String, Object> map : followerList) {
                User follower = (User) map.get("user");
                map.put("hasFollowed", followService.hasFollowed(hostHolder.getUser(), follower.getId()));
            }
        }
        model.addAttribute("users",followerList);
        return "/site/follower";
    }
    
}
