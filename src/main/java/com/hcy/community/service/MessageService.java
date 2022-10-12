package com.hcy.community.service;

import com.hcy.community.entity.Message;
import com.hcy.community.entity.User;
import com.hcy.community.mapper.MessageMapper;
import com.hcy.community.util.HostHolder;
import com.hcy.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Service
public class MessageService {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private SensitiveFilter sensitiveFilter;
    
    @Autowired
    private HostHolder hostHolder;
    
    @Autowired
    private UserService userService;
    
    /**
     * 查询当前用户的会话列表，针对每个会话只返回一条最新私信
     * @return
     */
    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }
    
    /**
     * 查询当前用户的总会话数量
     * @param userId
     * @return
     */
    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }
    
    /**
     * 查看某个会话包含的私信列表
     * @param conversationId
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }
    
    /**
     * 查询某个会话所包含的私信数量
     * @param conversationId
     * @return
     */
    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }
    
    /**
     * 查询 当前会话/当前用户 未读的私信数量
     * @return
     */
    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }
    
    /**
     * 新增消息
     * @param message
     * @return
     */
    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }
    
    /**
     * 修改消息的状态
     * @param ids
     * @return
     */
    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }
    
    /**
     * 生成会话列表详细信息页
     */
    public List<Map<String, Object>> getLetterListDetail(List<Message> conversationsList) {
        User user = hostHolder.getUser();
        
        List<Map<String, Object>> conversations = new ArrayList<>();
    
        //生成会话列表详情
        if (conversationsList != null) {
            for (Message message : conversationsList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", this.findLetterCount(message.getConversationId()));
                map.put("unreadCount", this.findLetterUnreadCount(user.getId(), message.getConversationId()));
                //会话列表里的私信有当前user给别人发的，也有别人发给当前user的
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));
            
                conversations.add(map);
            }
        }
        return conversations;
    }
    
    /**
     * 生成私信详情页
     * @return
     */
    public List<Map<String, Object>> getLetterMessageDetail(List<Message> letterList) {
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("letter",message);
                map.put("fromUser",userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        return letters;
    }
    
    /**
     * 获取私信的发送用户
     * @return
     */
    public User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        
        if (hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }
    
    /**
     * 获取私信id
     * @param letterList
     * @return
     */
    public List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId()) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }
    
    /**
     * 删除私信
     * @param id
     * @return
     */
    public int deleteMessage(int id) {
        return messageMapper.updateStatus(Arrays.asList(new Integer[]{id}), 2);
    }
    
    /**
     * 查询某个主题下最新的通知
     * @return
     */
    public Message findLatestNotice(int userId,String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }
    
    /**
     * 查询某个主题所包含的通知数量
     * @return
     */
    public int findNoticeCount(int userId,String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }
    
    /**
     * 查询未读的通知数量
     * @return
     */
    public int findNoticeUnreadCount(int userId,String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }
    
    public List<Message> findNotices(int userId,String topic,int offset,int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
}
