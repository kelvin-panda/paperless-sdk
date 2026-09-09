
#ifndef _WALLET_BASE_MACROPUBLIC_H
#define _WALLET_BASE_MACROPUBLIC_H

//encodemode
#define DEVICE_INVITECHAT_SYSDEFAULT        0xff //使用系统默认
#define DEVICE_INVITECHAT_ENCODEMODE_HIGH   0 //高带宽
#define DEVICE_INVITECHAT_ENCODEMODE_MIDDLE 1 //高性能
#define DEVICE_INVITECHAT_ENCODEMODE_LOW    2 //低带宽

#define MAX_RES_NUM 12 //本地最大资源数

#ifndef MEDIA_FILETYPE_AUDIO

#define DB_NOERROR		0
#define DB_OPERERROR	1	//操作错误
#define DB_EOF			2	//文件尾
#define DB_USER_ERROR	3	//用户认证失败
#define DB_BUSY			4	//备份恢复被其他用户占用
#define DB_NOREADY		5	//没准备好
#define DB_FILEERROR	6	//数据文件操作失败
#define DB_REBOOTERROR	7	//服务器自关闭出错
#define DB_ID_ERROR		8	//ID认证出错
#define DB_DBVERHIGH	9	//数据库版本比恢复的机器版本高
#define DB_DBDATAWRONG	10	//数据库文件数据错误
#define DB_TIMEOUT		11	//用户在指定的时间内未执行操作，强制超时处理

#define RETURN_ERROR_NONE			0		//返回的错误值，此为正常，无错误
#define RETURN_ERROR_NOTBEING		1		//不存在
#define RETURN_ERROR_NOTONLINE		2		//不在线
#define RETURN_ERROR_NOSERVER		3		//无法找到可用的服务器
#define RETURN_ERROR_DENIAL			4		//拒绝操作
#define RETURN_ERROR_PASSWORD		5		//密码错误
#define RETURN_ERROR_FORMAT			6		//格式错误
#define RETURN_ERROR_NOPOWER		7		//无权限操作
#define RETURN_ERROR_ISBEING		8		//已存在
#define RETURN_ERROR_UPDATEVER		9		//升级的版本号低于系统正在使用的版本号
#define RETURN_ERROR_DBOFFLINE		10		//数据库没连接上
#define RETURN_ERROR_NOTCONNTECT	11		//未联接到区域服务
#define RETURN_ERROR_NORES			12		//没有资源了
#define RETURN_ERROR_TIMEOUT		13		//超时
#define RETURN_ERROR_ZERO			14      //数据为0
#define RETURN_ERROR_SERVERERROR    15		//区域服务器操作出错
#define RETURN_ERROR_NOSPACE		16		//没有足够的空间了
#define RETURN_ERROR_DBOPERERROR	17		//数据操作错误
#define RETURN_ERROR_NOTIDENTITY	18		//没有标识码
#define RETURN_ERROR_MAXDEVICENUM	19		//超过最大在线设备数
#define RETURN_ERROR_MAXERRORTIMES	20		//超过最大错误次数数,拒绝登陆
#define RETURN_ERROR_PARSEERROR		21		//区域服务器认证失败
#define RETURN_ERROR_PROTOCALNOMATCH	22		//协议版本不匹配
#define RETURN_ERROR_EXPIRATIONTIME		23		//区域服务器已经过期了 expiration Time
#define RETURN_ERROR_COMMVERNOMATCH		24		//协议版本不匹配,拒绝请求
#define RETURN_ERROR_OVERRUN			25		//超出范围
#define RETURN_ERROR_NOTOPEN			26		//无法打开
#define RETURN_ERROR_FORCEDSTOP			27		//强行停止
#define RETURN_ERROR_LOGONERROR			28	    //登陆错误
#define RETURN_ERROR_MAXONLINENUM		29	    //超过最大并发设备数,拒绝登陆
#define RETURN_ERROR_LOOP				30	    //数据出现了循环
#define RETURN_ERROR_HAVENOTADDRESS		31	    //没有地址
#define RETURN_ERROR_NOFUNC				32	    //此功能不存在


