package com.gongsi.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
//测试代码为了引用CommunityApplication为配置类来自动创建Spring容器，标明以下注解
@ContextConfiguration(classes = CommunityApplication.class)
public class LoggerTest {
    //本来要先实例化（弄一个对应的接口并在该类中注入接口），这里我们自己实例化即可
    //final不可改变，static为了便于所有的方法去调用，设置成静态
    //创建一个与LoggerTest类关联的日志记录器（logger），用于在该类中记录日志信息
    private static final Logger logger = LoggerFactory.getLogger(LoggerTest.class);

    @Test
    //展示了不同日志级别的输出效果
    public void test() {
        System.out.println(logger.getName());
        logger.debug("debug Logger");
        logger.info("info Logger");
        logger.warn("warn Logger");//用的少
        logger.error("error Logger");
    }

}
