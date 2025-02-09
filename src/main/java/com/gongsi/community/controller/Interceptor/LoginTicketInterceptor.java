package com.gongsi.community.controller.Interceptor;

import com.gongsi.community.entity.LoginTicket;
import com.gongsi.community.entity.User;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CookieUtil;
import com.gongsi.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor{
    //应该在请求的一开始就获取ticket，从而利用ticket查找有没有对应的用户
    //拦截器中的逻辑主要服务于已经登录的用户,在请求调用之前，如用户个人信息界面的请求
    // 将用户信息提前准备好，从而方便后续的业务逻辑处理。
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    //此处不用@COOKIEVALUE注解去获取ticket是因为自带的接口方法定义好的参数不能随便更改
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //cookie是通过request传回来的，所以得到cookie
        String ticket = CookieUtil.getValue(request,"ticket");
        if(ticket!=null)
        {
            LoginTicket loginTicket= userService.findLoginTicket(ticket);
            //检查凭证是否有效
            if(loginTicket!=null&&loginTicket.getStatus()==0&&loginTicket.getExpired().after(new Date()))
            {
                User user= userService.findUserById(loginTicket.getUser_id());
                //浏览器和服务器是多对一的关系，服务器处理多个请求是并发的
                //服务器处理请求是多线程环境，为了让每个线程的用户信息之间不互相干扰且不必传递session对象，使用ThreadLocal
                //在本次请求中持有用户
                hostHolder.setUser(user);
            }
        }
        return true;
    }
    //模板引擎要用到user，所以在模板引擎被调用之前，应该把user存入到model中
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user=hostHolder.getUser();
        if(user!=null && modelAndView!=null)
        {
            modelAndView.addObject("loginUser", user);
        }
    }
    //模板都执行完之后，就清理这个user对象，不影响原先渲染的页面
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }

}
