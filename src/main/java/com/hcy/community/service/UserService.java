package com.hcy.community.service;

import com.hcy.community.entity.LoginTicket;
import com.hcy.community.entity.User;
import com.hcy.community.mapper.LoginTicketMapper;
import com.hcy.community.mapper.UserMapper;
import com.hcy.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private MailClient mailClient;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    //@Autowired
    //private LoginTicketMapper loginTicketMapper;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Value("${community.path.domain}")
    private String domain;//邮件中链接指向的地址
    
    @Value("${server.servlet.context-path}")
    private String contextPath;//项目路径
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    /**
     * 根据id查询用户信息
     *
     * @param id
     * @return
     */
    public User findUserById(int id) {
        //return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }
    
    /**
     * 普通用户注册
     * @return
     */
    public Map<String, Object> register(User user) {
        HashMap<String, Object> map = new HashMap<>();
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在！");
            return map;
        }
        //验证邮箱
        u = userMapper.selectByName(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }
        //注册
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());//激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        
        //发邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //http://localhost:8080/community/activation/101/code
        //激活功能的路径
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "账号激活", content);
        
        return map;
    }
    
    /**
     * 账号激活
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return CommunityConstant.ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return CommunityConstant.ACTIVATION_SUCCESS;
        } else {
            return CommunityConstant.ACTIVATION_FAILURE;
        }
    }
    
    /**
     * 登录
     *
     * @param username
     * @param password
     * @param expiredSeconds
     * @return
     */
    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        HashMap<String, Object> map = new HashMap<>();
        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        //验证账号合法性
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        //验证账号状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }
/*        LoginTicket isLogin = checkIsLogin(username);
        if (isLogin != null) {
            //将别处登录的ticket的status设为1，强制下线
            loginTicketMapper.updateStatus(isLogin.getTicket(), 1);
        }*/
        
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);
        //将ticket存入redis
        String reidsKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(reidsKey, loginTicket);
        
        map.put("ticket", loginTicket.getTicket());
        return map;
    }
    
    /**
     * 登出
     *
     * @param ticket
     */
    @Transactional//查-->改-->存
    public void logout(String ticket) {
        //loginTicketMapper.updateStatus(ticket, 1);
        String reidsKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(reidsKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(reidsKey, loginTicket);
    }
    
    /**
     * 获取ticket
     *
     * @param ticket
     * @return
     */
    public LoginTicket findLoginTicket(String ticket) {
        //return loginTicketMapper.selectByTicket(ticket);
        String reidsKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(reidsKey);
        
    }
    
    /**
     * 查看此账号是否在别处已登录
     *
     * @return 若不为null 账号已在别处登录;为null，则此次登录正常进行
     */
/*    public LoginTicket checkIsLogin(String username) {
        User user = userMapper.selectByName(username);
        return loginTicketMapper.selectByUserId(user.getId());
    }*/
    
    //为了保证redis和mysql数据一致性。建议使用消息队列或者延迟双删保证最终一致性
    public int updateHeader(int userId, String headerUrl) {
        int i = userMapper.updateHeader(userId, headerUrl);
        if (i > 0) {
            clearCache(userId);
        }
        return i;
        
    }
    
    /**
     * 根据用户名查找用户
     *
     * @param username
     * @return
     */
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }
    
    /**
     * 重置密码
     *
     * @param email
     * @param password
     * @return
     */
    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();
        
        // 空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        
        // 验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }
        
        // 重置密码
        password = CommunityUtil.md5(password + user.getSalt());
        int i = userMapper.updatePassword(user.getId(), password);
        if (i > 0) {
            clearCache(user.getId());
        }
        
        map.put("user", user);
        return map;
    }
    
    
    /**
     * 修改密码
     *
     * @param userId
     * @param oldPassword
     * @param newPassword
     * @return
     */
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();
        
        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }
        
        // 验证原始密码
        User user = userMapper.selectById(userId);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "原密码输入有误!");
            return map;
        }
        
        // 更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        int i = userMapper.updatePassword(userId, newPassword);
        if (i > 0) {
            clearCache(user.getId());
        }
        return map;
    }
    
    /**
     * 从缓存中查用户信息
     *
     * @param userId
     * @return
     */
    private User getCache(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }
    
    /**
     * 将用户信息缓存到redis
     *
     * @return
     */
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    
    /**
     * 用户信息变更时清除缓存
     */
    private void clearCache(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }
    
    /**
     * 获取用户拥有的权限
     *
     * @param userId
     * @return
     */
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
