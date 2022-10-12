package com.hcy.community;

import com.hcy.community.entity.LoginTicket;
import com.hcy.community.mapper.LoginTicketMapper;
import com.hcy.community.mapper.MessageMapper;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

@ContextConfiguration(classes = CommunityApplication.class)
@SpringBootTest
public class MapperTest {
    
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Test
    public void testInsertLoginTicket() {
        LoginTicket ticket = new LoginTicket();
        ticket.setUserId(103);
        ticket.setTicket("abc");
        ticket.setStatus(0);
        ticket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));
        loginTicketMapper.insertLoginTicket(ticket);
    }
    
    @Test
    public void testSelectAndUpdateLoginTicket() {
        LoginTicket ticket = loginTicketMapper.selectByTicket("abc");
        System.out.println(ticket);
        
        loginTicketMapper.updateStatus("abc",1);
        ticket = loginTicketMapper.selectByTicket("abc");
        System.out.println(ticket);
    }
    
    @Test
    public void testSelectTicketByUserId() {
        //System.out.println(messageMapper.selectConversations(111, 0, 20));
    
        //System.out.println(messageMapper.selectConversationCount(111));
    
        //System.out.println(messageMapper.selectLetters("111_112", 0, 10));
    
        //System.out.println(messageMapper.selectLetterUnreadCount(131, "111_131"));
    }
}
