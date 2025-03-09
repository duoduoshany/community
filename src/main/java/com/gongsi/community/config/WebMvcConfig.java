package com.gongsi.community.config;

import com.gongsi.community.controller.Interceptor.DataInterceptor;
import com.gongsi.community.controller.Interceptor.DataInterceptor;
import com.gongsi.community.controller.Interceptor.LoginRequiredInterceptor;
import com.gongsi.community.controller.Interceptor.LoginTicketInterceptor;
import com.gongsi.community.controller.Interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;
//    @Autowired
//    private LoginRequiredInterceptor loginRequiredInterceptor;
    @Autowired
    private MessageInterceptor messageInterceptor;
    @Autowired
    private DataInterceptor dataInterceptor;
    @Override
    //这里为什么不需要排除"/login","/register", "/kaptcha"做排除，因为我已经设置了ticket为空的时候就获取不到user的判断了，也就不会进行一系列的拦截器代码处理逻辑
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpeg","/**/*.jpg","/**/*.png");
//        registry.addInterceptor(loginRequiredInterceptor)
//                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpeg","/**/*.jpg","/**/*.png");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpeg","/**/*.jpg","/**/*.png");
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.jpeg","/**/*.jpg","/**/*.png");
    }
}
