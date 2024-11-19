package com.gongsi.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

//即代表这个Bean需要Spring容器去管理，又代表它是一个通用的Bean，在哪个层次都可以使用
@Component
public class MailClient {
    //后面需要记录日志，声明一个Logger,以当前类来命名
    private static final Logger logger= LoggerFactory.getLogger(MailClient.class);
    //注入核心组件,帮助我们构建邮件对象，发送邮件方法的具体实现依靠这个组件
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    //封装一个公有的方法，被外界调用,方法参数：发送目标，邮件主题，邮件内容
    public void sendMail(String to, String subject, String content) {
        /*创建一个MimeMessage邮件对象*/
        try {
            MimeMessage message= mailSender.createMimeMessage();
            //MimeMessageHelper帮助构建核心组件创建的message邮件对象中的内容
            MimeMessageHelper helper= new MimeMessageHelper(message);
            helper.setFrom(from);//发件人
            helper.setTo(to);//收件人
            helper.setSubject(subject);//设置主题
            helper.setText(content, true);//内容，参数的名字叫html，如果加了这个参数，允许html格式文本，没加就是普通文本
            mailSender.send(helper.getMimeMessage());//发送helper构建好的message
        } catch (MessagingException e) {
            logger.error("发送邮件失败:"+e.getMessage());
        }
    }
}
