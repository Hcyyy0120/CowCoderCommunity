package com.hcy.community.service;

import com.hcy.community.annotation.LoginRequired;
import com.hcy.community.entity.Comment;
import com.hcy.community.entity.User;
import com.hcy.community.mapper.CommentMapper;
import com.hcy.community.mapper.DiscussPostMapper;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.SensitiveFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService implements CommunityConstant {
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private DiscussPostService discussPostService;
    
    @Autowired
    private SensitiveFilter sensitiveFilter;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private LikeService likeService;
    
    /**
     * 根据实体（评论、帖子、恢复）查询评论
     *
     * @return
     */
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }
    
    /**
     * 根据实体查询评论总数
     * @return
     */
    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType, entityId);
    }
    
    /**
     * 添加评论
     * 保证 添加评论 和 修改帖子评论数 为同一个事务
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int addComment(Comment comment) {
        if (StringUtils.isBlank(comment.getContent())) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        
        //添加评论
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);
        
        //更新帖子评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(),count);
        }
        return rows;
    }
    
    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }
    
    public List<Comment> findUserComments(int userId, int offset, int limit) {
        return commentMapper.selectCommentsByUser(userId, offset, limit);
    }
    
    public int findUserCount(int userId) {
        return commentMapper.selectCountByUser(userId);
    }
    
    
    
}
