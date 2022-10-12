package com.hcy.community.controller;

import com.hcy.community.entity.DiscussPost;
import com.hcy.community.entity.Page;
import com.hcy.community.service.ElasticsearchService;
import com.hcy.community.service.LikeService;
import com.hcy.community.service.UserService;
import com.hcy.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {
    
    @Autowired
    private ElasticsearchService elasticsearchService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeService likeService;
    
    //search?keyword=xxx
    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        //搜索帖子
        //page.getCurrent()-1  方法要求从0开始
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent()-1, page.getLimit());
        
        //聚合数据
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                map.put("post",post);
                map.put("user",userService.findUserById(post.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);
        
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());
        
        return "/site/search";
    }
}
