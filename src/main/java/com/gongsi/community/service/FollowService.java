package com.gongsi.community.service;

import com.gongsi.community.entity.User;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;
    //编程式事务，把事务管理的方法放到自定义方法中,需要从前端获取的数据当作方法参数传入，便于构造键
    //1.关注
    public void follow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback(){

            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                //某个实体有哪些粉丝，（entityType,entityId)->ZSet(userId,now)
                String followerKey= RedisKeyUtil.getFollowerKey(entityType,entityId);
                //用户关注了哪些东西，(userId,entityType)->ZSet(entityId,now)
                String followeeKey=RedisKeyUtil.getFolloweeKey(userId,entityType);

                redisOperations.multi();;
                //往键对应的值的集合中添加。
                redisOperations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                redisOperations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());

                return redisOperations.exec();
            }
        });
    }
    //2.取消关注
    public void unfollow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback(){

            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                //某个实体有哪些粉丝，（entityType,entityId)->ZSet(userId,now)
                String followerKey= RedisKeyUtil.getFollowerKey(entityType,entityId);
                //用户关注了哪些东西，(userId,entityType)->ZSet(entityId,now)
                String followeeKey=RedisKeyUtil.getFolloweeKey(userId,entityType);

                redisOperations.multi();;
                //往键对应的值的集合中添加，remove方法不需要移除score。
                redisOperations.opsForZSet().remove(followerKey,userId);
                redisOperations.opsForZSet().remove(followeeKey,entityId);
                return redisOperations.exec();
            }
        });
    }
    //3.查询某用户关注的实体的数量,构造键，看键对应的ZSet的大小
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey=RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }
    //4.查询某实体的粉丝数，构造键，看键对应的ZSet的大小
    public long findFollowerCount(int entityType,int entityId){
        String followerKey=RedisKeyUtil.getFolloweeKey(entityType,entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    //5.我想的是实体的集合是否有用户的Id，而他想的是：用户关注的实体中是否有这个实体
    public boolean isFollower(int userId,int entityType,int entityId){
        String followeeKey=RedisKeyUtil.getFollowerKey(userId,entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId)!=null;
    }

    //6.查询某个用户关注的实体
    //关注列表：关注的人+关注的时间（就是score），利用命令查出这两个数据并整合到一起返回
    //传参：支持分页，传入offset和limit，要知道是查哪个用户的关注实体，传入用户id
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit){
        String followeeKey=RedisKeyUtil.getFolloweeKey(userId,ENTITY_TYPE_USER);
        //倒序输出指定的起始页码到起始页码+最大条数-1这个范围的每一个实体Id
        Set<Integer> followeeIds=redisTemplate.opsForZSet().reverseRange(followeeKey,offset,offset+limit-1);
        if(followeeIds==null)
        {
            return null;
        }
        //整合所需的数据：关注的人+关注的时间
        List<Map<String,Object>> listVo=new ArrayList<>();
        //集合中每个元素类型是Integer而不是int
        for(int id:followeeIds){
            Map<String,Object> map=new HashMap<>();
            User user=userService.findUserById(id);
            map.put("user",user);
            Double score=redisTemplate.opsForZSet().score(followeeKey,id);
            map.put("followTime",new Date(score.longValue()));
            listVo.add(map);
        }
        return listVo;
    }
    //7.粉丝列表，查询某个用户(实体）拥有哪些粉丝
    public List<Map<String,Object>> findFollowers(int userId,int offset,int limit){
        String followerKey=RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER,userId);
        //倒序输出指定的起始页码到起始页码+最大条数-1这个范围的每一个实体Id
        Set<Integer> followerIdSet=redisTemplate.opsForZSet().reverseRange(followerKey,offset,offset+limit-1);
        //粉丝列表为空，不需要装配数据显示
        if(followerIdSet==null) {
            return null;
        }
        //整合所需的数据：关注的人+关注的时间
        List<Map<String,Object>> listVo=new ArrayList<>();
        for(Integer id:followerIdSet){
            Map<String,Object> map=new HashMap<>();
            User user=userService.findUserById(id);
            map.put("user",user);
            Double score=redisTemplate.opsForZSet().score(followerKey,id);
            map.put("followTime",new Date(score.longValue()));
            listVo.add(map);
        }
        return listVo;
    }
}
