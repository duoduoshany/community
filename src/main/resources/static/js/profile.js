$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	//获取当前按钮
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		$.post(
			"/follow",//往这个路径发送POST请求,发送下面的数据，从html中获取
			{"entityType":3,"entityId":$(btn).prev().val()},
			function (data){
				data=$.parseJSON(data);
				if(data.code==0)
				{
					window.location.reload();//重新访问当前页面(/profile)，返回最新的数据
				}
				else{
					alert(data.msg);
				}
			}
		)
	} else {
		$.post(
			"/unfollow",//往这个路径发送POST请求,发送下面的数据，从html中获取
			{"entityType":3,"entityId":$(btn).prev().val()},
			function (data){
				data=$.parseJSON(data);
				if(data.code==0)
				{
					window.location.reload();//重新访问当前页面(/profile)，返回最新的数据
				}
				else{
					alert(data.msg);
				}
			}
		)
	}
}