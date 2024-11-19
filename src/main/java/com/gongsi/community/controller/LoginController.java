package com.gongsi.community.controller;

import com.gongsi.community.entity.User;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.google.code.kaptcha.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller

//获取注册的页面,最终返回模板的路径
public class LoginController implements CommunityConstant {
    public static final Logger logger= LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;//方便调用业务功能：注册
    @Autowired
    private Producer kaptchaProducer;
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
        //激活失败不能登录，就跳转到首页，需要model传递两个动态参数给模板，即操作结果信息和即将跳转页面的路径
    }
    @RequestMapping(path="/kaptcha",method=RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session)
    {
        String text=kaptchaProducer.createText();
        //根据配置生成字符串后用这个字符串画一个图片
        BufferedImage image=kaptchaProducer.createImage(text);

        //将验证码存入session，后续才可以和用户输入作比较
        session.setAttribute("kaptcha", text);
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
}
//写完service的注册功能，写controller处理前后端交互的逻辑，提交注册的表单数据
