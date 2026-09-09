#ifndef  _JNI_WALLETNET_INIT_INTERFACE_H_
#define _JNI_WALLETNET_INIT_INTERFACE_H_
#include "base.h"
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
//register jni functions
int wallet_netinit_register_native_methods(JNIEnv* env);

//init net system
//data --> pbui_MeetCore_InitParam
//Sucess return 0,failed retunr -1
int Init_walletSys(JNIEnv *env, jobject thiz, jbyteArray data);

//set java directbuf for decode video
void jni_setdirectbuf(JNIEnv *env, jobject thiz, jobject dbuf, jobject dbufexdata);

//benable 0:foregroud 1:backgroud
void jni_enablebackgroud(JNIEnv *env, jobject thiz, jint benable);

//for system function call
//sucess return a bytearray parse with type and method,failed return a null bytearray
jbyteArray jni_call(JNIEnv *env, jobject thiz, jint type, jint method, jbyteArray pdata);

//for init android camara capture
//channelstart is channelindex
int jni_AndroidDevice_initcapture(JNIEnv *env, jobject thiz, jint type, jint channelstart);

//for init android rtsp/rtmp/hls capture
//channelstart is channelindex
//num is rtspnum, read from client.ini
int jni_AndroidDevice_InitRtspcapture(JNIEnv *env, jobject thiz, jint channelstart, jint num);

//for pass android camara capture data
//channelstart is channelindex
//iskeyframe is video encode key frame flag 0代表普通帧，1代表关键帧
//pts playtimestamp microseconds
int jni_AndroidDevice_call(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe, jlong pts, jbyteArray pdata);
int jni_AndroidDevice_bytebuffercall(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe, jlong pts, jobject dbuf, jint length);

//Outdbuf大小最好设置为数据的width*height*2
//成功返回outdbuf的数据大小，失败返回0
int jni_AndroidDevice_NV21ToI420(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);
int jni_AndroidDevice_NV21ToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);
int jni_AndroidDevice_I420ToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);
//rgbmode= 0://(abgr in memory)
//rgbmode= 1://(bgra in memory)
//rgbmode= 2://(argb in memory)
//rgbmode= 3://(rgba in memory)
//rgbmode= 4://(bgr in memory)
//rgbmode= 5://(rgb in memory)
int jni_AndroidDevice_RGBToI420(JNIEnv *env, jobject thiz, jint rgbmode, jobject dbuf, jobject outdbuf, jint width, jint height);
//Outdbuf大小最好设置为数据的dstwidth*dstheight*2
//成功返回outdbuf的数据大小，失败返回0
int jni_AndroidDevice_I420Scale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);

//(ARGB=bgra in memory)
//成功返回outdbuf的数据大小，失败返回0
int jni_AndroidDevice_ARGBScale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);

//rgb转换
//(ARGB=bgra in memory)
//dst_rgbmode= 0://(abgr in memory)
//dst_rgbmode= 2://(argb in memory)
//dst_rgbmode= 3://(rgba in memory)
//dst_rgbmode= 4://(bgr in memory)
//dst_rgbmode= 5://(rgb in memory)
//成功返回outdbuf的数据大小，失败返回0
int jni_AndroidDevice_ARGBToXRGB(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, int dstrgbmode, jint width, jint height);

int jni_AndroidDevice_ARGBToNV21(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);
int jni_AndroidDevice_ARGBToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);

//为了保证效率，dbuf的内存jni内部会使用，确保dbuf的数据覆盖没有问题
//Outdbuf大小最好设置为数据的max(srcwidth*srcheight*2, dstwidth*dstheight*2)
//成功返回outdbuf的数据大小，失败返回0
//逻辑argb-->I420-->I420SCALE-->I4202NV12
int jni_AndroidDevice_RGBToNV12(JNIEnv *env, jobject thiz, int rgbmode, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);
//逻辑argb-->argbSCALE-->argb2NV12
int jni_AndroidDevice_RGBToNV12EX(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);
jbyteArray jni_AndroidDevice_RGBToNV12BA(JNIEnv *env, jobject thiz, int rgbmode, jbyteArray pdata, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);


//初始化一个下载会话
//成功返回一个会议索引 -1失败
int jni_downloadinit(JNIEnv *env, jobject thiz, jlong mdieaid);
//下载文件数据
//dbuf 用来接收数据的buf
//readsize需要的数据大小
//返回读取的大小
int jni_downloadread(JNIEnv *env, jobject thiz, jint opindex, jobject dbuf, jint readsize);
//设置下载的偏移
//成功返回0 失败返回-1
int jni_downloadseek(JNIEnv *env, jobject thiz, jint opindex, jlong offset);
void jni_downloadclose(JNIEnv *env, jobject thiz, jint opindex);

//direct get audio play audio data
//readsize 要读取的大小
//成功返回读取的大小
int jni_directgetaudiodata(JNIEnv *env, jobject thiz, jobject dbuf, jint readsize);

//direct get audio play audio data
//mode =0记录方法 =1注册线程 =2重置
void jni_crashhandle(JNIEnv *env, jobject thiz, jint mode, jstring pdata);

//回调函数 java callback function c++ return to 
//数据变更通知回调
//int callback_method(int type, int method, byte[] pdata, int datalen);

//流通道操作回调
//channelstart 流通道索引
//oper 操作值 参见libDevice_android.h头文件中ANDROID_OPERFLAG_PIXFORMAT的定义
//int callback(int channelstart, int oper);

//jni_call执行后的错误码回调函数
//type 类型
//method 类型对于的方法
//retcode 错误码，参见错误码的定义
//void error_ret(int type, int method, int retcode);

//将yuv的图像数据返回给安卓层显示
//res 资源id
//int callback_yuvdisplay(int res, int w, int h, byte[] y, byte[] u, byte[] v);

//将未解码的图像帧数据返回给安卓层解码显示
//res 资源id
//codecid 帧的编码id
//w,h宽高
//packet帧数据
//pts该帧数据的显示时间戳 单位：毫秒
//codecdata sps/pps数据
//int callback_videodecode(int iskeyframe, int res, int codecid, int w, int h, byte[] packet, long pts, byte[] codecdata);

//初始化解码显示通知
//res 资源id
//int callback_startdisplay(int res);

//停止解码显示通知
//res 资源id
//int callback_stopdisplay(int res);
#ifdef __cplusplus
}
#endif

#endif
