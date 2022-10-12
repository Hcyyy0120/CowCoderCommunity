package com.hcy.community.service;

import com.hcy.community.entity.User;
import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private UserService userService;
    
    /**
     * 关注
     * @param userId 进行关注操作的用户
     * @param entityType 被关注的实体类型
     * @param entityId 被关注的实体ID
     */
    public void follow(int userId,int entityType,int entityId) {
        //一次操作，关注数+1，粉丝数+1
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                
                operations.multi();
                redisTemplate.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return operations.exec();
            }
        });
    }
    
    /**
     * 取关
     * @param userId 进行取关操作的用户
     * @param entityType 被取关的实体类型
     * @param entityId 被取关的实体ID
     */
    public void unfollow(int userId,int entityType,int entityId) {
        //一次操作，关注数-1，粉丝数-1
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                
                operations.multi();
                redisTemplate.opsForZSet().remove(followeeKey,entityId);
                redisTemplate.opsForZSet().remove(followerKey,userId);
                return operations.exec();
            }
        });
    }
    
    /**
     * 查询关注的实体的数量
     * @param userId
     * @param entityType
     * @return
     */
    public long findFolloweeCount(int userId,int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }
    
    /**
     * 查询实体的粉丝数量
     * @return
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    
    /**
     * 查询当前用户是否已关注该实体
     * @return
     */
    public boolean hasFollowed(int userId,int entityType,int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        //zset没有可以直接查询某个值是否存在的api,故采用此方法
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null;
    }
    
    /**
     * 查询当前用户是否已关注该实体
     * @return
     */
    public boolean hasFollowed(User user,int userId) {
        if (user == null) {
            return false;
        }
        return this.hasFollowed(user.getId(),ENTITY_TYPE_USER,userId);
    }
    
    /**
     * 查询某用户关注的人(需要返回的是 用户对象+关注时间 的一个整合数据列,
     * 故使用List<Map<String,Object>>)
     * @return
     */
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        //根据score获取关注用户列表的id
        //此处和mysql不同，查询范围是[offset-->offset+limit-1]
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetIds == null) {
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            HashMap<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
    
    /**
     * 查询某用户的粉丝
     * @return
     */
    public List<Map<String,Object>> findFollowers(int userId,int offset,int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER,userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds == null) {
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            HashMap<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
