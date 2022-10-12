package com.hcy.community.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
/*@Component
@Aspect*/
public class ServiceLogAspect {
    
    @Pointcut("execution(* com.hcy.community.service.*.*(..))")
    public void pointcut() {
    
    }
    
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        //用户[1.2.3.4],在[xxx],访问了[xxx]
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteHost();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Signature signature = joinPoint.getSignature();
        String target = signature.getDeclaringTypeName() + "." + signature.getName();
        log.info(String.format("用户[%s],在[%s],访问了[%s].", ip, now, target));
    }
}
