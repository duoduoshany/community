package com.gongsi.community.util;

import com.gongsi.community.entity.User;
import org.springframework.stereotype.Component;

@Component
//持有用户信息，代替session对象
public class HostHolder {
    private ThreadLocal<User> users=new ThreadLocal<User>();
    //方便外界存入user与当前进程关联起来
    public void setUser(User user) {
        users.set(user);
    }
    //方便外界获取user
    public User getUser() {
        return users.get();
    }
    //往里存的时候也要记得清除不需要的user，不然容器一直存肯定过载
    //清除当前threadLocal的值，即threadLocal关联的用户值，而不是全部清空
    public void clear() {
        users.remove();
    }
}
