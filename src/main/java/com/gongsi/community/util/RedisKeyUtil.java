package com.gongsi.community.util;

public class RedisKeyUtil {
    //加个前缀
    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user";
    private static final String PREFIX_FOLLOWER="follower";
    private static final String PREFIX_FOLLOWEE="followee";
    private static final String PREFIX_KAPTCHA="kaptcha";
    private static final String PREFIX_TICKET="ticket";
    private static final String PREFIX_USER="user";
    private static final String PREFIX_UV="uv";
    private static final String PREFIX_DAU="dau";
    private static final String PREFIX_POST="post";
    private static final String PREFIX_POSTS="posts";
    private static final String PREFIX_ROWS="rows";

    public static String getEntityLike(int entityType,int entityId) {
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
        //查询对某实体点赞的数量，大家都对某个目标实体点赞，就构造关联目标实体信息的键
        //对帖子类型的某个帖子id，对评论类型的某条评论id
    }
    public static String getUserLike(int userId)
    {
        return PREFIX_USER_LIKE+SPLIT+SPLIT+userId;
    }
    //谁关注了哪些东西，(userId,entityType)->ZSet(entityId,now),Type是为了分类关注的实体
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWER+SPLIT+userId+SPLIT+entityType;
    }
    //被关注的实体有哪些粉丝，（entityType,entityId)->ZSet(userId,now)
    public static String getFollowerKey(int entityType,int entityId)
    {
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityId;
    }
    //拼验证码的key，要识别验证码是哪个用户的，不用userId，打开登录页面看见验证码的时候用户还没登录，不知道userId
    //所以可以在用户访问的时候给它发个凭证（一个随机生成的字符串），存到cookie中，以该字符串来标识是哪个用户
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA+SPLIT+owner;
    }

    //登录的凭证:用登录凭证来构造键，这样的键都是根据用户自己的凭证生成的
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET+SPLIT+ticket;
    }
    //缓存用户信息
    public static String getUserKey(int userId){
        return PREFIX_USER+SPLIT+userId;
    }
    //单日uv的key
    public static String getUVKey(String date){
        return PREFIX_UV+SPLIT+date;
    }
    //区间uv
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }
    //单日DAU
    public static String getDAUKey(String date){
        return PREFIX_DAU+SPLIT+date;
    }
    //区间DAU
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_ENTITY_LIKE+SPLIT+startDate+SPLIT+endDate;
    }
    //统计帖子分数的key,因为这个键表示的数据是个帖子集合，所以不要传单独的帖子id去表示集合
    public static String getPostScoreKey(){
        return PREFIX_POST+SPLIT+"score";
    }

    public static String getPostsKey(int offset,int limit){
        return PREFIX_POSTS+offset+SPLIT+limit;
    }

    public static String getRowsKey(int userId){
        return PREFIX_ROWS+SPLIT+userId;
    }
}

