package com.gongsi.community.config;

import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
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
    //重写该类的三个configure方法，只是所带的参数不一样
    //第一个环节忽略对静态资源的拦截
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }
    //第二个环节对登录和退出做认证，但由于我们已经写好了对登录的认证，所以绕过这个环节
    //登录的认证：对账号密码是否为空，账号合理性，账号和密码是否匹配，用户状态是否激活做认证

    //第三个环节：授权：哪些路径是登录以后才能访问的，哪些权限的用户可以访问，由security拦截没有权限的访问
    //管理员和版主特有的访问路径
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/user/setting",
                        "/user/upload",
                        "/user/profile",
                        "/discuss/add",//不能发帖
                        "/comment/add/**",//不能评论，下面还有一级，所以/**
                        "/letter/**",//要登录才能访问私信
                        "/notice/**",//要登录才能有通知
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers("/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers("/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                )
                .hasAnyAuthority(AUTHORITY_ADMIN)

                .anyRequest().permitAll()
                .and().csrf().disable();

        //没有权限却访问了某些路径的异常的处理方案
        // 对于普通请求，返回一个错误提示的网页；
        // 对于异步请求，不改变地址栏url的则在页面显示错误提示的json字符串，因此分类处理
        http.exceptionHandling()
                //有个entry：进入，也就是没有登录时要怎么处理
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {//xml中的x表明是异步请求ajax
                            //声明我要返回的数据类型:普通字符串，支持中文的字符集
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter out = response.getWriter();//从response中获取字符流
                            //通过字符流向前端输出json字符串,用前面定义的工具类构造json字符串
                            out.write(CommunityUtil.getJSONString(403, "你还没有登录！"));
                        } else {
                            //如果不是异步请求，就重定向到登录页面，发送/login请求，调用后端映射/login路径的方法，该方法返回登录页面
                            response.sendRedirect("/login");
                        }
                    }
                })
                //登录后权限不足时的处理
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter out = response.getWriter();
                            out.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限"));
                        } else {
                            response.sendRedirect("/denied");
                        }
                    }
                });
        //Security默认拦截/logout请求，一旦拦截，程序就不会继续往下走，于是我们自己写的logout的退出功能就被阻止：相较于security的返回首页，多了一个将登录凭证的状态修改为失效的过程
        //如果不想执行security的退出逻辑，就需要绕过,把它拦截的请求修改成别的即可,而这个请求我们没有写别的方法，只要不访问这个请求就不会触发security的退出
        http.logout().logoutUrl("/securitylogout");
    }


}

