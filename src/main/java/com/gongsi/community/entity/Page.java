package com.gongsi.community.entity;
//封装分页相关的信息
public class Page {
    //当前第几页
    private int current=1;
    //显示数据上限
    private int limit=10;
    //数据的总数，一共有多少条数据，这是服务端查出来的,用于计算总的页数
    private int rows;
    //查询路径,每一页都可以点，都是个查询链接（用于复用分页链接）
    private String path;
    //前两个传递给服务端的东西，后两个是客户端需要用的。

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        //用户可能输入一个无效的页码，需要判断不符合条件的页码
        if(current>=1)
        this.current = current;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        //万一用户给出要在当页显示超多条数的数据，给一下限制
        if(limit>=1&&limit<=100)
        this.limit = limit;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if(rows>=0)
        this.rows = rows;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    //在调用数据库查询需要我们传一个offset获取当前页的起始行
    public int getOffset(){
    return current*limit-limit;
    }
    //比如你的网站中有 53 篇帖子，每页显示 10 篇，rows 就是 53。然后你可以通过 rows 和 limit 计算出总页数为 6。
    public int getTotal()
    {
            return rows%limit==0?rows/limit:rows/limit+1;
    }
    //如果有很多页相关数据，联想查论文的返回页数，用户处于第 50 页，那么分页导航只显示 48、49、50、51、52 页
    //获取起始页码
    public int getFrom(){
        //当前页是1按当前页算
        int from=current-2;
        return from<1?1:from;
    }
    public int getTo(){
        //截止页超过总页数按最后一页算
        return Math.min(current + 2, getTotal());
    }
}
