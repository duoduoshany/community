package com.gongsi.community.controller;

import com.gongsi.community.entity.DiscussPost;
import com.gongsi.community.entity.Page;
import com.gongsi.community.service.ElasticsearchService;
import com.gongsi.community.service.LikeService;
import com.gongsi.community.service.UserService;
import com.gongsi.community.util.CommunityConstant;
import org.elasticsearch.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller//为了展现搜索结果的网页，访问搜索路径时自动调用该方法，方法返回模板网页
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    //GET请求就无法从请求体中获取用户提交的数据，只能从路径获取:/search?keyword=xx
    @RequestMapping(path="/search",method= RequestMethod.GET)
    public String search(String keyword, Page page, Model model){
        //用户提交的是第几页数据，查询的时候我们却是用该页的索引查询，因为es服务器从第0页开始
        //两个Page类冲突，自动带上包名
        org.springframework.data.domain.Page<DiscussPost> searchResult=
        elasticsearchService.searchDiscussPost(keyword,page.getCurrent()-1,page.getLimit());
        //聚合最终显示结果
        List<Map<String, Object>> discussPostList=new ArrayList<>();
        //查询结果非空才聚合！！
        if(searchResult!=null){
            for(DiscussPost post:searchResult){
                Map<String, Object> map=new HashMap<>();
                //先传整个帖子,评论数量本身帖子也带有
                map.put("post",post);
                //作者
                map.put("user",userService.findUserById(post.getUser_id()));
                //点赞数量
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));

                discussPostList.add(map);
            }
        }
        //别忘了把聚合后的数据传递给模板
        model.addAttribute("discussPosts",discussPostList);
        //为了搜索出结果后页面仍然显示搜索词，所以把它传过去
        model.addAttribute("keyword",keyword);
        //实现分页
        page.setPath("/search?keyword="+keyword);
        page.setRows(searchResult==null?0:(int)searchResult.getTotalElements());

        return "/site/search";
    }


}
