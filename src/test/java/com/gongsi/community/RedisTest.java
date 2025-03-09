package com.gongsi.community;

import com.gongsi.community.config.RedisConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
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
    @Test
    public void testHyperLogLog(){
        String redisKey="test:hll:01";//一个键对应一个集合，同hashmap和set和zset
        for(int i=1;i<=100000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }
        for(int i=1;i<=100000;i++){
            //random左闭右开区间，生成的10万个数在1,100001之间，左闭右开，且要求生成的是整数，所以一共有10万个重复的数
            redisTemplate.opsForHyperLogLog().add(redisKey,(int)(Math.random()*100000+1));
        }
        long size=redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }
    //第一个方法执行之后，test:hll:01 就已经在 Redis 中存储了数据，可以在其它方法中继续使用它。
    //将三组数据合并，再统计合并后的重复数据的独立总数
    @Test
    public void testHll(){
        String redisKey2="test:hll:02";
        for(int i=1;i<=10000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey2,i);
        }
        String redisKey3="test:hll:03";
        for(int i=5001;i<=15000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey3,i);
        }
        String redisKey4="test:hll:04";
        for(int i=10001;i<=20000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey4,i);
        }
        //把key看作是集合的名字
        String unionKey="test:hll:union";//第一个是合并要存在哪个key对应的集合中，其余都是要合并的集合
        redisTemplate.opsForHyperLogLog().union(unionKey,redisKey2,redisKey3,redisKey4);
        long size=redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }
    @Test
    public void testBitMap(){
        String redisKey="test:bitmap:01";
        //设置
        redisTemplate.opsForValue().setBit(redisKey,1,true);
        redisTemplate.opsForValue().setBit(redisKey,4,true);
        redisTemplate.opsForValue().setBit(redisKey,7,true);
        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,2));
        //统计,调用execute的时候会自动调用callBack回调方法，该方法会把redis连接传进去
        //利用redis连接统计1的位数
        Object obj=redisTemplate.execute(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                return redisConnection.bitCount(redisKey.getBytes());//结果虽然是long，但是重写的方法是Object类型，所以用Object去接收
            }
        });
        System.out.println(obj);

    }
    //统计三组数据的bool值
    //一个位图键对应的布尔值最多七位
    @Test
    public void testBitMapOperation(){
        String redisKey="test:bitmap:02";
        redisTemplate.opsForValue().setBit(redisKey,0,true);
        redisTemplate.opsForValue().setBit(redisKey,1,true);
        redisTemplate.opsForValue().setBit(redisKey,2,true);
        String redisKey2="test:bitmap:03";
        redisTemplate.opsForValue().setBit(redisKey2,2,true);
        redisTemplate.opsForValue().setBit(redisKey2,3,true);
        redisTemplate.opsForValue().setBit(redisKey2,4,true);
        String redisKey3="test:bitmap:04";
        redisTemplate.opsForValue().setBit(redisKey3,4,true);
        redisTemplate.opsForValue().setBit(redisKey3,5,true);
        redisTemplate.opsForValue().setBit(redisKey3,6,true);
        redisTemplate.opsForValue().setBit(redisKey3,7,true);
        String redisKeyOr="test:bitmap:or";
        Object obj=redisTemplate.execute(new RedisCallback<Object>() {

            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                //键标识的只是数据的位置，其实是把数据值转成字节数组
                //bitOp返回的结果不是字节数组本身，而是占用空间
                connection.bitOp(RedisCommands.BitOperation.OR,
                        redisKeyOr.getBytes(),redisKey.getBytes(),redisKey2.getBytes(),redisKey3.getBytes());
                return connection.bitCount(redisKeyOr.getBytes());
            }
        });
        System.out.println(obj);
        //字节数组内容用以下方式获取
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOr,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOr,1));
    }
}
