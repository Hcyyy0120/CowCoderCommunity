package com.hcy.community;

import com.hcy.community.mapper.DiscussPostMapper;
import com.hcy.community.util.MailClient;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ContextConfiguration(classes = CommunityApplication.class)
@SpringBootTest
class CommunityApplicationTests {
    
    @Autowired
    private DiscussPostMapper discussPostMapper;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private MailClient mailClient;
    
    @Test
    public void testSelectPosts() {
        System.out.println(discussPostMapper.selectDiscussPosts(149, 0, 10,0));
        System.out.println(discussPostMapper.selectDiscussPostRows(0));
    }
    
    /**
     * 发送文本邮件
     */
    @Test
    public void testTextMail() {
        mailClient.sendMail("HHH20010120@163.com","TEST","测试");
    }
    
    /**
     * 发送HTML邮件
     */
    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username","jack");
        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);
        mailClient.sendMail("HHH20010120@163.com","HTML",content);
    }
}
