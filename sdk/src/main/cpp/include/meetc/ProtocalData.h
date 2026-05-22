#ifndef PROTOCOLDATA_H
#define PROTOCOLDATA_H

#pragma pack(push, 4)

#if defined(_MSC_VER) && (_MSC_VER >= 1600)  
# pragma execution_character_set("utf-8")  
#endif  

#include "base.h"
#include "owbase.h"

/*                    协议基本结构
  ---------------------------------------------------
 |  请求头requestheader   或  应答头responseheader   |
  ---------------------------------------------------
 |                     数据信息头                    |
  ---------------------------------------------------
 |                       数据                        |
  ---------------------------------------------------
  "数据"部分中的各种数据按在"数据信息头"出现顺序与大小（字节）存储  */

//协议版本
//#define PROTOCOL_VERSION 20160608
#define PROTOCOL_VERSION_V1_0 20180803
#define PROTOCOL_VERSION2_0 20211018
#if SUPPORTVERUPDATE ==1
#define PROTOCOL_VERSION PROTOCOL_VERSION2_0 //将会议\目录\文件名称改为不定长
#else
#define PROTOCOL_VERSION PROTOCOL_VERSION_V1_0
#endif
//全局数据指针
//void *pm = NULL;


//为了整个程序比较清晰，把枚举类型统一放到类里面管理
class ProtocalData
{
	public:
#define NAME_LENG	48  //名称最大长度，包含字符串结束符，包括人员名字，设备名称
#define DESCRIBE_LENG 100  //一般描述字符长度
#define TIMECHARACTER_LENG	25 //时间字符长度，这里存有余量，方便不同格式时间字符传输
#define FILENAME_LENG	150	//文件名称，文件夹名称长度限制
#define VOTE_CONTENTLENG  200	//投票表决有关字符长度限制
#define VOTE_LENG		  60	//投票表决选择项字符长度限制
#define CHATTEXT_LENG	300 //聊天内容最大长度
#define PASSWORD_LENG	40	//密码最大长度
#define SHORT_DESCRIBE_LENG	    40  //一般描述字符较短长度
#define ONESIGNPASSWORD_LENG    16  //一次会议签到密码长度
#define PD_SHORT_PASSWORD_LENG   8 //默认短密码长度

	//**********阶段定义部分**************

	//阶段stages信息
	enum STARG_ENUM {  
		STAGE_SysTime,  //系统时间，这里暂时只有时间内容，可能还会增加公司名称之类标题的
		STAGE_DeviceId, //开机时获取的设备编号信息
		STAGE_SystemSet, //系统设置有关
		STAGE_MemberManage,//参会人员管理阶段
		STAGE_PeopleManage,//人员管理
		STAGE_MeetingManage, //会场管理
		STAGE_StartUpMeeting, //发起会议
		STAGE_MeetingAgenda, //会议议程
		STAGE_MeetingDatum,  //会议资料
		STAGE_MeetingVideo,  //会议视频
		STAGE_MeetingSeat,  //会议排位
		STAGE_MeetingChat,	//聊天
		STAGE_MeetingVote, //投票管理
		STAGE_SignIn,	//签到情况
		STAGE_Postil,	//批注查看
		STAGE_BaiBan,	//电子白板
		STAGE_MeetingStatus, //会议状态管理
		STAGE_DeviceASGN,   //设备分配阶段
		STAGE_Wait4User,	//等待用户 会议终端显示信息
		STAGE_PushFile,		//文件推送到会议终端
		STAGE_PushStream,	//流推送到会议终端
		STAGE_RePushStream,	//向主持人请求流推送到会议终端
		STAGE_PushUpdate,	//向所有设备推送数据更新
		STAGE_Admin,		//管理员
		STAGE_FunConf,		//会议功能配置
		STAGE_PeopleGroup,	//人员分组
		STAGE_MemberGroup,	//参会人员分组
		STAGE_StreamControl,	//屏幕流的鼠标键盘控制指令
		STAGE_DeviceOper,	//设备控制
		STAGE_ManagerRoom,   //管理员会议室
		STAGE_MeetStatistic,   //会议数据统计
		STAGE_MeetingFace,    //会议界面配置
		STAGE_FileScore,    //会议文件评分相关
		STAGE_FileEvaluate,    //会议文件评价相关
		STAGE_MeetEvaluate,    //会议评价相关
		STAGE_SystemLog,    //管理员操作日志相关
		STAGE_PublicInfo,    //一些系统全局使用的字串
		STAGE_FileScoreVote, //自定义文件评分
		STAGE_PDFWHITEBOARD, //PDF文件多人同时书写
		STAGE_ZKIDENTIFY, //生物认证
		STAGE_MeetTopic, //会议议题
		STAGE_Lecture, //会议演讲文稿
		STAGE_HomePage, //会议欢迎界面
		STAGE_MeetUserDef,//会议用户自定义数据
		STAGE_RoomUserDef,//会场用户自定义数据
		STAGE_MeetOrder,//会议预约
		STAGE_AVote,//新投票
		STAGE_SeatPlan,//席位方案
		STAGE_NewMeetAgenda,//新议题
		STAGE_SearchData,//数据库查询
		STAGE_DeviceFaceState,//数据库状态交互
		STAGE_MergePushUpdate,	//合并向所有设备推送数据更新
		STAGE_ComplexPublicUserInfo,	//全局复合自定义数据
		STAGE_ComplexMeetUserInfo,	//会议复合自定义数据
		STAGE_NewSystemLog,    //新操作日志相关
		STAGE_End,    //结束
	};

	//*************功能定义分类********************

	enum FUNCTION_ENUM{
		 //系统设置 功能分类
		FUN_TerminalControl,		//终端升降控制
		FUN_CompereControlAll,		//主持人控制多终端
		FUN_CompereControlOne,		//主持人控制单终端
		FUN_AdminPassword,			//管理员登录密码  管理员登录这里密码登录，取消用户名
		//		FUN_PermissionAll,			//多人员权限管理
		//		FUN_PermissionOne,			//单人员权限管理
		FUN_MeetingStatus,			//会议状态管理
		FUN_URL,					//默认打开网址
		FUN_FontColorBgp_welcome,	//首页欢迎界面有关字体大小,背景图，颜色的设置
		FUN_FontColorBgp_desk,		//桌牌有关字体大小,背景图，颜色的设置

		FUN_All,					//对所有操作
		FUN_One,					//对单个操作

		FUN_Agenda,					//会议议程
		FUN_Bulletin,				//会议公告

		FUN_Dir_All,				//全部目录
		FUN_Dir_One,				//目录
		FUN_File_All,				//全部文件
		FUN_File_One,				//文件


		FUN_File_Access,			//文件权限


		FUN_StartVote,	//发起投票
		FUN_Vote,		//投票
		FUN_VoteInfo,   //查询投票信息
		FUN_VoteCount,	 //查询投票计数
		FUN_Device_Asgn,  //设备分配
		FUN_Wait4User,	  //等待用户 会议终端显示信息
		FUN_PushFile,	  //文件推送到会议终端
		FUN_PushStream,	  //流推送到会议终端
		FUN_RePushStream,  //向主持人请求流推送到会议终端
		FUN_PushUpdate,		//向所有设备推送数据更新
		FUN_VoteRecord,		//查询记名投票记录

		FUN_ColorConfig,		//参会人员-白板颜色配置
		FUN_STREAMCONTROL,	    //屏幕流的鼠标键盘控制指令
		FUN_MemberPermission,	//参会人员权限
		FUN_DirPermission,		//目录参会人权限
		FUN_DevicePoint,  //设备指引轨迹
	};

		//*****************方法定义分类********************
	enum METHOD_ENUM{
		METHOD_Control,//控制
		METHOD_Set,   //设置
		METHOD_Query, //查询
		METHOD_Add,  //添加
		METHOD_Modify,//修改
		METHOD_Delete,//删除
		METHOD_Report,//告知
		METHOD_Stop, //关闭
		METHOD_Notify, //通知
		METHOD_Search, //搜索
		METHOD_QueryData, //查询数据
		METHOD_QueryResult, //查询结果
		METHOD_Dump, //复制
		METHOD_Start, //开始
		METHOD_Sign, //签到
		METHOD_Import, //导入
	};

	//**************具体每个功能控制定义参数**************************
	enum TerminalControl_ENUM{
		Control_Restart, //重启
		Control_Up,      //上升
		Control_Down,    //下降
		Control_Stop,    //停止关机
		Control_SoftRestart,    //软件重启
		Control_ChangeLOGO,//更换LOGO
		Control_ChangeMainBG,//更换主界面背景
		Control_ChangeSubBG,//更换子背景
		Control_ChangeProjectiveBG,//更换投影机背景
		Control_ChangeFontColor,//更换字体颜色
		Control_LiftStop,    //停止升降
		Control_MonitorON,//控制显示器亮屏
		Control_MonitorOFF,//显示器熄屏
		Control_LiftOpen,//开启话筒/主机开机
		Control_LiftClose,//关闭话筒/主机关机
		Control_CHECKIN, ////隐藏投影签到信息
		Control_LIFTON,//			//上升开机
		Control_LIFTOFF,//			//上升关机
		Control_RATOTE15,//			//翻转15
		Control_RATOTE30,//		//翻转30
		Control_PANELENABLE,//			//面板启用
		Control_PANELDISABLE,//		//面板禁用
		Control_SWITCHHDMI1,//			//切换hdmi1
		Control_SWITCHHDMI2,//			//切换hdmi2
		Control_ROTATESPEED,//		//翻转速度
		Control_LIFTSPEED,//			//升降速度
		Control_RATATEANGLE,//			//翻转指定角度
	};

	enum SEAT_DIRECTION{
		SEAT_UP,//朝上
		SEAT_DOWN,//朝上
		SEAT_LEFT,//朝左
		SEAT_RIGHT,//朝右
	};

	enum VOTEMAIN_TYPE{
		MAINTYPE_vote,//投票
		MAINTYPE_election,//选举
		MAINTYPE_questionnaire,//问卷调查
		MAINTYPE_vote_start,//即时投票
		MAINTYPE_vote_agenda,//议题文件投票
	};

	enum VOTE_MODE{
		VOTE_agonymous,//匿名投票
		VOTE_signed,//记名投票
	};

	enum VOTE_TYPE{
		VOTE_Many, //多选
		VOTE_Single,//单选 
		VOTE_4_5,	//多选
		VOTE_3_5,	//多选
		VOTE_2_5,	//多选
		VOTE_2_3,	//多选
	};

	enum CHAT_TYPE_ENUM {
		CHAT_Message, //文本消息
		CHAT_Link,	  //多媒体链接
		CHAT_Water,		//水
		CHAT_Tea,		//茶
		CHAT_Coffee,    //咖啡
		CHAT_Pen,		//笔
		CHAT_Paper,		//纸
		CHAT_Technical,	//技术员
		CHAT_Waiter,	//服务员
		CHAT_Other,		//其他服务
		CHAT_Emecc,		//申请主持
	};
	
	//通讯协议应答PD_Responseheader头里面的 请求状态处理内容包含
	enum STARTU_ENUM{
		STATUS_MULTIRECORD,		  //多条查询记录
		STATUS_SINGLERECORD,	  //单条查询记录
		STATUS_NORECORED,		  //无返回记录
		STATUS_DONE,			  //操作成功
		STATUS_FAIL,			  //请求失败
		STATUS_EXCPT_DB,		//数据库异常
		STATUS_EXCPT_SV,		//服务器异常
		STATUS_ACCESSDENIED,	//权限限制
		STATUS_PSWFAILED,		//密码错误
		STATUS_COLL_MEETING,		//创建会议有冲突	
		STATUS_PARAMETERZERO,		//参数错误,不应该为0
		STATUS_NOTEXIST,		//不存在的数据
		STATUS_PROTOLDISMATCH,		//协议版本不区配
	};
	
	enum MEETING_STATUS{
		MEETING_Ready = 0,//会议创建中，等待开始
		MEETING_Start = 1,//会议开始进行中
		MEETING_End = 2,//会议结束
		MEETING_PAUSE = 3,//会议暂停
		MEETING_MODEL = 4,//模板会议
		MEETING_UNACTIVE = 5,//未激活
		MEETING_APPROVALPED = 6,//等待审批
		MEETING_APPROVALING = 7,//审批中
		MEETING_APPROVALOK = 8,//审批通过
		MEETING_APPROVALFAILED = 9,//审批不通过
		MEETING_RESETDATA = 10,//重置会议，会议预演结束清空数据恢复到会议开始前的状态

	};

	enum MEETING_DEVICEOPER{
		MEETING_STOPRESWORK,//停止会议任务
		MEETING_ZOOMPER,//放大小缩小播放窗口操作
		MEETING_ENTERMEETFACE,//会议签到进入
		MEETING_RETURNMAINFACE,//主界面返回
		MEETING_NOTIFYMEMBERNUM,//广播某会议当前的参会人员数
		MEETING_NOTIFYMEMBERSTATE,//设备广播状态
		MEETING_REQUESTMEMBERSTATE,//请求设备的状态
		MEETING_PREPAGEREQUEST,//设置当前页上一页操作
		MEETING_NEXTPAGEREQUEST,//设置当前页下一页操作
		MEETING_SETPAGENUMREQUEST,//定位到某页操作
		MEETING_REQUESTTOMANAGE,//请求成为管理员
		MEETING_RESPONSETOMANAGE,//回复请求成为管理员
		MEETING_REQUESTPRIVELIGE,//请求某些权限
		MEETING_RESPONSEPRIVELIGE,//回复请求某些权限
		MEETING_TEXTBRODCAST,//文本信息广播
		MEETING_STOPBULLET,//停止公告
		MEETING_REMEDIAUPDATEPLAY,//跟随播放音视频文件
		MEETING_DEVICELOCATE,//设备定位
		MEETING_REQUESTCHAT,//邀请聊天
		MEETING_RESPONSECHAT,//回复邀请聊天
		MEETING_EXITCHAT,//退出聊天广播
		MEETING_NETBANDTEST,//带宽丢包测试
		MEETING_WELCOMEFACE,//切换到欢迎界面
		MEETING_REMOTESET,//远程配置参数
		MEETING_PAUSEPLAY,//推送视频的暂停恢复
		MEETING_SETPLAYPOS,//推送视频的进度拖动
		MEETING_PUSH,//设备自由交互的数据
		MEETING_DUMPSTACK,//设备自由交互的数据
	};

	enum MEET_DEVFACESTATEOPER{
		MEET_DEVFACESTATEOPER_REQ,//请求所有设备的状态
		MEET_DEVFACESTATEOPER_SET,//更新设备的状态
		MEET_DEVFACESTATEOPER_NOTIFY,//广播设备的状态
	};
};


//----------特殊id----------
#define ID_Send2AllDevice	0x00	//表示所有设备



//----------会议类型----------
#define TYPEMEET_LITTLEMASK    0x0F //会议类型低位掩码 保存会议类型
#define TYPEMEET_BIGMASK       0xF0 //会议类型高位掩码 保存会议属性

#define type_meeting_autoclose 0x10 //自动结束
#define type_meeting_secret    0x20 //保密

#define type_meeting_normal    0x01 //一般会议
#define type_meeting_team      0x02 //党委会议
#define type_meeting_gov       0x03 //政务会议
#define type_meeting_law       0x04 //法务会议
#define type_meeting_company   0x04 //企业会议
#define type_meeting_school    0x05 //学校会议
#define type_meeting_org       0x06 //机构会议
#define type_meeting_free      0x07 //自由会议

//----------参会人员角色----------
#define role_member_nouser 		0x00  //未使用
#define role_member_normal 		0x01  //一般参会人员
#define role_member_compere 	0x03  //主持人
#define role_member_secretary 	0x04  //秘书
#define role_device_projector	0x08  //投影仪
#define role_admin				0x09  //管理员
#define role_root				0x10  //后台管理员
#define role_sever				0x11  //服务器程序
#define role_oa					0x12  //第三方系统
#define role_liexi				0x13  //列席人员
#define role_report				0x14  //汇报人员
#define role_record				0x15  //记录人员

typedef char  TYPE_MEETING; //type_meeting_normal
typedef char  ROLE_MEMBER;  //role_member_nouser

//白板操作类型
#define  FIGURETYPE_INK			1 //线条
#define  FIGURETYPE_LINE		2 //直线
#define  FIGURETYPE_ELLIPSE		3 //椭圆
#define  FIGURETYPE_RECTANGLE	4 //矩形
#define  FIGURETYPE_FREETEXT	5 //文字
#define  FIGURETYPE_PICTURE		6 //
#define  FIGURETYPE_ARROW		7 //箭头
typedef int8u WHITEBOAD_FIGURETYPE;

#define PROTOCAL_STARTID		1 //分帧数据包的起始帖序号

#define ID_SharedDir			1 //默认共享目录ID，不可更改或删除
#define ID_PostilDir			2 //默认批注目录ID，不可更改或删除

#define Count_FontConf			3 //字体颜色配置数量

//----------会议签到类型----------
typedef char TYPE_SIGNIN;			//签到类型
#define signin_direct	0x00		//类型--直接签到
#define signin_psw		0x01		//类型--个人密码签到
#define signin_photo	0x02		//类型--拍照(手写)签到
#define signin_onepsw	0x03		//类型--会议密码签到
#define signin_onepsw_photo	0x04	//类型--会议密码+拍照(手写)签到
#define signin_psw_photo	0x05	//类型--个人密码+拍照(手写)签到
#define signin_idcard				0x06	//类型--身份证签到
#define signin_finger				0x07	//类型--指纹识别签到
#define signin_face					0x08	//类型--人脸识别签到
#define signin_idcard_finger		0x09	//类型--身份证+指纹识别签到
#define signin_idcard_face			0x10	//类型--身份证+人脸识别签到
#define signin_finger_face			0x11	//类型--指纹识别+人脸识别签到
#define signin_idcard_finger_face	0x12	//类型--身份证+指纹识别+人脸识别签到
#define signin_ask					0x13	//请假
#define meet_signin_late			0x14	//迟到

//----------查询分页大小----------
#define PageSize_query	16

//FONT align 字体对齐
#define MEET_FONTFLAG_LEFT			0x0001 //左对齐
#define MEET_FONTFLAG_RIGHT			0x0002 //右对齐
#define MEET_FONTFLAG_HCENTER		0x0004 //水平对齐
#define MEET_FONTFLAG_TOP			0x0008 //上对齐
#define MEET_FONTFLAG_BOTTOM		0x0010 //下对齐
#define MEET_FONTFLAG_VCENTER		0x0020 //垂直对齐

//********************协议头*****************************
#define MEETPROTOL_FLAG_MASKRESERVE			0xFF000000 //保留内部使用
#define MEETPROTOL_FLAG_ENCRYPT				0x01000000 //加密传输
#define MEETPROTOL_FLAG_SM4ecbENCRYPT		0x02000000 //国标SM4 ecb加密传输
#define MEETPROTOL_FLAG_SM4cbcENCRYPT		0x04000000 //国标SM4 cbc加密传输
#define MEETPROTOL_FLAG_SM2ENCRYPT			0x08000000 //国标SM2加密传输

/*通讯协议请求头*/
typedef struct
{
	 unsigned int datalength;		  //数据长度
	 unsigned int version;			  //版本
	 unsigned int stage;			  //会议阶段 -- 
	 unsigned int fun;				  //功能

	 //int16u  method;
	 //int8u   fill;
	 //int8u   flag;
	 unsigned int method;			  //方法 --高16位留空，取高16位的高8位来当标志
	 unsigned int sessionid;		  //授权id
	 unsigned long long utctime;	  //发送时间
}PD_Requestheader;

/*通讯协议应答头*/
typedef struct
{
	 unsigned int datalength;		  //数据长度
	 unsigned int status;			  //请求处理状态
	 unsigned int stage;			  //会议阶段
	 unsigned int fun;				  //功能	

	 //int16u  method;
	 //int8u   fill;
	 //int8u   flag;
	 unsigned int method;			  //方法 --高16位留空，取高16位的高8位来当标志
	 unsigned int deviceid;			  //设备ID
	 unsigned long long utctime;	  //发送时间
}PD_Responseheader;


/*协议应答头后接该结构体,用来区别不同请求的回复*/
typedef struct
{
	unsigned int meetingid;	//会议id,当请求内容与会议无关时为0
	unsigned int fid_req;		//请求内容的附加id
	unsigned int id_req;		//请求内容的id		
}PD_ResponseStatus;

//************************细项结构体定义********************************

/*设备ID，设备名称*/
typedef struct
{
	unsigned int id;  //设备ID
	char name[NAME_LENG];
}PD_Device;

//flag
#define MEET_TABLECARDFLAG_SHOW			0x00000001 //该位用于表示该项是否可见
#define MEET_TABLECARDFLAG_BOLD			0x00000002 //加粗
#define MEET_TABLECARDFLAG_LEAN			0x00000004 //倾斜
#define MEET_TABLECARDFLAG_UNDERLINE	0x00000008 //下划线

/*字体与颜色*/
typedef struct
{
	/* 桌面字体大小计算方法
	fontsize * 当前屏幕高度 / 基准屏幕高度
	eg: 收到字体大小是100，当前的屏幕高度是720，推算出真实的字体大小为：100 * 720 / 1080 = 67
	*/
	int font;  //字体大小
	unsigned int Argb;  //说明：An ARGB quadruplet on the format #AARRGGBB
	float Lx;	//L左坐标 R右坐标
	float Ly;
	float Rx;
	float Ry;
	unsigned int    flag;	//属性值
	unsigned short  align;  //对齐
	char type;			//配置类型：conf_mtgname conf_memname conf_job conf_company  conf_position
	char fill;			
	char fontname[NAME_LENG];//字体名称 utf8

}PD_Color;


