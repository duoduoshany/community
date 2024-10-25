package com.gongsi.community.dao;

import com.gongsi.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

//为了让Spring容器装配或者说标识这个Bean，注解@Repository也可以用Mybatis的@Mapper
@Mapper
public interface UserMapper {
    User getUserById(int id);//根据id查询User
    User getUserByName(String username);//根据用户名查询User
    User getUserByEmail(String email);//根据邮箱查询User
    int insertUser(User user);//插入User对象，返回增加的条数
    int updateStatus(int id,int status);//修改User状态，返回修改的条数
    int updateHeader(int id,String header_url);//条件是id，更新参数是用户头像
    int updatePassword(int id,String password);//条件是id，更新参数是密码

}
