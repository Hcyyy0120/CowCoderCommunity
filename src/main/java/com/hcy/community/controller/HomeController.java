package com.hcy.community.controller;

import com.hcy.community.entity.DiscussPost;
import com.hcy.community.entity.Page;
import com.hcy.community.entity.User;
import com.hcy.community.mapper.DiscussPostMapper;
import com.hcy.community.service.DiscussPostService;
import com.hcy.community.service.LikeService;
import com.hcy.community.service.MessageService;
import com.hcy.community.service.UserService;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private LikeService likeService;
    
    @GetMapping("/index")
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode",defaultValue = "0") int orderMode) {
        //方法调用前,SpringMVC会自动实例化Model和Page,并将Page注入Mode1。所以,在thymeleaf中可以直接访问Page对象中的数据.
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);
        
        List<DiscussPost> discussPostList = discussPostService
                .findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        ArrayList<Map<String, Object>> discussPosts = new ArrayList<>();
        if (discussPostList != null) {
            for (DiscussPost post : discussPostList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                
                //显示赞的数量
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);
                
                discussPosts.add(map);
            }
        }
        
        User user = hostHolder.getUser();
        if (user != null) {
            //查询未读消息数量
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            model.addAttribute("letterUnreadCount",letterUnreadCount);
        }
        
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }
    
    @GetMapping("/error")
    public String getErrorPage() {
        return "error/500";
    }
    
    @GetMapping("/denied")
    public String getDeniedPage() {
        return "/error/404";
    }
}
