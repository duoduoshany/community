package com.gongsi.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling//必须加上这个注解，才会执行定时任务
@EnableAsync
public class ThreadPoolConfig {
}
