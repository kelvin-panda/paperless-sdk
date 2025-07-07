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

//check type
int jni_checkcache(JNIEnv *env, jobject thiz, jint type, jint id, jint cacheflag);

//for init android camara capture
//channelstart is channelindex
int jni_AndroidDevice_initcapture(JNIEnv *env, jobject thiz, jint type, jint channelstart);

//for init android rtsp/rtmp/hls capture
//channelstart is channelindex
//num is rtspnum, read from client.ini
int jni_AndroidDevice_InitRtspcapture(JNIEnv *env, jobject thiz, jint channelstart, jint num);

//for pass android camara capture data
//channelstart is channelindex
//iskeyframe is video encode key frame flag 0������ͨ֡��1����ؼ�֡
//pts playtimestamp microseconds
int jni_AndroidDevice_call(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe, jlong pts, jbyteArray pdata);
int jni_AndroidDevice_bytebuffercall(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe, jlong pts, jobject dbuf, jint length);

//Outdbuf��С�������Ϊ���ݵ�width*height*2
//�ɹ�����outdbuf�����ݴ�С��ʧ�ܷ���0
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
//Outdbuf��С�������Ϊ���ݵ�dstwidth*dstheight*2
//�ɹ�����outdbuf�����ݴ�С��ʧ�ܷ���0
int jni_AndroidDevice_I420Scale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);

//(ARGB=bgra in memory)
//�ɹ�����outdbuf�����ݴ�С��ʧ�ܷ���0
int jni_AndroidDevice_ARGBScale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);

//rgbת��
//(ARGB=bgra in memory)
//dst_rgbmode= 0://(abgr in memory)
//dst_rgbmode= 2://(argb in memory)
//dst_rgbmode= 3://(rgba in memory)
//dst_rgbmode= 4://(bgr in memory)
//dst_rgbmode= 5://(rgb in memory)
//�ɹ�����outdbuf�����ݴ�С��ʧ�ܷ���0
int jni_AndroidDevice_ARGBToXRGB(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, int dstrgbmode, jint width, jint height);

int jni_AndroidDevice_ARGBToNV21(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);
int jni_AndroidDevice_ARGBToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width, jint height);

//Ϊ�˱�֤Ч�ʣ�dbuf���ڴ�jni�ڲ���ʹ�ã�ȷ��dbuf�����ݸ���û������
//Outdbuf��С�������Ϊ���ݵ�max(srcwidth*srcheight*2, dstwidth*dstheight*2)
//�ɹ�����outdbuf�����ݴ�С��ʧ�ܷ���0
//�߼�argb-->I420-->I420SCALE-->I4202NV12
int jni_AndroidDevice_RGBToNV12(JNIEnv *env, jobject thiz, int rgbmode, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight,jint rowStride);
//�߼�argb-->argbSCALE-->argb2NV12
int jni_AndroidDevice_RGBToNV12EX(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);
jbyteArray jni_AndroidDevice_RGBToNV12BA(JNIEnv *env, jobject thiz, int rgbmode, jbyteArray pdata, jint srcwidth, jint srcheight, jint dstwidth, jint dstheight);


//��ʼ��һ�����ػỰ
//�ɹ�����һ���������� -1ʧ��
int jni_downloadinit(JNIEnv *env, jobject thiz, jlong mdieaid);
//�����ļ�����
//dbuf �����������ݵ�buf
//readsize��Ҫ�����ݴ�С
//���ض�ȡ�Ĵ�С
int jni_downloadread(JNIEnv *env, jobject thiz, jint opindex, jobject dbuf, jint readsize);
//�������ص�ƫ��
//�ɹ�����0 ʧ�ܷ���-1
int jni_downloadseek(JNIEnv *env, jobject thiz, jint opindex, jlong offset);
void jni_downloadclose(JNIEnv *env, jobject thiz, jint opindex);

//direct get audio play audio data
//readsize Ҫ��ȡ�Ĵ�С
//�ɹ����ض�ȡ�Ĵ�С
int jni_directgetaudiodata(JNIEnv *env, jobject thiz, jobject dbuf, jint readsize);

//direct get audio play audio data
//mode =0��¼���� =1ע���߳� =2����
void jni_crashhandle(JNIEnv *env, jobject thiz, jint mode, jstring pdata);

void jni_enableagendav3(JNIEnv *env, jobject thiz, jint enable);

//�ص����� java callback function c++ return to 
//���ݱ��֪ͨ�ص�
//int callback_method(int type, int method, byte[] pdata, int datalen);

//��ͨ�������ص�
//channelstart ��ͨ������
//oper ����ֵ �μ�libDevice_android.hͷ�ļ���ANDROID_OPERFLAG_PIXFORMAT�Ķ���
//int callback(int channelstart, int oper);

//jni_callִ�к�Ĵ�����ص�����
//type ����
//method ���Ͷ��ڵķ���
//retcode �����룬�μ�������Ķ���
//void error_ret(int type, int method, int retcode);

//��yuv��ͼ�����ݷ��ظ���׿����ʾ
//res ��Դid
//int callback_yuvdisplay(int res, int w, int h, byte[] y, byte[] u, byte[] v);

//��δ�����ͼ��֡���ݷ��ظ���׿�������ʾ
//res ��Դid
//codecid ֡�ı���id
//w,h���
//packet֡����
//pts��֡���ݵ���ʾʱ��� ��λ������
//codecdata sps/pps����
//int callback_videodecode(int iskeyframe, int res, int codecid, int w, int h, byte[] packet, long pts, byte[] codecdata);

//��ʼ��������ʾ֪ͨ
//res ��Դid
//int callback_startdisplay(int res);

//ֹͣ������ʾ֪ͨ
//res ��Դid
//int callback_stopdisplay(int res);
#ifdef __cplusplus
}
#endif

#endif
