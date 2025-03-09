package com.gongsi.community.quartz;

import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.service.ElasticsearchService;
import com.gongsi.community.service.LikeService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {

    //最好记录日志，万一任务中断了还可以追根溯源
    private static final Logger logger= LoggerFactory.getLogger(PostScoreRefreshJob.class);

    //获取redis的数据
    @Autowired
    private RedisTemplate redisTemplate;
    //我们前面设置发布帖子的时候才触发了事件保存到es服务器中，点赞评论量增加时也要触发发帖事件保存到服务器
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    //牛客纪元
    private static final Date epoch;

    //静态代码块：初始化静态变量
    static {
        try {
            //用parse把常量字符串转换成指定日期格式yyyy-MM-dd HH:mm:ss的日期类型
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败！",e);
        }
    }


    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey= RedisKeyUtil.getPostScoreKey();
        //每个id都要去算一下，反复做这样的操作,用BoundSetOperations，boundSetOps用来操作 Redis 中的 Set 类型数据。
        BoundSetOperations operations=redisTemplate.boundSetOps(redisKey);
        //如果是半夜没有人访问社区，没有变化的值，导致redisKey对应的值为null，就给个日志，不做任何处理
        if(operations.size()==0){
            logger.info("任务取消！没有需要刷新的帖子！");
            return;
        }
        logger.info("任务开始！正在刷新帖子分数："+operations.size());
        while(operations.size()>0){
            //首先是一个集合，集合可以pop弹出值，没有给RedisTemplate指定类型为RedisTemplate<String,Integer>（键是String，值是Integer，弹出Integer类型）
            //没有配置RedisTemplate,默认返回object
            this.refresh((Integer)operations.pop());
        }
        logger.info("任务结束！帖子分数刷新完毕！");
    }
    private void refresh(int postId){
        DiscussPost post=discussPostService.findDiscussPostById(postId);
        //有指标变化的帖子可能被用户自己删了或者管理员删了
        if(post==null){
            logger.error("该帖子不存在：id="+postId);
            return;
        }
        //是否加精
        boolean wonderful=post.getStatus()==1;
        //评论数量
        int commentCount=post.getComment_count();
        //点赞数量
        long likeCount=likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId);

        //计算权重
        double w=(wonderful?75:0)+commentCount*10+likeCount*2;
        //分数=log(帖子权重)+距离天数，w有可能为0，为了避免出现负分的情况，所以设置一个最小值1，初始分为0
        //新帖有可能因为距离天数完胜点赞量高的旧帖，所以一开始加入redis缓存中参与排行
        //时间我们算的是毫秒，要换算成天数，/1000得到几秒，秒/3600换算成几小时，/24得到几天
        //Date.getTime()得到的是毫秒
        double score=Math.log10(Math.max(w,1))+(post.getCreate_time().getTime()-epoch.getTime())/1000/3600/24;
        //更新帖子分数
        discussPostService.updateScore(postId,score);
        //因为帖子的分数的属性更改了，所以把最新的post保存到es服务器
        //我们一开始查询的post是旧帖子，给查询的post设置新的score就是新帖子
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
