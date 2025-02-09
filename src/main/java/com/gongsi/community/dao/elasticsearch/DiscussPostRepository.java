package com.gongsi.community.dao.elasticsearch;


import com.gongsi.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

//接口泛型指明了实现接口的方法的返回类型可以是任意类型
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer> {



}