//人员信息有关组成
typedef struct 
{
	unsigned int id;		//人员ID;  //从1开始分配，0表示没有对应的ID信息，就没分配ID
	char name[NAME_LENG];  //名字，字符串信息
	char company[DESCRIBE_LENG]; //单位
	char job[DESCRIBE_LENG];		//职位
	char comment[DESCRIBE_LENG];		//描述
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱
	char password[PASSWORD_LENG];  //密码
}PD_PersonnelInfo;


//会议室信息组成
typedef struct 
{
	unsigned int id;		//会场ID
	char name[DESCRIBE_LENG];	//字符串信息
	char addr[DESCRIBE_LENG];   //会场地点
	char comment[DESCRIBE_LENG];   //备注
	unsigned int mapid;			//会场排位图id
	unsigned int fill;			//占位 未使用
}PD_FieldInfo;

typedef struct 
{
	long long utctime;//UTC时间
}PD_Time;

//会议名信息组成
typedef struct 
{
	unsigned int id;  //会议编号
	char name[DESCRIBE_LENG];  //会议名称
	unsigned int roomId; //会议室ID
	TYPE_MEETING type ;			 //会议类型
	PD_Time startTime;
	PD_Time endTime;
	TYPE_SIGNIN signin_type;			//签到类型
	unsigned int managerid;				//管理员id
	char meeting_psw[ONESIGNPASSWORD_LENG];				//会议签到密码 明文utf8
	unsigned int status;				//会议状态 MEETING_STATUS
	char ordername[NAME_LENG];  //会议预约人员名称
}PD_MeetingAllInfo;


typedef struct 
{
	 unsigned int id;  //会议ID
	char name[DESCRIBE_LENG];			//会议名称
	unsigned int roomId;				//会议室ID
	TYPE_MEETING type ;					//会议类型
	PD_Time startTime;					//开始时间
	PD_Time endTime;					//结束时间
	TYPE_SIGNIN signin_type;			//签到类型
	unsigned int managerid;				//管理员id
	char onepsw_signin[ONESIGNPASSWORD_LENG];			//会议密码签到密码 明文utf8
	char ordername[NAME_LENG];  //会议预约人员名称
}PD_MeetingOneInfo;

//会议名信息组成
typedef struct
{
	int32u id;				//会议编号
	int32u roomId;			//会议室ID
	int32u status;			//会议状态 MEETING_STATUS
	int32u managerid;		//管理员id
	PD_Time startTime;
	PD_Time endTime;
	char   meeting_psw[ONESIGNPASSWORD_LENG];				//会议签到密码 明文utf8
	char   ordername[NAME_LENG];							//会议预约人员名称
	int8u  type;			 //会议类型 TYPE_MEETING
	int8u  signin_type;		 //签到类型 TYPE_SIGNIN
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_MeetingAllInfo_V2;

typedef struct
{
	int32u id;				//会议编号
	int32u roomId;			//会议室ID
	int32u managerid;		//管理员id
	PD_Time startTime;
	PD_Time endTime;
	char   meeting_psw[ONESIGNPASSWORD_LENG];				//会议签到密码 明文utf8
	char   ordername[NAME_LENG];							//会议预约人员名称
	int8u  type;			 //会议类型 TYPE_MEETING
	int8u  signin_type;		 //签到类型 TYPE_SIGNIN
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_MeetingOneInfo_V2;


//appr 的取值
#define MEET_APPROVAL_IDLE		0 //未审批
#define MEET_APPROVAL_ASK		1 //请求审批
#define MEET_APPROVAL_ING		2 //审批中
#define MEET_APPROVAL_OK		3 //审批通过
#define MEET_APPROVAL_FAIL		4 //审批不通过

/*
{
"name":"会议名称",//会议名称
"appr":0,//审批状态
}
*/

#define DIR_FLAG_SHOWSTATUS 0x01 //该标志位表示隐藏属性 0=显示 1=表示隐藏
#define DIR_FLAG_VOTE		0x02 //该标志位表示设置投票属性 0=不绑定 1=表示绑定
#define DIR_FLAG_PASSWD		0x04 //该标志位表示设置密码属性 0=不绑定 1=表示绑定

//目录
typedef struct
{
	int32u id;
	int32u parentdirid;//父目录ID
	int32u dirpos;	//序号

	int16u namelen;			 //名称长度=字符串长度+结尾字节
	int8u  flag;//目录标志
	int8u  fill;
	int32u voteid;  //绑定问卷ID
	char   passwd[PD_SHORT_PASSWORD_LENG];//密码
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾

}PD_DirInfo_V2;

#define FILE_FLAG_SHOWSTATUS 0x01 //该标志位表示隐藏属性 0=显示 1=表示隐藏
#define FILE_FLAG_VOTE		 0x02 //该标志位表示设置投票属性 0=不绑定 1=表示绑定
//文件
typedef struct
{
	int32u id;
	int32u filepos;	//文件序号

	int32u uploaderid;	//上传者id
	char   uploader_name[NAME_LENG];
	int8u  uploader_role;	//上传者角色(文件为管理者上传时,uploaderid不填)
	int8u  flag;//目录文件标志

	int32u voteid;//关联投票ID -- 类型是问卷 flag |= FILE_FLAG_VOTE;
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾

}PD_FileInfo_V2;

//目录
typedef struct 
{
	unsigned int id;
	char name[FILENAME_LENG]; 
	unsigned int parentdirid;//父目录ID
	unsigned int dirpos;	//序号
}PD_DirInfo;

//文件
typedef struct 
{
	unsigned int id;
	char		 name[FILENAME_LENG]; //这里限制目录名称最大为150个字节
	unsigned int uploaderid;	//上传者id
	ROLE_MEMBER  uploader_role;	//上传者角色(文件为管理者上传时,uploaderid不填)
	char		 uploader_name[NAME_LENG];
	unsigned int filepos;	//文件序号
}PD_FileInfo;

//视频
typedef struct 
{
	unsigned int id;
	char name[DESCRIBE_LENG]; //视频名称
	char addr[DESCRIBE_LENG]; //视频地址
}PD_VideoStruct;

//排位信息
typedef struct 
{
	unsigned int nameId;  	//姓名ID  (设备为投影仪时为0)
	unsigned int seatId;	//座位编号    管理端分配位置编号，编号唯一对应座位描述
	ROLE_MEMBER role;  		//定义参会人员角色
}PD_Seat;

typedef struct 
{  
	unsigned int nameId;		//姓名ID
	PD_Time time;				//时间
	TYPE_SIGNIN signin_type;	//签到类型
	unsigned int length;		//后接数据长度
	//char signin_photo[];		//视频签到图片
}PD_SignInInfo;

typedef struct
{
	unsigned int nameId;		//姓名ID
	PD_Time time;				//时间
	TYPE_SIGNIN signin_type;	//签到类型
	char	signinpassword[PASSWORD_LENG];//签到密码	
	unsigned int length;		//后接数据长度
	//char signin_photo[];		//视频签到图片
}PD_AddSignInInfo;

//增加投票状态 votestate 的下发 code log by ct 20170408 10:00
#define vote_notvote 0 //未发起的投票
#define vote_voteing 1 //正在进行的投票
#define vote_endvote 2 //已经结束的投票

#define MAX_VOTEITEM_COUNT 6 //最大选项
typedef struct
{
	unsigned int voteid; //投票ID
	char content[VOTE_CONTENTLENG]; //投票内容 

	ProtocalData::VOTEMAIN_TYPE  maintype; //类别 投票 选举 问卷调查
	ProtocalData::VOTE_MODE mode; //匿名投票 记名投票
	ProtocalData::VOTE_TYPE type; //多选 单选

	unsigned int votestate;     //投票状态 
	unsigned int timeouts;     //计时结束 单位：秒

	unsigned int selectcount;     //有效选项数量
	char voteText[MAX_VOTEITEM_COUNT][VOTE_LENG];  //选择1描述文字

}PD_VoteStart;

typedef struct
{
	int32u voteid; //投票ID
	ProtocalData::VOTEMAIN_TYPE  maintype; //类别 投票 选举 问卷调查
	ProtocalData::VOTE_MODE mode; //匿名投票 记名投票
	ProtocalData::VOTE_TYPE type; //多选 单选

	int32u votestate;     //投票状态 
	int32u timeouts;     //计时结束 单位：秒

	int16u selectcount;     //有效选项数量
	int8u  fill[2];
	int32u jsonlen; //名称长度=字符串长度+结尾字节
	
	//char json[jsonlen]; //投票内容 //名称需要增加一个字节0作字符串结尾
	//{"content":"","item":["1","2"]}
}PD_VoteStartV3;

//发起投票
#define VOTING_FLAG_NOPOST		0x00000001 //不在投影机上显示投票结果
#define VOTING_FLAG_SECRETARY	0x00000002 //投票选项保密投票模式
#define VOTING_FLAG_FINISHEXIT	0x00000004 //全部提交完成后立即结束
#define VOTING_FLAG_REVOTE		0x00000008 //重投 清空之前的记录
#define VOTING_FLAG_BACKUP		0x00000010 //备份操作不需要显示投票

typedef struct
{
	int32u memberid;
}PD_VoteMemFlag;

typedef struct
{
	int32u voteid;
	int32u voteflag; //发起投票标志
	int32u timeouts; //计时结束 单位：秒
	int32u membernum;//参与投票的参会人员数
	//PD_VoteMemFlag members[];
}PD_VoteStartFlag;

//投票标志 select高8位
#define PD_VOTE_SELFLAG_MASK     0xff000000 //掩码
#define PD_VOTE_SELFLAG_CHECKIN  0x80000000 //该位为1表示已经签到

#define PD_VOTE_CHECKIN_BIT_INDEX 31 //签到标志位索引
typedef struct 
{  
	unsigned int voteid; //投票ID
	unsigned int selectcount; // 有效投票项数
	unsigned int select[MAX_VOTEITEM_COUNT];  //选择1状态 ,0没有选择，1选择
	unsigned int memberid;//提交的人员ID
}PD_VoteState;
typedef struct
{
	unsigned int voteid; //投票ID
	unsigned int selectcount; // 有效投票项数
	unsigned int select[MAX_VOTEITEM_COUNT];  //选择1状态 ,0没有选择，1选择
	unsigned int memberid;//提交的人员ID
	int16u namelen;			        //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_VoteState_V2;
typedef struct
{
	unsigned int voteid; //投票ID
	unsigned int memberid;//提交的人员ID

	int16u selectcount;     //有效选项数量
	int8u  fill[2];
	int32u jsonlen;//名称长度=字符串长度+结尾字节

	//int32u select[selectcount];  //选择1状态 ,0没有选择，1选择

	//char json[jsonlen]; //投票内容 //名称需要增加一个字节0作字符串结尾
	//{"mark":"","item":["1","2"]} 
}PD_VoteState_V3;

typedef struct 
{  
	unsigned int id;  //序号
	char name[FILENAME_LENG]; //名称
}PD_Postil;


typedef struct 
{
	unsigned int memid; //参会人员ID
	char name[NAME_LENG];  //人员名称
	char company[DESCRIBE_LENG]; //公司名称
}Pd_smpl_member;

//----------文件禁止权限状态----------
#define fileacce_reject   0x1
typedef char FILE_PERM;

//---------字体颜色配置类型-----------
#define conf_mtgname	0x01
#define conf_memname	0x02
#define conf_job		0x03
#define conf_company	0x04
#define conf_position	0x05

/*
新增的与会议信息相关结构体都不再有会议id字段;会议id字段为单独为一个结构体,用在其他会议相关结构体前
*/
//---------------会议ID--------------
typedef struct{
	unsigned int meetingid;
}PD_MeetingId;


/*
用于标注一级标识id
*/
typedef struct{
	unsigned int id;
}PD_SingleId;

/*
用于标注二级标识id
*/
typedef struct{
	unsigned int id1;
	unsigned int id2;
}PD_DoubleId;

/*
用于标注后面结构体的数量
*/
typedef struct{
	unsigned short blockcount;
}PD_BlockCount;


/*
适用于STAGE_MemberManage + FUN_MemberPermission + METHOD_Query/METHOD_Modify
请求PD_MeetingId + PD_BlockCount + PD_MemberPermission + ...
回复PD_MeetingId + PD_BlockCount + PD_MemberPermission + ...
*/
//--------------参会人权限------------
#define memperm_sscreen					0x00000001		//同屏权限
#define memperm_projective				0x00000002		//投影权限
#define memperm_upload					0x00000004		//上传权限
#define memperm_download				0x00000008		//下载权限
#define memperm_vote					0x00000010		//投票权限
#define memperm_postilview				0x00000020		//批注查看权限 -- 不保存到数据库
#define memperm_record					0x00000040		//录制权限 
#define memperm_lookvote				0x00000080		//投票查看权限 

typedef struct{
	unsigned int memberid;
	unsigned int permission;							//参会人权限
}PD_MemberPermission;


//*******************************************************
//**************************通讯部分*********************
//*******************************************************


//系统时间查询
//stages：STAGE_SysTime
//fun:FUN_One
//method:查询
typedef struct 
{  
	PD_Time time;
}PD_SysTime;

//开机时获取的整个系统设备编号信息
//stages：STAGE_DeviceId
//fun:ProtocalData::FUN_All
//method:查询
typedef struct 
{  
	unsigned int TotalNum ;	//总共有多少设备
	unsigned int StartId;	//当前帧开始序号  
	unsigned int  CurrNum;	//当前帧包括多少个设备序号
	//PD_Device		//设备内容
	//PD_Device		//设备内容
	//PD_Device		//设备内容
}PD_DeviceInfoAll;

//单元正常上电后告知服务器本设备的ID和名称
//stages：STAGE_DeviceId
//fun:FUN_One
//method:METHOD_Report
typedef struct 
{  
	PD_Device dev;			//设备内容
}PD_DeviceInfoReport;

//系统管理部分
//PD_AdminPassword logonflag
#define ADMIN_LOGONFLAG_COMMON 0x00000001 //常用人员手机模式

//管理员密码有关
//stages：STAGE_SystemSet
//fun:FUN_AdminPassword
//method:查询，修改
typedef struct 
{  
	char pass[PASSWORD_LENG];		//密码或旧密码
	char admin_user[NAME_LENG];		//管理员登录名
	char newpass[PASSWORD_LENG];	//修改密码时的新密码(登陆时不使用)
	int32u logonflag;//ADMIN_LOGONFLAG_COMMON 默认管理员模式
}PD_AdminPassword;

//发送密码校验正确时设备接收到的授权id
typedef struct
{
	unsigned int sessionid;	//授权id
}PD_LoginSessionId;


//终端升降控制
#define LIFT_FLAG_MACHICE		0x00000001
#define LIFT_FLAG_MIC			0x00000002
#define LIFT_FLAG_DESK			0x00000004
#define LIFT_FLAG_PRESIDENTMIC	0x00000008
#define LIFT_FLAG_PRESIDENT2MIC	0x00000010
#define LIFT_FLAG_MICMASK		0x0000001a

//stages：STAGE_SystemSet
//fun:FUN_TerminalControl
//method:METHOD_Set控制
typedef struct 
{  
	 unsigned int meetingid;
	 unsigned int devnum;  
	 ProtocalData::TerminalControl_ENUM control;  //控制内容
	 unsigned int val1;
	 unsigned int val2;
	// unsigned int devid[devnum];
}PD_TerminalControl;

#define URL_ADDR_MAXLEN 260
typedef struct
{
	unsigned int  id;	//urlid,用于标识修改删除操作
	char name[DESCRIBE_LENG];//网址别名字符串
	char addr[URL_ADDR_MAXLEN];//网址内容，字符串
}PD_URL_item;

#define MEETURL_FLAG_ISDEFAULT 0x01
//设置默认网址
//stages：STAGE_SystemSet
//fun:FUN_URL
//method:查询，设置
//客户端和服务端公用此数据结构
typedef struct 
{  
	unsigned int meetingid; //=0表示修改默认网址，所有会议在未设置网址时使用默认的
	int16u urlnum;
	int8u  flag;
	int8u  fill;
	//PD_URL_item addr[];
}Client_PD_URL;

//删除网址
//stages：STAGE_SystemSet
//fun:FUN_URL
//method:删除
//客户端和服务端公用此数据结构
typedef struct
{
	unsigned int meetingid; //=0表示删除默认网址，所有会议在未设置网址时使用默认的
	unsigned int urlnum;
	//int32u  urlid[];
}Client_PD_URL_Delete;

//设置字体大小，颜色
//stages：STAGE_SystemSet
//fun:FUN_FontColorBgp_desk
//method:查询，设置

typedef struct 
{  
	 unsigned int meeintid;
	PD_Color conf1[Count_FontConf];	//字体颜色配置
	unsigned int bg_photoid;		//桌牌背景图id
}PD__FontColor;

//参会人员
typedef struct
{
	unsigned int memberid;		//参会人员id
	char name[NAME_LENG];  //名字，字符串信息
	char company[DESCRIBE_LENG]; //单位
	char job[DESCRIBE_LENG];		//职位
	char comment[DESCRIBE_LENG];	//备注
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱
	char password[PASSWORD_LENG];  //密码
	int32u pos;//查询时返回pos位置，添加或者修改时不需要指定
}PD_Member_Edit;


#define IMPORT_MEET_FLAG_DELALLMEM 0x00000001 //导入清删除参会人员

//导入参会人员
//stages：STAGE_MemberManage
//fun: ProtocalData::FUN_All
//method:ADD
//客户端发送数据，服务端只返回PD_Responseheader应答部分
typedef struct
{
	int32u meetingid;
	int32u flag;//IMPORT_MEET_FLAG_DELALLMEM

	int32u jsonlen;
	/*
	{
	"member":  //
	[
	{
	"name::"陈工",
	"company":"xx",
	"job":"xx",
	"phone":"123456",
	"email":"",
	"comment":"",
	"password":"123456",
	"memid":"",
	"devid":"",
	"perm":"",
	"role":"",
	}
	]
	}
	*/

}PD_ImportMemberManage;

#define MEETSMS_OPERTEMP_START		0//=会议通知
#define MEETSMS_OPERTEMP_LATE		1//=会议延期
#define MEETSMS_OPERTEMP_CANCLE		2//=会议取消
#define MEETSMS_OPERTEMP_NORMAL		3//=普通文本
#define MEETSMS_OPERTEMP_APPROVAL	4//=审批催促

//短信通知会议
//stages：STAGE_MemberManage
//fun: FUN_All 会议短信通知
//method:METHOD_Notify
typedef struct
{
	int32u   MeetingId;  //会议ID
	int      templateindex;//=0会议通知, =1会议延期, =2会议取消 需要指定模板，后台会根据模板去发送
	int		 contentlen;
	//char   msg[];
}PD_MeetSMSNotify;

//参会人员管理
//stages：STAGE_MemberManage
//fun: ProtocalData::FUN_All
//method:查询 修改
//客户端发送数据，服务端只返回PD_Responseheader应答部分
typedef struct 
{  
	unsigned int meetingid;
	unsigned int TotalNum;//总共有多少个会议
	unsigned int StartId; //当前帧开始序号
	unsigned int CurrNum;//当前帧包括多少个会议信息

	//填充PerNUM个参会人员
	//PD_Member_Edit
}PD_OneMemberManage;

//参会人员  修改参会人员序号排序
//stages：STAGE_MemberManage
//fun: ProtocalData::FUN_All
//method:设置 删除(删除多个)
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int num;//参会人员
	//unsigned int memberid[];//按ID的排序
}PD_MeetingMemberPosSet;

//会场管理部分
//stages：STAGE_MeetingManage
//fun: ProtocalData::FUN_All, 所有会场信息
//method:查询
//客户端只发送PD_Requestheader，服务端应答内容
//会场名称是唯一的，不能重复
typedef struct 
{  
	unsigned int count ;//总共有多少个会场
	//PD_FieldInfo ; //根据实际人员填写PD_FieldInfo结构图信息
	//PD_FieldInfo ;
	
}PD_AllMeetingField;

//会场管理部分
//stages：STAGE_MeetingManage
//fun: FUN_One 单个会场信息
//method:修改
//客户端发送数据，服务端返回
//会场名称是唯一的，不能重复
typedef struct 
{  
	unsigned int MeetingroomId;		//会场ID
	char name[DESCRIBE_LENG];  //字符串信息
	char addr[DESCRIBE_LENG];  //会场地点
	char comment[DESCRIBE_LENG];  //备注
	unsigned int picid;		   //背景图id
}PD_OneMeetingField_Mod;

//会场管理部分
//stages：STAGE_MeetingManage
//fun: FUN_One 单个会场信息
//method:删除
//客户端发送数据，服务端返回
//会场名称是唯一的，不能重复
typedef struct 
{  
	unsigned int MeetingroomId;	//会场ID
}PD_OneMeetingField_Del;

typedef struct
{
	unsigned int MeetingroomId;	//会场ID
	unsigned int adminid;	//用户ID
}PD_AdminRoomItemInfo;

//用户可控的会场 查询会场时会返回，不需要主动查询
//stages：STAGE_MeetingManage
//fun: FUN_All
//method:METHOD_QueryData
typedef struct
{
	int Num;//
	//PD_MGRROOM [Num];
}PD_AdminRoomInfo;

//会议管理部分
//stages：STAGE_StartUpMeeting
//fun: ProtocalData::FUN_All, 所有会场信息
//method:查询
//客户端只发送PD_Requestheader，服务端应答内容
//会议名称是唯一，不能重复
typedef struct 
{  
	unsigned int TotalNum ;//总共有多少个会议
	unsigned int StartId; //当前帧开始序号
	unsigned int CurrNum ;//当前帧包括多少个会议信息
	//PD_MeetingAllInfo ; //根据实际会议填写PD_MeetingAllInfo结构图信息
	
}PD_AllMeetingManage;

typedef struct
{
	unsigned int TotalNum;//总共有多少个会议
	unsigned int StartId; //当前帧开始序号
	unsigned int CurrNum;//当前帧包括多少个会议信息
	//PD_MeetingAllInfo_V2 ; //根据实际会议填写PD_MeetingAllInfo结构图信息

}PD_AllMeetingManage_V2;

//会议管理部分  添加，删除会议
//stages：STAGE_StartUpMeeting
//fun: FUN_One 单个会场信息
//method:删除，修改，复制
//添加方法，客户端发送数据时没有对应ID，服务端返回数据时分配好ID号
//修改，删除方法以会议ID为准
//会议名称是唯一，不能重复
typedef struct 
{  
	PD_MeetingOneInfo meeting;
}PD_OneMeetingManage;
typedef struct
{
	PD_MeetingAllInfo_V2 meeting;
}PD_OneMeetingManage_V2;

//会议管理部分  添加，删除会议
//stages：STAGE_StartUpMeeting
//fun: FUN_One 单个会场信息
//method:添加，复制
typedef struct
{
	PD_MeetingOneInfo meeting;
	unsigned int status;//会议状态 MEETING_STATUS
}PD_OneMeetingManageEx;

typedef struct
{
	PD_MeetingOneInfo_V2 meeting;
	unsigned int status;//会议状态 MEETING_STATUS
}PD_OneMeetingManageEx_V2;

//complex query meet flag
#define COMPLEXQUERY_FLAG_MEETINGID 0x00000001 //meetingid有效 该标志有效会无视其它标志
#define COMPLEXQUERY_FLAG_STATUS    0x00000002 //status有效
#define COMPLEXQUERY_FLAG_TIME	    0x00000004 //starttime endtime有效
#define COMPLEXQUERY_FLAG_ROOMID	0x00000008 //roomid有效 
#define COMPLEXQUERY_FLAG_CACHE	    0x00000010 //缓存会议的相关资料 只有与COMPLEXQUERY_FLAG_MEETINGID才有效
#define COMPLEXQUERY_FLAG_PHONE		0x00000020 //常用人员电话 

//complex query cache flag 按如下的顺序返回
#define COMPLEXCACHE_FLAG_MEETFUNCTION			0x0000000000000001 //发送会议功能
#define COMPLEXCACHE_FLAG_MEETAGENDA			0x0000000000000002 //发送会议议程
#define COMPLEXCACHE_FLAG_MEETBULLET			0x0000000000000004 //发送会议公告
#define COMPLEXCACHE_FLAG_MEMBER				0x0000000000000008 //发送参会人员
#define COMPLEXCACHE_FLAG_MEMBERPERMISSION		0x0000000000000010 //发送参会人员权限
#define COMPLEXCACHE_FLAG_MEMBERSEAT			0x0000000000000020 //发送会议排位
#define COMPLEXCACHE_FLAG_MEETVIDEO				0x0000000000000040 //发送会议视频直播
#define COMPLEXCACHE_FLAG_MEETVOTE				0x0000000000000080 //发送会议投票
#define COMPLEXCACHE_FLAG_DIRFILE				0x0000000000000100 //发送会议目录和目录文件
#define COMPLEXCACHE_FLAG_ROOMDEVICE			0x0000000000000200 //发送会场与会场设备
#define COMPLEXCACHE_FLAG_MEETSIGN				0x0000000000000400 //发送会议签到
#define COMPLEXCACHE_FLAG_TABLECARD				0x0000000000000800 //发送会议桌牌

//会议管理部分  复合查询会议
//stages：STAGE_StartUpMeeting
//fun: FUN_One 单个会议信息
//method:查询
typedef struct
{
	int32u		 queryflag;	//查询标志位 COMPLEXQUERY_FLAG_MEETINGID
	int32u		 meetingid;//会议ID
	int64u		 cacheflag;//缓存标志 COMPLEXCACHE_FLAG_DIRFILE
	int32u		 roomid;//会场ID
	int32u	     status;	//会议状态 MEETING_STATUS
	PD_Time		 starttime;//开始时间 单位：UTC秒
	PD_Time		 endtime;//结束时间 单位：UTC秒
	char		 phone[SHORT_DESCRIBE_LENG];  //常用人员电话
}PD_ComplexQueryMeetingManage;

//agendatype
#define MEET_AGENDA_TYPE_TEXT  0 //文本
#define MEET_AGENDA_TYPE_FILE  1 //文件
#define MEET_AGENDA_TYPE_TIME  2 //时间轴式
#define MEET_AGENDA_TYPE_NOSET 0xff000000 //查询议程时可指定该标志表示查询当前会议使用的议程

//会议议程 
//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:查询，修改
//MEET_AGENDA_TYPE_TEXT 、MEET_AGENDA_TYPE_FILE
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int type;//类型 MEET_AGENDATYPE_NOSET
	unsigned int meetagendatype;//会议使用的议程类型

	//如果是文本,刚表示文本长度
	//如果是文件则表示媒体ID
	unsigned int val;
	//char text[]; //内容根据前面长度不定长
}PD_MeetingAgenda;

//时间轴式会议议程
#define MEETAGENDA_STATUS_IDLE		0 //未发起
#define MEETAGENDA_STATUS_RUNNING   1 //进行中
#define MEETAGENDA_STATUS_END		2 //已结束

#define MEETAGENDA_DESCTEXT_LENG    320

//时间轴议程的交换
//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:METHOD_Report
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u agendaid1;  //议程ID
	int32u agendaid2;  //议程ID
}PD_MeetingAgendaTimePos;

