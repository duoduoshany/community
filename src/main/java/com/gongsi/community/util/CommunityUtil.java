package com.gongsi.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//需要写一个工具类，提供两个公有的静态的方法，注册的时候方便调用，就不需要现写
public class CommunityUtil {
    //生成随机字符串，激活码每次都是随机生成的
    public static String generateUUID() {
        //随机生成UUID随机数对象，并转化为字符串，由于对象包含多个-字符
        //因此将所有-替换成空的字符串
        return UUID.randomUUID().toString().replace("-", "");
    }
    //MD5加密，对密码进行加密，防止明文被盗，只能加密不能解密，hello->abc123456，每次加密结果一样
    //密码简单，加密就简单，密码也容易被获取，因此在原始密码后加入随机字符串(用户类的salt字段）作为参数传入再加密更安全一些
    public static String md5(String key){
        //判断如果是空串或空格返回null，不去处理，否则调用Spring工具调用MD5方法加密成十六进制的字符串并返回
        if(StringUtils.isBlank(key)){
            return null;
        }
        //要求参数传入的是字节数组，所以把字符串对象key转换一下
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }
    public static String getJSONString(int code , String msg, Map<String,Object> map)
    {
        JSONObject json = new JSONObject();
        //json对象封装传入的参数数据，code，如200；msg：执行成功或失败的提示消息。
        json.put("code",code);
        json.put("msg",msg);
        //map：具体的业务数据；对于map对象需要单独获取它的键值对并装入json对象中，通过遍历map得到
        if(map!=null)
        {
            for(String key:map.keySet())
            {
                json.put(key,map.getOrDefault(key,0));
            }
        }
        //最终json对象转化为json格式的字符串
        return json.toJSONString();
    }
    //有可能封装的只有两个数据
    public static String getJSONString(int code,String msg)
    {
        return getJSONString(code,msg,null);
    }
    //有可能封装的只有一个数据
    public static String getJSONString(int code)
    {
        return getJSONString(code,null,null);
    }





}
