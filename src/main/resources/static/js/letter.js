$(function(){
	$("#sendBtn").click(send_letter);
	$(".close").click(delete_msg);
});

function send_letter() {
	//点击发送按钮会弹出框，同时点击事件触发了这个js方法：
	// 方法作用是发送的时候隐藏了弹出框，因为点击发送的时候说明我的数据已经写完可以提交了，浏览器像服务器发数据
	$("#sendModal").modal("hide");
	//获取用户提交的内容,弹出框的id与内容绑定,获取id选择器的val即可
	var toName=$("#recipient-name").val();
	var content=$("#message-text").val();
	//该请求对应三个参数：访问路径，传给服务器的数据:用键值对的方式传入,回调函数处理服务器返回的结果
	$.post(
		"/letter/send",
		{"toName":toName,"content":content},
		function(data){
			data=$.parseJSON(data);
			if(data.code=0)//成功了返回一个提示，找到提示框的id并发送内容
			{
				$("#hintBody").text("发送成功");
			}
			else{
				$("#hintBody").text(data.msg);
			}
			$("#hintModal").modal("show");//显示提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				location.reload();
			},2000);//过2s后自动隐藏提示框，并重载页面
		}
	)
	$("#hintModal").modal("show");
	setTimeout(function(){
		$("#hintModal").modal("hide");
	}, 2000);
}

function delete_msg() {
	// TODO 删除数据
	$(this).parents(".media").remove();
}