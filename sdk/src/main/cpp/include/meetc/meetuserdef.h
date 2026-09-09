#ifndef MEET_JSONPROTOCOLDEF_H
#define MEET_JSONPROTOCOLDEF_H

///start//////////////////////////////////////////////////////////////////////
//MeetUserDef_SLOGAN
//会议标语
 /*
 {
 "enble":"0",//是否启用0=不启用，1=启用
 "fileid":"0x6b000001",//会议标语界面用的底图
 "bgcolor":"0xffff5e3b"//会议标语使用的底色，如果没设置底图则使用底色
 "item": //显示的内容，可以自定实现多行显示
 [
	{ 
	"text":"欢迎某领导考察",//显示的文本
	"font":"宋体",//文本使用的字体
	"fontsize":"23",//字体大小,设置的是像素大小，客户端显示要考虑分辨率不同的自适应显示
	"bold":"0",//是否加粗显示 0不加粗，1加粗显示
	"align":"0x24",//对齐方式 自行组合设置 参见meetinterface_type.h MEET_FONTFLAG_LEFT
	"color":"0xffffffff",//使用ARGB的数值来控制颜色和透明度
	"topx":"0.1",//左上角X坐标 用百分比来计算 ,还原坐标也是如此，eg:x坐标是100，界面最大的长度是1920那么topx=100/1920
	"topy":"0.1",//参见topx
	"botomx":"0.6",//参见topx
	"botomy":"0.3"//参见topx
	},
 { "text":"欢迎某领导考察","font":"宋体","fontsize":"23","bold":"0","align":"0x24","color":"0xffffffff","topx":"0.1","topy":"0.1","botomx":"0.6","botomy":"0.3"},
 { "text":"欢迎某领导考察","font":"宋体","fontsize":"23","bold":"0","align":"0x24","color":"0xffffffff","topx":"0.1","topy":"0.1","botomx":"0.6","botomy":"0.3"}
 ]
 }
 */
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//文本广播
//投屏|停止投屏的协议定义
//texttype=TEXTBRODCAST_TYPE_JSON时,json的mode
#define 	ResShowMode_close		1 //停止投屏 无数据
#define 	ResShowMode_vote		2 //针对 投票(赞成|反对|弃权) id对应的是投票id 数组
#define 	ResShowMode_sign		3 //签到 无数据
#define 	ResShowMode_agenda		4 //时间议题 无数据
#define 	ResShowMode_othervote	5 //旧投票 id对应的是投票id  数组
#define 	ResShowMode_newvote		6 //新投票 id对应的是新投票id  数组
#define 	ResShowMode_image		7 //显示指定的图片 未实现
#define 	ResShowMode_fold		8 //折叠议题将议题附件隐藏 如果id数组为空默认是全部议题
#define 	ResShowMode_expand		9 //展开议题将议题附件显示 如果id数组为空默认是全部议题
#define 	ResShowMode_file	    10 //参会人向主持人|管理员|秘书角色获取他当前正在打开的文档ID
#define 	ResShowMode_notify	    11 //向参人发送一些消息提示

/*
//发送
{
"mode":1,//投屏模式
"markid":"123",//该值用于标识，在一些需要返回的数据中将该字段返回
"id":[1,2,3]//投票的ID
}

//发送 mode=ResShowMode_notify
{
"mode":11,//ResShowMode_notify 向参会人发送一些消息
"markid":"123",//该值用于标识，在一些需要返回的数据中将该字段返回
"per":"50",//显示比例 100表示全屏显示
"timeouts":10,//超时值 单位秒，0或空表示用户不用超时退出
"title":"提示",//可选 标题
"msg":"话筒已打开，您可以开始发言了",//
"btn":["确定","取消"]//可选 按钮文本
}

//返回---主持人|管理员|秘书角色收到文档同步请求后向请求的参会人返回的正在打开的文档信息
{
"mode":10,//
"markid":"123",//该值用于标识，在一些需要返回的数据中将该字段返回
"memberid":1,//参会人ID
"fileid":"0x6b000001"//文件ID
}
*/
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//SmartJsonProtol 批量议题修改状态 add on 20230705
//type=TYPE_MEET_INTERFACE_MEETAGENDA
//methon=METHOD_MEET_INTERFACE_SUBMIT
/*
{
 "item": //
 [
	{ 
	"agendaid":1,
	"status":1
	},
	{ 
	"agendaid":2,
	"status":0
	}
 ]
 }
 */
///end//////////////////////////////////////////////////////////////////////

///start add on 20240604//////////////////////////////////////////////////////////////////////
//SmartJsonProtol  数据库查询

#define DBSEARCH_TYPE_AGENDA		1 //根据议题标题匹配，返回匹配成功的会议ID和议题
#define DBSEARCH_TYPE_DIR			2 //根据目录名称匹配，返回匹配成功的会议ID和目录
#define DBSEARCH_TYPE_FILE			3 //根据文件标题匹配，返回匹配成功的会议ID，目录ID和文件ID,文件名称