//媒体或流服务器返回的错误值
#define RETURN_ERROR_SEC_NONE			0			//此为正常，无错误
#define RETURN_ERROR_SEC_NOTBEING		0xf001		//媒体或流通道不存在
#define RETURN_ERROR_SEC_ISBEING		0xf002		//媒体或流通道已存在
#define RETURN_ERROR_SEC_SERVER			0xf003		//媒体或流服务器处理异常
#define RETURN_ERROR_SEC_NOTBEINGENCODE	0xf004		//流服务器找不到可用的流编码
#define RETURN_ERROR_SEC_SERVERBUSY		0xf005		//媒体或流服务器繁忙，拒绝处理
#define RETURN_ERROR_SEC_CHANNELBUSY	0xf006		//流服务器流通道输出太多了，拒绝处理
#define RETURN_ERROR_SEC_STOP			0xf007		//未能正常停止流通道或媒体请求
#define RETURN_ERROR_SEC_NOSPACE		0xf008		//没有足够的空间了
#define RETURN_ERROR_SEC_IDENTIFYOCCUPY	0xf009		//标识码已被占用,请重试
#define RETURN_ERROR_SEC_SENDERROR		0xf00a		//数据发送失败
#define RETURN_ERROR_SEC_INITIALERROR	0xf00b		//初始化请求失败

//大类
#define MEDIA_FILETYPE_AUDIO    0x00000000 //音频
#define MEDIA_FILETYPE_VIDEO	0x20000000 //视频
#define MEDIA_FILETYPE_RECORD	0x40000000 //录制
#define MEDIA_FILETYPE_PICTURE	0x60000000 //图片
#define MEDIA_FILETYPE_UPDATE	0xe0000000 //升级
//#define MEDIA_FILETYPE_TEMP		0x80000000 //临时文件
#define MEDIA_FILETYPE_OTHER	0xa0000000 //其它文件
#define MAINTYPEBITMASK			0xe0000000
//小类
#define MEDIA_FILETYPE_PCM		0x01000000	//PCM文件
#define MEDIA_FILETYPE_MP3		0x02000000	//MP3文件
#define MEDIA_FILETYPE_ADPCM	0x03000000	//WAV文件
#define MEDIA_FILETYPE_FLAC		0x04000000	//FLAC文件
#define MEDIA_FILETYPE_MP4		0x07000000  //MP4文件
#define MEDIA_FILETYPE_MKV		0x08000000  //MKV文件
#define MEDIA_FILETYPE_RMVB		0x09000000  //RMVB文件
#define MEDIA_FILETYPE_RM		0x0a000000  //RM文件
#define MEDIA_FILETYPE_AVI		0x0b000000  //AVI文件
#define MEDIA_FILETYPE_BMP		0x0c000000  //bmp文件
#define MEDIA_FILETYPE_JPEG		0x0d000000  //jpeg文件
#define MEDIA_FILETYPE_PNG		0x0e000000  //png文件
#define MEDIA_FILETYPE_OTHERSUB	0x10000000  //其它文件
#define SUBTYPEBITMASK			0x1f000000

#define MEDIA_FILETYPE_NET		0x00000000 //网络文件
#define MEDIA_FILETYPE_TEMP		0x00800000 //临时文件
#define MEDIA_FILETYPE_LOCAL	0x00c00000 //本地文件
#define SAVETYPEBITMASK			0x00c00000

#define MEDIATYPEBITCOUNT       12		   //媒体类型占用多少位
#define MEDIATOTALTYPE			0xffc00000 //媒体类型

#define MAKEMEDIATYPE(x,y,z)	 (x | y | z)			//合成媒体类别
#define GETMAINTYPE(x)			 (x & MAINTYPEBITMASK)	//获取大类别
#define GETSUBTYPE(y)			 (y & SUBTYPEBITMASK)	//获取子类别
#define GETSAVETYPE(z)			 (z & SAVETYPEBITMASK)	//获取文件存放类型
#define IS_RECORD_MEDIA(mediaId) ((mediaId & MAINTYPEBITMASK) == MEDIA_FILETYPE_RECORD) //是否为录制文件
#define IS_UPDATE_MEDIA(mediaId) ((mediaId & MAINTYPEBITMASK) == MEDIA_FILETYPE_UPDATE) //是否为升级文件
#define IS_TEMP_MEDIA(mediaId)	 ((mediaId & SAVETYPEBITMASK) == MEDIA_FILETYPE_TEMP)   //是否为临时文件

#endif

#define IS_PICTURE_MEDIA(mediaId) ((mediaId & MAINTYPEBITMASK) == MEDIA_FILETYPE_PICTURE) //是否为图片文件

