package com.hcy.community.config;

import com.hcy.community.util.CommunityConstant;
import com.hcy.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    /*
    比如你没有登录然后直接访问user页面，会因为没有权限被重定向到登录页面，
    输入账号密码点击登录按钮那一刻发起登录请求，先经过filter，但因为security没有拦截login请求，所以通过，
    再经过LoginTicketInterceptor，此时还没执行到登录的业务，preHandle里那个if(ticket!=null)为false，
    也就是cookie里没有ticket凭证，SecurityContextHolder也不会存东西，之后才到登录controller执行登录业务，
    主要就是在cookie里存凭证，登录请求结束后回重定向到首页，也就是重新发起了一次请求，同样会先经过filter，
    此时SecurityContextHolder里也是没东西的，但因为security配置类并没有拦截首页index请求，
    所以通过filter进入LoginTicketInterceptor，此时if(ticket!=null)为true，才会执行if体里的hostHolder和存SecurityContextHolder，
    这才算认证成功，也就是老师所说的“认证的进行依赖于前一次请求的拦截器处理”，这时你再访问/letter/list，可以成功，
    因为在这次请求里，Filter根据刚才的认证结果，判断出来你有了权限
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        //忽略静态资源
        web.ignoring().antMatchers("/resources/**");
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(//拦截
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like/**",
                        "/follow/**",
                        "/unfollow/**"
                )
                .hasAnyAuthority(//所需权限
                        AUTHORITY_USER,
                        AUTHORITY_MODERATOR,
                        AUTHORITY_ADMIN
                )
                .antMatchers("/discuss/top","discuss/wonderful").hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete").hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll()//其余的放行
                .and().csrf().disable();
        
        //权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    //未登录时的处理
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {//如果是异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"请登录！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    //登录但权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {//如果是异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有权限访问！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        
        //security底层默认会拦截/logout请求，进行退出处理
        //而security是利用filter，会在controller执行前将请求拦截，然后结束请求
        //要覆盖它默认的逻辑，才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");//覆盖
        
    }
}
