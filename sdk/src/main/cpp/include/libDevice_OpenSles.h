#ifndef _LIBDEVICE_OPENSLES_H_
#define _LIBDEVICE_OPENSLES_H_


#ifdef __cplusplus
extern "C" {
#endif

	//功能：初始化视频采集
	//channelStart：起始流ID
	//num: 流通道数
	int Device_OpenSles_Init(int deviceId, int outputResStartId, int resNum, int inputChannelStartId, int channelNum);

	//功能：从SDK中的播放缓存区获取音频数据
	//audioid：声卡序号 一般是0
	//outputBuffer: 用于接受数据的内存
	//framesPerBuffer帧个数 也就是outputBuffer的大小除于通道数再除于每个样本的字节数 1024/2/2
	//返回获取的样本数
	int Device_OpenSles_GetAudio(int audioid, unsigned char *outputBuffer, int framesPerBuffer);

#ifdef __cplusplus
};
#endif
#endif