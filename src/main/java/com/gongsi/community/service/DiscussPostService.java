package com.gongsi.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.util.RedisKeyUtil;
import com.gongsi.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger= LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${caffeine.posts.maxsize}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //Caffeine核心接口：Cache，两个常用子接口：LoadingCache,AsyncLoadingCache
    //第一个是同步缓存，比如多个线程同时去访问缓存里的同一份数据，缓存未命中，就会通过互斥锁，同一时刻只允许一个线程加载数据到缓存，其他线程阻塞等待。

    //帖子列表的缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;
    //帖子总行数的缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init() {
        //初始化帖子列表缓存
        postListCache= Caffeine.newBuilder()//构建缓存并配置
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    //@NonNull参数不能为null，可以去掉，下面进行了是否为空的检查
                    @Override
                    public @Nullable List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key==null||key.length()==0){
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params=key.split(":");
                        if(params==null||params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset=Integer.valueOf(params[0]);
                        int limit=Integer.valueOf(params[1]);

                        //1.一级缓存未命中，先从二级缓存中获取，并填入一级缓存
                        String redisKey=RedisKeyUtil.getPostsKey(offset,limit);

                        //既然是每一页的帖子列表，就不可能存在缓存查询后帖子列表是空的情况，缓存不会存null值
                        //万一存储的不是list类型，强制类型转换为(List<DiscussPost>)会报错
                        Object posts= (List<DiscussPost>)redisTemplate.opsForValue().get(redisKey);//get的值是Object类型，因为Redis的String结构可以把任何类型的数据序列化，读出时再反序列化
                        //得到的Posts是否是List类型，是，说明posts不为null，存在，不是，说明不存咋这个key，查询的值才为空
                        if (posts instanceof List) {
                            logger.debug("load post list from redis");
                            return (List<DiscussPost>) posts;
                        }
                        //2.从数据库中取，取到之后不要忘了手动添加到redis缓存中，这样下次一级缓存可以从redis缓存中获取值
                        logger.debug("load post list from db");
                        posts= discussPostMapper.selectDiscussPost(0,offset,limit,1);

                        //缓存穿透：数据在redis和数据库中都不存在，防止缓存穿透
                        //强转为list类型
                        if(((List<?>) posts).isEmpty()){
                            logger.debug("Cache empty list for key");
                            //创建一个空的，不可修改的列表返回，这样就可以缓存在缓存中
                            posts= Collections.emptyList();
                        }

                        //设置缓存过期的时间，避免缓存雪崩,在基础过期时间上加一个随机数
                        redisTemplate.opsForValue().set(redisKey,posts,expireSeconds+(long)(Math.random()*10*60),TimeUnit.SECONDS);
                        return (List<DiscussPost>)posts;
                    }
                });
        //初始化帖子总数缓存
        postRowsCache= Caffeine.newBuilder()//构建缓存并配置
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(Integer integer) throws Exception {
                        String rediskey=RedisKeyUtil.getRowsKey(integer);
                        //对象类型允许为null，即Integer rows=nul正确
                        Integer rows=(Integer)redisTemplate.opsForValue().get(rediskey);
                        if(rows!=null){
                            logger.debug("load post list from redis");
                            return rows;
                        }

                        logger.debug("load post list from db");
                        rows= discussPostMapper.selectDiscussPostRows(integer);
                        //防止缓存穿透
                        if(rows==null){
                            rows=0;
                        }
                        redisTemplate.opsForValue().set(rediskey,rows);
                        return rows;
                    }
                });

    }

    public List<DiscussPost> getDiscussPost(int userId,int offset,int limit,int orderMode) {
        if(userId==0&&orderMode==1)
        {
            return postListCache.get(offset+":"+limit);//get方法已经包含了未命中后Caffeine会从redis和数据库中查的情况
        }
        //该方法涵盖了查询首页最新帖子，查询帖子等的其它功能用数据库查
        return discussPostMapper.selectDiscussPost(userId,offset,limit,orderMode);
    }
    public int getDiscussPostCount(int userId) {
        if(userId==0)
        {
           return  postRowsCache.get(userId);//get方法已经包含了未命中后Caffeine会从redis和数据库中查的情况
        }
        return discussPostMapper.selectDiscussPostRows(userId);
    }
    public int addDiscussPost(DiscussPost post) {
        //先判断用户传入的帖子是不是空的
        if(post==null)
        {
            throw new IllegalArgumentException("参数不能为空！");
        }
        //1.转义HTML标记，有可能用户写的文本包含类似于<script></script>的字符会被自动识别为网页
        //将这些特殊标记转化为文本
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        //2.过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        //调用数据访问层的方法插入处理好的数据。
        return discussPostMapper.insertDiscussPost(post);
    }
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }
    public int updateCommentCount(int id,int commentCount)
    {
        return discussPostMapper.updateCommentCount(id,commentCount);
    }
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id, score);
    }

}
