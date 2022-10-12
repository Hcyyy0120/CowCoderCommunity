package com.hcy.community.mapper;

import com.hcy.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    /**
     * 根据实体（评论、帖子、恢复）查询评论
     *
     * @return
     */
    List<Comment> selectCommentsByEntity(@Param("entityType")int entityType,@Param("entityId") int entityId,
                                         @Param("offset")int offset, @Param("limit")int limit);
    
    /**
     * 根据实体查询评论总数
     * @return
     */
    int selectCountByEntity(@Param("entityType")int entityType, @Param("entityId")int entityId);
    
    /**
     * 添加评论
     * @return
     */
    int insertComment(Comment comment);
    
    Comment selectCommentById(@Param("id")int id);
    
    List<Comment> selectCommentsByUser(@Param("userId")int userId, @Param("offset")int offset,@Param("limit") int limit);
    
    int selectCountByUser(@Param("userId")int userId);
}
