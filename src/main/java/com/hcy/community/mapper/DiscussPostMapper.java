package com.hcy.community.mapper;

import com.hcy.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    /**
     * 分页显示帖子
     * @param userId
     * @return
     */
    List<DiscussPost> selectDiscussPosts(@Param("userId")int userId,@Param("offset")int offset,@Param("limit")int limit,@Param("orderMode")int orderMode);
    
    /**
     * 查询帖子数量
     * @param userId
     * @return
     */
    int selectDiscussPostRows(@Param("userId") int userId);
    
    /**
     * 发布帖子
     * @return
     */
    int insertDiscussPost(DiscussPost discussPost);
    
    /**
     * 帖子详情
     * @param id
     * @return
     */
    DiscussPost selectDiscussPostById(@Param("id") int id);
    
    int updateCommentCount(@Param("id")int id,@Param("commentCount")int commentCount);
    
    int updateType(@Param("id")int id,@Param("type")int type);
    
    int updateStatus(@Param("id")int id,@Param("status")int status);
    
    int updateScore(@Param("id")int id, @Param("score")double score);
}
