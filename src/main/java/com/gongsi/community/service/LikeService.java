package com.gongsi.community.service;

import com.gongsi.community.util.HostHolder;
import com.gongsi.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;//才可以调用opsForValue等操作某类型数据的方法
    //点赞的实体 key(like:entity:entityType:entityId) value(set(userId))
    //点赞业务:得到键，看下键对应的集合中是否含有userId,包含就删掉
   public void like(int userId,int entityType,int entityId,int entityUserId) {
       redisTemplate.execute(new SessionCallback(){
           @Override
           public Object execute(RedisOperations operations) throws DataAccessException {
               //键是实体，值是粉丝id集合
               String entityLikeKey=RedisKeyUtil.getEntityLike(entityType,entityId);
               //弄了一个新的键，值是被点赞的实体的作者对应的点赞数，一旦点赞值+1
               String userLikeKey=RedisKeyUtil.getUserLike(entityUserId);
               boolean contains=redisTemplate.opsForSet().isMember(entityLikeKey,userId);
               operations.multi();//启动事务
               if(contains)
               {
                   redisTemplate.opsForSet().remove(entityLikeKey,userId);
                   //opsForValue是对String的值操作
                   redisTemplate.opsForValue().decrement(userLikeKey);
               }
               else{
                   redisTemplate.opsForSet().add(entityLikeKey,userId);
                   redisTemplate.opsForValue().increment(userLikeKey);
               }
               return operations.exec();
           }
       });
   }
    //查询目标实体的点赞数量：得到键，查询键对应的集合大小
    public long findEntityLikeCount(int entityType,int entityId){
        String redisKey= RedisKeyUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().size(redisKey);

    }
    //查询某人对某实体的点赞状态可以修改浏览器赞的显示:得到键，键对应的集合中是否包含用户id来判断点赞的状态
//返回int而不是bool，是因为true和false表示的状态少，而int可以扩展状态，比如1已点赞，0不点赞，-1表示踩的状态（暂无开发）
    public int findEntityLikeStatus(int userId,int entityType,int entityId){
        String redisKey= RedisKeyUtil.getEntityLike(entityType,entityId);
        return redisTemplate.opsForSet().isMember(redisKey,userId)?1:0;
    }

    //查询某个用户获得的赞
    public int findUserLikeCount(int userId){
       String userLikeKey=RedisKeyUtil.getUserLike(userId);
       //查询键对应的值
       Integer count=(Integer)redisTemplate.opsForValue().get(userLikeKey);
       return count==null?0:count.intValue();
    }
}
