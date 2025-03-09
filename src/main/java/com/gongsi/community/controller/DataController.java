package com.gongsi.community.controller;

import com.gongsi.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    //1.返回表单页面
    @RequestMapping(path="/data",method= {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage(){
        return "/site/admin/data";
    }

    //查询的方法要求传入的参数是DATE类型,拼接key传的才是String类型，所以有sdf格式化DATE为字符串
    //页面上存的日期是字符串，服务器无法识别字符串的日期格式，通过DateTimeFormat我们告诉服务器字符串的日期格式是yyyy-MM-dd，服务器把它转换成Date类型时也是年月日格式

    //2.查询uv
    @RequestMapping(path="/data/uv",method=RequestMethod.POST)
    //将接收的参数start类型转换为年月日格式的date类型
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,Model model){
        long uv=dataService.calculateUV(start,end);
        model.addAttribute("uvResult",uv);
        //点击开始统计又回到表单页面，而表单页面必然保存着我们指定的日期，这些用户输入的日期需要我们传回给模板
        model.addAttribute("uvStartDate",start);
        model.addAttribute("uvEndDate",end);
        return "forward:/data";
    }

    //3.查询dau
    @RequestMapping(path="/data/dau",method=RequestMethod.POST)
    //将接收的参数start类型转换为年月日格式的date类型
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end,Model model){
        long uv=dataService.calculateDAU(start,end);
        model.addAttribute("dauResult",uv);
        //点击开始统计又回到表单页面，而表单页面必然保存着我们指定的日期，这些用户输入的日期需要我们传回给模板
        model.addAttribute("dauStartDate",start);
        model.addAttribute("dauEndDate",end);
        return "forward:/data";
    }

}
