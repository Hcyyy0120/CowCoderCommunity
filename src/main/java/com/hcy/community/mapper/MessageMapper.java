package com.hcy.community.mapper;

import com.hcy.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    
    //查询当前用户的会话列表，针对每个会话只返回一条最新私信
    List<Message> selectConversations(@Param("userId") int userId,@Param("offset") int offset,@Param("limit") int limit);
    
    //查询当前用户的总会话数量
    int selectConversationCount(@Param("userId")int userId);
    
    //查看某个会话包含的私信列表
    List<Message> selectLetters(@Param("conversationId")String conversationId,@Param("offset") int offset,@Param("limit") int limit);
    
    //查询某个会话所包含的私信数量
    int selectLetterCount(@Param("conversationId")String conversationId);
    
    //查询 当前会话/当前用户 未读的私信数量
    int selectLetterUnreadCount(@Param("userId")int userId,@Param("conversationId") String conversationId);
    
    // 新增消息
    int insertMessage(Message message);
    
    // 修改消息的状态
    int updateStatus(List<Integer> ids, int status);
    
    //查询某个主题下最新的通知
    Message selectLatestNotice(@Param("userId")int userId,@Param("topic")String topic);
    
    //查询某个主题所包含的通知数量
    int selectNoticeCount(@Param("userId")int userId,@Param("topic")String topic);
    
    //查询未读的通知的数量
    int selectNoticeUnreadCount(@Param("userId")int userId,@Param("topic")String topic);
    
    //查询某个主题所包含的通知列表详情
    List<Message> selectNotices(@Param("userId")int userId,@Param("topic")String topic,@Param("offset")int offset,@Param("limit")int limit);
    
    
}
