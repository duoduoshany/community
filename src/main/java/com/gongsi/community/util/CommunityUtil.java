package com.gongsi.community.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

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
    //上传头像，上传文件，每次上传都需要给文件生成随机的名字


}
