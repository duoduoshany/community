package com.gongsi.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    @Bean
    public Producer kaptchaProducer() {
        //设置kaptcha的配置项
        Properties properties = new Properties();
        //不在properties文件中配是因为给的代码格式不好
        properties.put("kaptcha.image.width", "100");
        properties.put("kaptcha.image.height", "40");
        properties.put("kaptcha.textproducer.font.color", "black");
        properties.put("kaptcha.textproducer.font.size", "32");//字体大小
        //生成验证码允许使用的字符集合
        properties.put("kaptcha.textproducer.char.string", "0123456789ABCDEFGHJKLMNOPQRSTUVWXYZ");
        //字符长度
        properties.put("kaptcha.textproducer.char.length", "4");
        //声明选择的噪声类，给生成的图片加干扰，防止机器人暴力破解。
        properties.setProperty("kaptcha.noise.impl","com.google.code.kaptcha.impl.NoNoise");

        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