//时间轴议程的
//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:METHOD_Control
typedef struct
{
	int32u MeetingId;  //会议ID
	int    num;
	//int32u agendaid[num];  //议程ID
}PD_MeetingAgendaTimeSavePos;

#define AGENDA_AUTOADDBINDDIR 0xff000000 //自动添加目录
typedef struct
{
	unsigned int  agendaid;  //议程ID
	unsigned int  status;  //议程状态
	unsigned int  dirid;  //绑定目录ID AGENDA_AUTOADDBINDDIR
	int64u		  startutctime;//单位秒
	int64u	      endutctime;  //单位秒
	char desctext[MEETAGENDA_DESCTEXT_LENG]; //描述内容
}PD_AgendaTimeInfo;

#define AGENDA_FLAG_SHOWSTATUS   0x0001 //该标志位表示隐藏属性 0=显示 1=表示隐藏
#define AGENDA_FLAG_VOTE		 0x0002 //该标志位表示设置投票属性 0=不绑定 1=表示绑定
#define AGENDA_FLAG_PASSWD		 0x0004 //该标志位表示设置密码属性 0=不绑定 1=表示绑定
typedef struct
{
	int32u  agendaid;  //议程ID
	int32u  status;  //议程状态
	int32u  dirid;  //绑定目录ID AGENDA_AUTOADDBINDDIR
	int64u	startutctime;//单位秒
	int64u	endutctime;  //单位秒
	char    desctext[MEETAGENDA_DESCTEXT_LENG]; //描述内容
	int32u  voteid;  //绑定问卷ID
	int16u  flag;  //关联的标志
	int8u   fill[2];
	char    passwd[PD_SHORT_PASSWORD_LENG];//密码
}PD_AgendaTimeInfo_V2;

typedef struct
{
	int32u  agendaid;  //议程ID
	int32u  status;  //议程状态
	int32u  dirid;  //绑定目录ID AGENDA_AUTOADDBINDDIR
	int64u	startutctime;//单位秒
	int64u	endutctime;  //单位秒
	int32u  pos;//暂不使用
	int32u  voteid;  //绑定问卷ID
	int16u  flag;  //关联的标志
	int8u   fill[2];
	char    passwd[PD_SHORT_PASSWORD_LENG];//密码
	int32u  jsonlen;//
	//char   json[jsonlen + 1];//{"name":""}
}PD_AgendaTimeInfo_V3;

//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:查询
//MEET_AGENDA_TYPE_TIME
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int type;//类型 MEET_AGENDATYPE_NOSET
	unsigned int meetagendatype;//会议使用的议程类型
	unsigned int TotalNum;//总共有多少个议程
	unsigned int StartId; //当前帧开始议程序号
	unsigned int CurrNum;//当前帧包括多少个议程信息
	//PD_AgendaTimeInfo; //根据前面总数列写PD_AgendaTimeInfo

}PD_MeetingAgendaTimeEx;

//时间轴式会议议程
//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:添加，修改，删除
typedef struct
{
	unsigned int  MeetingId;  //会议ID
	unsigned int  type;//类型
	PD_AgendaTimeInfo item;
	unsigned int meetagendatype;//会议使用的议程类型
}PD_MeetingAgendaTime;

//时间轴式会议议程
//stages：STAGE_MeetingAgenda
//fun: FUN_Agenda 会议议程
//method:添加，修改，删除
typedef struct
{
	unsigned int  MeetingId;  //会议ID
	unsigned int  type;//类型
	unsigned int  meetagendatype;//会议使用的议程类型
	PD_AgendaTimeInfo_V2 item;
}PD_MeetingAgendaTime_V2;

typedef struct
{
	unsigned int  MeetingId;  //会议ID
	unsigned int  type;//类型
	unsigned int  meetagendatype;//会议使用的议程类型
	PD_AgendaTimeInfo_V3 item;
}PD_MeetingAgendaTime_V3;


//会议大白板公告 
//stages：STAGE_MeetingAgenda
//fun: FUN_One //会议公告
//method:查询，修改
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int textlen;
	//char text[]; //内容根据前面长度不定长
}PD_MeetingBigBulletText;

//会议公告
#define MEETBULLET_TEXT_LENG 320
#define PD_BULLET_DIRECTSTART 0xff000000
typedef struct
{
	unsigned int bulletid;  //公告ID 发起公告是可以指定PD_BULLET_DIRECTSTART表示添加完成立即发起
	unsigned int type;//公告类型
	unsigned int starttime;//公告自动发起的时间,按会议开始时间开始 单位秒
	unsigned int timeouts;//公告超时关闭的时间 按公告发起时间开始 单位秒
	char title[DESCRIBE_LENG];//标题
	char content[MEETBULLET_TEXT_LENG]; //内容
}PD_BulletInfo;

//stages：STAGE_MeetingAgenda
//fun: FUN_Bulletin, //会议公告
//method:查询
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum;//总共有多少个公告
	unsigned int StartId; //当前帧开始公告序号
	unsigned int CurrNum;//当前帧包括多少个公告信息
	//PD_BulletInfo; //根据前面总数列写PD_BulletInfo

}PD_MeetingBulletinEx;

//会议公告
//stages：STAGE_MeetingAgenda
//fun: FUN_Bulletin, //会议公告
//method:添加，修改，删除
typedef struct 
{  
	unsigned int  MeetingId;  //会议ID
	PD_BulletInfo item;
}PD_MeetingBulletin;

//发布会议公告
//stages：STAGE_MeetingAgenda
//fun: FUN_Bulletin, //会议公告
//method:通知
typedef struct
{
	unsigned int  MeetingId;  //会议ID
	PD_BulletInfo item;
	int32u operdeviceid;//发起设备
	int32u opermember;//发起人员ID
	int devnum;
}PD_PublishMeetingBulletin;

#ifndef MEET_FACEID_MAINBG
//会议界面设置
//faceid
#define MEET_FACEID_MAINBG			1  //主界面 png
#define MEET_FACEID_SUBBG			2  //子界面 png
#define MEET_FACEID_LOGO			3  //logo png
#define MEET_FACEID_MEETNAME		4  //会议名称 text
#define MEET_FACEID_MEMBERNAME		5  //参会人名称 text
#define MEET_FACEID_MEMBERCOMPANY	6  //参会人单位 text
#define MEET_FACEID_MEMBERJOB		7  //参会人职业 text
#define MEET_FACEID_SEATNAME		8  //座席名称 text
#define MEET_FACEID_TIMER			9  //日期时间 text
#define MEET_FACEID_COMPANY			10 //单位名称 text
#define MEET_FACEID_SHOWFILE		11 //开会预读文件、视频等
#define MEET_FACEID_COLTDTEXT		12 //公司名称 onytext
#define MEET_FACE_LOGO_GEO		13 //text
#define MEET_FACE_topstatus_GEO		14 //会议状态 text 
#define MEET_FACE_checkin_GEO		15 //进入会议按钮 text
#define MEET_FACE_manage_GEO		16 //进入后台 text
#define MEET_FACE_remark_GEO		17 //备注 text
#define MEET_FACE_role_GEO		18 //角色 text
#define MEET_FACE_ver_GEO		19 //版本 text

#define MEET_FACE_SeatIcoShow_GEO		20 //排位图标是否显示 text

#define MEET_FACE_SeatLayoutShow_top		21 //桌牌显示上  text
#define MEET_FACE_SeatLayoutShow_main		22 //桌牌显示中  text
#define MEET_FACE_SeatLayoutShow_bottom		23 //桌牌显示下  text

#define  MEET_FACE_MEETAGENDAFont         24

#define MEET_FACE_BulletinBK        25  // 会议公告背景  png （仅ID）
#define MEET_FACE_BulletinLogo      26  // 会议公告LOGO  png （仅ID）
#define MEET_FACE_BulletinTitle     27  // 会议公告标题  text
#define MEET_FACE_BulletinContent   28  // 会议公告内容  text
#define MEET_FACE_BulletinBtn       29  // 公告关闭按钮  text

#define MEET_FACEID_PROJECTIVE_MIANBG		101  //投影界面 png
#define MEET_FACEID_PROJECTIVE_LOGO			102  //logo png
#define MEET_FACEID_PROJECTIVE_MEETNAME		103  //会议名称 text
#define MEET_FACEID_PROJECTIVE_SEATNAME		104  //座席名称 text
#define MEET_FACEID_PROJECTIVE_TIMER		105  //日期时间 text
#define MEET_FACEID_PROJECTIVE_COMPANY		106  //单位名称 text
#define MEET_FACEID_PROJECTIVE_SIGNINFO		107  //签到情况 text
#define MEET_FACEID_PROJECTIVE_MEETTIME		108  //会议时间 text
#define MEET_FACEID_PROJECTIVE_COMPANYNAME	109  //公司名称位置 text
#define MEET_FACEID_PROJECTIVE_STATUS		110  //会议状态 text
#define MEET_FACEID_PROJECTIVE_SHOWFILE		111  //开会预读文件、视频等  //仅ID
#define MEET_FACEID_PROJECTIVE_COLTDTEXT	112  //公司名称 onytext

#define MEET_FACEID_PROJECTIVE_SIGN_ALL     113  // 签到：应到
#define MEET_FACEID_PROJECTIVE_SIGN_IN      114  // 签到：已到
#define MEET_FACEID_PROJECTIVE_SIGN_OUT     115  // 签到：未到
#define MEET_FACEID_PROJECTIVE_DATE         116  // 投影显示开会(当天)日期,格式：2022年1月2日

#define MEET_FACEID_PROJECTIVE_CUSTOMTITLE1 117  //投影自定义标题
#define MEET_FACEID_PROJECTIVE_AgendaInfo   118  //议题
//fontflag
#define MEET_FONTFLAG_BOLD		0x00000001 //加粗
#define MEET_FONTFLAG_LEAN		0x00000002 //倾斜
#define MEET_FONTFLAG_UNDERLINE 0x00000004 //下划线

//flag
#define MEET_FACEFLAG_SHOW			0x00000001 //该位用于表示该项是否可见
#define MEET_FACEFLAG_TEXT			0x00000002 //该位用于表示数据是文本类型,否则为文件ID
#define MEET_FACEFLAG_ONLYTEXT		0x00000004 //该位用于表示数据是纯文本类型
#endif

//文本项
typedef struct
{
	unsigned int faceid;  //界面项ID
	unsigned int flag;	  //属性值

	unsigned short  fontsize;	//字体大小
	unsigned int    color;		//字体rgba颜色
	unsigned short  align;		//对齐
	unsigned short  fontflag;	//字体属性
	char			fontname[NAME_LENG];//字体名称
	
	float lx;//坐标 左上角x  (x * 100 / width)
	float ly;//坐标 左上角y  (y * 100 / height)
	float bx;//坐标 右下角x
	float by;//坐标 右下角y
	
}PD_FaceTextItemInfo;

//图片项
typedef struct
{
	unsigned int faceid;  //界面项ID
	unsigned int flag;    //属性值
	unsigned int mediaid; //项值
}PD_FacePictureItemInfo;

//纯文本项
typedef struct
{
	unsigned int faceid;  //界面项ID
	unsigned int flag;    //属性值
	char		 text[DESCRIBE_LENG];//文本
}PD_FaceOnlyTextItemInfo;

//stages：STAGE_MeetingFace
//fun: FUN_One
//method:查询，修改
typedef struct
{
	unsigned int getall;  //为真表示获取全部,否则获取设备所属类型的项
	unsigned int TotalNum;//总共有多少个公告
	unsigned int StartId; //当前帧开始公告序号
	unsigned int CurrNum; //当前帧包括多少个公告信息
	//PD_FaceTextItemInfo PD_FacePictureItemInfo; //根据前面总数列写PD_FaceTextItemInfo PD_FacePictureItemInfo交错
}PD_MeetingFaceEx;

#define MEETDIR_NEEDFILE 0xff000000

//会议资料 目录查询
//stages：STAGE_MeetingDatum
//fun: FUN_Dir_All, 全部目录
//method:查询
//一个会议的资料目录名称唯一，不能有重复
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum ;//总共有多少个目录
	unsigned int StartId; //当前帧开始目录序号
	unsigned int  CurrNum ;//当前帧包括多少个目录信息
	//PD_DirInfo; //根据前面总数列写PD_DirInfo
}PD_MeetingDirAll;
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum;//总共有多少个目录
	unsigned int StartId; //当前帧开始目录序号
	unsigned int  CurrNum;//当前帧包括多少个目录信息
	//PD_DirInfo_V2; //根据前面总数列写PD_DirInfo_V2
}PD_MeetingDirAll_V2;

//会议资料  目录增加，修改，删除
//stages：STAGE_MeetingDatum
//fun: FUN_Dir_One, 单个目录
//method:增加，删除
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_DirInfo Dir; 
}PD_MeetingDirOne;
typedef struct
{
	unsigned int MeetingId;  //会议ID
	PD_DirInfo_V2 Dir;
}PD_MeetingDirOne_V2;

//会议资料  目录重命名
//stages：STAGE_MeetingDatum
//fun: FUN_Dir_One, 单个目录
//method:修改
typedef struct
{
	unsigned int MeetingId;  //会议ID
	PD_DirInfo oldDir; //目录原来的名称
	PD_DirInfo newDir; //目录现有名称
}PD_MeetingDirChange;

//会议文件 
//stages：STAGE_MeetingDatum
//fun: FUN_File_All, 全部文件
//method:查询

//同一个目录下面的文件名称唯一，不能重复
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_DirInfo dir;	//目录
	unsigned int TotalNum ;//总共有多少个文件
	unsigned int StartId; //当前帧开始文件序号
	unsigned int  CurrNum ;//当前帧包括多少个文件信息
	//PD_FileInfo; //根据前面总数列写PD_FileInfo
}PD_MeetingFileAll;

typedef struct
{
	int32u MeetingId;  //会议ID
	int32u dirid; //目录ID
	int32u TotalNum;//总共有多少个文件
	int32u StartId; //当前帧开始文件序号
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_FileInfo_V2; //根据前面总数列写PD_FileInfo_V2
}PD_MeetingFileAll_V2;

//会议文件  增加，删除
//stages：STAGE_MeetingDatum
//fun: FUN_File_One, 单个文件
//method:增加，删除
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_DirInfo dir;	//目录
	PD_FileInfo file;
}PD_MeetingFileOne;

typedef struct
{
	int32u MeetingId;  //会议ID
	int32u dirid;	//目录id
	PD_FileInfo_V2 file;
}PD_MeetingFileOne_V2;

//会议文件  修改
//stages：STAGE_MeetingDatum
//fun: FUN_File_One, 单个文件
//method:修改
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_DirInfo dir;	//目录
	PD_FileInfo oldFile; //
	PD_FileInfo newFile; 
}PD_MeetingFileChange;

///start FileScore
typedef struct
{
	int32u fileid;  //文件ID
	int32u memberid;//参会人员ID
	int32u score;//评分
	int64u scoretime;//评分utc时间 微秒
}PD_Item_FileScore;

//会议文件评分  修改
//stages：STAGE_FileScore
//fun: FUN_One, 单个
//method:添加、修改、删除、查询(数据库返回)
typedef struct
{
	int32u meetingid;  //会议ID
	int32u TotalNum;//总共有多少个文件
	int32u StartId; //当前帧开始文件序号
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_Item_FileScore; //根据前面总数列写PD_Item_FileScore
}PD_MeetingFileScore;

