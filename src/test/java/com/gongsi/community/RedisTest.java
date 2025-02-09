package com.gongsi.community;

import com.gongsi.community.config.RedisConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTest {
    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    public void test() {
        String redisKey="test:count";
        //返回一个对象，对象有方法能够操作键，如给它设置值，增加或减少它的值
        redisTemplate.opsForValue().set("test:count",1);
        System.out.println(redisTemplate.opsForValue().get("test:count"));
        System.out.println(redisTemplate.opsForValue().increment("test:count"));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
        redisTemplate.opsForValue().increment("test:count");
    }
    @Test
    public void test1() {
        String redisKey="test:user";
        //键都是字符串，username要用”“括起来
        redisTemplate.opsForHash().put("test:user","username","zhangsan");
        redisTemplate.opsForHash().put("test:user","age","18");
        System.out.println(redisTemplate.opsForHash().get("test:user","username"));
    }
    @Test
    public void test2() {
        String redisKey="test:ids";
        redisTemplate.opsForList().leftPush("test:ids",1);
        redisTemplate.opsForList().leftPush("test:ids",2);
        System.out.println(redisTemplate.opsForList().size("test:ids"));
        System.out.println(redisTemplate.opsForList().index("test:ids",0));
        System.out.println(redisTemplate.opsForList().range("test:ids",0,1));
        System.out.println(redisTemplate.opsForList().leftPop("test:ids"));
    }
    @Test
    public void test3() {
        String redisKey="test:students";
        redisTemplate.opsForSet().add("test:students","tan","jian","song","liu");
        System.out.println(redisTemplate.opsForSet().members("test:students"));
        System.out.println(redisTemplate.opsForSet().pop("test:students"));
        System.out.println(redisTemplate.opsForSet().size(redisKey));
    }
    @Test
    public void test4() {
        String redisKey="test:persons";
        //后端代码中一次只能add，push，put，set一个值
        redisTemplate.opsForZSet().add("test:persons","zhao",1);
        redisTemplate.opsForZSet().add("test:persons","qian",2);
        redisTemplate.opsForZSet().add("test:persons","wang",3);
        System.out.println(redisTemplate.opsForZSet().score("test:persons","zhao"));
        System.out.println(redisTemplate.opsForZSet().range("test:persons",0,2));
        System.out.println(redisTemplate.opsForZSet().reverseRange("test:persons",0,2));
        System.out.println(redisTemplate.opsForZSet().rank("test:persons","qian"));
        //从大到小排多少
        System.out.println(redisTemplate.opsForZSet().reverseRank("test:persons","wang"));
        System.out.println(redisTemplate.opsForZSet().zCard("test:persons"));

    }
    @Test
    public void test5() {
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        //也是用expire，最后参数的意思是10的单位是s
        redisTemplate.expire("test:persons",10, TimeUnit.SECONDS);
    }
    //多次访问同一个key
    @Test
    public void testBoundOperations() {
        //opsForZSet()返回的对象调用方法需要传递key参数，而boundValueOps()返回对象调用方法则不需要传递key参数
        String redisKey="test:count";
        BoundValueOperations ops=redisTemplate.boundValueOps(redisKey);
        ops.increment();
        ops.decrement();
        ops.increment();
        System.out.println(ops.get());
    }
    //关系型数据库才严格满足ACID事务的四个基本特性
    //对于nosql型的，当执行redis命令时不会立刻执行，而是放在队列暂存，提交事务的时候把命令全发给redis服务器一起执行
    //所以在事务之内做了查询，查询不会立即返回结果
    //声明式事务的事务范围大，整个方法就是一个事务，方法之内就无法查询
    //所以redis用编程式事务，缩小事务的范围
    @Test
    public void test6() {
       Object obj= redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations rps) throws DataAccessException {
                String redisKey="test:tx";
                rps.multi();//启用事务,后续的所有操作都会被加入到事务队列中，Redis 会在事务提交时一次性执行这些操作。
                rps.opsForSet().add(redisKey,"wang");
                rps.opsForSet().add(redisKey,"sun");
                //还没有对redis执行操作，查询结果为空
                System.out.println(rps.opsForSet().members(redisKey));
                return rps.exec();//提交事务
            }
        });
       //返回的obj对象包含添加操作执行的结果，0已存在未添加，1已添加，还有menbers方法的执行结果
       System.out.println(obj);
    }


}
