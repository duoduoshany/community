package com.gongsi.community.controller;

import com.gongsi.community.util.CommunityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class Little {
    @RequestMapping(path="/cookie/set",method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response)
    {
        //创建cookie
        Cookie cookie=new Cookie("cookie", CommunityUtil.generateUUID());
        //设置cookie有效范围
        cookie.setPath("/alpha");
        //设置cookie的生存时间
        cookie.setMaxAge(600);
        //发送cookie
        response.addCookie(cookie);
        return "set cookie success!";
    }
    @RequestMapping(path="/cookie/get",method = RequestMethod.GET)
    @ResponseBody
    public String getCookie()
    {
        return "get cookie success!";
    }
    //cookie是小数据原因是因为它需要来回传，存储大量数据影响性能，再者存Java数据传给客户端也识别不了
    //而session就可以存大量键值对数据了，传的只是session ID
    @RequestMapping(path="/session/set",method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session)
    {
        //存储于服务器的session对象属性设置如下：
        session.setAttribute("id",1);
        session.setAttribute("name","test");
        //存完自动生成sessionID
        //没有设置有效范围，默认整个项目有效，没有设置有效时间，默认关闭浏览器失效
        return "set session success!";
    }
    //从session中取值
    @RequestMapping(path="/session/get",method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session)
    {
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session success!";
    }

}
