package com.gongsi.community.service;

import com.gongsi.community.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;//导入第三方类RedisTemplate方便使用
    //每个方法都和日期紧密相关，日期需要格式化，构造日期对象便于后续调用格式化方法format，返回的是String类型
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    //截获每一次请求，把相关数据记录到redis

    //1.将指定的IP记入UV，controller层获取ip传给service层，便于构造键值对
    public void recordUV(String ip){
        String redisKey= RedisKeyUtil.getUVKey(sdf.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);
    }
    //2.将指定的userId记入DAU:时间不用传，可以直接获取,记录的是值，传值进集合
    public void recordDAU(int userId){
        String redisKey=RedisKeyUtil.getDAUKey(sdf.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey,userId,true);
    }

    //提供查询键对应的值的方法

    //1.统计指定日期范围内的uv
    public long calculateUV(Date start,Date end){
        if(start==null || end==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        //得收集到这个区间范围内的key，再将值进行合并
        List<String> keyList=new ArrayList<>();
        //日期遍历不同于整数遍历的for循环，以下方法返回一个 Calendar 对象，表示当前系统的日期和时间
        Calendar calendar=Calendar.getInstance();
        //设置指定的时间
        calendar.setTime(start);
        while(!calendar.getTime().after(end)){
            keyList.add(RedisKeyUtil.getUVKey(sdf.format(calendar.getTime())));
            //将calendar表示的日期增加一天
            calendar.add(Calendar.DATE,1);
        }
        //合并后的数据存放的键
        String redisKeySum= RedisKeyUtil.getUVKey(sdf.format(start),sdf.format(end));
        //第一个参数填入合并后的key，第二个参数填要合并的数据对应的key，由于要求传入数组，所以做转化

        //合并后Hyperloglog底层会帮我们去重，求合并后的数据个数即可
        redisTemplate.opsForHyperLogLog().union(redisKeySum,keyList.toArray());

        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKeySum);
    }

    //2.统计指定日期范围内的dau
    public long calculateDAU(Date start,Date end){
        if(start==null || end==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        //位图运算区别于其它数据结构，键是字节数组类型而不是String，位图运算的方法必须传入字节数组类型的键
        List<byte[]> keyList=new ArrayList<>();

        //遍历时间构造key

        Calendar calendar=Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)){
            keyList.add(RedisKeyUtil.getDAUKey(sdf.format(calendar.getTime())).getBytes());
            calendar.add(Calendar.DATE,1);
        }
        //进行OR运算，因为我们要求区间内用户访问过一次就算活跃用户

        //注意回调函数的返回类型是包装类Long，所以最后强转成原始类型long

        return (long) redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKeySum= RedisKeyUtil.getDAUKey(sdf.format(start),sdf.format(end));
                //keyList是List类型，要求也要转为字节数组类型
                // 二位数组：字节数组的数组，把数组看作是行变量，有多个数组，就是列的个数
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKeySum.getBytes(),keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKeySum.getBytes());
            }
        });
    }
}