//会议文件评分  查询
//stages：STAGE_FileScore
//fun: FUN_One, 单个
//method:查询、查询结果
typedef struct
{
	int32u meetingid;//会议ID
	int32u fileid;   //文件ID
}PD_QueryFileScore;

//会议文件评分  返回查询结果
//stages：STAGE_FileScore
//fun: FUN_One, 单个
//method:查询结果
typedef struct
{
	int32u meetingid;//会议ID
	int32u fileid;   //文件ID
	int32u score;	 //平均评分
}PD_QueryAverageFileScore;
///end FileScore

#define MAX_EVALUATETEXTLEN 260 //评价的文本最大长度
//evaluate flag
#define FILEEVALUATE_FLAG_SECRETARY 0x00000001 //该标志为1表示评价对外不可见

///start Fileevaluate
typedef struct
{
	int32u fileid;  //文件ID
	int32u memberid;//参会人员ID
	int32u flag;//标志 参见evaluate flag 宏定义
	int64u evaluatetime;//评分utc时间 微秒
	char   evaluate[MAX_EVALUATETEXTLEN];//评分的文本
}PD_Item_FileEvaluate;

//会议文件评价 
//stages：STAGE_FileEvaluate
//fun: FUN_One, 单个
//method:添加、查询(数据库返回)
typedef struct
{
	int32u meetingid;  //会议ID
	int32u totalrecord;//符合条件的总记录条数
	int32u startrow;//查询返回用户传过来的开始行
	int32u TotalNum;//总共有多少个文件
	int32u StartId; //当前帧开始文件序号
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_Item_FileEvaluate; //根据前面总数列写PD_Item_FileScore
}PD_MeetingFileEvaluate;

//会议文件评价  查询
//stages：STAGE_FileEvaluate
//fun: FUN_One, 单个
//method:查询
typedef struct
{
	int32u meetingid;//会议ID 必须有效
	int32u fileid;   //文件ID 可以为0表示所有文件
	int32u memberid;//参会人员ID 可以为0表示所有参会人
	int64u startevaluatetime;//查询的起始评分utc时间 微秒 必须有效
	int64u endevaluatetime;//查询的结束评分utc时间 微秒 必须有效
	int32u startrow;//查询开始行 实现分页查询 必须有效
}PD_QueryFileEvaluate;

//会议文件评价  删除
//stages：STAGE_FileEvaluate
//fun: FUN_One, 单个
//method:删除
typedef struct
{
	int32u meetingid;//会议ID 必须有效
	int32u fileid;   //文件ID 可以为0表示所有文件
	int32u memberid;//参会人员ID 可以为0表示所有参会人
	int64u evaluatetime;//评分utc时间 微秒 当fileid、memberid有效时 可以有效表示删除指定的评论
}PD_DelFileEvaluate;
///end Fileevaluate

//////////////////////////////////////////////////////////////////////////
///start Meetevaluate
typedef struct
{
	int32u memberid;//参会人员ID
	int32u flag;//标志 参见file evaluate flag 宏定义
	int64u evaluatetime;//评分utc时间 微秒
	char   evaluate[MAX_EVALUATETEXTLEN];//评分的文本
}PD_Item_MeetEvaluate;

//会议文件评价 
//stages：STAGE_MeetEvaluate
//fun: FUN_One, 单个
//method:添加、查询(数据库返回)
typedef struct
{
	int32u meetingid;  //会议ID
	int32u totalrecord;//符合条件的总记录条数
	int32u startrow;//查询返回用户传过来的开始行
	int32u TotalNum;//总共有多少个文件
	int32u StartId; //当前帧开始文件序号
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_Item_MeetEvaluate; //根据前面总数列写PD_Item_FileScore
}PD_MeetingMeetEvaluate;

//会议文件评价  查询
//stages：STAGE_MeetEvaluate
//fun: FUN_One, 单个
//method:查询
typedef struct
{
	int32u meetingid;//会议ID 必须有效
	int32u memberid;//参会人员ID 可以为0表示所有参会人
	int64u startevaluatetime;//查询的起始评分utc时间 微秒 必须有效
	int64u endevaluatetime;//查询的结束评分utc时间 微秒 必须有效
	int32u startrow;//查询开始行 实现分页查询 必须有效
}PD_QueryMeetEvaluate;

//会议文件评价  删除
//stages：STAGE_FileEvaluate
//fun: FUN_One, 单个
//method:删除
typedef struct
{
	int32u meetingid;//会议ID 必须有效
	int32u memberid;//参会人员ID 可以为0表示所有参会人
	int64u evaluatetime;//评分utc时间 微秒 当memberid有效时 可以有效表示删除指定的评论
}PD_DelMeetEvaluate;
///end Meetevaluate

//////////////////////////////////////////////////////////////////////////
///start systemlog
#define MAX_LOGPARAMETER_NUM 4
typedef struct
{
	int32u pageid;//界面id  参见systemlogoperid.h SYSTEMLOG_PAGEID
	int32u operid;//操作类别  参见systemlogoperid.h  SYSTEMLOG_OPERID
	int32u meetid;//操作的会议ID
	int32u roomid;//操作的会场ID
	int32u deviceid;//操作的设备ID
	int32u urole;//参见role_admin
	int32u uid;//人员ID

	int64u opertime;//操作utc时间 微秒

	int32u param[MAX_LOGPARAMETER_NUM];//根据操作对应的操作参数，用于快速统计，eg:播放的文件ID，播放的流设备ID,
}PD_Item_MeetSystemLog;


//会议文件评价 
//stages：STAGE_SystemLog
//fun: FUN_One, 单个
//method:添加、查询(数据库返回)
typedef struct
{
	int32u totalrecord;//本次查询总记录数
	int32u startrow;//查询返回用户传过来的开始行
	int32u TotalNum;//总共有多少个文件
	int32u StartId; //当前帧开始文件序号
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_Item_MeetSystemLog; //根据前面总数列写PD_Item_MeetSystemLog
}PD_MeetingMeetSystemLog;

//会议文件评价  查询
//stages：STAGE_SystemLog
//fun: FUN_One, 单个
//method:查询
typedef struct
{
	int32u pageid;//界面id  参见systemlogoperid.h SYSTEMLOG_PAGEID
	int32u operid;//操作方法 参见systemlogoperid.h  SYSTEMLOG_OPERID 为0表示不作为查询条件
	int32u meetid;//操作的会议ID 为0表示不作为查询条件
	int32u roomid;//操作的会场ID 为0表示不作为查询条件
	int32u deviceid;//操作的设备ID 为0表示不作为查询条件
	int32u urole;//参见role_admin 为0表示不作为查询条件
	int32u uid;//人员ID 为0表示不作为查询条件

	int32u param[MAX_LOGPARAMETER_NUM];//根据操作对应的操作参数  为0表示不作为查询条件

	int64u startopertime;//查询的起始记录utc时间 微秒 必须有效
	int64u endopertime;//查询的结束记录utc时间 微秒 必须有效
	int32u startrow;//查询开始行 实现分页查询 必须有效
}PD_QueryMeetSystemLog;
///end systemlog

//////////////////////////////////////////////////////////////////////////
//会议文件  修改文件序号排序
//stages：STAGE_MeetingDatum
//fun: FUN_File_All, 所有文件
//method:设置
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int dirid;//目录ID
	unsigned int filenum;//文件数
	//unsigned int fileid[];//按ID的排序
}PD_MeetingFilePosSet;

typedef struct
{
	unsigned int dirid;  //会议目录
	unsigned int pos;    //序号
}PD_MeetingDirPosItem;

//会议目录  修改目录号排序
//stages：STAGE_MeetingDatum
//fun: FUN_Dir_All, 所有目录
//method:设置
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int num;    //目录数
	//PD_MeetingDirPosItem item[];//
}PD_MeetingDirPosSet;

//视频会议
//stages：STAGE_MeetingVideo
//fun: ProtocalData::FUN_All, 全部视频文件
//method:查询
//一个会议的视频文件名称唯一，不能重复
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum ;//总共有多少个文件
	unsigned int StartId;  //当前开始序号
	unsigned int CurrNum;  //当前帧包括多少个会议信息
	//PD_VideoStruct; //根据前面总数列写PD_FileInfont 
}PD_MeetingVideoAll;

//视频会议
//stages：STAGE_MeetingVideo
//fun: FUN_One, 单个视频文件
//method:增加，删除
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_VideoStruct video; //根据前面总数列写PD_FileInfont 
}PD_MeetingVideoOne;


//会议排位
//stages：STAGE_MeetingSeat
//fun: ProtocalData::FUN_All, 全部排位情况
//method:查询,批量修改
//
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum ;//总共有多少个座位编号
	unsigned int StartId;  //当前开始序号
	unsigned int CurrNum;  //当前帧包括多少个座位编号信息
	//PD_Seat; //根据前面总数列写PD_Seat 
}PD_MeetingSeatAll;


//会议排位
//stages：STAGE_MeetingSeat
//fun: FUN_One, 
//method:增加，删除,修改
//
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int roomid;//会场ID
	PD_Seat seat; //PD_Seat 
}PD_MeetingSeatOne;

//返回会议主持人查询
typedef struct
{
	int32u meetingid;//会议ID
	int32u membernameid;//主持人ID
	char   membername[NAME_LENG];//管理员登录名
}PD_Item_MeetCompee;

//stages:STAGE_MeetingSeat
//fun：FUN_All
//method: METHOD_Query
typedef struct
{
	int num;//

	//PD_Item_MeetCompee item[num];
}PD_QueryMeetCompee;

//会议聊天
//stages：STAGE_MeetingChat
//fun: FUN_One, 
//method:METHOD_Add  , 
typedef struct
{
	unsigned int MeetingId;     //会议ID
	unsigned int sendmemId;		//发送人人员ID
	unsigned int membernum;		//接收的参会人员id数,其中0为所有人员
	ROLE_MEMBER  role;
	ProtocalData::CHAT_TYPE_ENUM type;//聊天类型，包括

	char meetname[DESCRIBE_LENG];//会议名称
	char roomname[DESCRIBE_LENG];//会议室名
	char membername[DESCRIBE_LENG];//人员名称
	char seatename[DESCRIBE_LENG];//席位名

	PD_Time startTime;
	char text[CHATTEXT_LENG];//必须加上空字符结尾
	//int32u []; //参会人员id
}PD_MeetingChat;

//收到消息确认的回复
//stages：STAGE_MeetingChat
//fun: FUN_One, 
//method:METHOD_Report 
typedef struct
{
	//原消息的数据
	int32u		meetid;//会议ID
	ProtocalData::CHAT_TYPE_ENUM	    msgtype;//消息类型
	PD_Time	    utcsecond;//UTC时间 单位：UTC 秒
	ROLE_MEMBER		role;//原发送者角色
	int32u	    memberid;//原发送者ID
	int32u		senddevid;//原发送的设备ID

	char		confirmmsg[CHATTEXT_LENG];//确认的消息文本
	PD_Time	    confirmutcsecond;//确认的UTC时间 单位：UTC 秒
	int32u		confirmdevid;//确认的设备ID
	char		confirmseatename[DESCRIBE_LENG];//确认席位名
}PD_ChatConfirm, *pPD_ChatConfirm;
/*******************************投票***********************************/
//发起投票
//stages：STAGE_MeetingVote
//fun: FUN_StartVote, 发起投票
//method:METHOD_Control,控制
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int voteNum;  //有多少项投票内容
	//PD_VoteStart	vote; 填充VoteNum项投票信息
}PD_MeetingVoteStart;

//设置投票的超时值
//stages：STAGE_MeetingVote
//fun: FUN_StartVote, 发起投票
//method:METHOD_Set,设置
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int voiteid;  //投票ID
	unsigned int timeouts;  //超时值

}PD_MeetingVoteTimeouts;

//投票
//stages：STAGE_MeetingVote
//fun: FUN_Vote, 投票
//method:	METHOD_Control控制，METHOD_Stop结束投票（发起投票后需要发送METHOD_Stop结束投票）
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int voteNum;  //有多少项投票内容
	//PD_VoteState state; 填充VoteNum项投票信息
}PD_MeetingVoteState;
 
#define MEETVOTE_NEEDRECORD 0xff000000

//查询会议所有投票信息
//stages：STAGE_MeetingVote
//fun：FUN_VoteInfo
//method:  METHOD_Query  查询
typedef struct
{
	unsigned int MeetingId;
	unsigned int TotalNum; //总共有多少个 -- MEETVOTE_NEEDRECORD
	unsigned int StartId;  //当前开始序号
	unsigned int CurrNum;  //当前帧包括多少个信息

	//PD_VoteStart  //填充VoteNum项投票信息
}PD_MeetingVote_All;


//投票计数
typedef struct 
{  
	unsigned int voteid;  //投票ID
	unsigned int selectcount; //有效投票项
	unsigned int select[MAX_VOTEITEM_COUNT]; //选择1投票数
}PD_VoteCount;

//查询会议所有投票计数
//stages：			STAGE_MeetingVote
//fun：				FUN_VoteCount
//method:			METHOD_Query 查询
typedef struct
{
	unsigned int MeetingId;
	unsigned int VoteNum;
	//PD_VoteCount vc;  填充VoteNum项投票计数
}PD_MeetingVote_Count;


typedef unsigned int VOTE_SELECT;	//投票记录
//0x00000001选择了选项一,0x00000002选择了选项二
//投票人记录
typedef struct
{
	unsigned int memberid;
	VOTE_SELECT selects;	//投票选项记录
}PD_VoteStatic_Signed_One;
typedef struct
{
	unsigned int memberid;
	VOTE_SELECT selects;	//投票选项记录
	int16u namelen;			        //名称长度=字符串长度+结尾字节
	//char meetingname[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_VoteStatic_Signed_One_V2;
//查询记名投票投票人
//stage:		STAGE_MeetingVote
//fun:			FUN_VoteRecord
//method:		METHOD_Query 查询
typedef struct
{
	unsigned int meetingid;
	unsigned int voteid;
	unsigned int Num;
	//填充Num个PD_VoteStatic_Signed_One
}PD_VoteStatic_Signed;


//删除投票
//stages:STAGE_MeetingVote
//fun：FUN_VoteInfo
//method:  METHOD_Delete	删除
typedef struct
{
	unsigned int meetingid;
	unsigned int delcount;	//要删除的投票数量
	//unsigned int voteid;	//填充需要删除的投票id
	//...
	//unsigned int voteid;
}PD_MeetingVote_Mul;
/***************************投票***********************************/


//签到信息
//stages：STAGE_SignIn
//fun: ProtocalData::FUN_All, 获取所有的签到信息
//method:	查询
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	TYPE_SIGNIN  signin_type;//签到类型
	unsigned int TotalNum ;//总共有多少个
	unsigned int StartId;  //当前开始序号
	unsigned int CurrNum;  //当前帧包括多少个信息
	//PD_SignInInfo //不定长，根据前面参数填写
}PD_MeetingSignInAll;
 
//终端签到
//stages：STAGE_SignIn
//fun: FUN_One, 
//method:	METHOD_Add 添加
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_AddSignInInfo signIn; //
}PD_MeetingSignInOne;
 

//删除签到
//stages：STAGE_SignIn
//fun: FUN_One, 
//method:	METHOD_Delete 删除
typedef struct
{
	unsigned int MeetingId;  //会议ID
	int membernum; //
	//int32u memberid[];
}PD_MeetingDeleteSignIn; 

//批注信息
//stages：STAGE_Postil
//fun: ProtocalData::FUN_All, 获取所有
//method:	查询
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	unsigned int TotalNum ;//总共有多少个
	unsigned int StartId;  //当前开始序号
	unsigned int CurrNum;  //当前帧包括多少个信息
	//PD_Postil //不定长，根据前面参数填写
}PD_MeetingPostilAll; 

//批注信息
//stages：STAGE_Postil
//fun: FUN_One, 
//method: 添加 删除
typedef struct 
{  
	unsigned int MeetingId;  //会议ID
	PD_Postil pstl;
}PD_MeetingPostilOne; 


//白板-设置参会人员颜色
typedef struct
{
	unsigned int memberid;
	unsigned int ARGB;
}PD_WhiteBoard_MemberColor_Common;

//白板-设置参会人员颜色
//stages:STAGE_BaiBan
//fun:	FUN_ColorConfig
//method: 修改
typedef struct
{
	unsigned int meetingid;
	unsigned int memberid;
	unsigned int ARGB;
}PD_WhiteBoard_MemberColor_Modify_Client;

//白板-设置参会人员颜色
//stages:STAGE_BaiBan
//fun:	FUN_ColorConfig
//method: 查询
typedef struct
{
	unsigned int meetingid;
}PD_WhiteBoard_MemberColor_Query_Client;

//白板-设置参会人员颜色
//stages:STAGE_BaiBan
//fun:	FUN_ColorConfig
//method: 查询
typedef struct
{
	unsigned int meetingid;
	unsigned int Num;
	//Num个PD_WhiteBoard_MemberColor_Common
}PD_WhiteBoard_MemberColor_Query_Sever;

//白板项
//stages：STAGE_BaiBan
//fun: FUN_One,ProtocalData::FUN_All  说明：撤销所有的时候使用ProtocalData::FUN_All ，方法为删除
//method:	增加，删除，修改
typedef struct 
{  
	int32u MeetingId;   //会议ID
	int32u srcmemid; //白板发起人员ID
	int64u srcwbid;		//白板id

	int32u  fileid;
	int32u  pageindex;

	//此两项用于标识这个操作
	int32u memberid;    //人员ID
	int32u operid;	    //操作ID 终端计算产生
	int64u utcstamp;	//时间戳
	WHITEBOAD_FIGURETYPE figuretype;	//图形类型
}PD_WhiteBoardHeader, *pPD_WhiteBoardHeader;

//PDF del clear
typedef struct
{
	PD_WhiteBoardHeader header;

	float  pos[2];		    //(lx,ly坐标)
}PD_WhiteBoard_pdfDel, *pPD_WhiteBoard_pdfDel;

//line elapse rect
typedef struct 
{  
	PD_WhiteBoardHeader header;

	int8u  linesize;		//线条宽度
	int32u Argb;			//线条颜色
	float  pt[4];		    //(lx,ly,rx,ry 左上角,右下角坐标)
}PD_WhiteBoard_rect, *pPD_WhiteBoard_rect;

typedef struct 
{  
	PD_WhiteBoardHeader header;

	int8u  linesize;			//线条宽度
	int32u Argb;			//线条颜色
	int32u ptnum;//ink point Num
	//ink  --- float[2 * optionlen](x,y依次排列)
}PD_WhiteBoard_ink, *pPD_WhiteBoard_ink;

typedef struct 
{  
	PD_WhiteBoardHeader header;

	float  pos[2];		    //(lx,ly,左上角坐标)
	int32u picsize;			//data size png format
}PD_WhiteBoard_picture, *pPD_WhiteBoard_picture;

//font flag
#define WHITEBOARD_FONT_STRIKEOUT 0x01 //删除线
#define WHITEBOARD_FONT_BOLD	  0x02 //加粗
#define WHITEBOARD_FONT_ITALIC	  0x04 //倾斜
#define WHITEBOARD_FONT_UNDERLINE 0x08 //下划线

typedef struct 
{  
	PD_WhiteBoardHeader header;

	int8u  fontsize;			//字体大小
	int8u  fontflag;			//字体其它属性
	int32u Argb;				//字体颜色
	char   fontname[NAME_LENG]; //字体名称
	float  pos[2];				//(lx,ly,左上角坐标)

	int32u textlen;			    //char[](长度为 textlen)
}PD_WhiteBoard_text, *pPD_WhiteBoard_text;

//////////////////////////////////////////////////////////////////////////
//----------人员管理----------
//人员管理部分
//人员管理
//stages：STAGE_PeopleManage
//fun: ProtocalData::FUN_All 对所有人员操作
//method:查询
//人员管理中，人员名称是唯一的信息，不能重复
typedef struct 
{  
	unsigned int TotalNum;	//总共有多少设备
	unsigned int StartId;	//当前帧开始序号  
	unsigned int  CurrNum;	//当前帧包括多少个设备序号
	//PD_PersonnelInfo ; //根据实际人员填写PD_PersonnelInfo结构信息
	
}PD_AllPeopleManage;

//人员管理
//stages: STAGE_PeopleManage
//fun: FUN All
//method: 搜索
//客户端发送该结构体
typedef struct
{
	PD_PersonnelInfo people;
}PD_PeopleSearch;

//人员管理
//stages: STAGE_PeopleManage
//fun: FUN All
//method: 搜索
//服务端发送该结构体
typedef struct
{
	unsigned int num;
	//PD_PersonnelInfo ; //填写num个PD_PersonnelInfo结构信息
}PD_PeopleSearchResult;

//人员管理
//stages：STAGE_PeopleManage
//fun: FUN_One 对单个人员操作
//method:添加，删除
//客户端发送数据，服务端只返回PD_Responseheader应答部分
typedef struct 
{  
	PD_PersonnelInfo per;
	
}PD_OnePeopleManage;

//批量删除人员管理
//stages: STAGE_PeopleManage
//fun: FUN All
//method: del
//客户端发送数据，服务端只返回PD_Responseheader应答部分
typedef struct
{
	unsigned int num;
	//int32u ; //填写num个peopleid
}PD_MutilOperPeople;

//控制会议的状态
//stages：STAGE_MeetingStatus
//fun: STAGE_MeetingStatus, 控制会议的状态
//method:	查询、控制
typedef struct{
	unsigned int meetingid;//会议id
	ProtocalData::MEETING_STATUS status;//会议状态
}PD_MeetingStatusControl;

