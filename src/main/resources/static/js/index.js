$(function(){
	$("#publishBtn").click(publish);
});
//获取发布按钮，并给按钮定义一个单击事件：单击时调下面这个方法
function publish() {
	$("#publishModal").modal("hide");
	//获取标题和内容
	//选中这个框并获取框中的值
	var title=$("#recipient-name").val();
	var content=$("#message-text").val();
	// 发送异步请求（POST），因为客户端要向服务器提交数据
	//该请求对应三个参数：访问路径，传给服务器的数据，用键值对的方式传入,回调函数处理服务器返回的结果
	// 回调函数的意思是函数作为另一个函数的参数
	$.post(
		"/discuss/add",
		{"title":title,"content":content},
		function(data)
		{
			data=JSON.parse(data);//将 data 解析为一个 JavaScript 对象。
			//在提示框中显示返回消息
			$("#hintBody").text(data.msg);
			//显示提示框，一开始提示框是隐藏的，2s后自动隐藏提示框
			$("#hintModal").modal("show");
			setTimeout(function(){
				$("#hintModal").modal("hide");
				//成功的话刷新页面
				if(data.code==0)
				{
					window.location.reload();
				}
			},2000);
		}
	);
}