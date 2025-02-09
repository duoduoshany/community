//异步请求可以发送给服务器数据，也可以接收服务器返回的数据
function like(btn,entityType,entityId,entityUserId,postId){
    $.post(
        "/like",//浏览器会向设置的路径url参数发送POST请求，并把下面的数据参数发送给服务器
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},//传给服务器的数据
        function (data){
            data=$.parseJSON(data);
            //如果返回的数据中的code是0，表示操作成功，改变赞的数量和显示赞的状态
            if(data.code==0){
                //找到 btn 按钮下的 <i> 元素，并将其文本更新为服务器返回的 likeCount（点赞数量）。
                $(btn).children("i").text(data.likeCount);
                //找到 btn 按钮下的 <b> 元素，并根据 likeStatus 的值更新为 '已赞' 或 '赞'。
                $(btn).children("b").text(data.likeStatus==1?'已赞':'赞');
            }
            //否则显示服务器错误信息
            else{
                alert(data.msg);
            }
        }//处理返回的数据
    )
}