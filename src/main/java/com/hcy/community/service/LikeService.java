package com.hcy.community.service;

import com.hcy.community.util.HostHolder;
import com.hcy.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    HostHolder hostHolder;
    
    /**
     * 点赞并增加数量
     * 一次点赞两次增加，要保证原子性
     * @param userId 点赞人
     * @param entityType 点赞目标实体
     * @param entityId 点赞目标实体ID
     * @param entityUserId 被    赞人的ID
     */
    public void like(int userId,int entityType,int entityId,int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                //该用户是否已给该实体点赞
                //此查询要放在事务之外,否则查询会在事务之后执行
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
                
                operations.multi();//开启事务
                if (isMember) {
                    //若已点赞，则取消
                    operations.opsForSet().remove(entityLikeKey,userId);
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    operations.opsForSet().add(entityLikeKey,userId);
                    operations.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
            }
        });
    }
    
    /**
     * 查询某实体获得点赞的数量
     * @return
     */
    public long findEntityLikeCount(int entityType,int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }
    
    /**
     * 查询某用户对某实体的点赞状态
     * @return 返回一个int是为了之后业务拓展，已点赞/未点赞/已踩/未踩
     */
    public int findEntityLikeStatus(int userId,int entityType,int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey,userId) ? 1 : 0;
    }
    
    /**
     * 查询某个用户获得的赞
     * @param userId
     * @return
     */
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }
}
