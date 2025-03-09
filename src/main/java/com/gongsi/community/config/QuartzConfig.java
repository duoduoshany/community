package com.gongsi.community.config;

import com.gongsi.community.quartz.AlphaJob;
import com.gongsi.community.quartz.PostScoreRefreshJob;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.service.ElasticsearchService;
import com.gongsi.community.service.LikeService;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

//配置信息只有第一次读取到，封装的配置信息初始化到数据库，以后通过访问数据库去获取信息，而不是配置文件

//FactoryBean可简化Bean的实例化过程，因为有些bean实例化比较复杂，如JobDetail
//因此jobDetailFactoryBean底层详细封装了JobDetail的实例化过程并对实例化过程做了简化
//1.通过FactoryBean封装Bean的实例化过程
//2.通过@Bean将FactoryBean装配到Spring容器中
//3.将FactoryBean注入到其它的bean中，Spring会自动调用FactoryBean的实例化方法，使得该Bean获取FactoryBean管理的目标实例
//不同于不同注入：注入目标实例才能得到目标实例
//因为有些Bean的实例化过程比较麻烦
@Configuration
public class QuartzConfig {

    //配置JobDetail
    //@Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        //FactoryBean管理的bean的类型
        jobDetailFactoryBean.setJobClass(AlphaJob.class);
        //管理的bean的配置：配置JobDetail，其实就是配置任务
        //配置任务名字，不同任务的名字不可以重复
        jobDetailFactoryBean.setName("alphaJob");
        //任务的组名，多个任务可以同属于一组
        jobDetailFactoryBean.setGroup("alphaJobGroup");
        //声明任务是否是持久保存
        jobDetailFactoryBean.setDurability(true);
        //任务是否可恢复
        jobDetailFactoryBean.setRequestsRecovery(true);

        return jobDetailFactoryBean;
    }

    //配置Trigger(SimpleTriggerFactoryBean,CronTriggerFactoryBean)
    //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
        SimpleTriggerFactoryBean triggerFactoryBean = new SimpleTriggerFactoryBean();
        //把之前定义的JobDetail绑定到触发器上，触发器会执行JobDetail对应的任务
        //触发器和要绑定的JobDetail的名字最好类似，以便不会绑定到其它JobDetail
        triggerFactoryBean.setJobDetail(alphaJobDetail);
        triggerFactoryBean.setName("alphaTrigger");//给trigger起名字
        triggerFactoryBean.setGroup("alphaJobTriggerGroup");//起组名
        triggerFactoryBean.setRepeatInterval(3000);//3000ms=3s，每3s要执行一遍绑定的任务
        triggerFactoryBean.setJobDataMap(new JobDataMap());//传递任务执行时需要的额外数据，这里我们实例化出一个空的JobDataMap 对象。

        return triggerFactoryBean;
    }

    //刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        //FactoryBean管理的bean的类型
        jobDetailFactoryBean.setJobClass(PostScoreRefreshJob.class);
        //管理的bean的配置：配置JobDetail，其实就是配置任务
        //配置任务名字，不同任务的名字不可以重复
        jobDetailFactoryBean.setName("postScoreRefreshJob");
        //任务的组名，我们设置community的所有定时任务都属于同一组
        jobDetailFactoryBean.setGroup("communityJobGroup");
        //声明任务是否是持久保存
        jobDetailFactoryBean.setDurability(true);
        //任务是否可恢复
        jobDetailFactoryBean.setRequestsRecovery(true);

        return jobDetailFactoryBean;
    }

    //配置Trigger(SimpleTriggerFactoryBean,CronTriggerFactoryBean)
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJob) {
        SimpleTriggerFactoryBean triggerFactoryBean = new SimpleTriggerFactoryBean();
        //把之前定义的JobDetail绑定到触发器上，触发器会执行JobDetail对应的任务
        //触发器和要绑定的JobDetail的名字最好类似，以便不会绑定到其它JobDetail
        triggerFactoryBean.setJobDetail(postScoreRefreshJob);
        triggerFactoryBean.setName("postScoreRefreshJobTrigger");//给trigger起名字
        triggerFactoryBean.setGroup("communityTriggerGroup");//设置所有的触发器在一组
        triggerFactoryBean.setRepeatInterval(1000 * 60*10);//1000ms=1s，每5min要执行一遍绑定的任务
        triggerFactoryBean.setJobDataMap(new JobDataMap());//传递任务执行时需要的额外数据，这里我们实例化出一个空的JobDataMap 对象。

        return triggerFactoryBean;
    }
}
