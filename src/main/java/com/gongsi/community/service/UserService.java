package com.gongsi.community.service;

import com.gongsi.community.dao.UserMapper;
import com.gongsi.community.entity.LoginTicket;
import com.gongsi.community.entity.User;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.MailClient;
import com.gongsi.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
//    @Autowired
//    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${community.path.domain}")
    private String domain;


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
            clearCache(user_id);
            return ACTIVATION_SUCCESS;//
        }
        else{
            return ACTIVATION_FAILURE;
        }
    }
    //登录的时候会输入用户账号，密码和希望凭证过期的时间
    public Map<String,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map=new HashMap<>();
        //1.空值处理
        if(StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)) {
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        //2.对合法性进行验证
        //验证账号，根据传入的用户名查询数据库中是否存在账号
        User user=userMapper.getUserByName(username);
        if(user==null)
        {
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        //验证账号状态，账号存在的话看有没有激活，没有激活不能登录
        if(user.getStatus()==0)
        {
            map.put("usernameMsg","该账号未激活");
            return map;
        }
        //验证密码：账号已激活就可以验证密码
        password = CommunityUtil.md5(password+user.getSalt());
        if(!password.equals(user.getPassword()))
        {
            map.put("passwordMsg","密码不正确");
            return map;
        }
        //登录成功，需要生成登录凭证（发放个令牌）
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setStatus(0);//0是有效的状态哈，这里
        loginTicket.setUser_id(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        //因为单位是ms，所以乘1000
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey= RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //redisTemplate配置相应的序列化方式会把对象序列化为字符串，再利用opsForValue操作对象把这个String类型的数据存入到redis中。
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        map.put("ticket",loginTicket.getTicket());//把ticket放到map中方便后续返回给客户端
        //因为controller会调用业务层方法得到这个返回值map，就可以获取ticket
        return map;

    }
    public void logout(String ticket)
    {
        String redisKey=RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket=(LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }
    public LoginTicket findLoginTicket(String ticket){
//        return loginTicketMapper.selectLoginTicketByTicket(ticket);
        String redisKey=RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }
    public int updateHeader(int userId, String headerUrl) {
        int rows=userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }
    public User findUserByName(String username) {
        return userMapper.getUserByName(username);
    }
    public User findUserById(int userId){
        //return userMapper.getUserById(id);
        User user=getCache(userId);
        if(user==null)
        {
            user=initCache(userId);
        }
        return user;
    }
    //1.优先从缓存中取值
    public User getCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
        return (User)redisTemplate.opsForValue().get(redisKey);
    }
    //2.缓存未命中时初始化缓存数据：存的是用户对象而不是用户id
    public User initCache(int userId){
        User user=userMapper.getUserById(userId);
        String redisKey=RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }
    //3.数据变更时清除缓存数据
    public void clearCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}
