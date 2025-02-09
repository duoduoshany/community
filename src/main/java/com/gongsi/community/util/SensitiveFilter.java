package com.gongsi.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component//托管给容器管理

//定义敏感词过滤器，到时候直接调用这个工具类就可以实现敏感期过滤
public class SensitiveFilter {
    private static final Logger logger=LoggerFactory.getLogger(SensitiveFilter.class);
    //替换符
    private static final String REPLACEMENT="***";

    //初始化根节点
    private TrieNode rootNode=new TrieNode();

    @PostConstruct
    //Spring容器实例化外部类之后，就会自动调用标记注解的初始化方法。该方法会在其它自定义方法前执行，一个类只能有一个这样的注解
    // 2.根据txt文件初始化前缀树的数据
    public void init()
    {
        //类加载器从类目录classes目录下加载资源。程序一编译所有代码都会编译到classes之下，包括配置文件.txt
        //字节流也需要我们自己关闭，也在try-catch的try中即可
        try(InputStream is=this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            InputStreamReader isr=new InputStreamReader(is);
            BufferedReader br=new BufferedReader(isr);//输入流->reader字符流->缓冲流，读取效率更高
            )
        {
            String keyword;//每读取一个词就放到keyword中，文件中一行是一个敏感词
            while( (keyword=br.readLine())!=null)
            {
                //添加到前缀树，逻辑比较复杂，封装成一个方法来调用
                this.addKeyWord(keyword);

            }
        }
        catch (IOException e){
            logger.error("加载敏感词文件失败:"+e.getMessage());
         }
    }
    //被外界调用过滤敏感词，所以方法是公有的，返回替换后的结果
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        //过滤时依赖三个指针，指针1指向树
        TrieNode currentNode = rootNode;
        //指针2和指针3指向字符串的首位，真的很类似于left和right
        int begin = 0;
        int position = 0;
        //结果的存储变量用StringBuilder变长字符串
        StringBuilder sb = new StringBuilder();
        //指针3优先指针2到达结尾，所以用指针3作为判断条件
        while (position < text.length()) {
            //遍历每个字符
            char c = text.charAt(position);
            //如果是特殊字符,且前缀树指向根节点，证明特殊字符在敏感词之前，可以直接添加，不影响敏感词判断
            if (isSymbol(c)) {
                if (currentNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                //无论符号在敏感词前面或中间，指针3也会向下走一步
                position++;
                continue;//如果是特殊符号,不获取子节点，进入下一轮循环
            }
            //根据符号c获取子节点!(不是当前节点)以下的敏感词的特殊符号被过滤掉了
            currentNode = currentNode.getSubNodes(c);
            //如果子节点不存在，说明不在前缀树的敏感词判断，直接添加到结果集中
            //指向字符串的两个指针begin向后移一位，position跳到begin的同一位置，指向前缀树的指针重新指向根节点
            if (currentNode == null) {
                sb.append(text.charAt(begin));
                position = ++begin;
                currentNode = rootNode;
                //如果是末尾节点，那么向结果集中添加替换符，并且position向后移一位，begin跳到position的同一位置
            } else if (currentNode.isKeyWordEnd()) {
                sb.append(REPLACEMENT);
                begin = ++position;
                currentNode = rootNode;
            } else {
                //子节点存在且不是末尾节点，前缀树指针指向了子节点，position指针向后移动一位，与end指针构成一个字符串，end指针不动
                position++;
            }
        }
        //还会出现position到达末尾，但begin-position的还没有构成敏感词这部分
        sb.append(text.substring(begin));
        return sb.toString();
    }
    //跳过符号，有一些人可能在敏感词之间穿插特殊的符号避免被认出敏感词，如开#票#,所以遍历时判断是否为特殊符号
    //在敏感词中间的特殊符号跳过不记录
    private boolean isSymbol(Character c)
    {
        //0x2E80-0x9FFF是东亚文字范围：比如中文日语韩文登
        //工具类方法的作用是如果字符不是英文字符而是那些特殊符号的就会return
        //加上后面的是为了避免把中文日语和韩文也当作特殊符号了
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c> 0x9FFF);

    }


    //方法内部使用，也不需要公有，把这个敏感词添加到前缀树对象中
    //对每个关键词：遍历关键词中的每个字，一层一层往下挂。用根节点把这一条一条接起来。
    private void addKeyWord(String keyWord)
    {
        TrieNode currentNode=rootNode;//指针默认指向根节点
        for(int i=0;i<keyWord.length();i++) {
            char ch = keyWord.charAt(i);
            //把这个字符挂到当前节点的下面，已经存在的子节点不需要再次添加，指针可以指向存在的节点，继续为存在的节点添加节点
            TrieNode trieNode = currentNode.getSubNodes(ch);
            if (trieNode == null) {
                trieNode = new TrieNode();
                currentNode.addSubNode(ch, trieNode);
            }
            //进入下一层循环前，控制指针指向下一个节点，便于添加下一层节点的子节点
            currentNode=trieNode;
            //设置结束的标识
            if(i==keyWord.length()-1) {
                trieNode.setKeyWordEnd(true);
            }
        }
    }
    //1.定义前缀树
    private class TrieNode{
        private boolean isKeyWordEnd=false;
        //是不是末尾节点，相当于一个数组只要其中有个末尾节点就做个标记即可
        public boolean isKeyWordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }
        //当前节点的子节点，子节点可能是多个，用Map封装当前节点的子节点
        // 以字符为key，因为key后续要和其它字符比较，value为前缀树的Node
        private Map<Character, TrieNode> subNodes=new HashMap<>();
        //必须提供往里装数据和获取数据的办法，往里装数据是为了初始化前缀树
        public void addSubNode(Character c,TrieNode node)
        {
            subNodes.put(c,node);
        }
        //subNodes通过add方法已经
        //通过给定的字符来判断字符是否存在于当前节点的子节点中。调用这个方法的节点对象就是当前节点
        //根据子节点的key获取整个子节点是为了节点属性isKeyWordEnd 是否为 true。
        public TrieNode getSubNodes(Character c) {
            return subNodes.get(c);
        }
    }

}
