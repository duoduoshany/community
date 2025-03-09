package com.gongsi.community;

import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringBootTests {
    //声明为成员变量，before方法只负责初始化成员变量，这样其余所有测试方法都可以接着利用这个被初始化好的变量data
    //before注解的方法内的变量在方法运行完后，出了before方法，其余方法都用不了这个变量

    private DiscussPost data;

    @Autowired
    private DiscussPostService discussPostService;

    @BeforeClass
    public static void beforeClass() {
        System.out.println("Before class");
    }
    @AfterClass
    public static void afterClass() {
        System.out.println("After class");
    }
    @Before
    public void before(){
        System.out.println("Before method");
        data=new DiscussPost();
        data.setUser_id(111);
        data.setTitle("Test");
        data.setContent("Test");
//        data.setCreate_time(new Date());创建的时候有一个时间戳，而数据库通常会配置该字段为自动生成，即数据库会在插入数据时自动填充时间值。
        //除了时间戳，其余字段设置的值都会被正常写入数据库
        discussPostService.addDiscussPost(data);
    }
    @After
    public void after(){
        System.out.println("After method");
        //单个测试方法执行完之后就删除
        discussPostService.updateStatus(data.getId(),2);
    }
    @Test
    public void test(){
        System.out.println("test");
    }
    @Test
    public void test2(){
        System.out.println("test2");
    }

    @Test
    public void testFindById(){
        DiscussPost post=discussPostService.findDiscussPostById(data.getId());
        //判断数据是否为空
        Assert.assertNotNull(post);
        //查看传入的两个参数是否相等
        Assert.assertEquals(data.getTitle(),post.getTitle());
        Assert.assertEquals(data.getContent(),post.getContent());
        Assert.assertEquals(data.getUser_id(),post.getUser_id());
        //也可以重写toString方法，比较两个对象是否相等
        Assert.assertEquals(data.toString(),post.toString());

    }
    @Test
    public void testUpdateScore(){
        int rows=discussPostService.updateScore(data.getId(),2000.00);
        //报错注意，当前的data是原先的data，插入数据库后，方法修改了数据库data的score，所以我们要获取数据库data的score判断是否改变
        data=discussPostService.findDiscussPostById(data.getId());
        Assert.assertEquals(rows,1);//判断方法有没有成功执行
        //最后一个是精度，计算机难以精确比较两个小数是否相等，只能精确比较小数，所以给出小数位有两位，单独判断这两位是否相等
        //判断是否成功修改
        Assert.assertEquals(data.getScore(),2000.00,2);


    }


}