//type=TYPE_MEET_INTERFACE_DBSERACH
//methon=METHOD_MEET_INTERFACE_SEARCH
//发送数据client--->meetdb
/*
{
"type": 1,//执行查询的类型 参见DBSEARCH_TYPE_AGENDA
 "name":"要匹配的文本"
}
*/

//结果通知通知返回，监听通知即可
//返回结果meetdb--->client
/*
{
"type":1,//执行查询的类型 参见DBSEARCH_TYPE_AGENDA
 "item": //
 [
	{ 
	"meetid":1,
	"agendaid":1,//查询议题时才返回
	"dirid":1,//查询目录时才返回
	"fileid":"0x6b00001",//查询文件时才返回
	"name":"匹配后的完整名称"
	},
	{ 
	"meetid":1,
	"agendaid":3,//查询议题时才返回
	"dirid":3,//查询目录时才返回
	"fileid":"0x6b00000",//查询文件时才返回
	"name":"匹配后的完整名称"
	},
 ]
 }
 */
 ///end//////////////////////////////////////////////////////////////////////


///start add on 20240604//////////////////////////////////////////////////////////////////////
//#define  MeetUserDef_APPROVALLOG   2   //会议审批日志 协议详见meetuserdef.h

//appr 的取值
#define MEET_APPROVAL_IDLE		0 //未审批
#define MEET_APPROVAL_ASK		1 //请求审批
#define MEET_APPROVAL_ING		2 //审批中
#define MEET_APPROVAL_OK		3 //审批通过
#define MEET_APPROVAL_FAIL		4 //审批不通过

/*
{
	"data":[
		{
		"asktm":"178959632",//申请审批的时间 utc秒
		"askid":5,//申请的adminid
		"askmsg":"用于部门讨论",//申请原因
		"apptm":"17892213",//审批的时间 utc秒
		"appid":1,//审批管理员id
		"status":1,//本次审批的状态参见MEET_APPROVAL_IDLE定义
		"appmsg":"审批通过" //本次审批的备注
		}
	]
}
*/
///end//////////////////////////////////////////////////////////////////////


#define OA_SYNCTYPE_PULLDDMEET 1 //拉取钉钉会议
#define OA_SYNCTYPE_PULLPEOPLE 2 //拉取常用人员
#define OA_SYNCTYPE_PULLADMIN  3 //拉取管理员

/*
///start add on 20240814//////////////////////////////////////////////////////////////////////
//无纸化会议系统拉取第三方会议
Type_FastCreateMeet METHOD_MEET_INTERFACE_SUBMIT
{
"type":1,//1=钉钉预约会议 必填
"roomid":"1",//拉取到指定会议室，为空表示第一个会议室
"phone":"13268091178"//拉取的手机号--阿里钉钉使用 为空会默认拉取管理员的手机号
}
///end//////////////////////////////////////////////////////////////////////

///start add on 20241014//////////////////////////////////////////////////////////////////////
//无纸化会议系统拉取第三方人员
Type_FastCreateMeet METHOD_MEET_INTERFACE_SUBMIT
{
"type":2//2=拉取并同步常用人员 必填
}

{
"type":3//3=拉取并同步管理人员 必填
}

{
"type":4//4=拉取并同步参会人员 必填
"meetid:1
}
///end//////////////////////////////////////////////////////////////////////
*/


///start//////////////////////////////////////////////////////////////////////
/*

发送设备硬件信息 Type_DeviceMacInfo json协议
{
"id":"0x110000",//设备id
"dy":20,//剩余电量百分比
"kj":"50",//已经使用的存储空间百分比
"wifi":"",//当前的wifi名称
"time":"75897562"//utc 秒
}

/*
查询设备硬件信息 Type_DeviceMacInfo json协议
{
"dev":[
{
"id":"0x110000",//设备id
"speed":"128k/s",
"cpu":"36%",
"memory":"4/8 G",
"dy":20,//剩余电量百分比
"kj":"50",//已经使用的存储空间百分比
"wifi":"",//当前的wifi名称
"uper":50,//升级包下载进度
"uerr":0,//升级状态码 参见meetinterface_type.h MEET_UPDATE_STATUS_IDLE
"umsg":"",//升级状态日志信息
"time":"75897562"//utc 秒
},
{
"id":"0x110001",//设备id
"dy":26,//剩余电量百分比
"kj":"55",//已经使用的存储空间百分比
"wifi":"",//当前的wifi名称
"uper":50,//升级包下载进度
"uerr":0,//升级状态码 参见meetinterface_type.h MEET_UPDATE_STATUS_IDLE
"umsg":"",//升级状态日志信息
"time":"75897599"//utc 秒
}
]
}

*/
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//SmartJsonProtol 批量添加文件到目录 add on 20250611
//type=TYPE_MEET_INTERFACE_MEETDIRECTORY
//methon=METHOD_MEET_INTERFACE_DUMP
/*
{
"dirid":1,//为0表示新建目录,指定id时，其它目录参数不需要赋值
"dirpos":1,//指定添加的序号,为0表示默认处理
"parentid":1,//为0表示添加到根目录
"name":"123",//新建目录的名称
"item": //
[
	{ 
	"fileid":"0x27000001",//文件id
	"size":"55648",//文件大小 字节单位
	"upid":1,//为0表示添加到根目录
	"uprole":1,//为0表示添加到根目录
	"upname":"ct",//上传者
	"flag":1,//为0表示添加到根目录
	"name":"123.mp4"//文件的名称
	},
	{ 
	"fileid":"0x6b000005",//文件id
	"size":"55648",//文件大小 字节单位
	"upid":1,//为0表示添加到根目录
	"uprole":1,//为0表示添加到根目录
	"upname":"ct",//上传者
	"flag":1,//为0表示添加到根目录
	"name":"4569.pdf"//文件的名称
	}
]
}
 */
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//SmartJsonProtol 批量修改可控会场 add on 20250612
//type=TYPE_MEET_INTERFACE_MANAGEROOM
//methon=METHOD_MEET_INTERFACE_DUMP
/*
{
"save":0,//0=表示执行添加到可控会场,1=表示重新保存到可控会场
"adminid":[1,2],
"roomid":[1,2]//数组为空或者字段不存在则表示执行清空可控会场
}
 */
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//SmartJsonProtol 参会人员文件自定义数据协议 add on 20250619
//type=TYPE_MEET_INTERFACE_MANAGEROOM
//methon=METHOD_MEET_INTERFACE_DUMP
/*
{
"opentime":"123456789",//文件打开的时间 utc秒
"tag":["good","bad"],//用户给文件加的标签
"opinion":"这个方案非常好",//文件评价
"score":"9.5",//文件评分
"mark":0//表示文件是否设置了收藏
}
*/
///end//////////////////////////////////////////////////////////////////////

