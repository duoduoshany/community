package com.gongsi.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceLogAspect {
    private static final Logger logger= LoggerFactory.getLogger(ServiceLogAspect.class);
        //切点：service包下的所有组件的所有方法都要处理,(..)意思是不管方法的参数是什么
        @Pointcut("execution(* com.gongsi.community.service.*.*(..))")
        public void pointCut() {

        }
        @Before("pointCut()")
        public void before(JoinPoint joinPoint) {
            //用户[ip地址]在[时间]访问了[com.gongsi.community.service.xxx()]方法
            //用户的ip可以通过request获取，怎么获取request对象，为什么不可以参数获取request对象
            ServletRequestAttributes attributes=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            if(attributes==null)
            {
                //logger.info("没有 HTTP 请求上下文，无法记录用户 IP 和访问信息。");
                return;
            }
            HttpServletRequest request=attributes.getRequest();
            String ip=request.getRemoteHost();
            //SimpleDateFormat 对象，指定日期格式,使用 format()方法来格式化当前时间。
            String now=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //连接点指代的是Service目标组件下的某个类某个方法
            //可以利用连接点获取目标方法所在类的全类名和目标方法名
            String target=joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName();
            //记录日志信息
            logger.info(String.format("用户[%s],在[%s],访问了[%s]",ip,now,target));
        }
    }
