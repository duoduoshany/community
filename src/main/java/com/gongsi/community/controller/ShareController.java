package com.gongsi.community.controller;

import com.gongsi.community.entity.Event;
import com.gongsi.community.event.EventProducer;
import com.gongsi.community.util.CommunityConstant;
import com.gongsi.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {
    private static final Logger logger= LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;
    //由于我们没有设置项目名所以不引进

    @Value("${wk.image.storage}")
    private String wktohtmlStorage;

    //1.异步地生成长图，如果同时分享的人特别多，异步请求可以减少阻塞现象
    @RequestMapping(path="/share",method= RequestMethod.GET)
    @ResponseBody//把请求做成json，返回字符串
    public String share(String htmlUrl) {
        String filename= CommunityUtil.generateUUID();
        Event event=new Event()
                .setTopic(TOPIC_SHARE)
                //这里不需要传递作用的实体和触发的对象，因为kafka生成图片不需要这两个
                //给kafka传递模板路径，文件名和后缀，根据这个在本地保存生成的长图
                .setData("htmlUrl",htmlUrl)
                .setData("filename",filename)
                .setData("suffix",".png");
        eventProducer.fireEvent(event);
        //返回访问路径,用map来封装
        Map<String,Object> map=new HashMap<>();
        map.put("shareUrl",domain+"/share/image/"+filename);
        return CommunityUtil.getJSONString(0,null,map);
    }

    //2.其它用户或者我点击长图这个链接后就能获取到长图：从本地路径读取再写入到输出流中即可
    //提供获取长图的方法, map.put("shareUrl",domain+"/share/image/"+filename);也要按照这个路径去获取图片
    @RequestMapping(path="/share/image/{filename}",method = RequestMethod.GET)
    public void getShareImage(@PathVariable("filename")String filename, HttpServletResponse response) {
        //文件名为空
        if(StringUtils.isBlank(filename)){
            throw new IllegalArgumentException("文件名不能为空");
        }

        //指定本地路径便于输入流去读入
        File file=new File(wktohtmlStorage+"/"+filename+".png");
        response.setContentType("image/png");//一般来说是+suffix变量，因为图片格式有可能是jpg或png
        try {
            OutputStream os=response.getOutputStream();
            FileInputStream fis=new FileInputStream(file);
            //一批一批读取和写入
            byte[] buffer=new byte[1024];//1kB=1024个字节
            int b=0;
            //只要b!=-1说明确实读到了数据
            while((b=fis.read(buffer))!=-1){
                //读取缓冲区从0到b的值，b是实际读到的字节数
                os.write(buffer,0,b);
            }
            logger.info("生成长图成功");

        } catch (IOException e) {
            logger.error("获取长图失败",e.getMessage());
        }

    }


}
