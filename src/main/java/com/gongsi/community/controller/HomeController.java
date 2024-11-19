package com.gongsi.community.controller;

import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.dao.UserMapper;
import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.entity.Page;
import com.gongsi.community.entity.User;
import com.gongsi.community.service.DiscussPostService;
import com.gongsi.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @RequestMapping(path="/index",method= RequestMethod.GET)
    //分页的时候页面会传入有关的条件
    public String getIndexPage(Model model, Page page)
    {
        //方法调用前，SpringMVC会自动实例化Model和Page，并将Page注入给Modele，所以在thymleaf中就可以直接访问Page对象的数据
        //总行数需要服务器来获取并设置到page对象中
        page.setRows(discussPostService.getDiscussPostCount(0));
        page.setPath("/index");//当前访问路径是index
        //offset和limit都来源于客户端,不写si
        List<DiscussPost> list=discussPostService.getDiscussPost(0,page.getOffset(),page.getLimit());
        System.out.println("讨论帖数量: " + (list != null ? list.size() : "无数据"));
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(list!=null) {
            for (DiscussPost post : list) {
                Map<String,Object> map=new HashMap<>();//先有空对象
                map.put("post",post);//put放第一个属性
                User user=userService.findUserById(post.getuser_id());
                map.put("user",user);//放第二个属性
                //放完所有属性后才是对象add加进集合。
                discussPosts.add(map);
            }
        }
        //得把要在页面展示的结果discussPosts装到model里页面才能得到
        model.addAttribute("discussPosts",discussPosts);
        return "index";
    }
}
