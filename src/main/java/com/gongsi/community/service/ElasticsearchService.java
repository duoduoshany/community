package com.gongsi.community.service;

import com.gongsi.community.dao.elasticsearch.DiscussPostRepository;
import com.gongsi.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ElasticsearchService {
    //实现帖子保存到es服务器和从es服务器删除的功能
    @Autowired
    private DiscussPostRepository repository;
    //实现搜索结果带上高亮显示的字段。因为repository底层没有实现高亮字段和原始文档的数据整合
    @Autowired
    private ElasticsearchTemplate esTemplate;

    public void saveDiscussPost(DiscussPost post) {
        repository.save(post);
    }
    public void deleteDiscussPost(int id){
        repository.deleteById(id);
    }
    //关键词，当前第几页，每页最多数据
    public Page<DiscussPost> searchDiscussPost(String keyword,int current,int limit) {
        //构建封装查询条件的对象 作为参数传递进template的搜索方法
        SearchQuery searchQuery=new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword,"title","content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("create_time").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current,limit))//查询结果10条分一页，当前显示第一页数据
                .withHighlightFields(new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"))
                .build();
        //三个参数：封装查询条件的对象、查询结果类型、匿名实现接口
        return esTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                //通过response获取匹配的文档
                SearchHits hits=searchResponse.getHits();
                //有可能没查到数据，直接return null
                if(hits.getTotalHits()<=0) {
                    return null;
                }
                //查到数据做处理,根据命中的数据构造实体并放到集合中
                List<DiscussPost> list=new ArrayList<>();
                for(SearchHit hit:hits){
                    DiscussPost post=new DiscussPost();
                    //把json格式数据转化为map形式存,数据类型还是json，所以主键有双引号
                    //根据主键得到相应的值是Object类型
                    String id=hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));
                    String user_id=hit.getSourceAsMap().get("user_id").toString();
                    post.setUser_id(Integer.valueOf(user_id));
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
                    //有高亮显示的setContent只显示第一个包含高亮词的片段
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
    }


}
