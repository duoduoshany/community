package com.gongsi.community.controller.Interceptor;


import com.gongsi.community.entity.User;
import com.gongsi.community.service.MessageService;
import com.gongsi.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {
    @Autowired
    private MessageService messageService;
    @Autowired
    private HostHolder hostHolder;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) throws Exception {
        User user=hostHolder.getUser();
        //如果用户登录了，才会拦截显示未读消息数量，确保要携带数据的modelAndView不为空
        if(user!=null&&modelAndView!=null){
            int letterUnreadCount=messageService.findLetterUnreadCount(user.getId(),null);
            int noticeUnreadCount=messageService.findNoticeUnreadCount(user.getId(),null);
            modelAndView.addObject("allUnreadCount",letterUnreadCount+noticeUnreadCount);
        }
    }
}