///start//////////////////////////////////////////////////////////////////////
//全局自定义数据--自定义快捷操作协议 add on 20260803
//功能标志
#define MEET_PULICINFO_FLAG_WARNING 0x00000001 //点击按钮是否弹框提示
#define MEET_PULICINFO_FLAG_MEET    0x00000002 //表示无纸化内部快捷操作
/*
{
"item":[
{"id":1,//标识这个操作的id,要求系统内是唯一的,如果不需要组合使用的可以不指定
"flag":"0x1",//功能标志 MEET_PULICINFO_FLAG_WARNING
"name":"打开灯光",//按键显示名称
"addr":{
"type":"tcp",//表示使用tcp链接
"ip":"192.168.1.208","port":3399},//第三方中控地址信息
"data":"lightON"//点击按钮发送的文本数据
},
{"id":2,//标识这个操作的id,要求系统内是唯一的,如果不需要组合使用的可以不指定
"flag":"0x1",//功能标志 MEET_PULICINFO_FLAG_WARNING
"name":"打开窗帘",//按键显示名称
"addr":{
"type":"tcp",//表示使用tcp链接
"ip":"192.168.1.208","port":3399},//第三方中控地址信息
"data":"curtainsON"//点击按钮发送的文本数据
},
{"flag":"0x1",//功能标志 MEET_PULICINFO_FLAG_WARNING
"name":"开始会议",//按键显示名称
"addr":{
"type":"group",//表示这是一个组操作
"dstid":[{"id":1,"sleep":500,},{"id":2,"sleep":500,}],//id数组，按顺序执行,sleep表示执行后的延时单位毫秒
},
{"flag":"0x3",//功能标志
"name":"外部信号1",//按键显示名称
"addr":{"type":"start",//表示是无纸化同屏
"srcdev":"0x10c0000",//采集设备ID
"sub":2,//子通道号
"dstdev":["0x110000"],//播放设备ID数组,为空表示会议内的所有播放设备，如果是同屏要根据场景排除自身ID
"res":[0]//目录播放资源数组
},//地址信息
{"flag":"0x3",//功能标志
"name":"停止播放",//按键显示名称
"addr":{"type":"stop",//表示是无纸化停止资源
"dstdev":["0x110000"],//设备ID数组,为空表示会议内的所有播放设备
"res":[0]//目录播放资源数组
},
{"flag":"0x3",//功能标志
"name":"签到",//按键显示名称
"addr":{"type":"routepage",//表示跳转到无纸化指定的页面
"page":2,//SYSTEMLOG_PAGE_FRONTMAIN 参见systemlogoperid.h -- SYSTEMLOG_PAGEID定义
"dstdev":["0x110000"]//设备ID数组,为空表示会议内的所有客户端设备
},
{"flag":"0x3",//功能标志
"name":"请安静",//按键显示名称
"addr":{"type":"msg",//表示是无纸化停止资源
"dstdev":["0x110000"],//设备ID数组,为空表示会议内的所有客户端设备
"data":{ //点击按钮发送的文本数据
"per":"50",//可选显示比例 100表示全屏显示
"timeouts":10,//可选超时值 单位秒，0或空表示用户不用超时退出 默认8秒
"title":"提示",//可选 标题
"msg":"话筒已打开，您可以开始发言了",//
}
}
}
]
}
*/
///end//////////////////////////////////////////////////////////////////////
#endif