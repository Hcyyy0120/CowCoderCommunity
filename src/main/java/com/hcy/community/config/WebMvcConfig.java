package com.hcy.community.config;

import com.hcy.community.controller.interceptor.LoginRequiredInterceptor;
import com.hcy.community.controller.interceptor.LoginTicketInterceptor;
import com.hcy.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
    
    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;
    
    @Autowired
    private MessageInterceptor messageInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注意拦截器配置的顺序
        
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
        
        
        //registry.addInterceptor(loginRequiredInterceptor)
        //        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
        
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
    
}
