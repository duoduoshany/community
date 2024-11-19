package com.gongsi.community.service;

import com.gongsi.community.dao.UserMapper;
import com.gongsi.community.entity.User;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Value("${community.path.domain}")
    private String domain;

    public User findUserById(int id){
        return userMapper.getUserById(id);
    }
    //返回的内容主要包含多种错误信息,希望返回类型可以封装多种内容。
    //注册时希望把用户注册信息传递给方法
    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<String,Object>();
        // 对空值做判断处理，直接抛异常
        if(user==null)
        {
            throw new IllegalArgumentException("参数不能为空！");
        }
        //对象虽然不是空，但对象里的属性可能有问题,比如用户名为空
        //用户名为空不是程序的问题，所以不能抛个异常，应该返给客户端，提示这样操作不行
        if(StringUtils.isBlank(user.getUsername()))
        {
            //往map中装入提示信息
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword()))
        {
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail()))
        {
            map.put("emailMsg","邮箱不能为空");
            return map;
        }
        //关键信息都在，访问数据库查看传入的用户名在数据库中是否存在,存在用户的话不能注册
        User u=userMapper.getUserByName(user.getUsername());
       if(u!=null)
       {
           map.put("usernameMsg","该用户已存在");
           return map;
       }
       //查看传入的邮箱在数据库中是否已注册
       u=userMapper.getUserByEmail(user.getEmail());
       if(u!=null){
           map.put("emailMsg","该邮箱已被注册");
       }

        user.setSalt(CommunityUtil.generateUUID().substring(0,5));//subString左闭右开，生成的随机字符串可以不用特别长，这里限制了长度
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        //user的其余属性也给设置一下，因为用户填写的表单信息没有完全覆盖用户的属性
        user.setStatus(0);//状态都是0，表示还没有激活
        //要激活，给用户设置激活码，到时候激活链接包含这个激活码，激活码还是随机字符串
        user.setActivation_code(CommunityUtil.generateUUID().toString());
        user.setHeader_url(String.format("https://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        //账号创建时间为当前时间即可
        user.setCreate_time(new Date());
        userMapper.insertUser(user);//把这个初始化好的对象添加到数据库,这时传进的user对象就自动生成id了，mybatis底层实现的
        User insertedUser = userMapper.getUserById(user.getId());
        //要给用户发送激活邮件，那肯定是发送html邮件才能携带激活链接
        //context设置thymleaf模板中的动态参数
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/activation/user_id/activation_code
        //拼好url
        String url=domain+"/activation/"+user.getId()+"/"+user.getActivation_code();
        context.setVariable("url",url);
        String content=templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);
        //map是空，表明没有问题，看前面，map封装的都是错误信息的内容
        return map;
    }
    //处理激活逻辑的时候需要用到常量，就让当前这个类实现这个接口，才能使用接口中的常量
    //int类型返回成功还是失败的状态
    //通过激活链接可见需要给用户id和激活码
    public int activation(int user_id,String activation_code){
        User user=userMapper.getUserById(user_id);
        if(user.getStatus()==1)//表明已经激活过了，就return重复激活
        {
            return ACTIVATION_REPEAT;
        }
        if(user.getActivation_code().equals(activation_code))
        {
            //在不是重复激活前提下的传入的激活码跟用户的激活码一样，才是激活成功
            userMapper.updateStatus(user_id,1);//需要把用户的状态改成1
            return ACTIVATION_SUCCESS;//
        }
        else{
            return ACTIVATION_FAILURE;
        }
    }
}
