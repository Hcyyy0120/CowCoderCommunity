package com.hcy.community.controller;

import com.hcy.community.annotation.LoginRequired;
import com.hcy.community.entity.Comment;
import com.hcy.community.entity.DiscussPost;
import com.hcy.community.entity.Page;
import com.hcy.community.entity.User;
import com.hcy.community.service.*;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.CommunityUtil;
import com.hcy.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    
    @Value("${community.path.upload}")
    private String uploadPath;
    
    @Value("${community.path.domain}")
    private String domain;
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    HostHolder hostHolder;
    
    @Autowired
    private LikeService likeService;
    
    @Autowired
    private FollowService followService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage() {
        return "/site/setting";
    }
    
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "???????????????????????????");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        /*long fileSize = headerImage.getSize();
        System.out.println("fileSize" + fileSize);
        if (fileSize > 5300000) {
            model.addAttribute("error", "??????????????????????????????5M????????????");
            return "/site/setting";
        }*/
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (CommunityUtil.checkHeaderImageType(fileType)) {
            model.addAttribute("error", "????????????????????????");
            return "/site/setting";
        }
        if (StringUtils.isEmpty(suffix)) {
            model.addAttribute("error", "??????????????????????????????");
            return "/site/setting";
        }
        //?????????????????????
        fileName = CommunityUtil.generateUUID() + suffix;
        //???????????????????????????
        File dest = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(dest);//?????????????????????(?????????)?????????????????????
        } catch (IOException e) {
            log.error("?????????????????????" + e.getMessage());
            throw new RuntimeException("?????????????????????????????????????????????", e);
        }
        
        //???????????????????????????????????????web???????????????
        //eg:http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headUrl);
        
        return "redirect:/index";//??????????????????????????????
    }
    
    //<img th:src="${loginUser.headerUrl}" class="rounded-circle" style="width:30px;"/>
    //headerUrl  http://localhost:8080/community/user/header/5ebb6b887b744fbc9c6639ecd3ef4b9f.jpg
    @GetMapping("/header/{fileName}")
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //??????????????????????????????
        fileName = uploadPath + "/" + fileName;
        // ????????????
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        // ????????????
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);// ??????fis???????????????????????????????????????
                //java7?????????fis???????????????????????????finally?????????finally?????????
        ) {
            OutputStream os = response.getOutputStream();//os???SpringMVC????????????
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log.error("?????????????????????" + e.getMessage());
        }
    }
    
    // ????????????
    @PostMapping("/updatePassword")
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:/logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "/site/setting";
        }
    }
    
    //????????????
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("??????????????????");
        }
        //??????
        model.addAttribute("user", user);
        //???????????????????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        
        //????????????
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //????????????
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        //????????????????????????????????????
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {//?????????????????????
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        
        return "/site/profile";
    }
    
    // ????????????
    @RequestMapping(path = "/mypost/{userId}", method = RequestMethod.GET)
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user", user);
        
        // ????????????
        page.setPath("/user/mypost/" + userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        
        // ????????????
        List<DiscussPost> discussList = discussPostService
                .findDiscussPosts(userId, page.getOffset(), page.getLimit(),0);
        List<Map<String, Object>> discussVOList = new ArrayList<>();
        if (discussList != null) {
            for (DiscussPost post : discussList) {
                Map<String, Object> map = new HashMap<>();
                map.put("discussPost", post);
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussVOList.add(map);
            }
        }
        model.addAttribute("discussPosts", discussVOList);
        
        return "/site/my-post";
    }
    
    // ????????????
    @RequestMapping(path = "/myreply/{userId}", method = RequestMethod.GET)
    public String getMyReply(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("?????????????????????");
        }
        model.addAttribute("user", user);
        
        // ????????????
        page.setPath("/user/myreply/" + userId);
        page.setRows(commentService.findUserCount(userId));
        
        // ????????????
        List<Comment> commentList = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentVOList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("discussPost", post);
                commentVOList.add(map);
            }
        }
        model.addAttribute("comments", commentVOList);
        
        return "/site/my-reply";
    }
    
}
