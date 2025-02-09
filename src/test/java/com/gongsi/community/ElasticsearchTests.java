package com.gongsi.community;

import com.gongsi.community.dao.DiscussPostMapper;
import com.gongsi.community.dao.elasticsearch.DiscussPostRepository;
import com.gongsi.community.entity.DiscussPost;
import com.tdunning.math.stats.Sort;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {
    @Autowired
    private DiscussPostMapper discussPostMapper;
    //es服务器从mysql中取到数据后，也需要访问es服务器，注入该接口
    @Autowired
    private DiscussPostRepository discussPostRepository;
    //有些特殊情况这个repository解决不了，就注入template
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Test
    public void testInsert(){
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
    }
    @Test
    public void testInsertAll(){
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(101,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(102,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(103,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(111,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(112,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(131,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(132,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(133,0,100));
        discussPostRepository.saveAll(discussPostMapper.selectDiscussPost(134,0,100));
    }
    @Test
    public void testUpdate(){
        DiscussPost post=discussPostMapper.selectDiscussPostById(231);
        post.setContent("我是新人，请多指教");
        discussPostRepository.save(post);
    }
    @Test
    public void testDelete(){
        discussPostRepository.deleteById(231);
    }
    @Test
    //搜索结果是否排序、分页或包含搜索条件的关键词则高亮显示的组件
    public void testSearchByRepository(){
        //左边接口、右边是一个查询对象，设置了输入内容会与标题和内容进行匹配的查询条件
        //设置了按照帖子的type（是否置顶,1是置顶），score（帖子的价值），create_time（时间）来输出搜索结果，先看type再看后面两个
        //可能查出几万条，一页不可能容纳这么多，构造分页的条件
        //构造高亮显示，该字段中有关键词被匹配到就加上html标签：可以将文字变红的标签
        // build返回封装了查询条件的searchQuery对象
        SearchQuery searchQuery=new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("create_time").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))//查询结果10条分一页，当前显示第一页数据
                .withHighlightFields(new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                            new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"))
                .build();
        //把封装了各种查询条件的searchQuery对象传进去由discussPostRepository对象访问es服务器执行查询
        Page<DiscussPost> page= discussPostRepository.search(searchQuery);
        //测试
        System.out.println(page.getTotalElements());//111条
        System.out.println(page.getTotalPages());//12页
        System.out.println(page.getNumber());//得到当前页：0
        System.out.println(page.getSize());//得到每一页最大数据量：10
        for(DiscussPost post:page){
            System.out.println(post);//只打印当前页的数据
        }
        //repository底层获取到了高亮显示的值，但是没有返回，导致输出的结果没有高亮显示关键词，用esTemplate方法
    }
    @Test
    public void testSearchQuery(){
        //Template也是执行相同的查询对象
        SearchQuery searchQuery=new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("create_time").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))//查询结果10条分一页，当前显示第一页数据
                .withHighlightFields(new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"))
                .build();
        //三个参数：封装查询条件的对象、查询结果类型、匿名实现接口
        Page<DiscussPost> page= elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                //通过response获取匹配的文档
                SearchHits hits=searchResponse.getHits();
                //有可能没查到数据，直接return null
                if(hits.getTotalHits()<=0) {
                    return null;
                }
                //查到数据做处理,把数据封装到集合中并返回
                List<DiscussPost> list=new ArrayList<>();
                for(SearchHit hit:hits){
                    DiscussPost post=new DiscussPost();
                    //把json格式数据转化为map形式存,数据类型还是json，所以主键有双引号
                    //根据主键得到相应的值是Object类型
                    String id=hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));
                    String user_id=hit.getSourceAsMap().get("user_id").toString();
                    post.setId(Integer.valueOf(user_id));
                    //先获取匹配的文档的title和content，再去找有包含关键字的，然后对关键字高亮显示
                    String title=hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);
                    String content=hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);
                    String status=hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));
                    String createTime=hit.getSourceAsMap().get("create_time").toString();
                    post.setCreate_time(new Date(Long.valueOf(createTime)));
                    String commentCount=hit.getSourceAsMap().get("comment_count").toString();
                    post.setComment_count(Integer.valueOf(commentCount));
                    //处理高亮显示
                    HighlightField titleField=hit.getHighlightFields().get("title");
                    if(titleField!=null){
                        post.setTitle(titleField.getFragments()[0].toString());
                    }
                    HighlightField contentField=hit.getHighlightFields().get("content");
                    if(contentField!=null){
                        post.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(post);
                };
                return new AggregatedPageImpl(list,pageable,hits.getTotalHits(),
                        searchResponse.getAggregations(),searchResponse.getScrollId(),hits.getMaxScore());
            }
        });
        //测试
        System.out.println(page.getTotalElements());//111条
        System.out.println(page.getTotalPages());//12页
        System.out.println(page.getNumber());//得到当前页：0
        System.out.println(page.getSize());//得到每一页最大数据量：10
        for(DiscussPost post:page){
            System.out.println(post);//只打印当前页的数据
        }
    }

}
