package com.gongsi.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

//这里没有第三方类需要注入，所以其中没有@Bean
//只是一个检查有没有目标文件夹的程序,确保执行命令之前文件夹已经存在
// 有@Configuration，Spring会认为这是一个配置类，就会先加载这个类
//也就是先实例化类，实例化类之后就会自动调用初始化方法一次
@Configuration
public class WkConfig {
    private static final Logger logger=LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")//把文件存储的位置注入进来
    private String wkImageStorage;

    @PostConstruct
    public void init() {
        //创建File对象代表路径，指向文件或目录
        File file=new File(wkImageStorage);
        //检查File对象指向的目录或文件是否存在，如果不存在则创建
        if(!file.exists()){
            file.mkdirs();//这里创建的是目录
            logger.info("创建wk图片存储目录："+wkImageStorage);
        }
    }
}
