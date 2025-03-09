package com.gongsi.community.actuator;

import com.gongsi.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

//不属于任意层，是公共的组件
@Component
//给端点取个ID，到时候就可以通过/actuator/id来访问它
@Endpoint(id="database")
public class DatabaseEndPoint {
    private static  final Logger logger= LoggerFactory.getLogger(DatabaseEndPoint.class);
    //调用端点的时候尝试访问数据库，获取一个连接，连接取不到，有问题
    //注入dataSource，通过getConnection获取连接
    @Autowired
    private DataSource dataSource;

    @ReadOperation//注解表示该方法只能被GET请求访问到
    //把方法放到try()中，这样通过dataSource获取的连接在使用后最终会被自动释放掉，无需我们手动关闭
    public String checkConnection() {
        try(
                Connection connection=dataSource.getConnection()
        ) {
            return CommunityUtil.getJSONString(0,"获取连接成功");
        } catch (SQLException e) {
            //获取连接池的连接失败也要返回异常，记录日志
            logger.error("获取连接失败："+e.getMessage());
            return CommunityUtil.getJSONString(0,"获取连接失败");
        }
    }

}