#define MODIFY_FILEACCESS_FLAG_CLEAR 0x00000001 //保存前先清空

//会议文件权限
//stages：STAGE_MeetingDatum
//fun: FUN_File_Access, 单个文件的权限
//method:查询,修改
typedef struct
{
	int32u  MeedingId;  //会议ID
	int32u  flag;//1=表示清空再保存,0表示直接添加
	int32u  jsonlen;
	//char    json[];
	/*
	{
	"data":[
	{"fileid":"0x6b0000001","mem":[{"id":1},{"id":2}]},
	{"fileid":"0x6b0000003","mem":[{"id":1},{"id":2}]},
	]
	}
	*/
}PD_MeetingFileAccess;


//目录人员权限
/*
适用于STAGE_MeetingDatum + FUN_DirPermission + METHOD_Query/METHOD_Modify
修改请求PD_MeetingId + PD_SingleId(目录id) + PD_BlockCount(参会人数量) + PD_SingleId(参会人id) + ...
查询请求PD_MeetingId + PD_SingleId(目录id)
查询回复PD_MeetingId + PD_SingleId(目录id) + PD_BlockCount(参会人数量) + PD_SingleId(参会人id) + ...
*/

typedef struct
{
	int32u devid;
	int32u ptsize;
	//char  pt[ptsize]; //填充ptsize / (size(float) * 2) 个float用来记录指旨轨迹
}PD_DevPointItem;

//会场设备查询
//stages：STAGE_DeviceASGN
//fun: FUN_Device_Asgn,
//method:查询
typedef struct
{
	int32u roomid;
	int32u devnum;
	//PD_DevPointItem deviceid[devnum];  
}PD_DevPoint;


typedef struct PD_DEVPOS
{
	unsigned int devid;
	float		 x;
	float		 y;
	ProtocalData::SEAT_DIRECTION direction;
}PD_DEVPOS;

//会场设备
//stages：STAGE_DeviceASGN
//fun: FUN_One 单个会场设备
//method:添加 删除
typedef struct
{
	unsigned int MeetingroomId;		//会场ID
	unsigned int DeviceNum;
	//unsigned int deviceid;  填充DeviceNum个设备ID
}PD_FieldDevice;

//会场设备查询
//stages：STAGE_DeviceASGN
//fun: FUN_Device_Asgn,
//method:查询
typedef struct
{
	unsigned int MeetingroomId;		//会场ID
	unsigned int DeviceNum;
	//PD_DEVPOS deviceid[DeviceNum];  填充DeviceNum个设备ID//PD_DEVPOS
}PD_DeviceAssign; 


//终端发送数据头---等待用户
//stages: STAGE_Wait4User
//fun: FUN_Wait4User
//method: 查询
typedef struct 
{
	unsigned int deviceid; //设备ID
}PD_Wait4User_TERMINAL;


//服务端返回数据头---等待用户
//stages: STAGE_Wait4User
//fun: FUN_Wait4User
//method: 查询
typedef struct 
{
	unsigned int deviceid;				//设备ID
	unsigned int meetingid;				//会议ID
	unsigned int memberid;				//人员ID
	unsigned int roomid;				//会场ID
	char meetingname [DESCRIBE_LENG];	//会议名称
	char membername [NAME_LENG];		//人员名称
	char company [DESCRIBE_LENG];		//公司名称
	char job [DESCRIBE_LENG];			//职位名称
	TYPE_SIGNIN signin_type;			//签到类型
}PD_Wait4User_SERVER;
typedef struct
{
	unsigned int deviceid;				//设备ID
	unsigned int meetingid;				//会议ID
	unsigned int memberid;				//人员ID
	unsigned int roomid;				//会场ID
	char membername[NAME_LENG];		//人员名称
	char company[DESCRIBE_LENG];		//公司名称
	char job[DESCRIBE_LENG];			//职位名称
	char signin_type;					//签到类型 TYPE_SIGNIN
	char file;
	int16u namelen;			        //名称长度=字符串长度+结尾字节
	//char meetingname[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_Wait4User_SERVER_V2;

#define MEETPOTIL_FLAG_FORCEOPEN	   1 //该标志表示需要强制执行批注白板强行打开
#define MEETPOTIL_FLAG_FORCECLOSE	   2 //该标志表示需要强制执行批注白板强行关闭
#define MEETPOTIL_FLAG_REQUESTOPEN	   3 //该标志表示有人发起白板
#define MEETPOTIL_FLAG_REJECTOPEN	   4 //该标志表示对方拒绝打开白板
#define MEETPOTIL_FLAG_EXIT			   5 //该标志表示有人离开白板
#define MEETPOTIL_FLAG_ENTER		   6 //该标志表示有人进入白板

#define MEETPOTIL_FLAG_MAXHUBREQUESTOPEN	   101 //该标志表示有人发起maxhub白板
#define MEETPOTIL_FLAG_MAXHUBREJECTOPEN		   102 //该标志表示对方拒绝打开maxhub白板
#define MEETPOTIL_FLAG_MAXHUBEXIT			   103 //该标志表示有人离开maxhub白板
#define MEETPOTIL_FLAG_MAXHUBENTER			   104 //该标志表示有人进入maxhub白板
#define MEETPOTIL_FLAG_MAXHUBENTERSYNER		   105 //该标志进入maxhub白板协同状态
#define MEETPOTIL_FLAG_MAXHUBCLOSESYNER		   106 //该标志退出maxhub白板协同状态

//批注白板操作 -- add by ct 20160822
//stages: STAGE_BaiBan
//fun:    ProtocalData::FUN_All
//method: 通知
typedef struct 
{
	int32u  meetingid;
	char    medianame [DESCRIBE_LENG];	//名称
	int32u  opermemberid;//当前该命令的人员ID
	int32u  srcmemid;//发起人的人员ID 白板标识使用
	int64u  srcwbid;//发起人的白板标识 取微秒级的时间作标识 白板标识使用
	int32u  operflag;//操作标志
	int32u  fileid;
	int32u  pageindex;
	int32u  userdevnum; //推送的用户设备ID数量 为0表示推送到当前会议的所有用户
	//int32u userdevid[];
}PD_PushPotil, *pPD_PushPotil;

//文件推送 -- add by ct 20160716
//stages: STAGE_PushFile
//fun:    FUN_PushFile
//method: 通知
typedef struct 
{
	int32u  meetingid;
	int32u  fileid;  //推送的文件ID
	int32u  triggeruserval;//文件标志
	int32u  devnum;   //推送的设备数量 为0表示推送到当前会议的所有设备
	//int32u devid[];
}PD_PushFile, *pPD_PushFile;

//文件推送 -- add by ct 20220205
//stages: STAGE_PushFile
//fun:    FUN_PushFile
//method: 通知
typedef struct
{
	int32u  meetingid;
	int32u  fileid;  //推送的文件ID
	int32u  flag;//
	int32u  dirid; //目录id
	char    passwd[PD_SHORT_PASSWORD_LENG];//文档加密密码, 为空表示无密码
	int32u  triggeruserval;//文件标志
	int32u  devnum;   //推送的设备数量 为0表示推送到当前会议的所有设备
	//int32u devid[];
}PD_PushFile_V2, *pPD_PushFile_V2;

//流推送 -- add by ct 20160718
//stages: STAGE_PushStream
//fun:    FUN_PushStream
//method: 通知
typedef struct 
{
	int32u  meetingid;
	int32u  triggeruserval;//流标志
	int32u  id;		   //指定的流ID或设备ID
	int8u   subid;	   //设备ID的字通道号
	int8u   encodemode;   //参见 DEVICE_INVITECHAT_ENCODEMODE_HIGH
	int8u   fill[2];   //填充字节
	int32u  devnum;   //推送的设备数量 为0表示推送到当前会议的所有设备
	//int32u devid[];
}PD_PushStream, *pPD_PushStream;

//向主持人请求流推送到会议终端 -- add by ct 20160718
//stages: STAGE_RePushStream
//fun:    FUN_RePushStream
//method: 告知
typedef struct 
{
	int32u  meetingid;
	int32u  handledeviceid; //处理该请求的设备
	int8u   subid;	   //设备ID的字通道号
	int8u   encodemode;   //参见 DEVICE_INVITECHAT_ENCODEMODE_HIGH
	int8u   fill[2];   //填充字节
	int32u  triggeruserval;//流标志
	int32u  deviceid; //发起请求的设备ID
	int32u  memberid; //发起请求的人员ID

	int32u  devnum;   //推送的设备数量 为0表示推送到当前会议的所有设备
	//int32u devid[];

}PD_RePushStream, *pPD_RePushStream;

//see screencontrol.h define
//向桌面源发送控制指令 -- add by ct 20161101
//stages: STAGE_StreamControl
//fun:    FUN_STREAMCONTROL
//method: METHOD_Control
typedef struct 
{
	int32u  flag;			//控制的标志
	int32u  deviceid;       //发起请求的设备ID
	int32u  memberid;       //发起请求的人员ID
	int32u  handledeviceid; //处理该请求的设备
	int8u   streammode;		//模式
	int8u	fill[3];
	int32u  val1;
	int32u  val2;
}PD_HeaderStreamControl, *pPD_HeaderStreamControl;

typedef struct 
{
	int32u  otherflag; //other flag
	float   x; //x% width的百分
	float   y; //y% height的百分
}PD_MouseStreamControl, *pPD_MouseStreamControl;

typedef struct 
{
	int32u  otherflag;
	int32u  key; //对应的key值,参考qt enum Key的值
}PD_KeyBoardStreamControl, *pPD_KeyBoardStreamControl;

#define MemState_MainFace  0
#define MemState_MemFace   1
#define MemState_AdminFace 2
#define optionid_SIGNINNUM 0
//通知终端停止当前的触发器 -- add by ct 20170216
//stages: STAGE_DeviceOper
//fun:    ProtocalData::FUN_All
//method: METHOD_Control
typedef struct
{
	int32u  meetingid;
	int32u  oper;			//控制码 enum MEETING_DEVICEOPER
	int32u  deviceid;       //发起请求的设备ID
	int32u  memberid;       //发起请求的人员ID
	int32u  val;
	//MEETING_ZOOMPER		  zoompercent (0 ~ 100)
	//MEETING_NOTIFYMEMBERNUM membernum 
	//MEETING_NOTIFYMEMBERSTATE MemState_MainFace 0停留在主界面，1停留在参会人界面,2停留在秘书界面
	//MEETING_TEXTBRODCAST texttype;

	int32u  devnum;			//处理该请求的设备数量 为0表示全部
	int32u  optiondatalen;
	//int32u []  //devid
	//char[] //optiondata

	//MEETING_STOPRESWORK		int8u  resid[];
	//MEETING_NOTIFYMEMBERNUM   int32u data[], data[0]=已经签到的参会人员数;
	//MEETING_TEXTBRODCAST char  *text; optiondatalen=strlen(text) + 1;
}PD_DeviceOper, *pPD_DeviceOper;

//设备界面状态交互 -- add by ct 20240729
//stages: STAGE_DeviceFaceState
//fun:    ProtocalData::FUN_One
//method: METHOD_Query/METHOD_Set
typedef struct
{
	int64u  timestamp;
	int32u  devnum;			//
	//int32u []  //devid
	//PD_DevFaceState_Item []
}PD_DeviceFaceState, *pPD_DeviceFaceState;

typedef struct
{
	int32u  meetingid;
	int32u  deviceid;    
	int32u  memberid;       
	int32u  facestate;
}PD_DevFaceState_Item, *pPD_DevFaceState_Item;

//设备实时信息 -- add by ct 20250523
//stages: STAGE_DeviceFaceState
//fun:    ProtocalData::FUN_All
//method: METHOD_Query/METHOD_Set
typedef struct
{
	int64u  timestamp;
	int32u  jsonlen;
	//int32u []  //devid
	//PD_DevFaceState_Item []
}PD_DevsInfo, *pPD_DevsInfo;

/********************************变更推送START************************************/
//----------part_change----------	//变更内容
#define seat_change 			0x01	//排位变更
#define agenda_change 			0x02	//议程变更
#define bulletin_change			0x03	//公告变更
#define dir_change 				0x04	//目录变更
#define file_change 			0x05	//文件变更
#define people_change			0x06	//人员变更
#define stream_change			0x07	//流变更
#define field_change			0x08	//会场变更
#define url_change				0x09	//默认网址变更 data=PD_URL_item* id_change=num  
#define	desk_change				0x0a	//桌牌配置变更
#define flddevice_change		0x0b	//会场设备变更
#define meeting_change			0x0c	//会议变更
#define member_change			0x0d	//参会人员变更
#define admin_change			0x0e	//管理员变更
#define batch_member_change		0x0f	//批量参会人员变更
#define signin_change			0x10	//签到变更
#define groupofpeople_change	0x11	//人员分组变更
#define groupofmember_change	0x12	//参会人员分组变更
#define peopleingroup_change	0x13	//人员分组成员变更
#define memberingroup_change	0x14	//参会人员分组成员变更
#define membercolor_change		0x15	//白板颜色配置变更
#define votecount_change		0x16	//投票计数变更
#define votestat_change			0x17	//投票状态变更 fid_change=voteid id_change=votestate
#define voteinfo_change			0x18	//投票信息变更
#define memperm_change			0x19	//参会人员权限变更
#define dirperm_change			0x1a	//目录参会人权限变更
#define meetingstatus_change    0x1b	//会议状态变更
#define votetimeouts_change		0x1c	//投票超时值变更 fid_change=voteid id_change=timeouts
#define meetfuncfg_change		0x1d	//会议功能配置变更
#define fieldmanager_change		0x1e	//管理员可控会场变更 fid_change=adminid, id_change=num 
#define facecfg_change			0x1f	//界面设置变更 id_change=faceid  PD_FaceTextItemInfo PD_FacePictureItemInfo
#define fieldbg_change			0x20	//会场背景图变更 fid_change=roomid, id_change=mediaid 
#define filepos_change			0x21	//会议目录文件排序变更  data=PD_MeetingFilePosSet*
#define filescore_change		0x22	//文件评分变更 data=PD_Item_FileScore* id_change=num  
#define fileevaluate_change		0x23	//文件评价变更 data=PD_Item_FileEvaluate* id_change=num| data=PD_DelFileEvaluate*
#define meetevaluate_change		0x24	//会议评价变更 data=PD_Item_MeetEvaluate* id_change=num| data=PD_DelMeetEvaluate*
#define systemlog_change		0x25	//系统日志变更 data=PD_Item_MeetSystemLog* id_change=num
#define bigbulletin_change		0x26	//会议大白板公告变更
#define dirpos_change			0x27	//会议目录排序变更  data=PD_MeetingDirPosSet*
#define publicinfo_change		0x28	//全局字串变更  data=PD_PublicInfo*
#define userdeffilescore_baseinfo_change 0x29	//会议自定义评分基础数据变更  data=PD_UserDefineFileScore* | drop fid_change=voteid
#define userdeffilescore_timeout_change 0x2a	//会议自定义评分超时值变更  fid_change=voteid id_change=timeouts
#define userdeffilescore_votestate_change 0x2b	//会议自定义评分状态变更  data=PD_change_UserDefineFileScore fid_change=voteid id_change=votestate
#define userdeffilescore_recordcount_change 0x2c	//会议自定义评分状态变更  data=PD_Item_FileScoreMemberStatistic* fid_change=voteid
#define memberpos_change			0x2d	//会议参会人员排序变更  data=PD_MeetingMemberPosSet*
#define meettopic_change			0x2e	//会议议题变更  data=PD_TopicItemInfo* id_change=num 
#define meettopicgroup_change		0x2f	//会议议题单位变更  data=PD_TopicGroupItemInfo* id_change=num 
#define meettopicperm_change		0x30	//会议议题权限变更  data=PD_MeetTopics* id_change=num 
#define meetlecture_change		    0x31	//会议演讲稿变更  data=PD_LectureItemInfo* id_change=num 
#define meethomepage_change		    0x32	//会议欢迎界面变更  data=PD_HomePageItemInfo* id_change=num 
#define fileaccess_change 			0x33	//文件权限变更 data=(userid)int32u*  | id_change=num | fid_change=fileid
#define meetuserdef_change		    0x34	//会议自定义数据变更  data=PD_MeetUserdefItemInfo* id_change=num 
#define roomuserdef_change		    0x35	//会场自定义数据变更  data=PD_RoomUserdefItemInfo* id_change=num 
#define avote_change		        0x36	//新投票数据变更  data=PD_AVote* id_change=num 
#define avotestat_change			0x37	//新投票状态变更 fid_change=voteid id_change=votestate
#define avotecount_change			0x38	//新投票计数变更
#define seatplan_change		        0x39	//席位方案变更  data=PD_AddSeatPlan* id_change=num 
#define seatplanbind_change		    0x3a	//席位方案ID绑定变更  data=PD_SeatPlanBindItem* id_change=num 
#define seatplanmem_change		    0x3b	//席位方案人员绑定变更  data=PD_SeatPlanMemItem* id_change=num 
#define devfacestate_change		    0x3c	//设备界面状态变更  data=PD_DeviceFaceState* id_change=num 
#define devinfo_change		        0x3d	//设备信息变更  data=PD_DevsInfo* 
#define complexpublicuserdef_change		        0x3e	//全局复合自定义数据变更  data=PD_ComplexPublicUserInfo* 
#define complexmeetuserdef_change		        0x3f	//会议复合自定义数据变更  data=PD_ComplexMeetcUserInfo* 


/*人员分组成员变更 / 参会人员分组成员变更 发生时,只发送变更的头部数据,客户端下拉框刷新获取*/

//----------type_change----------	//变更类型
#define drop_change 	0x01	//已删除(删除的内容包含其他内容时,如会场包含会场设备,这些内容也被删除,而且不会推送这些变更)
#define new_change 		0x02	//新增
#define mod_change		0x03	//已修改(id不变)
#define clr_change		0x04	//清空(用于清空一个id对应多条数据的内容，如清空某个目录的访问黑名单，以便于批量改变内容)
#define status_change 	0x05	//状态
#define pos_change 	    0x06	//位置
#define fastadd_change 	0x07	//快速新增
#define filevote_change 0x08	//文件投票重置
#define agendadir_change 0x09	//议程关联目录重置
#define memrole_change   0x0a	//参会人角色重置
#define device_change    0x0b	//设备
#define import_change    0x0c	//导入

//stages:STAGE_PushUpdate
//fun:FUN_PushUpdate
//method:通知

/* 变更项父id 用于标识 变更项id 的更多信息,
   1.用于标识 文件id所在的目录id: 
	fid_change=dirid
	id_change=fileid
   2.用于标识 设备id所在的会场id：
    fid_change=fieldid;
	id_change=deviceid;
	*/
typedef struct
{
	unsigned char part_change;	//变更内容
	unsigned char type_change;	//变更类型
	unsigned int meetingid;		//会议id(变更内容,如会场、会场设备、人员等,独立于会议的,填0)			
	unsigned int fid_change;	//变更项父id
	unsigned int id_change;		//变更项id(变更内容为会议时,会议id只在meetingid填写)
	unsigned int length_change;	//后接内容长度(字节)
	//变更后内容(新增、已修改的内容)
}PD_ContntChng;


//排位变更内容
//设备id赋值给PD_ContntChng的id_change
typedef struct
{
	unsigned int 	memberid;				//座位分配的人员id,若座位未分配给人员memberid为0;
	ROLE_MEMBER 	role;					//角色
}PD_chng_seat;

//人员变更内容
typedef struct
{
	char name[NAME_LENG];		//名字
	char company[DESCRIBE_LENG]; //单位
	char job[DESCRIBE_LENG];		//职位
	char comment[DESCRIBE_LENG];	//备注
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱
	char password[PASSWORD_LENG];  //密码
}PD_chng_peopl;

//目录变更内容
typedef struct
{
	char name[FILENAME_LENG];		//文件名
	unsigned int parentdirid;	    //父目录ID
	unsigned int dirpos;		    //序号
}PD_chng_dir;

//文件变更内容--新增
typedef struct
{
	char name[FILENAME_LENG];					//文件名
	unsigned int uploaderid;					//上传者id,上传角色为管理员时,uploaderid=0
	ROLE_MEMBER  uploader_role;					//上传者角色
	char		 uploader_name[NAME_LENG];		//上传者名字
	unsigned int filepos;	//文件序号
}PD_chng_file_new;

//文件变更内容--修改
typedef struct
{
	char name[FILENAME_LENG];		//文件名
}PD_chng_file_mod;

//流变更内容
typedef struct
{
	char name[DESCRIBE_LENG]; //视频名称
	char addr[DESCRIBE_LENG]; //视频地址
}PD_chng_stream;

//会场变更内容
typedef struct
{
	char name[DESCRIBE_LENG];  //字符串信息
	char addr[DESCRIBE_LENG];  //会场地点
	char comment[DESCRIBE_LENG];  //备注
	unsigned int picid;		   //背景图id
}PD_chng_field;

//会议变更内容
typedef struct
{
	char name[DESCRIBE_LENG];		//会议名称
	unsigned int roomId;			//会议室ID
	TYPE_MEETING type;				//会议类型
	PD_Time startTime;				//开始时间
	PD_Time endTime;				//结束时间
	unsigned int managerid;			//管理员ID
	TYPE_SIGNIN signin_type;		//会议签到类型
	char meeting_psw[ONESIGNPASSWORD_LENG];			//会议签到密码
	char ordername[NAME_LENG];      //会议预约人员名称
}PD_chng_meeting;

//桌牌变更内容
typedef struct
{
	PD_Color conf1[Count_FontConf];	//桌牌配置
	unsigned int bg_photoid;		//桌牌背景图id
}PD_chng_desk;

//参会人员变更
typedef struct
{
	char psw[ONESIGNPASSWORD_LENG];					//个人签到密码
}PD_chng_member;

//管理员变更内容
typedef struct
{
	char admin_user[NAME_LENG];		//管理员登录名
	char desc[DESCRIBE_LENG];		//管理员描述
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱

}PD_chng_admin;

//批量参会人员变更--添加
typedef struct
{
	unsigned int MemNum;			//添加参会人员的数量
	//PD_Member_Edit[MemNum];		//填充MemNum个参会人员信息
}PD_chng_member_batch;

//批量参会人员变更--删除
typedef struct
{
	unsigned int MemNum;			//删除参会人员的数量
	//unsigned int memberid;		//填充MemNum个参会人员Id
}PD_chng_member_del_batch;

//签到变更--新增
//参会人员的id填在id_change
typedef struct
{
	PD_Time time;					//时间
	TYPE_SIGNIN signin_type;		//签到类型
	unsigned int length;			//后接数据长度
	//char signin_photo[length];	//视频签到图片
}PD_chng_signin;

//人员分组变更内容
typedef struct
{
	char		groupname[NAME_LENG];
}PD_chng_groupofpeople;

//参会人员分组变更能容
typedef struct
{
	char		groupname[NAME_LENG];
}PD_chng_groupofmember;

//白板颜色配置变更
typedef struct
{
	unsigned int rgb;
}PD_chng_membercolor;

//投票信息变更
typedef struct{
	char content[VOTE_CONTENTLENG]; //投票内容 
	ProtocalData::VOTEMAIN_TYPE  maintype; //类别 投票 选举 问卷调查
	ProtocalData::VOTE_MODE mode; //匿名投票 记名投票
	ProtocalData::VOTE_TYPE type; //多选 单选
	unsigned int timeouts;     //计时投票 秒数
	unsigned int selectcount;     //有效选项数量
	char voteText[MAX_VOTEITEM_COUNT][VOTE_LENG];  //选择1描述文字		
}PD_chng_voteinfo;


//参会人员权限变更
/*
fid:memberid		//参会人员id
id:permission		//参会人权限
*/


//会议状态变更
/*
id:status			//会议状态
*/

/********************************变更推送END************************************/


/*--------------------管理员--------------------*/
//管理员
typedef struct
{
	unsigned int adminid;			//管理员id
	char admin_user[NAME_LENG];		//管理员登录名
	char desc[DESCRIBE_LENG];		//管理员描述
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱

}PD_AdminInfo;

//stages:STAGE_Admin
//fun:FUN_One
//method:添加,修改
typedef struct
{
	PD_AdminInfo	admininfo;
	char psw[DESCRIBE_LENG];		//管理员密码
}PD_Amdin_One;

//stages:STAGE_Admin
//fun:FUN_One
//method:删除(只有超级管理员root才能删除)
typedef struct
{
	unsigned int adminid2del;				//被删除的管理员id
}PD_Admin_One_Del;

//stages:STAGE_Admin
//fun:ProtocalData::FUN_All
//method:查询
typedef struct
{
	unsigned int Num;
	//PD_AmdinInfo admininfo;	//填充Num个管理员信息
}PD_Admin_All;

#define ADMINPOWER_DEVICEVIEW    0x00000001 //设备浏览
#define ADMINPOWER_DEVICECONTROL 0x00000002 //设备管理 删除修改参数配置
#define ADMINPOWER_ROOMQUERY	 0x00000020 //会场浏览
#define ADMINPOWER_ROOMCONTROL   0x00000040 //会场增删改
#define ADMINPOWER_ROOMOPER	     0x00000080 //会场管理设备的增删
#define ADMINPOWER_ROOMSET	     0x00000100 //会场布局修改
#define ADMINPOWER_ADMINVIEW     0x00000200 //管理员浏览
#define ADMINPOWER_ADMINCONTROL  0x00000400 //管理员增删改 只能对子级及以下修改
#define ADMINPOWER_PEOPLEQUERY   0x00000800 //常用人员和部门浏览
#define ADMINPOWER_PEOPLECONTROL 0x00001000 //常用人员和部门增删改
#define ADMINPOWER_DEVICEUPDATE  0x00002000 //设备升级
#define ADMINPOWER_OTHERSET      0x00004000 //其它设置 （全局的界面配置等）
#define ADMINPOWER_MEETVIEW 	 0x00008000 //会议浏览
#define ADMINPOWER_MEETCONTROL   0x00010000 //会议管理 增删改复制
#define ADMINPOWER_MEETOPER	     0x00020000 //会议状态控制
#define ADMINPOWER_MEMBERQUERY   0x00040000 //会前、中、后的数据浏览权限
#define ADMINPOWER_MEMBERCONTROL 0x00080000 //会前、中、后数据编辑修改删除的权限
#define ADMINPOWER_FILEOPEN      0x00100000 //会议资料下载打开的权限
#define ADMINPOWER_DEVICEOPER    0x00200000 //设备操作 升降开关机签到辅助等
#define ADMINPOWER_RECORDOPEN	 0x00400000 //录像查看
#define ADMINPOWER_LOGVIEW       0x00800000 //日志查看
#define ADMINPOWER_RECORDDOWN    0x01000000 //录像下载
#define ADMINPOWER_ARCHIVE       0x02000000 //会议归档
#define ADMINPOWER_DATAEXPORT    0x04000000 //数据（会议、人员、投票、签到、排位、日志等）导出

typedef struct
{
	int32u adminid;			//管理员id
	int32u parentid;//管理员的父ID
	int32u power;//权限
	char admin_user[NAME_LENG];		//管理员登录名
	char desc[DESCRIBE_LENG];		//管理员描述
	char phone[SHORT_DESCRIBE_LENG];  //电话
	char email[SHORT_DESCRIBE_LENG];  //邮箱

}PD_AdminInfo_V2;

//stages:STAGE_Admin
//fun:FUN_One
//method:添加,修改
typedef struct
{
	PD_AdminInfo	admininfo;
	char psw[PASSWORD_LENG];		//管理员密码
}PD_Amdin_One_V2;

//stages:STAGE_Admin
//fun:ProtocalData::FUN_All
//method:查询
typedef struct
{
	unsigned int Num;
	//PD_AdminInfo_V2 admininfo;	//填充Num个管理员信息
}PD_Admin_All_V2;

#define IMPORT_ADMIN_FLAG_DELALL 0x00000001 //导入前删除人员

//批量操作管理人员
//stages：STAGE_Admin
//fun: ProtocalData::FUN_All
//method:IMPORT|Delete
typedef struct
{
	int32u flag;//IMPORT_PEOPLE_FLAG_DELALL

	int32u jsonlen;
	/*
	{
	"data":  //
	[
	{
	"name::"admin",
	"phone":"123456",
	"email":"",
	"comment":"",
	"password":"123456",//md5
	"admin":"1",
	}
	]
	}
	*/

}PD_MutilOperAdmin, *pPD_MutilOperAdmin;
/*--------------------管理员--------------------*/

/*--------------------会议功能配置--------------------*/
//功能配置
typedef struct
{
	unsigned int funcode;	//功能标识
	unsigned int position;	//位置
}PD_MeetingFunc_conf;

//stages:STAGE_FunConf
//fun:ProtocalData::FUN_All
//method:修改、查询
typedef struct
{
	unsigned int meetingid;
	unsigned int fun_num;
	//填充fun_num个PD_MeetingFunc_conf
}PD_MeetingFuncs;
/*--------------------会议功能配置--------------------*/
/*-------------------人员分组start---------------------*/
typedef struct
{ 
	unsigned int groupid;
	char groupname[NAME_LENG];//分组名
}PD_GroupOfPeople;

//stages:ProtocalData::STAGE_PeopleGroup
//fun:ProtocalData::FUN_All
//method:查询
typedef struct
{
	unsigned int totalnum;
	//PD_GroupOfPeople;  填充totalnum个PD_GroupOfPeople
}PD_AllGroupOfPeople;

//stages:ProtocalData::STAGE_PeopleGroup
//fun:FUN_One
//method:添加、修改、删除
typedef struct
{
	PD_GroupOfPeople group;
}PD_OneGroupOfPeople;

//stages:ProtocalData::STAGE_PeopleGroup
//fun:ProtocalData::FUN_All
//method:修改、删除、设置(将指定人员复制到某分组)
typedef struct
{
	unsigned int groupid;
	unsigned int pepnum; //分组人员数量
	//unsigned int peopleid;  填充pepnum个peopleid
}PD_SaveGroupOfPeople;

//用于添加分组人员
//stages:ProtocalData::STAGE_PeopleGroup
//fun:ProtocalData::FUN_All
//method:添加
typedef struct
{
	unsigned int groupid;//同时将该添加的人员加入到指定分组
	PD_PersonnelInfo per;

}PD_AddPeopleGroupManage;

//stages:ProtocalData::STAGE_PeopleGroup
//fun:FUN_One
//method:查询
typedef struct
{
	unsigned int groupid;
	unsigned int pepnum; //分组人员数量
	//PD_PersonnelInfo people;  填充pepnum个PD_PersonnelInfo
}PD_PeopleInGroup;

#define IMPORT_PEOPLE_FLAG_DELALL 0x00000001 //导入前删除人员

//导入常用人员
//stages：STAGE_PeopleGroup
//fun: ProtocalData::FUN_All
//method:IMPORT
typedef struct
{
	int32u groupid;//如果导入全部=0，指定组写入即可
	int32u flag;//IMPORT_PEOPLE_FLAG_DELALL

	int32u jsonlen;
	/*
	{
	"data":  //
	[
	{
	"name::"陈工",
	"company":"xx",
	"job":"xx",
	"phone":"123456",
	"email":"",
	"comment":"",
	"password":"123456",//md5
	"peopleid":"",
	}
	]
	}
	*/

}PD_ImportPeople;
/*-------------------人员分组end---------------------*/

/*-------------------参会人员分组end---------------------*/
/*----阶段:STAGE_MemberGroup----*/
typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
	char groupname[NAME_LENG];//分组名
}PD_GroupOfMember_common;

