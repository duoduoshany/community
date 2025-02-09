package com.gongsi.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    //声明了Bean，方法的参数也会被注入到Spring容器中
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);//template配了连接工厂之后才会具备访问数据库的能力
        //设置key的序列化方式：将键转换成字符串类型再序列化为字节流存储在redis，不过redis的键本身就是string类型
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化方式：将Java对象转换成json格式再转化为字节流存储在redis
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        //做了如上参数设置后需要触发它生效
        template.afterPropertiesSet();
        return template;//配置好的template作为返回值被加入到Spring容器中
    }
}
