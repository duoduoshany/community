package com.gongsi.community.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    //key，比如ticket这个键名
    public static String getValue(HttpServletRequest request, String key) {
        if(request==null||key==null)
        {
            throw new IllegalArgumentException("参数为空！");
        }
        Cookie[] cookies=request.getCookies();
        //cookie!=null的时候遍历cookie数组
        if(cookies!=null)
        {
            for(Cookie c:cookies)
            {
                if(c.getName().equals(key))
                {
                    return c.getValue();
                }
            }
        }
        //遍历之后没有我想要的数据，return null
        return null;
    }
}
