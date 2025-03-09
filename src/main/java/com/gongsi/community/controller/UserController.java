package com.gongsi.community.controller;

import com.gongsi.community.annotation.LoginRequired;
import com.gongsi.community.entity.User;
import com.gongsi.community.service.FollowService;
import com.gongsi.community.service.LikeService;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import com.gongsi.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
//给类声明访问类的路径user

@RequestMapping(path="/user")
public class UserController implements CommunityConstant {
    public static final Logger logger= LoggerFactory.getLogger(UserController.class);
    //把上传路径、域名、项目路径这些固定值传进来使用
    @Value("${community.path.upload}")
    private String uploadPath;//再声明一个变量来接受
    @Value("${community.path.domain}")
    private String domain;
    @Autowired
    private UserService userService;
    //注入hostHolder通过当前线程取当前用户，其余线程来自于其余客户端或同一客户端的其它请求
    // 你运行哪个就会得到哪个的用户信息，选择一个运行后，无法获取其余线程的用户信息
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    //点击账号设置的链接，显示账号设置的页面
    @LoginRequired
    @RequestMapping(path="/setting",method= RequestMethod.GET)
    public String setting() {
        return "site/setting";
    }
    @LoginRequired
    @RequestMapping(path="/upload",method= RequestMethod.POST)
    public String uploadHeader(MultipartFile headImage, Model model) {
        if(headImage==null)
        {
            model.addAttribute("error","您还没有选择图片！");
            return "site/setting";
        }
        //不能按照文件名来传，因为这样很多人如果命名了同一个文件名，就会发生覆盖，所以我们给文件起一个随机的，不重复的名字
        //读取文件后缀，并存储。方法是先读取文件名，再截取文件名的.往后的字符串
        String filename=headImage.getOriginalFilename();
        String suffix=filename.substring(filename.lastIndexOf("."));
       //有可能后缀是空的
        if(StringUtils.isBlank(suffix))
        {
            model.addAttribute("error","文件的格式不正确");
            return "site/setting";
        }
        //生成随机的文件名
        filename=CommunityUtil.generateUUID()+suffix;
        //File 对象，用于表示上传文件在本地硬盘的存储路径
        File dest=new File(uploadPath+"/"+filename);
        try {
            //将用户上传的文件保存到 dest 指定的路径。
            headImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败："+e.getMessage());
            //把异常抛出去，将来统一处理
            throw new RuntimeException("生成文件失败，服务器发生异常！",e);
        }
        //更新当前用户头像的文件路径(web访问路径）,跟激活链接的设置一样
        // 写一个自己定义的路径模板,允许外界访问图片的web路径
        //https:localhost:8080/community/user/header/xxx.png
        User user=hostHolder.getUser();
        String headerUrl=domain+"/user/header/"+filename;
        //调用userService的更新头像web路径的方法
        userService.updateHeader(user.getId(),headerUrl);
        //更新成功后返回首页
        return "redirect:/index";
    }
    //我们定义好这个路径，那肯定是要有个方法，在访问图像web路径时获取相应的头像
    //变量用{}括起来,方法返回值是void，因为要通过流输出这个二进制图片,通过response获取输出的字节流
    @RequestMapping(path="/header/{filename}",method=RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response) {
        //输入流根据文件名从本地读取文件
        filename=uploadPath+"/"+filename;
        //向浏览器输出图片，之前弄激活链接输出html，都是先向浏览器说明要输出的文件类型是什么
        String suffix=filename.substring(filename.lastIndexOf("."));
        //图片类型声明的固定格式
        response.setContentType("image/"+suffix);
        //创建文件的输入流，读取本地硬盘的图片,输入流是自己创建的，不是response的，要我们自己手动关闭
        //所以放在try()中
        try(FileInputStream fis=new FileInputStream(filename);) {
            OutputStream os=response.getOutputStream();
            //不是一字节一字节的读取和写入，而是一批一批的读取和写入，提高了文件传输的效率
            byte[] buffer=new byte[1024];
            int b=0;
            while((b=fis.read(buffer))!=-1)
            {
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败："+e.getMessage());
        }
    }
    @RequestMapping(path="/profile/{userId}",method=RequestMethod.GET)
    public String profile(@PathVariable("userId") int userId, Model model) {
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在！");
        }
        //查到用户，用户返回前端
        model.addAttribute("user",user);
        //点赞数量
        int count=likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",count);
        //关注的实体数量
        long followeeCount=followService.findFolloweeCount(userId,ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //某实体的粉丝（只能是用户）数量，个人主页这里的实体是用户
        long followerCount=followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",followerCount);
        //是否关注：默认没关注，当前用户是否登录，登录的话调用followService的是否关注方法
        boolean isFollower=false;
        if(hostHolder.getUser()!=null){
            isFollower=followService.isFollower(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",isFollower);

        return "site/profile";
    }

}