#define WALLET_TRIGGER_ID_TEMP	0xff000000		//临时ID号，此ID号的内容区域服务器不保存到数据库
#define WALLET_TRIGGER_ID_SELF	0xfff00000		//只作用于设备自身，此ID的内容不上传到区域服务器

#define DEVICECHAT_AUDIO_B_RESINDEX 9 //对讲远端音频播放资源ID
#define DEVICECHAT_VIDEO_B_RESINDEX 10 //对讲远端端视频播放资源ID
#define DEVICECHAT_VIDEO_A_RESINDEX 11 //对讲本机视频播放资源ID


#define VIDEO_START_RESID 2 //本地视频起始资源ID
#define AUDIO_START_RESID 6//本地音频频起始资源ID

#define NET_EXCHANGE_TYPE 0x1e //数据库后台点对点传输类型

#define RES_RECORDVIDEOSTREAM_INDEX  0 //视频流索引
#define RES_SCREENSTREAM_INDEX		 1 //屏幕流索引

#define NETINT32DATA_ATTRIBID				47 //标识当前NETBLOBDATA_ATTRIBID的二进制数据是发送给那个设备的寄存器ID
#define MCSERVER_NETBLOBDATA_ATTRIBID		48 //后台服务器   二进制数据使用的寄存器ID
#define COMMONCLIENT_NETBLOBDATA_ATTRIBID   49 //普通会议终端 二进制数据使用的寄存器ID
#define COMMONCLIENT_EXCHANGE_NETBLOBDATA_ATTRIBID   48 //普通会议终端 之间中转二进制数据使用的寄存器ID ,注:数据头使用服务器的应答头

#define  USE_DEVICE_EXCHANGE  1 //启用设备点对点交换数据
#define  CTOS_EXCHANGE		  0 //为1表示由客户端发起交换 0表示从服务端发起交换 

#define MEET_SUPPORTOLDANDNEW 0 //为0表示强制匹配会议使用的设备类型

#define MEETPUSHSTREAM_FLAG_MIC		   0x00000001 //音频采集流
#define MEETPUSHSTREAM_FLAG_SCREEN	   0x00000002 //屏幕流
#define MEETPUSHSTREAM_FLAG_USB	       0x00000004 //usb流
#define MEETPUSHSTREAM_FLAG_HKIPC      0x00000008 //usb流

//SystemAttrib -- >dns2
#define OCCUPYSYSTEMATTRIBDNSNUM    3 //占用设备63号寄存器的变量dns2的字节数
#define MEETDEVICE_LIFTGROUPID		0 //升降机组ID索引 dns2[0]
#define MEETDEVICE_MICLIFTGROUPID	1 //升降话筒组ID索引 dns2[1]

#define MEETDEVICE_FLAG			    2 //会议设备的一些标志 dns2[2]
#define MEETDEVICE_FLAG_DIRECTENTER 0x01 //免签到进入会议
#define MEETDEVICE_FLAG_OPENOUTSIDE 0x02 //使用外部软件打开文档
#define MEETDEVICE_FLAG_GUESTMODE	0x04 //使用来宾模式-进入时必须选择参会人签到
#define MEETDEVICE_FLAG_WELCOMEPAGE	0x08 //显示欢迎界面-当设备有会议并且绑定人员时点击后直接签到进入会议，其它情况不显示欢迎界面
#define MEETDEVICE_FLAG_DATAPROTECT 0x10 //资料保护,禁止截图

//会议系统上传文件属性
#define MEET_FILEATTRIB_BACKGROUND		0x10000 //背景文件
#define MEET_FILEATTRIB_TABLECARD		0x20000 //桌牌背景文件
#define MEET_FILEATTRIB_DEVICEUPDATE	0x40000 //设备升级用的文件
#define MEET_FILEATTRIB_PUBLISH		    0x80000 //会议发布
#define MEET_FILEATTRIB_WELCOMEPAGE		0x100000 //会议欢迎界面
#define MEET_FILEATTRIB_TTS	    		0x200000 //tts语音文件

#ifndef SUPPORTVERUPDATE 
#define ENABLE_SINGLEENTER_ENCRYPT 0 //是否启用独立企业文档加密 启用后，不同企业的客户端使用不同的密码加密，不能混用
#define SUPPORTVERUPDATE 1 //启用新版本协议
#endif

#endif
