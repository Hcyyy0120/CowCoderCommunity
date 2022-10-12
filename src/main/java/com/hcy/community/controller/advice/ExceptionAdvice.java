package com.hcy.community.controller.advice;

import com.hcy.community.util.CommunityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    
    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletResponse response, HttpServletRequest request) throws IOException {
        log.error("服务器发生异常" + e.getMessage());
    
        String xRequestedWith = request.getHeader("x-requested-with");
        //如果是异步请求
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1,"服务器异常！"));
        } else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