typedef struct
{
	unsigned int meetingid;
	char groupname[NAME_LENG];
}PD_GroupOfMember_Add_client;//FUN:FUN_One METHOD:METHOD_Add

typedef struct
{
	unsigned int	meetingid;
}PD_AllGroupOfMember_Query_client;//FUN:ProtocalData::FUN_All METHOD:METHOD_Query

typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
	char groupname[NAME_LENG];//分组名
}PD_GroupOfMember_Modify_client;//FUN:FUN_One METHOD:METHOD_Modify

typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
}PD_GroupOfMember_Delete_client;//FUN:FUN_One METHOD:METHOD_Delete

typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
	unsigned int	num;
	//unsigned int  memberid;//填充num个memberid
}PD_MemberInGroup_Modify_client;//FUN:ProtocalData::FUN_All METHOD:METHOD_Modify

typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
}PD_MemberInGroup_Query_client;//FUN:FUN_One METHOD:METHOD_Query

/*---client/server---*/

typedef struct
{
	unsigned int	meetingid;
	unsigned int	totalnum;
	//PD_GroupOfMember_common;  填充totalnum个PD_GroupOfMember_common
}PD_AllGroupOfMember_Query_server;//FUN:ProtocalData::FUN_All METHOD:METHOD_Query

typedef struct
{
	unsigned int	meetingid;
	unsigned int	groupid;
	unsigned int	num;
	//PD_PersonnelInfo  member;//填充num个member
}PD_MemberInGroup_Query_server;//FUN:FUN_One METHOD:METHOD_Query
/*-------------------参会人员分组end---------------------*/


//struct for deleting db records 
enum SRC_TYPE {
	SRC_MEETING = 1,
	SRC_MEMBER,
	SRC_ROOM,
	SRC_PEOPLE
};

typedef struct PD_DELSRC
{
	unsigned int srctype;
	unsigned int subid1;
	unsigned int subid2;
	unsigned int subid3;
	unsigned int subid4;
}PD_DELSRC;

typedef struct PD_SETMEMROLE
{
	unsigned int meetingid;
	unsigned int memberid;
	unsigned int role;
}PD_SETMEMROLE;

typedef struct PD_SETROOMBG
{
	unsigned int roomid;
	unsigned int bgid;
}PD_SETROOMBG;

typedef struct PD_SETDEVICEPOS
{
	unsigned int roomid;
	unsigned int deviceid;
	float		 x;
	float		 y;
	ProtocalData::SEAT_DIRECTION direction;
}PD_SETDEVICEPOS, PD_QUERYDEVICEPOS;

typedef struct PD_SETDEVICEPOSEX
{
	unsigned int roomid;
	unsigned int devnum;
}PD_SETDEVICEPOSEX;

typedef struct PD_SETDEVICEPOSEX_dev
{
	unsigned int deviceid;
	float		 x;
	float		 y;
	ProtocalData::SEAT_DIRECTION direction;
}PD_SETDEVICEPOSEX_dev;

typedef struct
{
	unsigned int roomid;
	unsigned int bgid;
}PD_ROOMBG;

typedef struct
{
	unsigned int managerid;
	unsigned int roomnum;
}PD_MGRROOM;

//会议统计
enum MEET_STATISTIC_TYPE {
	MEET_STATISTIC_FILEGET = 1,
	MEET_STATISTIC_SCREENGET,
	MEET_STATISTIC_STREAMGET,
	MEET_STATISTIC_CHATCOUNT,
	MEET_STATISTIC_SERVICECOUNT,
	MEET_STATISTIC_WBOPENCOUNT,
	MEET_STATISTIC_WBUSECOUNT,
	MEET_STATISTIC_VOTECOUNT,
	MEET_STATISTIC_ELECTIONCOUNT,
	MEET_STATISTIC_QUESTIONCOUNT,
	MEET_STATISTIC_BULLETCOUNT,
};

//更新会议统计
//stages:STAGE_MeetStatistic
//fun：FUN_One
//method:  METHOD_Add METHOD_Query
typedef struct
{
	unsigned int meetingid;//
	MEET_STATISTIC_TYPE type;//统计类型
}PD_MeetStatistic;

