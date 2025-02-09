package com.gongsi.community.controller.advice;

import com.gongsi.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

//用组件的好处是可以对所有controller做统一的处理
@ControllerAdvice(annotations= Controller.class)
public class ExceptionAdvice {
    private static final Logger logger= LoggerFactory.getLogger(ExceptionAdvice.class);
    @ExceptionHandler(Exception.class)
    public void handlerException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //方法被调用，肯定发生异常，把异常记录到日志中即可，e.getMessage()只是异常的概括
        logger.error("服务器发生异常："+e.getMessage());
        //逐行打印具体的异常堆栈信息，并将其记录到日志中.
        for(StackTraceElement element:e.getStackTrace()){
            logger.error(element.toString());
        }
        //判断请求是普通请求还是异步请求
        String xRequestedWith=request.getHeader("X-Requested-With");
        if(xRequestedWith.equals("XMLHttpRequest")){
            //说明是异步请求，设置为json向浏览器返回一个字符串浏览器会自动把它转换为js对象，设置为plain则需要我们人为转换
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();//获取响应的输出流，写入响应数据，直接抛出流异常
            //向输出流写入响应的数据
            writer.write(CommunityUtil.getJSONString(1,"服务器异常"));
        }
        else{
            //本来有request.getContextPath
            response.sendRedirect("/error");
        }
    }
}
