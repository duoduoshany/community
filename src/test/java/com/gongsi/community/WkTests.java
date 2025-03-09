package com.gongsi.community;


import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class WkTests {
    public static  void main(String[] args){
        String cmd="C:/work/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com C:/work/data/wk-images/3.png";
        try {
            //执行cmd命令的代码
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");//如果没有异常就输出ok
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
