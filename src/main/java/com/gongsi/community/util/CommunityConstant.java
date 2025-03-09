package com.gongsi.community.util;

public interface CommunityConstant {
    //激活成功
    int ACTIVATION_SUCCESS = 0;
    //重复激活
    int ACTIVATION_REPEAT  = 1;
    //激活失败
    int ACTIVATION_FAILURE = 2;
    //默认状态的登录过期时间:12h
    int DEFAULT_EXPIRED_SECONDS=3600*12;
    //记住我状态下的登录凭证超时时间:100天
    int REMENBER_EXPIRED_SECONDS=3600*24*100;
    //评论的目标实体类型：帖子
    int ENTITY_TYPE_POST=1;
    //评论的目标实体类型：评论
    int ENTITY_TYPE_COMMENT=2;
    //增加实体类型：用户
    int ENTITY_TYPE_USER=3;
    //主题：点赞
    String TOPIC_LIKE="like";
    //主题：评论
    String TOPIC_COMMENT="comment";
    //主题：关注
    String TOPIC_FOLLOW="follow";
    //主题：发帖
    String TOPIC_PUBLISH="publish";
    //主题：删帖
    String TOPIC_DELETE="delete";
    //主题：分享
    String TOPIC_SHARE="share";
    //为了让别人知道系统用户user_id是1
    int SYSTEM_USER_ID=1;
    //普通用户
    String AUTHORITY_USER="user";
    //管理员
    String AUTHORITY_ADMIN="admin";
    //版主
    String AUTHORITY_MODERATOR="moderator";

}
