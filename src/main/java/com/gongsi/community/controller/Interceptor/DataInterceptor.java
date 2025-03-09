package com.gongsi.community.controller.Interceptor;

import com.gongsi.community.entity.User;
import com.gongsi.community.service.DataService;
import com.gongsi.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {
    //要记入uv/dau的值，所以引入
    @Autowired
    private DataService dataService;
    //每台机子都有一个当前登录的用户
    @Autowired
    private HostHolder hostHolder;

    //在请求之前截获
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //统计uv
        String ip=request.getRemoteHost();
        dataService.recordUV(ip);

        //统计dau
        //从当前线程获取用户
        User user=hostHolder.getUser();
        if(user!=null){
            dataService.recordDAU(user.getId());
        }
        return true;//让请求继续下去
    }


}
