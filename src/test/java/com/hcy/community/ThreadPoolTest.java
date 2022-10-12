package com.hcy.community;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ContextConfiguration(classes = CommunityApplication.class)
@SpringBootTest
public class ThreadPoolTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTest.class);
    
    //JDK普通线程池
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    //JDK可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    
    //Junit不像main方法，会等待创建的子线程执行结束
    private void sleep(long m) {//不需去关注此方法(是为弥补Junit的缺陷而采取的方案)
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    //JDK普通线程池
    @Test
    public void testExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("ExecutorService");
            }
        };
        
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }
        
        sleep(10000);
    }
    
    //JDK定时任务线程池
    @Test
    public void testScheduledExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("ScheduledExecutorService");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task,10000,1000, TimeUnit.MILLISECONDS);
        sleep(30000);
    }
    
}
