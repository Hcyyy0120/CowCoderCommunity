package com.hcy.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {
    
    @PostConstruct//构造器被调用后执行此方法
    public void init() {
        //解决netty启动冲突问题
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }
    
    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
    
}