//返回查询会议统计
//stages:STAGE_MeetStatistic
//fun：FUN_One
//method:   METHOD_Query
typedef struct
{
	unsigned int meetingid;//会议ID
	char		 meetname[DESCRIBE_LENG];//会议ID
	unsigned int streamgetcount;
	unsigned int screengetcount;
	unsigned int filegetcount;
	unsigned int chatcount;
	unsigned int servicegetcount;
	unsigned int whiteboardopencount;
	unsigned int whiteboardusecount;

	unsigned int votecount;
	unsigned int electioncount;
	unsigned int questioncount;
	unsigned int bulletcount;
	unsigned long long addtime; //utc 秒
}PD_MeetOneStatistic;
typedef struct
{
	unsigned int meetingid;//会议ID
	char		 meetname[DESCRIBE_LENG];//会议ID
	unsigned int streamgetcount;
	unsigned int screengetcount;
	unsigned int filegetcount;
	unsigned int chatcount;
	unsigned int servicegetcount;
	unsigned int whiteboardopencount;
	unsigned int whiteboardusecount;

	unsigned int votecount;
	unsigned int electioncount;
	unsigned int questioncount;
	unsigned int bulletcount;
	unsigned long long addtime; //utc 秒
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	int8u  fill[2];
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_MeetOneStatistic_V2;

//时间段会议统计
enum MEET_STATISTIC_TIMEQUARTER {
	MEET_STATISTIC_BYMONTH = 1,//按月份查询,最多一次可查12个月
	MEET_STATISTIC_BYQUARTER,//按季度查询,最多一次可查12个季度
	MEET_STATISTIC_BYYEAR,//按年查询,最多一次可查12个年
};


//查询时间段会议统计
//stages:STAGE_MeetStatistic
//fun：FUN_All
//method:METHOD_QueryResult
typedef struct
{
	MEET_STATISTIC_TIMEQUARTER type;//统计时间段

	//统计时间段
	unsigned short startyear; //
	unsigned short startmonth; //按月查询才有效
	unsigned short endyear; //
	unsigned short endmonth; ////按月查询才有效

}PD_QueryStatistic_Quarter;

typedef struct
{
	//统计时间段
	unsigned short startyear; //
	unsigned short startmonth; //
	unsigned short endyear; //
	unsigned short endmonth; //

	unsigned int meetingcount;//总会议数

	//总计数
	unsigned int streamgetcount;
	unsigned int screengetcount;
	unsigned int filegetcount;
	unsigned int chatcount;
	unsigned int servicegetcount;
	unsigned int whiteboardopencount;
	unsigned int whiteboardusecount;
	unsigned int votecount;
	unsigned int electioncount;
	unsigned int questioncount;
	unsigned int bulletcount;
	
}PD_MeetOneStatisticItem;

//返回查询时间段会议统计
//stages:STAGE_MeetStatistic
//fun：FUN_One
//method:   METHOD_QueryResult
typedef struct
{
	MEET_STATISTIC_TIMEQUARTER type;//统计时间段
	unsigned int num;  //当前帧包括多少个信息

	//PD_MeetOneStatisticItem  //填充PD_MeetOneStatisticItem项投票信息
}PD_MeetStatistic_Quarter;

#define PUBLICINFO_DATALEN 260

//dataid
#define MAXHUB_SERVERURL      2 //MAXHUB白板系统的服务器地址 {"scanServerIp":"10.248.6.208","scanServerPort":"8889","serverIp":"10.248.6.11","serverPort":"61000"}
#define PUBLIINFO_DATACACHE   3 //会议资料缓存 {"enable":"1","size":"150"} enable:是否启用 size:超过指定大小的 单位:M
#define PUBLIINFO_MEETRECYCLE 4 //会议定期清理 {"enable":"1","day":"7"} enable:是否启用 day:清理指定天数前的 单位:天
#define PUBLIINFO_OFFICEBINDPDF 5 //office绑定的pdf文件 {"arr":[{"a":"0xb0000001","b":"0xb0000002"}]} a=office文件id,b=pdf文件Id V2协议才支持
#define MEET_PUBLIINFO_AGENDA_QUICKUSER 6   //  方图快捷添加汇报人or传达人{"contents"["1","2"]}
#define MEET_PUBLIINFO_AGENDA_QUICKMEMBER 7   // 方图快捷添加列席人员{"members"["1","2"]}
#define MEET_PUBLIINFO_MEETDBINI 8 //会议数据库后台client.ini文件 

//查询系统全局字串
//stages:STAGE_PublicInfo
//fun：FUN_All
//method: METHOD_Query
typedef struct
{
	int num;//

	//int32u dataid[num];
}PD_QueryPublicInfo;

//返回查询系统全局字串
//单个全局字串
typedef struct
{
	int32u dataid;//字串ID
	char   dataval[PUBLICINFO_DATALEN];
}PD_Item_PublicInfo;

//返回查询系统全局字串
//单个全局字串
typedef struct
{
	int32u dataid;//字串ID

	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_Item_PublicInfo_V2;

//stages:STAGE_PublicInfo
//fun：FUN_All
//method: METHOD_Query METHOD_Set
typedef struct
{
	int num;//

	//PD_Item_PublicInfo item[num];
}PD_PublicInfo;

//扫码加入会议
//stages:STAGE_MeetingSeat
//fun：FUN_One
//method: METHOD_Notify
typedef struct
{
	int32u meetingid;//扫码加入的会议ID
	int32u memberrole;//参会人角色
	PD_Member_Edit memberinfo;//参会人员的信息,如果参会人ID为0，表示新建一个参会人员

}PD_ScanEnterMeet;

//快速入会
//stages:STAGE_MeetingSeat
//fun：FUN_One
//method: METHOD_Start
typedef struct
{
	/*
	{
	
	}
	*/
	int32u meetid;
	int32u deviceid;
	char   phone[SHORT_DESCRIBE_LENG];  //电话

	//预留数据后面可能会进行使用
	int32u jsonlen;//长度=字符串长度+结尾字节
	//char   jsontext[jsonlen]; //需要增加一个字节0作字符串结尾
}PD_FastEnterMeet;

/*--------------------------------文件自定义选项评分 20180723-----------------------------------------*/
#define FILESCORE_VOTECONTENT_MAXLEN 200 //文件评分标题最大长度
#define FILESCORE_MAXITEM 4				 //文件评分选项最大个数
#define FILESCORE_MAXITEM_LEN 60				 //文件评分选项最大长度
typedef struct
{
	int32u voteid;//评分项ID，会议中唯一，用来标识删除和修改、发起、停止等操作
	int32u fileid;//文件ID
	char content[FILESCORE_VOTECONTENT_MAXLEN]; //投票内容 

	ProtocalData::VOTE_MODE mode; //匿名投票 记名投票

	unsigned int votestate;     //投票状态  参见 vote_notvote
	unsigned int timeouts;     //计时结束 单位：秒
	int64u starttime;//开时投票的时间 UTC秒
	int64u endtime;//结束投票的时间 UTC秒

	int32u shouldmembernum;  //应到人数
	int32u realmembernum;    //已投人数

	unsigned int selectcount;     //有效选项数量
	int32u itemsumscore[FILESCORE_MAXITEM];  //每个选项的目前总分--根据已投人数计算
	char voteText[FILESCORE_MAXITEM][FILESCORE_MAXITEM_LEN];  //描述文字
}PD_Item_UserDefineFileScore;

//文件自定义选项评分
//stages:STAGE_FileScoreVote
//fun：FUN_All
//method: METHOD_Add\METHOD_Modify\METHOD_Query
typedef struct
{
	unsigned int	MeetingId;
	unsigned int	TotalNum;
	unsigned int	StartId;
	unsigned int	CurrNum;
	//PD_Item_UserDefineFileScore;  填充curnum个PD_Item_UserDefineFileScore
}PD_UserDefineFileScore;

//删除文件自定义选项评分
//stages:STAGE_FileScoreVote
//fun：FUN_All
//method: METHOD_Delete、METHOD_Stop
typedef struct
{
	unsigned int	MeetingId;
	unsigned int	TotalNum;
	//int32u voteid[totalnum];//投票ID
}PD_DeleteUserDefineFileScore;

//文件自定义选项评分状态变更
typedef struct
{
	int32u	votestate;//参见 VOTING_FLAG_REVOTE
	int64u  starttime;//发起投票时间 UTC秒
	int64u  endtime;//结束投票时间 UTC秒
}PD_change_UserDefineFileScore;

//设置文件自定义选项评分投票的超时值
//stages：STAGE_FileScoreVote
//fun: FUN_All
//method:METHOD_Set,设置
typedef struct
{
	unsigned int MeetingId;  //会议ID
	unsigned int voiteid;  //投票ID
	unsigned int timeouts;  //超时值

}PD_MeetingUserDefineFileScoreTimeouts;

typedef struct
{
	unsigned int	voteid;
	unsigned int	voteflag;//参见 VOTING_FLAG_REVOTE
	unsigned int	timeouts;
	unsigned int	membernum;
	//int32u memberid[membernum];//人员ID
}PD_Item_StartUserDefineFileScore;

//发起文件自定义选项评分
//stages:STAGE_FileScoreVote
//fun：FUN_All
//method: METHOD_Start
typedef struct
{
	unsigned int	meetingid;
	PD_Item_StartUserDefineFileScore item;
}PD_StartUserDefineFileScore;

//收到发起文件自定义选项评分
//stages:STAGE_FileScoreVote
//fun：FUN_All
//method: METHOD_Start
typedef struct
{
	int32u meetingid;
	int32u voteid;//评分项ID，会议中唯一，用来标识删除和修改、发起、停止等操作
	int32u fileid;//文件ID
	char   content[FILESCORE_VOTECONTENT_MAXLEN]; //投票内容 

	ProtocalData::VOTE_MODE mode; //匿名投票 记名投票
	int32u timeouts; //计时结束 单位：秒
	int64u starttime;//开时投票的时间 UTC秒
	int32u voteflag;//参见 VOTING_FLAG_REVOTE
	
	int32u selectcount;     //有效选项数量
	char   voteText[FILESCORE_MAXITEM][FILESCORE_MAXITEM_LEN];  //描述文字

	int32u membernum;
	//int32u memberid[membernum];//人员ID
}PD_StartUserDefineFileScoreNotify;

typedef struct
{
	int32u voteid;//评分项ID，会议中唯一，用来标识删除和修改、发起、停止等操作
	int32u shouldmembernum;  //应到人数
	int32u realmembernum;    //已投人数
	int32u itemsumscore[FILESCORE_MAXITEM];  //每个选项的目前总分--根据已投人数计算
}PD_Item_FileScoreStatistic;

//文件自定义选项评分统计
//stages:STAGE_FileScoreVote
//fun：FUN_VoteCount
//method: METHOD_Query
typedef struct
{
	unsigned int	MeetingId;
	unsigned int	VoteNum;
	//PD_Item_FileScoreStatistic;  填充curnum个PD_Item_FileScoreStatistic
}PD_UserDefineFileScoreStatistic;

typedef struct
{
	int32u memberid;  //人员ID
	int32u state;  //是否已经提交 1表示已经提交 0未提交
	int32u score[FILESCORE_MAXITEM];  //每个选项的分--根据已投人数计算
	int64u votetime;//提交时间 UTC 秒
	char   content[FILESCORE_VOTECONTENT_MAXLEN]; //意见 

}PD_Item_FileScoreMemberStatistic;

//文件自定义选项评分人员统计
//stages:STAGE_FileScoreVote
//fun：FUN_VoteInfo
//method: METHOD_Query、METHOD_Add（提交投票）
typedef struct
{
	unsigned int	MeetingId;
	unsigned int    voteid;//评分项ID，会议中唯一，用来标识删除和修改、发起、停止等操作
	unsigned int	TotalNum;
	unsigned int	StartId;
	unsigned int	CurrNum;
	//PD_Item_FileScoreMemberStatistic;  填充curnum个PD_Item_FileScoreMemberStatistic
}PD_UserDefineFileScoreMemberStatistic;

//生物认证删除
typedef struct
{
	int32u memberid;//标识用-查询结果会返回该值
	char   phone[SHORT_DESCRIBE_LENG];  //电话-查询结果会返回该值
	char   idcard[SHORT_DESCRIBE_LENG];  //身份证号码-查询结果会返回该值
}PD_ZKIdentify_SingleItem;

#define IDENTIFY_FLAG_ISLASTFRAME 0x00000001 //标记为最后一帧
#define IDENTIFY_FLAG_NEEDDATA    0x00000002 //标记只返回数据的长度

//stages:STAGE_ZKIDENTIFY
//fun：FUN_One
//method:METHOD_Delete\METHOD_Query
typedef struct
{
	int    queryflag;//查询标志 
	int	   num; //当方法为删除时,为0表示删除全部

	//PD_ZKIdentify_SingleItem item[];
}PD_ZKIdentify_Simple;

//生物认证
//stages:STAGE_ZKIDENTIFY
//fun：FUN_One
//method: METHOD_Query（返回查询结果）、METHOD_Modify
typedef struct
{
	int    flag;//IDENTIFY_FLAG_ISLASTFRAME
	int    fingerimg1len;//1手指指纹模板 为0表示没有模板数据
	int    fingerimg2len;//2手指指纹模板 为0表示没有模板数据
	int    faceimglen;//人脸模板 为0表示没有模板数据

	int64u addtime;//录入的时间-查询返回时有效
	PD_ZKIdentify_SingleItem base;
	
}PD_ZKIdentify_Oper;


// 短信服务
#define  MAX_PHONE_NUMBER    100  // 一次可发送的最大数量
#define  PHONE_NUMBER_LEN    12   // 手机号码长度
#define  MAX_MSG_LEN         300  // 短信最大字符数
#define  DEFAULT_MSG_LEN     40   // 默认字符串长度

// 请求用
typedef struct  
{
	char  serverIP[DEFAULT_MSG_LEN];                    // 服务器IP
	char  serverPort[DEFAULT_MSG_LEN];                  // 服务器端口号
	char  ctrlType[DEFAULT_MSG_LEN];                    // 短信服务类型（发送，查询等）

	char  uname[DEFAULT_MSG_LEN];                       // 短信服务账户 名称
	char  upwd[DEFAULT_MSG_LEN];                        // 短信服务账户 密码
	char  numbers[MAX_PHONE_NUMBER][PHONE_NUMBER_LEN];  // 电话号码
	char  messageInfo[MAX_MSG_LEN];                     // 短信内容
}PD_MSNInfo, *pPD_MSNInfo;

// 接收响应 用
typedef struct 
{
	int  statuscode;  // 状态码（表示请求结果）
}PD_ItemMsnCode, *pPD_ItemMsnCode;

// 查询余量（剩余可发送条数）
typedef struct 
{
	PD_ItemMsnCode  recvCode;

	int  account;  // 剩余短信条数
}PD_MsnLastCnt, *pPD_MsnLastCnt;

//////////////////////////// topic 议题 //////////////////////////////////////////////
//时间轴式会议议程
//status
#define MEETTOPIC_STATUS_OFF		0 //未开始
#define MEETTOPIC_STATUS_ON         1 //开始
typedef struct
{
	int32u  topiciid;  //议题ID
	int32u  status;  //议题状态 参见 MEETTOPIC_STATUS_OFF
	int32u  fileid;  //绑定文件ID
	int64u	startutctime;//单位秒
	int64u	endutctime;  //单位秒
	char	topicname[DESCRIBE_LENG]; //议题名称
	char	reporter[NAME_LENG];//汇报单位
}PD_TopicItemInfo;

//stages：STAGE_MeetTopic
//fun: FUN_All 会议议题
//method:查询、增加、修改、删除、设置
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u TotalNum;//总共有多少个议题
	int32u StartId; //当前帧开始议题序号
	int32u CurrNum;//当前帧包括多少个议题信息
	//PD_TopicItemInfo; //根据前面总数列写PD_TopicItemInfo

}PD_MeetTopics;

//stages：STAGE_MeetTopic
//fun: FUN_All 会议议题
//method:METHOD_Control
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u num;//

	//int32u topicids[num];//议题ID的顺序
}PD_SetMeetTopicsPos;

//////////////////////////// topic group //////////////////////////////////////////////
typedef struct
{
	int32u  topiciid;  //议题ID
	int32u  groupid;  //分组ID
	char	groupname[NAME_LENG];//分组名称
}PD_TopicGroupItemInfo;

//stages：STAGE_MeetTopic
//fun: FUN_One 会议议题
//method:查询、增加、修改、删除
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u TotalNum;//总共有多少个议题
	int32u StartId; //当前帧开始议题序号
	int32u CurrNum;//当前帧包括多少个议题信息
	//PD_TopicGroupItemInfo; //根据前面总数列写PD_TopicGroupItemInfo

}PD_MeetTopicGroups;

//////////////////////////// topic nopermission //////////////////////////////////////////////
typedef struct
{
	int32u  topiciid;  //议题ID
	int32u  memberid;  //参会人员ID
}PD_TopicPermItemInfo;

//stages：STAGE_MeetTopic
//fun: FUN_MemberPermission 会议议题
//method:查询、增加、修改、删除
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u topicid;
	int32u TotalNum;//总共有多少个议题
	int32u StartId; //当前帧开始议题序号
	int32u CurrNum;//当前帧包括多少个议题信息
	//PD_TopicPermItemInfo; //根据前面总数列写PD_TopicPermItemInfo

}PD_MeetTopicPerms;

//////////////////////////// lecture //////////////////////////////////////////////
typedef struct
{
	int32u  fileid;  //议题ID
	int32u  memberid;  //参会人员ID
}PD_LectureItemInfo;

//stages：STAGE_Lecture
//fun: FUN_All 会议讲稿
//method:查询、增加、修改、删除
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u TotalNum;//总共有多少个议题
	int32u StartId; //当前帧开始议题序号
	int32u CurrNum;//当前帧包括多少个议题信息
	//PD_LectureItemInfo; //根据前面总数列写PD_LectureItemInfo

}PD_MeetLecture;

//////////////////////////// home page //////////////////////////////////////////////
typedef struct
{
	int32u  MeetingId;  //会议ID
	int32u  fileid;  //图片ID
	int32u  fontsize;  //字体大小
	int32u  fontcolor;//字体颜色
	char    fontname[NAME_LENG];//字体名称
	int32u  deviceflag;//设备的全局标志参见 owbase.h 宏定义 MEETDEVICE_FLAG
}PD_HomePageItemInfo;

//stages：STAGE_HomePage
//fun: FUN_One 会议欢迎界面
//method:查询、修改
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u TotalNum;//总共有多少个
	int32u StartId; //当前帧开始序号
	int32u CurrNum;//当前帧包括多少个信息
	//PD_HomePageItemInfo; //根据前面总数列写PD_HomePageItemInfo

}PD_HomePage;

//////////////////////////// 会议用户自定数据 //////////////////////////////////////////////
#define MeetUserDef_SLOGAN		 1 //会议标语

#define MeetUserDef_APPROVALLOG  2 //会议审批日志

//data数组类型 asktm申请审批的时间 utc秒， askid申请的adminid，askmsg申请原因，apptm审批的时间 utc秒，appid审批管理员id,status本次审批的状态参见MEET_APPROVAL_IDLE定义，appmsg本次审批的备注
//{"data":[{"asktm":"178959632","askid":5,"askmsg":"用于部门讨论","apptm":"17892213","appid":1,"status":1,"appmsg":"审批通过"}]}


