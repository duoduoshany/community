package com.gongsi.community.controller;

import com.gongsi.community.entity.User;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller

//获取注册的页面,最终返回模板的路径
public class LoginController implements CommunityConstant {
    public static final Logger logger= LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;//方便调用业务功能：注册
    @Autowired
    private Producer kaptchaProducer;
    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "site/register";
    }
    //增加控制层方法，处理要访问/login路径的请求
    // 其它方法虽然返回了路径/login，但不能直接访问登录页面，因此访问这个路径时要有相应处理这个路径请求的controller方法。
    // 方法需要返回site/login才能找到登陆页面
    @RequestMapping(path="/login",method=RequestMethod.GET)
    public String getLoginPage() {
        return "site/login";
    }

    //浏览器向服务器提交数据，请求必须是post请求.
    // 注意路径是一样的，但请求方法是不一样的,所以调用不同的控制层方法
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    //model对象封装数据携带给模板，user被自动注入到model对象中，html页面可以通过model访问user
    public String postRegisterPage(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty())//说明注册成功，注册成功后跳到一个操作结果的页面，提示跳转到首页，慢慢跳转到首页不会立即跳
        {
            //model传递动态参数给模板，模板再添加到操作结果页面
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活");
            //跳转的目标
            model.addAttribute("target", "/index");
            return "site/operate-result";
        } else {//注册失败，返回注册页面
            // 把错误消息的参数都封装到model中传递给模板,模板选择错误消息对应的参数添加到注册页面
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "site/register";
        }
    }

    //http://localhost:8080/activation/user_id/activation_code
    //user_id和激活码不能写si，用{}
    //激活的时候邮件上不是给提供一个表单，所以请求方法是get即可
    //依然使用model向模板传参
    //服务器需要有处理激活链接请求的方法
    @RequestMapping(path = "/activation/{user_id}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("user_id") int user_id
            , @PathVariable("code") String code) {
        //将路径中获取的参数赋值给方法的参数，再将方法的参数传递给activation方法。
        int result = userService.activation(user_id, code);
        if (result == ACTIVATION_SUCCESS) {//激活成功，跳到登录页面
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作：该账号已经激活");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确");
            model.addAttribute("target", "/index");
        }
        return "site/operate-result";
        //激活失败不能登录，就跳转到首页模板，此时model已经传递两个动态参数给模板，即操作结果信息和即将跳转页面的路径
    }
    @RequestMapping(path="/kaptcha",method=RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session)
    {
        String text=kaptchaProducer.createText();
        //根据配置生成字符串后用这个字符串画一个图片
        BufferedImage image=kaptchaProducer.createImage(text);

        //将验证码存入session，后续才可以和用户输入作比较
        //session.setAttribute("kaptcha", text);

        //先有字符串，把这个字符串传给cookie用于客户端标识，再按照这个字符串创建一个key
        String kaptchaOwner= CommunityUtil.generateUUID();
        Cookie cookie=new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(2*60);//这个cookie的生存时间设成60s
        //cookie.setPath(CONTEXTPATH);不做设置，默认整个项目有效
        //构建好cookie，发送给客户端
        response.addCookie(cookie);
        //将验证码存入redis
        String redisKey= RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,2*60, TimeUnit.SECONDS);//超过60s失效

        //将图片输出给浏览器，需要先向浏览器声明返回的数据类型
        response.setContentType("image/png");
        //正式输出图片：response向浏览器做响应需要获取输出流
        //流不需要手动关闭，因为SpringMVC会管理字节流
        try {
            OutputStream os=response.getOutputStream();//流需要有异常处理
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败："+e.getMessage());//顺便把失败的信息做一个记录
        }

    }
    //两个方法使用一样的请求路径也可以，但是请求方法必须不一样，跟注册是相同道理的,这样才会识别出该请求要执行哪个方法
    @RequestMapping(path="/login",method=RequestMethod.POST)
    public String login(String username, String password, String code,Model model,
                        boolean remember  /*HttpSession session*/,HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        //String kaptcha=session.getAttribute("kaptcha").toString();//当时存的是Object类型，要进行相应的转换

        //kaptcha从redis中取，先构造key->key需要字符串->字符串从cookie中取
        //前面给cookie设置了失效时间，如果cookie过期，@CookieValue获取不到值，后面的参数就被初始化为null

        String kaptcha=null;//还没从redis中取验证码，kaptcha是空的
        //如果cookie失效，就不会获取键对应的值
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey= RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha=(String)redisTemplate.opsForValue().get(redisKey);
        }
        if( StringUtils.isBlank(kaptcha)||StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code))
        {
            model.addAttribute("codeMsg","验证码不正确");
            return "site/login";
        }
        //检查账号、密码
        int expiredSeconds=remember?REMENBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        Map<String,Object> map=userService.login(username,password,expiredSeconds);
        if(map.containsKey("ticket"))
        {
            Cookie cookie=new Cookie("ticket",map.get("ticket").toString());//cookie的名称和cookie具体存储的内容，从map中获取键ticket对应的value值
            //要求把Object类型转为String，因为Cookie要传入两个String类型的参数
            //cookie的有效路径，在访问哪些url时发送给服务器，这里应该是整个项目，但没有设置/community，所以设置/代表根路径
            cookie.setPath("/");
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);//设置好cookie就放入响应对象中发送给浏览器
            return "redirect:/index";
        }
        else{
            //激活状态和用户不在数据库中存在都对应同一个键
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login";//登录失败无需跳转
        }

    }
    @RequestMapping(path="/logout",method=RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket)
    {
        userService.logout(ticket);
        //退出的话可以重新登录，所以重定向到登录
        return "redirect:/login";
    }
}
//写完service的注册功能，写controller处理前后端交互的逻辑，提交注册的表单数据
