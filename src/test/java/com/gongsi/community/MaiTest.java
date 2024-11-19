package com.gongsi.community;

import com.gongsi.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
//测试代码为了引用CommunityApplication为配置类来自动创建Spring容器，标明以下注解
@ContextConfiguration(classes = CommunityApplication.class)
public class MaiTest {
    //注入MailClient类然后去调用它的方法（包含创建和构建邮件）
    @Autowired
    private MailClient mailClient;
    //发送html邮件,通常我们调用模板在MVC场景下，只要在Controller里返回模板的路径，前端控制器会自动帮我们调用模板引擎
    //在测试类中不能这么做，需要我们主动调用模板引擎
    @Autowired
    private TemplateEngine templateEngine;
    @Test
    public void testTextMail() {
        mailClient.sendMail("yilin1652@gmail.com","TEST","Success");
    }
    @Test
    public void testHtmlMail() {
        //由模板内容可见，需要给模板传个参数username,利用context传递参数，名字必须保持一致，值随便设
        Context context = new Context();
        context.setVariable("username","tansongyun");
        //把设置好的数据context给它,从模板可见生成的网页只有字符串,用content字符串对象接收以便当作发邮件方法中的参数
        String content=templateEngine.process("mail/demo",context);
        //模板引擎的作用还是帮我们生成一个动态网页
        System.out.println(content);//打印到控制台看是不是网页
        //发邮件还是得通过设置好的类mailClient来发（有我们定义的发邮件方法）
        mailClient.sendMail("yilin1652@gmail.com","TESTHTML",content);
    }
}
