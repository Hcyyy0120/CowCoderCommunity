package com.hcy.community.controller;

import com.google.code.kaptcha.Producer;
import com.hcy.community.entity.User;
import com.hcy.community.service.UserService;
import com.hcy.community.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class LoginController implements CommunityConstant {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private Producer kaptchaProducer;
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    @Autowired
    private MailClient mailClient;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @GetMapping("/register")
    public String getRegisterPage() {
        return "/site/register";
    }
    
    @GetMapping( "/login")
    public String getLoginPage() {
        return "/site/login";
    }
    
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }
    
    // http://localhost:8080/community/activation/101/code
    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }
    
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        
        // 将验证码存入session
        //session.setAttribute("kaptcha", text);
        
        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        // 要在未登录的时候生成该用户的标识
        // 增加一个有效时间较短的cookie
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入redis
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey,text,60, TimeUnit.SECONDS);
        
        
        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            log.error("响应验证码失败:" + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response,
                        @CookieValue(value = "kaptchaOwner",required = false,defaultValue = "")String kaptchaOwner) {
        // 检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        
        if (StringUtils.isNoneBlank(kaptchaOwner)) {//验证码未过期
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        } else {
            model.addAttribute("codeMsg", "验证码已失效，请在60秒内输入验证码!");
            return "/site/login";
        }
        
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }
        
        //是否 “记住我”
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        // 检查账号,密码
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {//登录成功即生成ticket
            /**
             * todo 同一个浏览器的多个页面不能操作多个账户
             * 将key改为ticket+userid的形式就不会被覆盖了
             */
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }
    
    //登出，改变ticket的状态并删除cookie
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket,HttpServletResponse response) {
        userService.logout(ticket);
        CookieUtil.deleteCookie(response,contextPath,"ticket");
        SecurityContextHolder.clearContext();
        return "redirect:/login";//get请求的login
    }
    
    // 忘记密码-页面
    @GetMapping("/forget")
    public String getForgetPage() {
        return "/site/forget";
    }
    
    // 忘记密码-获取验证码
    @GetMapping("/forget/code")
    @ResponseBody
    public String getForgetCode(String email, HttpSession session) {
        if (StringUtils.isBlank(email)) {
            return CommunityUtil.getJSONString(1, "邮箱不能为空！");
        }
        
        // 发送邮件
        Context context = new Context();
        context.setVariable("email", email);
        String code = CommunityUtil.generateUUID().substring(0, 4);
        context.setVariable("verifyCode", code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "找回密码", content);
        
        // 保存验证码
        session.setAttribute("verifyCode", code);
        
        return CommunityUtil.getJSONString(0);
    }
    
    // 忘记密码-重置密码
    @PostMapping("/forget/password")
    public String resetPassword(String email, String verifyCode, String password, Model model, HttpSession session) {
        String code = (String) session.getAttribute("verifyCode");
        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg", "验证码错误!");
            return "/site/forget";
        }
        
        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) {
            return "redirect:/login";
        } else {
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }
    
}
