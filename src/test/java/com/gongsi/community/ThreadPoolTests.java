package com.gongsi.community;

import com.gongsi.community.service.DataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {
    //使用logger去输出内容，因为它会输出线程的id
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    //JDK普通线程池通过工厂Executors实例化出对象，并指定线程池的线程数为多少。
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    //JDK可执行定时任务的线程池同理
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    //Spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor threadPoolExecutor;

    //Spring可执行定时任务的线程池
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private DataService dataService;


    //sleep经常会抛异常，所以把sleep方法和抛异常一起封装到一个新的方法方便后续调用
    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExecutorService() {
        //对于普通线程池，定义一个任务传给线程池，线程池会分配一个线程去执行
        Runnable task = new Runnable() {
            public void run() {
                logger.debug("Hello");
            }
        };
        //让线程池多次执行同一任务
        for (int i = 1; i <= 10; i++) {
            executorService.submit(task);
        }
        sleep(10000);//10000ms=10s
    }
    @Test
    public void testScheduledExecutorService() {
        Runnable task = new Runnable() {
            public void run() {
                logger.debug("Hello");
            }
        };
        //定时任务的线程池也是定义一个任务传给线程池，额外传入时间间隔,第二个参数是第一次执行延迟10s开始
        // 间隔10s去多次执行同一任务,不用我们前面for循环去让线程池多次执行同一任务。
        scheduledExecutorService.scheduleAtFixedRate(task,10,10,TimeUnit.SECONDS);
        sleep(30000);
    }
    //Spring普通线程池区别在于需不需要自己手动实例化
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable task = new Runnable() {
            public void run() {
                logger.debug("Hello");
            }
        };
        threadPoolExecutor.submit(task);
        sleep(40000);
    }
    //Spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler2() {
        Runnable task = new Runnable() {
            public void run() {
                logger.debug("Hello");
            }
        };
        //延迟时间不同于普通线程的设置，是当前时间的毫秒表示加上10000ms
        Date startTime=new Date(System.currentTimeMillis()+10000);
        //默认单位是ms，所以不同于jdk需要我们自己设置时间单位
        threadPoolTaskScheduler.scheduleAtFixedRate(task,startTime,1000);
        sleep(30000);
    }

}