typedef struct
{
	int32u id;//数据id
	int8u fill[2];
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_MeetUserdefItemInfo;

//stages：STAGE_MeetUserdef
//fun: FUN_One 会议欢迎界面
//method:删除、查询、修改
typedef struct
{
	int32u MeetingId;  //会议ID
	int32u TotalNum;//总共有多少个
	int32u StartId; //当前帧开始序号
	int32u CurrNum;//当前帧包括多少个信息
	//PD_MeetUserdefItemInfo; //根据前面总数列写PD_MeetUserdefItemInfo

}PD_MeetUserdefInfo;

//////////////////////////// 会场用户自定数据 //////////////////////////////////////////////
typedef struct
{
	int32u id;//数据id
	int8u fill[2];
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_RoomUserdefItemInfo;

//stages：STAGE_RoomUserdef
//fun: FUN_One 会议欢迎界面
//method:删除、查询、修改
typedef struct
{
	int32u roomid;  //会场ID
	int32u TotalNum;//总共有多少个
	int32u StartId; //当前帧开始序号
	int32u CurrNum;//当前帧包括多少个信息
	//PD_RoomUserdefItemInfo; //根据前面总数列写PD_RoomUserdefItemInfo

}PD_RoomUserdefInfo;

#define FASTMEET_AGENDAFLAG_DIR  0x00000001 //议程自动关联目录
#define FASTMEET_AGENDAFLAG_VOTE 0x00000002 //议程自动关联投票
#define FASTMEET_AGENDAFLAG_SHOW 0x00000004 //议程添加时设为隐藏
#define FASTMEET_AGENDAFLAG_PSW  0x00000008 //议程添加时设为隐藏

//stages：STAGE_StartUpMeeting
//fun: FUN_One 会议欢迎界面
//method:METHOD_Report 快速创建会议
typedef struct
{
	/*
	{
	"name":"会议名称",
	"type":"0",//会议类型
	"status":"0",//会议状态 可选 参见MEETING_STATUS 仅新增才有效
	"starttime":"",//UTC 秒数
	"endtime":"",//UTC 秒数
	"signin_type":"0",//签到类型
	"managerid":"0",//管理ID
	"passwd":"",//签到密码  可选
	"ordername":"",//预约者 可选
	"agendatype":"",//议题类型 可选
	"agendadesc":"",//文本议题 可选
	"agendafile":"",//文件议题 可选
	"nomember":1,//不添加参会人
	"member":  //可选
	[
	{"name::"陈工","company":"xx","job":"xx","phone","123456","password":"123456","perm":"0xff","role":3,"devid":"0x1100000"}
	{"name::"陈工","company":"xx","job":"xx","phone","123456","password":"123456","perm":"0xff","role":4,"devid":"0x1100001"},
	{"name::"陈工","company":"xx","job":"xx","phone","123456","password":"123456","perm":"0xff","role":1,"devid":"0x1100002"}
	],
	"agenda":  //议题 可选
	[
	{
	"flag":"",//参见 FASTMEET_AGENDAFLAG_DIR
	"desc:"",//议题内容 不限长，可以使用json格式，为json时a 是议题标题
	"starttime":"",//议题设置的开始时间 UTC 秒数 eg:1683854858 可选
	"endtime":"",//议题设置的结束时间  UTC 秒数 eg:1683891858 可选
	"passwd":"",//议题设置访问密码 限长8字节 可选
	"perm":[0,2]//议题黑名单，数值对应了Member中的参会人索引从0开始，指定member时才有效，会设置到目录权限中
	}
	],
	"userdef":  //会议自定义数据 可选
	[
	{
	"id":"1",//数据id
	"text:"",//数据 不限长，可以使用json格式
	}
	],
	}
	*/
	int64u buildtime;//微秒级UTC时间，创建完毕后会将该值返回
	int32u roomid;//会场ID
	int32u jsonlen;//长度=字符串长度+结尾字节
	//char   jsontext[jsonlen]; //需要增加一个字节0作字符串结尾

}PD_FastCreateMeetInfo;

//会议名信息组成
typedef struct
{
	int64u buildtime;       //微秒级UTC时间，创建完毕后会将该值返回
	int32u senddeviceid;    //创建者的设备ID
	int32u id;				//会议编号
	int32u roomId;			//会议室ID
	int32u status;			//会议状态 MEETING_STATUS
	int32u managerid;		//管理员id
	PD_Time startTime;
	PD_Time endTime;
	char   meeting_psw[ONESIGNPASSWORD_LENG];				//会议签到密码 明文utf8
	char   ordername[NAME_LENG];							//会议预约人员名称
	int8u  type;			 //会议类型 TYPE_MEETING
	int8u  signin_type;		 //签到类型 TYPE_SIGNIN
	int16u namelen;			 //名称长度=字符串长度+结尾字节
	//char name[namelen];    //名称需要增加一个字节0作字符串结尾
}PD_FastCreateMeetInfo_OK;


//////////////////////////会议预约////////////////////////////////////////////////
//stages：STAGE_MeetOrder
//fun: FUN_One 会议预约
//method:METHOD_Add 添加|修改
typedef struct
{
	/*
	{
	"roomid":"1",
	"name":"会议名称",
	"mode":"",//预约模式 可选
	"type":"0",//会议类型 可选
	"starttime":"",//UTC 秒数
	"endtime":"",//UTC 秒数
	"signin_type":"0",//签到类型
	"passwd":"",//签到密码  可选
	"ordername":"",//预约者 可选
	"desc":"",//请求的理由  可选
	"member":  //可选
	[
	{"name::"陈工","company":"xx","job":"xx","phone":"123456","password":"123456"}
	{"name::"陈工","company":"xx","job":"xx","phone":"123456","password":"123456"},
	{"name::"陈工","company":"xx","job":"xx","phone":"123456","password":"123456"}
	],
	}
	*/
	int32u markid;
	int64u buildtime;//UTC 微秒
	char   phone[SHORT_DESCRIBE_LENG];  //电话
	int32u jsonlen;//长度=字符串长度+结尾字节
	//char   jsontext[jsonlen]; //需要增加一个字节0作字符串结尾

}PD_MeetOrderInfo;

//会议预约录入成功
typedef struct
{
	int32u markid;
	int64u buildtime;//UTC 微秒
	int32u senddeviceid;    //创建者的设备ID
	int32u id;				//会议编号
	char   phone[SHORT_DESCRIBE_LENG];  //电话
	int32u jsonlen;//长度=字符串长度+结尾字节
	//char   jsontext[jsonlen]; //需要增加一个字节0作字符串结尾

}PD_MeetOrderInfo_Reture;

typedef struct
{
	int32u id;				//会议编号
	int64u buildtime;//UTC 微秒
	char   phone[SHORT_DESCRIBE_LENG];  //电话
	int8u  status;//审批状态
	int8u  fill;//
	int16u reasonlen;//审批回执 长度=字符串长度+结尾字节
	int32u jsonlen;//长度=字符串长度+结尾字节
	//char   reason[reasonlen]; //需要增加一个字节0作字符串结尾
	//char   jsontext[jsonlen]; //需要增加一个字节0作字符串结尾

}PD_MeetOrderItemInfo;

typedef struct
{
	int32u num;
	//PD_MeetOrderItemInfo item[num];

}PD_MeetOrderInfo_Query;

typedef struct
{
	int64u starttime;//UTC 微秒
	int64u endtime;//UTC 微秒
	char   phone[SHORT_DESCRIBE_LENG];  //电话
	int8u  status;//审批状态
	int8u  fill[3];//
	char   faststr[DESCRIBE_LENG];  //对预约内容进行模糊搜索
}PD_MeetOrder_ComplexQuery;

//////////////////////////20230313 add ////////////////////////////////////////////////
#define SWITCH_NEWVOTE 0 //使用新投票代替旧投票 这个是针对议程目录文件绑定的投票关联以及解除

#define AVOTEBASE_TYPE_VOTE  0x00 //投票
#define AVOTEBASE_TYPE_ELE   0x01 //选举
#define AVOTEBASE_TYPE_QUE   0x02 //问卷调查
 
#define AVOTEBASE_MODE_AGONYMOUS   0x00 //匿名
#define AVOTEBASE_MODE_SIGNED      0x01 //记名

#define AVOTEBASE_SELTYPE_SINGLE   0x00 //单选
#define AVOTEBASE_SELTYPE_MANY     0x01 //多选

#define AVOTEBASE_STATUS_IDLE     0x00 //未发起的投票
#define AVOTEBASE_STATUS_ING      0x01 //正在进行的投票
#define AVOTEBASE_STATUS_END      0x02 //已经结束的投票

//投票添加，修改指定的标志
#define AVOTEBASE_FLAG_MARK		0x0000001 //表示投票要提交备注
#define AVOTEBASE_FLAG_SCORE	0x0000002 //表示投票要提交选项分数
#define AVOTEBASE_FLAG_START	0x0000004 //表示添加成功后立即发起
#define AVOTEBASE_FLAG_DIRID	0x0000008 //表示添加时从目录ID获取投票发起的参会人
#define AVOTEBASE_FLAG_SIGNPNG  0x0000010 //表示提交时需要签名
#define AVOTEBASE_FLAG_PSW		0x0000020 //表示提交时需要参会人密码认证
#define AVOTEBASE_FLAG_RAND		0x0000040 //表示投票发起选项随机显示

//发起投票标志
#define AVOTING_FLAG_NOPOST		0x00000001 //不在投影机上显示投票结果
#define AVOTING_FLAG_SECRETARY	0x00000002 //投票选项保密投票模式
#define AVOTING_FLAG_FINISHEXIT	0x00000004 //全部提交完成后立即结束
#define AVOTING_FLAG_REVOTE		0x00000008 //重投 清空之前的记录


typedef struct
{
	int32u voteflag; //发起投票标志 AVOTING_FLAG_NOPOST
	int32u timeouts; //计时结束 单位：秒
	int32u membernum;//参与投票的参会人员数
	//int32u members[];
}PD_AVoteStartFlag;

typedef struct
{
	int32u voteid; //投票ID
	int32u fileid;//
	int8u  type;//AVOTEBASE_TYPE_VOTE
	int8u  mode;//AVOTEBASE_MODE_AGONYMOUS
	int8u  seltype;// AVOTEBASE_SELTYPE_SINGLE
	int8u  state;  //投票状态 未发起=0 进行中=1 结束=2
	int32u timeouts;     //计时结束 单位：UTC秒
	int8u  per;//投票通过率 选项1已投人数占应到人数的比例
	int8u  fill[3];

	int32u flag;//AVOTEBASE_FLAG_MARK
	int32u jsonlen;//长度=字符串长度+结尾字节

	//char json[jsonlen]; //投票内容 
	//{"content":"","item":["1","2"],"dirid":0}
}PD_AVoteBaseInfo;

#define AVOTE_SELFLAG_CHECKIN  0x00000001 //该位为1表示已经签到
#define AVOTE_SELFLAG_SUBMIT   0x00000002 //该位为1表示已经提交
#define AVOTE_SELFLAG_SIGNDATA 0x00000004 //该位为1表示有签名
#define AVOTE_SELFLAG_PSWCHECK 0x00000008 //该位为1表示经过参会人密码认证

typedef struct
{
	int32u voteid; //投票ID
	int32u memberid;//提交的人员ID

	int32u flag;//AVOTE_FLAG_SIGN
	int32u jsonlen;//长度=字符串长度+结尾字节
	int32u pnglen;//签名

	//char json[jsonlen]; //投票内容 
	//{"mark":"","item":[{"a":1,"b":"1"},{"a":2,"b":"1"}]} 
	//char   pngdata[pnglen];
}PD_AVoteSubmitInfo;

typedef struct
{
	int32u memberid;

	int32u flag;//AVOTE_FLAG_SIGN
	int32u jsonlen;//长度=字符串长度+结尾字节
	int32u pnglen;//签名

	//char json[jsonlen]; //投票内容 
	//{"mark":"","item":[{"a":1,"b":"1"},{"a":2,"b":"1"}]} 
	//char   pngdata[pnglen];
}PD_AVoteStaticInfo;

//stages:STAGE_AVote
//fun：FUN_VoteRecord
//method: METHOD_Query
typedef struct
{
	int32u	MeetingId;
	int32u  voteid;
	int32u	Num;
	//PD_AVoteStaticInfo;  填充curnum个PD_AVoteStaticInfo
}PD_AVoteStatic;

//stages:STAGE_AVote
//fun：FUN_All
//method: METHOD_Add\METHOD_Modify\METHOD_Query
typedef struct
{
	int32u	MeetingId;
	int32u	TotalNum;
	int32u	StartId;
	int32u	CurrNum;
	//PD_AVoteBaseInfo;  填充curnum个PD_AVoteBaseInfo
}PD_AVote;

//stages:STAGE_AVote
//fun：FUN_Vote
//method: METHOD_Delete、METHOD_Stop
typedef struct
{
	int32u	MeetingId;
	int32u	TotalNum;
	//int32u voteid[totalnum];//投票ID
}PD_DeleteAVote;

typedef struct
{
	int32u meetid;
	int32u voteflag; //发起投票标志 AVOTING_FLAG_NOPOST
	int32u timeouts; //计时结束 单位：秒
	int32u votenum;
	int32u memnum;//参与投票的参会人员数

	//int32u voteid[votenum];
	//int32u members[memnum];
}PD_AVoteStartInfo;

//orderid
#define NEWVOTE_ORDERID_FTJDPT 1
//1=方图戒毒平台订制投票推送
//stages:STAGE_AVote
//fun：FUN_VoteInfo
//method: METHOD_Notify
typedef struct
{
	int32u  orderid;//NEWVOTE_ORDERID_FTJDPT
	int32u  jsonlen;//长度=字符串长度+结尾字节

	//char json[jsonlen]; // 
	//{"meetid":1,"voteid":[1,2,3]} //voteid 为空表示全部投票
}PD_NotifyAVoteDetail;

///////////////////////////////20230321///////////////////////////////////////////
//这个结构用于表示方案关联的ID的类型，可以是议程、目录，文件等不同的类型
typedef struct
{
	int8u  type;//=0 agenda,=1 dir,=2 file
	int8u  fill[3];
	int32u id;//type=0是，这个是议程的ID
}PD_SeatPlanBindItem;

typedef struct
{
	int32u memberid;//
	int32u deviceid;
	int8u  role;//参会人角色 role_admin
	int8u  fill[3];
}PD_SeatPlanMemItem;

typedef struct
{
	int32u planid;//方案id
	int32u flag;
	int32u jsonlen;//
	//char   json[jsonlen + 1];//{"name":""}
	int32u bindnum;
	//PD_SeatPlanBindItem binditem[num];//方案的绑定的

	int32u memnum;
	//PD_SeatPlanMemItem memitem[num];//方案的参会人员
}PD_SeatPlanItem;

//stages:STAGE_SeatPlan
//fun：FUN_All
//method: METHOD_Query
typedef struct
{
	int32u meetid;

	int32u num;
	//PD_SeatPlanItem item[num];//方案
}PD_SeatPlan;

//stages:STAGE_SeatPlan
//fun：FUN_All
//method: METHOD_Add,METHOD_Modify
typedef struct
{
	int32u meetid;

	int64u markid;//添加时指定的标识ID，成功添加后会返回该值
	int32u planid;//方案id
	int32u flag;
	int32u jsonlen;//
	//char   json[jsonlen + 1];//{"name":""}
}PD_AddSeatPlan;

//stages:STAGE_SeatPlan
//fun：FUN_All
//method: METHOD_Delete,METHOD_Start
typedef struct
{
	int32u meetid;

	int32u num;
	//int32u planid[num];//方案id
}PD_DelSeatPlan;

//stages:STAGE_SeatPlan
//fun：FUN_One
//method: METHOD_Set
typedef struct
{
	int32u meetid;
	int32u planid;//方案id

	int32u bindnum;
	//PD_SeatPlanBindItem binditem[num];//方案的绑定的
}PD_ModSeatPlanBindInfo;

//stages:STAGE_SeatPlan
//fun：FUN_One
//method: METHOD_Add,METHOD_Modify,METHOD_Delete
typedef struct
{
	int32u meetid;
	int32u planid;//方案id

	int32u memnum;
	//PD_SeatPlanMemItem memitem[num];//方案的参会人员
}PD_ModSeatPlanMemInfo;

//自定义json协议
//stages：*
//fun: *
//method:*
typedef struct
{
	int32u deviceid;//提交的设备ID
	int64u markid;//utc微秒数或者其它用户的标识ID
	int32u MeetingId;  //会议ID
	int    jsonlen;//json + 1
	//char   json[];//jsonlen 参见meetuserdef.pro的协议定义
}PD_SmartJsonProtol;

//自定义json协议
//stages：*
//fun: *
//method:*
#define SMARTJSON_FLAG_FIRST 0x00000001 //是否为第一个包
#define SMARTJSON_FLAG_LAST  0x00000002 //是否为最后一个包
#define SMARTJSON_FLAG_7Z    0x00000004 //是否压缩数据

typedef struct
{
	int32u total;//总包数
	int32u cur;//当前包的索引
	int32u flag;//SMARTJSON_FLAG_FIRST
	int32u deviceid;//提交的设备ID
	int64u markid;//utc微秒数或者其它用户的标识ID
	int32u MeetingId;  //会议ID
	int    jsonlen;//json + 1
	//char   json[];//jsonlen 参见meetuserdef.pro的协议定义
}PD_SmartJsonProtolEx;
///////////////参会人员文件自定义数据///////////////////////////////////////////////////////////

//stages：STAGE_MemFileUserInfo
//fun: FUN_One, 单个
//method:查询|删除
typedef struct
{
	int32u meetid;//操作的会议ID 为0表示不作为查询条件
	int8u  memrole;//角色 为0表示不作为查询条件
	int8u  fill[3];
	int32u memid;//人员ID 为0表示不作为查询条件
	int32u dirid;//目录ID 为0表示不作为查询条件
	int32u fileid;//文件ID 为0表示不作为查询条件
	int32u flag;//类型标志 为0表示不作为查询条件
	int32u param;//根据操作对应的操作参数，用于快速统计
	int32u startrow;//查询开始行 实现分页查询 必须有效
	int32u matchtextlen;//为0表示不作为查询条件
	//char text[matchtextlen];
}PD_DoMeetOtherInfo;

//stages：STAGE_MemFileUserInfo
//fun: FUN_One, 单个
//method:添加、查询(数据库返回)
typedef struct
{
	int32u totalrecord;//本次查询总记录数
	int32u startrow;//查询返回用户传过来的开始行
	int32u CurrNum;//当前帧包括多少个文件信息
	//PD_Item_MeetOtherInfo; //根据前面总数列写PD_Item_MeetOtherInfo
}PD_MeetOtherInfo;

typedef struct
{
	int32u meetid;//
	int8u  memrole;//角色 
	int8u  fill[3];
	int32u memid;//人员ID 
	int32u dirid;//目录ID
	int32u fileid;//文件ID
	int32u flag;//类型标志
	int32u param;//根据操作对应的操作参数，用于快速统计
	int    jsonlen;//json + 1
	//char   json[];//jsonlen 类型对应的json协议定义

}PD_Item_MeetOtherInfo, *pPD_Item_MeetOtherInfo;

///////////////////////////审批 start  未实现///////////////////////////////////////////////
//审批类型
#define  APPROVAL_TYPE_MEET		0 //会议
#define  APPROVAL_TYPE_AGENDA	1 //议程
#define  APPROVAL_TYPE_DIR		2 //目录
#define  APPROVAL_TYPE_DIRFILE	3 //文件
#define  APPROVAL_TYPE_VOTE		4 //投票
#define  APPROVAL_TYPE_MEMBER	5 //参会人

//审批流转状态
#define  APPROVAL_STATUS_PED		0 //发起审批并等待审批
#define  APPROVAL_STATUS_ING		1 //审批中
#define  APPROVAL_STATUS_OK			2 //审批通过
#define  APPROVAL_STATUS_FAILED		3 //审批不通过

//审批结果结果码
#define  APPROVAL_CODE_OK		0 //成功
#define  APPROVAL_CODE_TIME		1 //时间冲突

//审批优先级
#define  APPROVAL_PRI_L0		0 //普通级别
#define  APPROVAL_PRI_L1		1 //加急
#define  APPROVAL_PRI_L2		2 //紧急
#define  APPROVAL_PRI_L3		3 //突发

//stages：*
//fun: *
//method:*
typedef struct
{
	int8u  type;//审批类型 APPROVAL_TYPE_MEET 
	int8u  pri;//审批优先级  APPROVAL_PRI_L0
	int8u  fill[2];//
	int32u adminid;//指定管理员审批 为0表示默认所有管理可审批
	int32u flag;//
	int16u status;//APPROVAL_STATUS_PED
	int16u code;  //APPROVAL_CODE_OK
	int32u id1; //根据APPROVAL_TYPE_MEET来结合使用
	int32u id2;
	int32u id3;
	int32u id4;
	int32u id5;
	int32u id6;
	int16u times;//重复发起审批的次数
	int32u unactivetime;//utc秒数 指定审批失效的时间，为0表示永久有效
	int64u addtime;//utc秒数 创建的时间

	int    jsonlen;//json + 1
	//char   json[];//jsonlen
	/*
	{
		"logs":[
		{"sj":"12345678","bz":"发起审批"}
		]
	}
	*/
}PD_Approval;
/////////////////////////审批 end/////////////////////////////////////////////////

//////////////////////////// 复合自定义数据 2026.4.20//////////////////////////////////////////////
#define OTHER_USERINFO_FLAG_CLEARSAVE  0x000000001//表示清空再修改，否则就是更新

//typ最大值不能超过65535
//datid最大值不能超过65535
#define COMPLEX_PUBLIC_TYP_ADMIN   1//typ=1指管理员,id1是管理员id
#define COMPLEX_PUBLIC_TYP_PEOPLE  2//typ=2指常用人员,id1是常用人员id
#define COMPLEX_PUBLIC_TYP_DEVICE  3//typ=3指设备,id1是设备id
#define COMPLEX_PUBLIC_TYP_MEET    4//typ=3指会议,id1是会议id

//typ=COMPLEX_PUBLIC_TYP_ADMIN
#define CMX_PUBFILE_DATAID_BASE 1 //管理员的基本信息
/*
{
}
*/

//typ=COMPLEX_PUBLIC_TYP_PEOPLE
#define CMX_PUBPEOPLE_DATAID_BASE 1 //常用人员的基本信息
/*
{
}
*/

//typ=COMPLEX_PUBLIC_TYP_DEVICE
#define CMX_PUBDEVICE_DATAID_BASE 1 //设备的基本信息
/*
{
}
*/

//typ=COMPLEX_PUBLIC_TYP_MEET
#define CMX_PUBMEET_DATAID_BASE 1 //会议的基本信息

#define CMX_PUBMEET_REGUMODE_EVERYDAY 0//每天
#define CMX_PUBMEET_REGUMODE_WEEK	  1//按周
#define CMX_PUBMEET_REGUMODE_MONTH	  2//按月

/* json用Compact（紧凑/压缩）风格减少存储空间
{
"regularenable":0,//是否启用 =1启用，=0不启用
"lastyday":130,//记录最近一次创建的年日struct tm.tm_year
//指定次数的停止时间的计算方法每天则是+24*60*60,按周+7*24*60*60，按月则是+30*24*60*60
"stoptime":"456789233",//UTC秒，截止日期 为空或者0表示不限制 
"mode":0,//0=每天，1=按周，2=按月 
"days"[1,5],//代表一周、一月的第几天1=周一|某月的1号
"before":"1800"//提前多少秒创建
}
*/

//stages：STAGE_ComplexPublicUserInfo
//fun: FUN_One 
//method:查询、修改

/*修改使用PD_SmartJsonProtol
{
"typ":1,//COMPLEX_PUBLIC_TYP_ADMIN
"flag":1,//OTHER_USERINFO_FLAG_CLEARSAVE表示针对typ对应的项执行清空再修改，否则就是更新
"data":[
{"id1":"1","item":[{"dataid":"1","str":"abc"},{"dataid":2,"str":"efg"}]},//如果是删除则不需要设置str字段值
{"id1":"2","item":[{"dataid":"1","str":"abc"},{"dataid":2,"str":"efg"}]}

]
}
*/
/*
查询使用PD_SmartJsonProtol，仅支持查询全部
返回查询使用PD_SmartJsonProtolEx
{
"data":[
{"typ":1,"id1":"1,"dataid":"1","str":"abc"},
{"typ":2,"id1":"1,"dataid":"2","str":"abc"},
{"typ":1,"id1":"2,"dataid":"3","str":"abc"},
]
}
*/

//会根据typ和id1，在删除指定的id1时会关联删除这些自定义数据
//typ最大值不能超过255
//datid最大值不能超过255
#define COMPLEX_MEET_TYP_MEMBER		1///typ=1指参会人员,id1是人员id，id2=0,id3=0
#define COMPLEX_MEET_TYP_AGENDA		2//typ=2指议题,id1是议题id，id2=0,id3=0
#define COMPLEX_MEET_TYP_DIR		3//typ=3指目录,id1是目录id，id2=0,id3=0
#define COMPLEX_MEET_TYP_FILE		4//typ=4指目录文件,id1是目录id，id2=文件id,id3=0
#define COMPLEX_MEET_TYP_MEMFILE	5//typ=5指参会人文件,id1是参会人员id，id2=目录id,id3=文件id
#define COMPLEX_MEET_TYP_VOTE		6//typ=6指投票,id1是投票id，id2=0,id3=0
#define COMPLEX_MEET_TYP_NEWVOTE	7//typ=6指新投票,id1是投票id，id2=0,id3=0

//约定各typ下的dataid=1用于存放基本信息
//typ=COMPLEX_MEET_TYP_MEMBER
#define CMX_MEET_MEM_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//typ=COMPLEX_MEET_TYP_AGENDA
#define CMX_MEET_AGENDA_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//typ=COMPLEX_MEET_TYP_DIR
#define CMX_MEET_DIR_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//typ=COMPLEX_MEET_TYP_FILE
#define CMX_MEET_FILE_DATAID_BASE 1 //目录文件的基本信息
/*
{
"pagenum":3,//页码
}
*/
//typ=COMPLEX_MEET_TYP_MEMFILE
#define CMX_MEET_MEEFILE_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//typ=COMPLEX_MEET_TYP_VOTE
#define CMX_MEET_VOTE_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//typ=COMPLEX_MEET_TYP_NEWVOTE
#define CMX_MEET_NEWVOTE_DATAID_BASE 1 //目录文件的基本信息
/*
{
}
*/
//stages：STAGE_ComplexMeetUserInfo
//fun: FUN_One 
//method:查询、修改

/*修改使用PD_SmartJsonProtol
{
"typ":1,//COMPLEX_MEET_TYP_MEMBER
"flag":1,//OTHER_USERINFO_FLAG_CLEARSAVE表示针对typ的项清空再修改，否则就是更新
"data":[
{"id1":"1,"id2":"0","id3":"0","item":[{"dataid":"1","str":"abc"},{"dataid":"2","str":"efg"}]},//如果是删除则不需要设置str字段值
{"id1":"2,"item":[{"dataid":"1","str":"abc"},{"dataid":"2","str":"efg"}]},

]
}
*/

/*
查询使用PD_SmartJsonProtol，仅支持查询指定会议的全部
返回查询使用PD_SmartJsonProtolEx
{
"data":[
{"typ":1,"id1":"1,"id2":"0","id3":"0","dataid":"1","str":"abc"},
{"typ":2,"id1":"1,"id2":"1","id3":"0","dataid":"2","str":"abc"},
{"typ":1,"id1":"2,"id2":"0","id3":"1","dataid":"3","str":"abc"},
]
}
*/


//stages：STAGE_NewSystemLog
//fun: FUN_One 
//method:查询、修改
/*
//添加用PD_SmartJsonProtol
{

"data":[
{"pageid":1,"operid":1,"meetid":1,"roomid":1,"devid",:"0x110000","urole":1,"uid","text":"test", "time":"123145645"}
]
}
*/

/*
//查询使用PD_SmartJsonProtol
//查询 字段指定值不为0是指定查询,为0表示匹配全部,maxrow默认100，每次不能超过500
//startrow表示从第几条记录返回,获取下一页时使用
{"startrow":0,"maxrow":100, "pageid":1,"operid":1,"meetid":1,"roomid":1,"devid",:"0x110000","urole":1,"uid","text":"test", "start":"123145645","end":"123145645"}

返回查询使用PD_SmartJsonProtol
{
"total":999,//本次查询的记录条数
"maxrow":"100",//单次最多查询多少行，默认100条记录
"start":0,//开始的行号
"data":[
{"pageid":1,"operid":1,"meetid":1,"roomid":1,"devid",:"0x110000","urole":1,"uid":1,"text":"test", "time":"123145645"},
{"pageid":1,"operid":1,"meetid":1,"roomid":1,"devid",:"0x110000","urole":1,"uid":1,"text":"123456", "time":"123145645"}
]
}
*/


////V1详细的会议统计//////////////////////////////////////////////////////////////////////
/*
//查询使用PD_SmartJsonProtol
{
"ver":1,//版本号，为了兼容后期协议变更
"meetid":1,//如果指定meetid会忽略roomid和时间字段匹配
"roomid":1,"start":"123145645","end":"123145645"}

返回查询使用PD_SmartJsonProtol
{
"totalpages":"100",//查询时长的总纸张数
"totalduration":"180000",//查询时长的总时长数
"meetnum":23,
"data":[
{
"meetid":1,
"name":"text",//会议名称
"roomid":1,
"roomname":"会议室名称"
"starttime":"4236892000",//会议开始时间，单位UTC秒
"endtime":"42368972000",//会议结束时间，单位UTC秒
"duration":"7200",//会议时长，单位秒
"devicenum":20,//会议室设备数
"memnum":20,//参会人数
"signnum":18,//签到人数
"filenum":41,//上传材料总数
"filesize":"8989898989",//上传材料总空间大小 单位字节
"totalpages":"860",//文档类会议总纸张数
"streamcnt":1,//流请次数
"screencnt":1,//同屏次数
"filecnt":1,//文件请求次数
"chatcount":1,//交流次数
"servicecnt":1,//服务请求次数
"wbopencnt":1,//白板发起次数
"wbusecnt":1,//白板交互次数
"votecnt":1,//投票表决次数
"electioncnt":1,//选举次数
"questioncnt":1,//问卷次数
"bulletcnt":1,//公告标语次数
},
],
"room":[
{
"roomid":1,
"meetnum":2,//查询时间段的会议数量
"roomname":"会议室名称"
}
]
}
*/

//////////////////////////////////////////////////////////////////////////
#pragma pack(pop)

#endif