package com.hcy.community;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

@SpringBootTest
public class RedisTest {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Test
    public void testStrings() {
        String redisKey = "test:count";
        redisTemplate.opsForValue().set(redisKey,1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }
    
    //编程式事务
    /*
        redis事务：（简单描述）
            当开启事务后，每执行一个redis命令，都先将该命令存放在一个队列中，
            当提交事务的时候，在从队列中将命令发送给redis服务器。
            因此，我们不能在事务中间进行查询操作。
            也因此redis不推荐使用声明式事务（一整个方法中不能执行查询操作）。
     */
    @Test
    public void testTransactional() {
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";
                operations.multi();//启动事务
                operations.opsForSet().add(redisKey,"张三");
                operations.opsForSet().add(redisKey,"李四");
                operations.opsForSet().add(redisKey,"王五");
                System.out.println(operations.opsForSet().members(redisKey));
                return operations.exec();//提交事务
            }
        });
        System.out.println(obj);
    }
    
}
