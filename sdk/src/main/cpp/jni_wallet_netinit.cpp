#include <jni.h>
#include <wallet_net.h>
#include <malloc.h>
#include <string.h>
#include <list>
//#include <libDevice_androidcapture.h>
#include "jni_wallet_netinit.h"
#include "meetc/meetinterface_type.h"
#include "meetc/InterFaceCorePBModule.h"
#include "libDevice_androidcapture.h"
#include "control.h"
// c call java method

typedef struct JniCacheData {
    int32u type;
    int32u method;
    void *pdata;
    int datalen;
} JniCacheData, *pJniCacheData;

typedef struct InternalGlobalParam {
    JavaVM *javavm;
    jobject thiz;
    jclass clssJNINet;

    jmethodID merrorProc;
    jmethodID mCallBack_DataProc;
    jmethodID mDoCaptureOper;
    jmethodID mCallBack_YUVDisplayProc;
    jmethodID mCallBack_VideoDecodeProc;
    jmethodID mCallBack_DIRECTVideoDecodeProc;
    jmethodID mCallBack_StartDisplay;
    jmethodID mCallBack_StopDisplay;

    jint bisbackgroud;//是否处于后台运行
    std::list<pJniCacheData> *cachelist;
} InternalGlobalParam, *pInternalGlobalParam;

static pInternalGlobalParam g_pinternalparam = NULL;
static char *g_disbuf = NULL;
static int g_disbufsize = 0;
static char *g_disbufexdata = NULL;
static int g_disbufexdatasize = 0;

void AddCacheData(int32u type, int32u method, void *pdata, int datalen) {
    pJniCacheData ptmp = new JniCacheData;
    do {
        if (ptmp == NULL)
            return;

        ptmp->type = type;
        ptmp->method = method;
        ptmp->datalen = datalen;
        if (ptmp->datalen > 0) {
            ptmp->pdata = new char[datalen];
            if (ptmp->pdata == NULL)
                break;
            memcpy(ptmp->pdata, pdata, datalen);
        }
        g_pinternalparam->cachelist->push_back(ptmp);
        return;
    } while (0);
    delete ptmp;
}

/* functions*/
pInternalGlobalParam GetGlobalParam() {
    return g_pinternalparam;
}

JNIEnv *Adapter_GetEnv() {
    int status;
    JNIEnv *envnow = NULL;

    if (g_pinternalparam == NULL)
        return NULL;

    status = g_pinternalparam->javavm->GetEnv((void **) &envnow, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&envnow, NULL);
        if (status < 0) {
            if (logable) LOGI("Adapter_GetEnv get status2 return null");
            return NULL;
        }
    }
    return envnow;
}

int jni_initforjava(JNIEnv *env, jobject obj) {
    int ret;
    jclass clss;
    do {
        if (logable) LOGI("jni_initforjava call %d", __LINE__);
        if (g_pinternalparam)
            return 0;

        g_pinternalparam = (pInternalGlobalParam) malloc(
                sizeof(InternalGlobalParam));
        if (g_pinternalparam == NULL) {
            if (logable) LOGI("jni_initforjava %d", __LINE__);
            break;
        }
        memset(g_pinternalparam, 0, sizeof(InternalGlobalParam));
        g_pinternalparam->cachelist = new std::list<pJniCacheData>;
        ret = env->GetJavaVM(&g_pinternalparam->javavm);
        if (ret) {
            if (logable) LOGI("jni_initforjava %d", __LINE__);
            break;
        }

        g_pinternalparam->thiz = env->NewGlobalRef(obj);
        if (g_pinternalparam->thiz == NULL) {
            if (logable) LOGI("jni_initforjava %d", __LINE__);
            break;
        }

        clss = env->FindClass(main_interface_class_path_name);
        if (clss == NULL) {
            if (logable) LOGI("jni_initforjava %d", __LINE__);
            break;
        }
        g_pinternalparam->clssJNINet = (jclass) env->NewGlobalRef(clss);
        if (g_pinternalparam->clssJNINet == NULL) {
            env->DeleteLocalRef(clss);
            if (logable) LOGI("jni_initforjava %d", __LINE__);
            break;
        }
        env->DeleteLocalRef(clss);

        g_pinternalparam->mCallBack_DataProc = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_method",
                "(II[BI)I");//(int type, int method, byte[] pdata, int datalen)
        if (g_pinternalparam->mCallBack_DataProc == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_DataProc %d", __LINE__);
            break;
        }
        g_pinternalparam->mDoCaptureOper = env->GetMethodID(g_pinternalparam->clssJNINet,
                                                            "callback",
                                                            "(II)I");//(int channelstart, int oper)
        if (g_pinternalparam->mDoCaptureOper == NULL) {
            if (logable) LOGI("jni_initforjava mDoCaptureOper %d", __LINE__);
            break;
        }
        g_pinternalparam->merrorProc = env->GetMethodID(g_pinternalparam->clssJNINet,
                                                        "error_ret",
                                                        "(III)V");//(int type, int method, int retcode)
        if (g_pinternalparam->merrorProc == NULL) {
            if (logable) LOGI("jni_initforjava merrorProc %d", __LINE__);
            //break;
        }
        g_pinternalparam->mCallBack_YUVDisplayProc = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_yuvdisplay",
                "(III[B[B[B)I");//(int res,int w, int h, byte[] y,byte[] u,byte[] v)
        if (g_pinternalparam->mCallBack_YUVDisplayProc == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_YUVDisplayProc %d", __LINE__);
            break;
        }
        g_pinternalparam->mCallBack_VideoDecodeProc = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_videodecode",
                "(IIIII[BJ[B)I"); //(int iskeyframe, int res,int codecid, int w, int h, byte[] packet,long pts,byte[] codecdata)
        if (g_pinternalparam->mCallBack_VideoDecodeProc == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_VideoDecodeProc %d", __LINE__);
            break;
        }
        g_pinternalparam->mCallBack_DIRECTVideoDecodeProc = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_directvideodecode",
                "(IIIIIIJI)I"); //(int iskeyframe, int res,int codecid, int w, int h, int datalen,long pts,int codecdatalen)
        if (g_pinternalparam->mCallBack_DIRECTVideoDecodeProc == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_DIRECTVideoDecodeProc %d", __LINE__);
            break;
        }
        g_pinternalparam->mCallBack_StartDisplay = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_startdisplay", "(I)I"); //(int res)
        if (g_pinternalparam->mCallBack_StartDisplay == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_StartDisplay %d", __LINE__);
            break;
        }
        g_pinternalparam->mCallBack_StopDisplay = env->GetMethodID(
                g_pinternalparam->clssJNINet, "callback_stopdisplay", "(I)I");//(int res)
        if (g_pinternalparam->mCallBack_StopDisplay == NULL) {
            if (logable) LOGI("jni_initforjava mCallBack_StopDisplay %d", __LINE__);
            break;
        }
        ret = 0;
    } while (0);

    if (logable) LOGI("jni_initforjava return %d, retcode:%d", __LINE__, ret);
    return ret;
}

int localCB_DISPSTART(int resId)                        //开始显示
{
    if (logable) LOGI("-------------localCB_DISPSTART line:%d  resId:%d!\n", __LINE__, resId);
    //ctprintf("----start play res:%d !\n", resId);
    int status, battach = 0;
    JNIEnv *env = NULL;

    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    status = g_pinternalparam->javavm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            if (logable) LOGI("Adapter_GetEnv get status2 return null");
            return 0;
        }
        battach = 1;
    }

    env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_StartDisplay,
                       (jint) resId);
    if (battach)
        g_pinternalparam->javavm->DetachCurrentThread();
    return 0;
}

int localCB_DISPSTOP(int resId)                            //停止显示
{
    if (logable) LOGI("-------------localCB_DISPSTOP line:%d  resId:%d!\n", __LINE__, resId);
    //ctprintf("****stop play res:%d !\n", resId);
    int status, battach = 0;
    JNIEnv *env = NULL;

    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    status = g_pinternalparam->javavm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            if (logable) LOGI("Adapter_GetEnv get status2 return null");
            return 0;
        }
        battach = 1;
    }

    env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_StopDisplay,
                       (jint) resId);
    if (battach)
        g_pinternalparam->javavm->DetachCurrentThread();
    return 0;
}

void localCB_DISPDATA(int *resId, int resNum, char *pData, void *param)    //显示数据
{
//    return;
    int status, battach = 0;
    JNIEnv *env = NULL;
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    status = g_pinternalparam->javavm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            if (logable) LOGI("Adapter_GetEnv get status2 return null");
            return;
        }
        battach = 1;
    }

    PStuAndroidDisplay_Param pParam = (PStuAndroidDisplay_Param) (param);
    if (pParam->param & ANDROIDDISPLAY_PARAM_DECODE) {
        //ctprintf("no decode data****play resnum:%d pData:%x !\n", resNum, pData);
        //jint iskeyframe = (pParam->param & ANDROIDDISPLAY_PARAM_VIDEOKEYFRAME)? 1: 0;
        jint iskeyframe = pParam->param;
        if (pParam->codecid == 28)
            pParam->codecid = 27;
        else if (pParam->codecid == 13)
            pParam->codecid = 12;
        else if (pParam->codecid == 174)
            pParam->codecid = 173;
        else if (pParam->codecid == 140)
            pParam->codecid = 139;
        else if (pParam->codecid == 168)
            pParam->codecid = 167;
        if (g_disbufsize >= pParam->size && g_disbufexdatasize >= pParam->codecsize) {
            if (pParam->size)
                memcpy(g_disbuf, pData, pParam->size);
            if (pParam->codecsize)
                memcpy(g_disbufexdata, pParam->codecdata, pParam->codecsize);

            for (int ni = 0; ni < resNum; ++ni) {
                //if(logable) LOGI("-------video codec:%d resId:%d packetsize:%d!\n", pParam->codecid, resId[ni], pParam->size);
                env->CallIntMethod(g_pinternalparam->thiz,
                                   g_pinternalparam->mCallBack_DIRECTVideoDecodeProc, iskeyframe,
                                   (jint) resId[ni], (jint) pParam->codecid, (jint) pParam->w,
                                   (jint) pParam->h, (jint) pParam->size, (jlong) pParam->pts,
                                   (jint) pParam->codecsize);
            }

            if (battach)
                g_pinternalparam->javavm->DetachCurrentThread();
            return;
        }

        jbyteArray barray = env->NewByteArray(pParam->size);
        jbyteArray codecdata = env->NewByteArray(pParam->codecsize);
        env->SetByteArrayRegion(barray, 0, pParam->size, (const jbyte *) pData);
        env->SetByteArrayRegion(codecdata, 0, pParam->codecsize, (const jbyte *) pParam->codecdata);
        for (int ni = 0; ni < resNum; ++ni) {
            //if(logable) LOGI("-------video codec:%d resId:%d packetsize:%d!\n", pParam->codecid, resId[ni], pParam->size);
            env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_VideoDecodeProc,
                               iskeyframe,
                               (jint) resId[ni], (jint) pParam->codecid, (jint) pParam->w,
                               (jint) pParam->h, barray, (jlong) pParam->pts, codecdata);
        }
        env->DeleteLocalRef(barray);
        env->DeleteLocalRef(codecdata);
    } else {
        //ctprintf("decode data ****play resnum:%d pData:%x!\n", resNum, pParam->param);
        int ysize = pParam->linesize[0] * pParam->h;
        int usize = pParam->linesize[1] * (pParam->h / 2);
        int vsize = usize;
        jbyteArray y = env->NewByteArray(ysize);
        jbyteArray u = env->NewByteArray(usize);
        jbyteArray v = env->NewByteArray(vsize);
        env->SetByteArrayRegion(y, 0, ysize, (const jbyte *) pData);
        env->SetByteArrayRegion(u, 0, usize, (const jbyte *) pData + ysize);
        env->SetByteArrayRegion(v, 0, vsize, (const jbyte *) pData + ysize + usize);
        for (int ni = 0; ni < resNum; ++ni) {
            //if(logable) LOGI("-------------display yuv resId:%d yuvsize:{%d,%d,%d}!\n", resId[ni], pParam->linesize[0], pParam->linesize[1], pParam->linesize[2]);
            env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_YUVDisplayProc,
                               (jint) resId[ni], (jint) pParam->w, (jint) pParam->h, y, u, v);
        }
        env->DeleteLocalRef(y);
        env->DeleteLocalRef(u);
        env->DeleteLocalRef(v);
    }

    if (battach)
        g_pinternalparam->javavm->DetachCurrentThread();
}

void CBLogInfo(int loglevel, const char *pmsg, void *puser) {
    if (logable) LOGI("demolog [%d] %s", loglevel, pmsg);
}

int CallbackFunc(int32u meetingid, int32u type, int32u method, void *pdata, int datalen, void *puser) {
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    if (type != TYPE_MEET_INTERFACE_TIME && type != TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO) {
        switch (type) {
            default:
                if (logable) LOGI("demoCb:%d:%s, %d:%s!\n", type, meetcore_GettypeStr(type), method,
                                  meetcore_GetMethodStr(method));
                break;
        }
    }

    if (g_pinternalparam->bisbackgroud) {
        if (type != TYPE_MEET_INTERFACE_TIME && type != TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO) {
            AddCacheData(type, method, pdata, datalen);
        }
        return 0;
    }
    int status, battach = 0;
    JNIEnv *env = NULL;

    status = g_pinternalparam->javavm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            if (logable) LOGI("Adapter_GetEnv get status2 return null");
            return 0;
        }
        battach = 1;
    }

    if (!g_pinternalparam->cachelist->empty()) {
        pJniCacheData ptmp;
        std::list<pJniCacheData>::iterator iter = g_pinternalparam->cachelist->begin();
        for (; iter != g_pinternalparam->cachelist->end();) {
            ptmp = *iter;
            ++iter;
            if (ptmp->datalen > 0) {
                jbyteArray barray = env->NewByteArray(ptmp->datalen);
                env->SetByteArrayRegion(barray, 0, ptmp->datalen, (const jbyte *) ptmp->pdata);
                env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_DataProc,
                                   (jint) ptmp->type,
                                   (jint) ptmp->method, barray, (jint) ptmp->datalen);
                env->DeleteLocalRef(barray);
                ptmp->datalen = 0;
                delete[] ((char *) ptmp->pdata);
            } else {
                env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_DataProc,
                                   (jint) ptmp->type,
                                   (jint) ptmp->method, NULL, 0);
            }
            delete ptmp;
        }
        g_pinternalparam->cachelist->clear();
    }

    if (datalen > 0) {
        jbyteArray barray = env->NewByteArray(datalen);
        env->SetByteArrayRegion(barray, 0, datalen, (const jbyte *) pdata);
        env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_DataProc,
                           (jint) type, (jint) method, barray,
                           (jint) datalen);
        env->DeleteLocalRef(barray);
    } else {
        env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mCallBack_DataProc,
                           (jint) type, (jint) method, NULL, 0);
    }

    if (battach)
        g_pinternalparam->javavm->DetachCurrentThread();
    return 0;
}

void InternalAndroidDevice_LogCB(int channelstart, int level, char *pmsg) {
    if (logable) LOGI("InternalAndroidDevice_LogCB %s!", pmsg);
}

int InternalAndroidDevice_GetInfoCB(int channelstart, int oper) {
    int status, battach = 0, vals = 0;
    JNIEnv *env = NULL;

    if (logable) LOGI("InternalAndroidDevice_GetInfoCB %d", __LINE__);
    status = g_pinternalparam->javavm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status != JNI_OK) {
        status = g_pinternalparam->javavm->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            if (logable) LOGI("InternalAndroidDevice_GetInfoCB %d", __LINE__);
            return 0;
        }
        battach = 1;
    }
    if (logable) LOGI("InternalAndroidDevice_GetInfoCB %d", __LINE__);
    vals = env->CallIntMethod(g_pinternalparam->thiz, g_pinternalparam->mDoCaptureOper,
                              (jint) channelstart, (jint) oper);

    if (battach)
        g_pinternalparam->javavm->DetachCurrentThread();

    return vals;
}

//<editor-fold desc="java与cpp对应方法">

extern "C" JNIEXPORT void JNICALL
Java_com_paperless_sdk_Call_enablebackgroud(JNIEnv *env, jobject thiz, jint benable) {
    g_pinternalparam->bisbackgroud = benable;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_checkcache(JNIEnv *env, jobject thiz, jint type, jint id, jint cacheflag) {
    int ret = -1;

    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    Type_MeetCacheOper tmp = {0};
    tmp.hdr.type = type;
    tmp.hdr.method = METHOD_MEET_INTERFACE_CACHE;
    tmp.cacheflag = cacheflag;
    tmp.id = id;
    ret = meetcore_Call((pType_HeaderInfo) &tmp, 0, 0);

    return ret;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_paperless_sdk_Call_callMethod(JNIEnv *env, jobject thiz, jint type, jint method, jbyteArray pdata) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    int ret = -1;
    jbyte *jBuf = 0;
    jint length = 0;

    //if(logable) LOGI("195 jni_call  :type:%d method:%d ", type, method);
    if (NULL != pdata) {
        jBuf = env->GetByteArrayElements(pdata, JNI_FALSE);
        length = env->GetArrayLength(pdata);
    }
    void *pretdata = NULL;
    int retdatalen = 0;
    if (logable) LOGI("jni_call: type:%s(%d), method:%s(%d)--pamlen:%d ", meetcore_GettypeStr(type),
                      type,
                      meetcore_GetMethodStr(method), method, length);
    ret = meetPB_call(type, method, jBuf, length, &pretdata, &retdatalen, 0);
    if (jBuf) {
        //只要非0 就可以进入方
        //释放
        env->ReleaseByteArrayElements(pdata, jBuf, 0);
    }
    if (g_pinternalparam->merrorProc != NULL)
        env->CallVoidMethod(g_pinternalparam->thiz, g_pinternalparam->merrorProc, (jint) type,
                            (jint) method, ret);
    if (ret < ERROR_MEET_INTERFACE_NULL || retdatalen <= 0) {
        if (ret < ERROR_MEET_INTERFACE_NULL) {
            if (logable) LOGI("jni_call failed: type:%s(%d), method:%s(%d)--err:%s(%d),outlen:%d",
                              meetcore_GettypeStr(type), type, meetcore_GetMethodStr(method),
                              method,
                              meetcore_GetErrorStr(ret), ret, retdatalen);
        } else {
            if (logable) LOGI("jni_call failed: type:%s(%d), method:%s(%d)--ret:0,outlen:%d",
                              meetcore_GettypeStr(type), type, meetcore_GetMethodStr(method),
                              method, ret,
                              retdatalen);
        }
        return NULL;
    }
    jbyteArray barray = env->NewByteArray(retdatalen);
    env->SetByteArrayRegion(barray, 0, retdatalen, (const jbyte *) pretdata);
    meetPB_free(pretdata, retdatalen);
    return barray;
}

extern "C" JNIEXPORT void JNICALL
Java_com_paperless_sdk_Call_initDirectBuf(JNIEnv *env, jobject thiz, jobject dbuf, jobject dbufexdata) {
    g_disbuf = (char *) (env->GetDirectBufferAddress(dbuf));
    g_disbufsize = env->GetDirectBufferCapacity(dbuf);
    g_disbufexdata = (char *) (env->GetDirectBufferAddress(dbufexdata));
    g_disbufexdatasize = env->GetDirectBufferCapacity(dbufexdata);
    if (logable) LOGI("%s: %d SetDirectBuf g_disbufsize=%d, g_disbufexdatasize=%d ", __FUNCTION__,
                      __LINE__,
                      g_disbufsize, g_disbufexdatasize);
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_userDownloadinit(JNIEnv *env, jobject thiz, jlong mdieaid) {
//    int32u tmpmediaid = (int32u)mdieaid;
//    if (tmpmediaid <= 0)
//    {
//        LOGI("%s: %d  error mdieaid=0x%x!", __FUNCTION__, __LINE__, tmpmediaid);
//        return -1;
//    }
//
//    int64u filelen = 0;
//    int opindex = meetcore_downloadinit2(tmpmediaid, &filelen);
//    if (opindex < 0)
//    {
//        LOGI("%s: %d error mdieaid=0x%x!", __FUNCTION__, __LINE__, tmpmediaid);
//        return -1;
//    }
    return -1;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_userDownloadread(JNIEnv *env, jobject thiz, jint opindex, jobject dbuf, jint readsize) {
//    char* pbuf = (char *)(env->GetDirectBufferAddress(dbuf));
//    int  bufsize = env->GetDirectBufferCapacity(dbuf);
//    if (!pbuf || bufsize <= 0)
//    {
//        LOGI("%s: %d error bufsize=0x%x!", __FUNCTION__, __LINE__, bufsize);
//        return -1;
//    }
//
//    int needsize = readsize;
//    if (bufsize < readsize)
//        needsize = bufsize;
//    return meetcore_downloadread2(opindex, pbuf, needsize);
    return -1;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_userDownloadseek(JNIEnv *env, jobject thiz, jint opindex, jlong offset) {
//    int64u tmpoffset = (int64u)offset;
//    return meetcore_downloadseek2(opindex, tmpoffset);
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_paperless_sdk_Call_userDownloadclose(JNIEnv *env, jobject thiz, jint opindex) {
//    return meetcore_downloadclose2(opindex);
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_initWalletSys(JNIEnv *env, jobject thiz, jbyteArray pdata) {
    int ret = -1;
    int *cpu = 0;
    do {
        if (logable) LOGI("%s: %d try to int", __FUNCTION__, __LINE__);
        if (logable) LOGI("%s: %d try to int sizeof:%d", __FUNCTION__, __LINE__, sizeof(cpu));
        if (g_pinternalparam)
            return 0;
        if (logable) LOGI("%s: %d start to init ", __FUNCTION__, __LINE__);
        if (0 != jni_initforjava(env, thiz))
            break;
        if (logable) LOGI("%s: %d finish init base jni for java, try to init meetsystem ", __FUNCTION__, __LINE__);
        jbyte *jBuf = env->GetByteArrayElements(pdata, JNI_FALSE);
        jint length = env->GetArrayLength(pdata);
        MeetPBCore_CallBack cb = {0};
        cb.pfunc = CallbackFunc;
        cb.plogfunc = CBLogInfo;
        if (logable) LOGI("%s: %d ", __FUNCTION__, __LINE__);
        SetmeetPB_callbackFunction(&cb);
        if (logable) LOGI("%s: %d ", __FUNCTION__, __LINE__);
        meetcore_InitExceptionCacth();
        ret = meetPB_call(0, 0, jBuf, length, 0, 0, 0);
        meetcore_crashtraceinit("android mainthread");
        env->ReleaseByteArrayElements(pdata, jBuf, 0);
        StuAndroidDispCB dispinfo = {0};
        dispinfo.cbStart = localCB_DISPSTART;
        dispinfo.cbStop = localCB_DISPSTOP;
        dispinfo.cbData = localCB_DISPDATA;
        AndroidDevice_initvideoPlay(&dispinfo);
        if (logable) LOGI("%s: %d finish init meet system ret:%d", __FUNCTION__, __LINE__, ret);
        if (ret == ERROR_MEET_INTERFACE_NULL) {
            if (logable) LOGI("%s: %d, init meetsystem error, errret:%d, maybe server not online ", __FUNCTION__, __LINE__, ret);
            ret = -1;
        } else {
            if (logable) LOGI("%s: %d, success init meetsystem ", __FUNCTION__, __LINE__);
            ret = 0;
        }
    } while (0);
    if (logable) LOGI("%s: %d, breakout init meetsystem", __FUNCTION__, __LINE__);
    return ret;
}

//android device capture
extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_initAndCapture(JNIEnv *env, jobject thiz, jint type, jint channelstart) {
    if (logable) LOGI(
            "------------------------------------------------jni_AndroidDevice_initcapture line:%d!\n",
            __LINE__);
    SetAndroidDeviceLogCallBack(InternalAndroidDevice_LogCB);
    SetAndroidDeviceGetInfoCallBack(InternalAndroidDevice_GetInfoCB);
    if (AndroidDevice_initcapture(type, channelstart) == 0) {
        if (logable) LOGI(
                "----------------------------------------------------------jni_AndroidDevice_initcapture type: %d, channel:%d!\n",
                type, channelstart);
        return 0;
    }

    if (logable) LOGI(
            "------------------------------------------------jni_AndroidDevice_initcapture line:%d erro!\n",
            __LINE__);
    return -1;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_initRtspcapture(JNIEnv *env, jobject thiz, jint channelstart, jint num) {
    LOGI("----jni_AndroidDevice_InitRtspcapture startch:%d, num:%d!\n", __LINE__, channelstart, num);
    if (AndroidDevice_InitRtspcapture(channelstart, num) == 0) {
        LOGI("------jni_AndroidDevice_InitRtspcapture startch:%d, num:%d ok!\n", __LINE__, channelstart, num);
        return 0;
    }

    LOGI("---------jni_AndroidDevice_InitRtspcapture faild, startch:%d, num:%d!\n", __LINE__, channelstart, num);
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_paperless_sdk_Call_crashinit(JNIEnv *env, jobject thiz, jint mode, jstring pdata) {
    if (mode == 0) {
        char bufferstr[128] = {0};
        const char *jBuf = env->GetStringUTFChars(pdata, NULL);
        if (jBuf) {
            int length = strlen(jBuf);
            if (length >= 128)
                length = 120;
            memcpy(bufferstr, jBuf, length);
            bufferstr[length] = 0;
            meetcore_crashtrace_smartpushex("%s", bufferstr);
            env->ReleaseStringUTFChars(pdata, jBuf);
        }
    } else if (mode == 1) {
        char bufferstr[128] = {0};
        const char *jBuf = env->GetStringUTFChars(pdata, NULL);
        if (!jBuf) {
            sprintf(bufferstr, "other android thread");
        } else {
            int length = strlen(jBuf);
            if (length >= 128)
                length = 120;
            memcpy(bufferstr, jBuf, length);
            bufferstr[length] = 0;
            env->ReleaseStringUTFChars(pdata, jBuf);
        }
        meetcore_crashtraceinit(bufferstr);
    } else {
        meetcore_crashtrace_reset();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_paperless_sdk_Call_switchAgendav3(JNIEnv *env, jobject thiz, jint enable) {
    meetcore_enableagendav3(enable);
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_call(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe, jlong pts,
                           jbyteArray pdata) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    jbyte *jBuf = env->GetByteArrayElements(pdata, JNI_FALSE);
    jint length = env->GetArrayLength(pdata);
    int64u pts_l = (int64u) pts;
    //if(logable) LOGI("capturedata:type:%d iskeyframe: %d, pts:%llu, len:%d!\n", channelstart, iskeyframe, pts_l, length);

    AndroidDevice_call(channelstart, (char *) jBuf, length, pts_l, iskeyframe);
    env->ReleaseByteArrayElements(pdata, jBuf, 0);
    return 0;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_bytebuffercall(JNIEnv *env, jobject thiz, jint channelstart, jint iskeyframe,
                                     jlong pts, jobject dbuf, jint length) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    char *jBuf = (char *) (env->GetDirectBufferAddress(dbuf));
//    int length = env->GetDirectBufferCapacity(dbuf);
    int64u pts_l = (int64u) pts;
    LOGI("capturedata: jBuf %p type:%d iskeyframe: %d, pts:%llu, len:%d!\n", jBuf, channelstart,
         iskeyframe, pts_l, length);

    AndroidDevice_call(channelstart, (char *) jBuf, length, pts_l, iskeyframe);
    return 0;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_NV21ToI420(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width,
                             jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_NV21ToI420(jBuf, width, jBuf + (width * height), width,
                                      jyuvBuf, width, jyuvBuf + (width * height), width / 2,
                                      jyuvBuf + (width * height) + (width / 2 * height / 2),
                                      width / 2, width, height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = width * height * 3 / 2;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_NV21ToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width,
                             jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_NV21ToNV12(jBuf, width, jBuf + (width * height), width, jyuvBuf, width,
                                      jyuvBuf + (width * height), width, width, height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = width * height * 3 / 2;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_I420ToNV12(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width,
                             jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_I420ToNV12(jBuf, width, jBuf + (width * height), width / 2,
                                      jBuf + (width * height) + (width / 2 * height / 2), width / 2,
                                      jyuvBuf, width, jyuvBuf + (width * height), width, width,
                                      height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    ret = width * height * 3 / 2;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_RGBToI420(JNIEnv *env, jobject thiz, int rgbmode, jobject dbuf, jobject outdbuf,
                            jint width, jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_RGBToI420(rgbmode, jBuf, width * 4, jyuvBuf, width,
                                     jyuvBuf + (width * height), width / 2,
                                     jyuvBuf + (width * height) + (width / 2 * height / 2),
                                     width / 2, width, height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = width * height * 3 / 2;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_I420Scale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth,
                            jint srcheight, jint dstwidth, jint dstheight) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_I420Scale(jBuf, srcwidth, jBuf + (srcwidth * srcheight), srcwidth / 2,
                                     jBuf + (srcwidth * srcheight) + (srcwidth / 2 * srcheight / 2),
                                     srcwidth / 2, srcwidth, srcheight,
                                     jyuvBuf, dstwidth, jyuvBuf + (dstwidth * dstheight),
                                     dstwidth / 2, jyuvBuf + (dstwidth * dstheight) +
                                                   (dstwidth / 2 * dstheight / 2), dstwidth / 2,
                                     dstwidth, dstheight)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = dstwidth * dstheight * 3 / 2;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_ARGBScale(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint srcwidth,
                            jint srcheight, jint dstwidth, jint dstheight) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 != AndroidDevice_ARGBScale(jBuf, srcwidth * 4, srcwidth, srcheight, jyuvBuf, dstwidth * 4,
                                     dstwidth, dstheight)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = dstwidth * dstheight * 4;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_ARGBToXRGB(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf,
                                 int dstrgbmode, jint width, jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    int dst_stride_argb = 0;
    switch (dstrgbmode) {
        case 0://(abgr in memory)
        case 2://(argb in memory)
        case 3://(rgba in memory)
            dst_stride_argb = width * 4;
            break;
        case 4://(bgr in memory)
        case 5://(rgb in memory)
            dst_stride_argb = width * 3;
            break;
        default:
            return ret;
    }

    if (0 != AndroidDevice_ARGBToXRGB(jBuf, width * 4, dstrgbmode, jyuvBuf, dst_stride_argb, width,
                                      height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = dst_stride_argb * height;
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_ARGBToNV21(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf, jint width,
                             jint height) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    if (jBuf == NULL || jyuvBuf == NULL) {
        LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
        return ret;
    }

    if (0 !=
        AndroidDevice_ARGBToNV21(jBuf, width * 4, jyuvBuf, width, jyuvBuf + (width * height), width,
                                 width, height)) {
        LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
        return ret;
    }
    ret = width * height * 3 / 2;
    return ret;
}


extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_RGBToNV12(JNIEnv *env, jobject thiz, int rgbmode, jobject dbuf, jobject outdbuf,
                            jint srcwidth, jint srcheight, jint dstwidth, jint dstheight,
                            jint rowStride) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);
    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    do {
        if (dstheight <= 0 || dstwidth == 0) {
            dstwidth = srcwidth;
            dstheight = srcheight;
        }
        if (jBuf == NULL || jyuvBuf == NULL) {
            LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
            break;
        }

        if (rowStride <= 0)
            rowStride = srcwidth * 4;
        if (0 != AndroidDevice_RGBToI420(rgbmode, jBuf, rowStride, jyuvBuf, srcwidth,
                                         jyuvBuf + (srcwidth * srcheight), srcwidth / 2,
                                         jyuvBuf + (srcwidth * srcheight) + (srcwidth / 2 * srcheight / 2),
                                         srcwidth / 2, srcwidth, srcheight)) {
            LOGI("-%s line:%d convert failed,rgbmode:%d!\n", __FUNCTION__, __LINE__, rgbmode);
            break;
        }


        if (srcwidth != dstwidth || srcheight != dstheight) {
            if (0 != AndroidDevice_I420Scale(jyuvBuf, srcwidth, jyuvBuf + (srcwidth * srcheight),
                                             srcwidth / 2, jyuvBuf + (srcwidth * srcheight) +
                                                           (srcwidth / 2 * srcheight / 2),
                                             srcwidth / 2, srcwidth, srcheight,
                                             jBuf, dstwidth, jBuf + (dstwidth * dstheight),
                                             dstwidth / 2, jBuf + (dstwidth * dstheight) +
                                                           (dstwidth / 2 * dstheight / 2),
                                             dstwidth / 2, dstwidth, dstheight)) {
                LOGI("-%s line:%d scale failed, w*h:%d*%d-->%d*%d!\n", __FUNCTION__, __LINE__,
                     srcwidth, srcheight, dstwidth, dstheight);
                break;
            }

            if (0 != AndroidDevice_I420ToNV12(jBuf, dstwidth, jBuf + (dstwidth * dstheight) + (dstwidth / 2 * dstheight / 2),
                                              dstwidth / 2,
                                              jBuf + (dstwidth * dstheight),
                                              dstwidth / 2,
                                              jyuvBuf, dstwidth, jyuvBuf + (dstwidth * dstheight),
                                              dstwidth, dstwidth, dstheight)) {
                LOGI("-%s line:%d scale convert failed!\n", __FUNCTION__, __LINE__);
                return ret;
            }
            ret = dstwidth * dstheight * 3 / 2;
        } else {
            if (0 !=
                AndroidDevice_I420ToNV12(jyuvBuf, srcwidth, jyuvBuf + (srcwidth * srcheight) + (srcwidth / 2 * srcheight / 2),
                                         srcwidth / 2,
                                         jyuvBuf + (srcwidth * srcheight),
                                         srcwidth / 2,
                                         jBuf, srcwidth, jBuf + (srcwidth * dstheight),
                                         srcwidth,
                                         srcwidth, srcheight)) {
                LOGI("-%s line:%d noscale convert failed!\n", __FUNCTION__, __LINE__);
                return ret;
            }
            ret = srcwidth * srcheight * 3 / 2;
            memcpy(jyuvBuf, jBuf, ret);
        }
    } while (0);
    return ret;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_RGBToNV12EX(JNIEnv *env, jobject thiz, jobject dbuf, jobject outdbuf,
                                  jint srcwidth, jint srcheight, jint dstwidth, jint dstheight) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0;
    int8u *jBuf = (int8u *) (env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *) (env->GetDirectBufferAddress(outdbuf));
    do {
        if (dstheight <= 0 || dstwidth <= 0) {
            dstwidth = srcwidth;
            dstheight = srcheight;
        }
        if (jBuf == NULL || jyuvBuf == NULL) {
            LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
            break;
        }

        if (srcwidth != dstwidth || srcheight != dstheight) {
            if (0 != AndroidDevice_ARGBScale(jBuf, srcwidth * 4, srcwidth, srcheight, jyuvBuf,
                                             dstwidth * 4, dstwidth, dstheight)) {
                LOGI("-%s line:%d convert failed!\n", __FUNCTION__, __LINE__);
                break;
            }
            if (0 != AndroidDevice_ARGBToNV12(jyuvBuf, dstwidth * 4,
                                              jBuf, dstwidth, jBuf + (dstwidth * dstheight),
                                              dstwidth, dstwidth, dstheight)) {
                LOGI("-%s line:%d scale convert failed!\n", __FUNCTION__, __LINE__);
                break;
            }
            ret = dstwidth * dstheight * 3 / 2;
            memcpy(jyuvBuf, jBuf, ret);
        } else {
            if (0 != AndroidDevice_ARGBToNV12(jBuf, srcwidth * 4,
                                              jyuvBuf, srcwidth, jyuvBuf + (srcwidth * dstheight),
                                              srcwidth, srcwidth, dstheight)) {
                LOGI("-%s line:%d noscale convert failed!\n", __FUNCTION__, __LINE__);
                break;
            }
        }

    } while (0);
    return ret;
}

 extern "C" JNIEXPORT jbyteArray JNICALL
 Java_com_paperless_sdk_Call_RGBToNV12BA(JNIEnv *env, jobject thiz, int rgbmode, jbyteArray pdata,
                                         jint srcwidth, jint srcheight, jint dstwidth,
                                         jint dstheight) {
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    int ret = 0, size;
    int8u *jBuf = (int8u *) env->GetByteArrayElements(pdata, JNI_FALSE);
    jint length = env->GetArrayLength(pdata);
    int8u *jyuvBuf = NULL;
    int8u *ptmpbuf = NULL;
    do {
        size = srcwidth * srcheight * 3 / 2;
        if (dstheight <= 0 || dstwidth == 0) {
            dstwidth = srcwidth;
            dstheight = srcheight;
        }
        if (dstwidth > srcwidth || dstheight > srcheight)
            size = dstwidth * dstheight * 3 / 2;
        ptmpbuf = new int8u[size];
        jyuvBuf = new int8u[size];
        if (jBuf == NULL || ptmpbuf == NULL || jyuvBuf == NULL) {
            LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
            break;
        }

        if (0 != AndroidDevice_RGBToI420(rgbmode, jBuf, srcwidth * 4, ptmpbuf, srcwidth,
                                         ptmpbuf + (srcwidth * srcheight), srcwidth / 2,
                                         ptmpbuf + (srcwidth * srcheight) +
                                         (srcwidth / 2 * srcheight / 2), srcwidth / 2, srcwidth,
                                         srcheight)) {
            LOGI("-%s line:%d convert failed,rgbmode:%d!\n", __FUNCTION__, __LINE__, rgbmode);
            break;
        }

        if (srcwidth != dstwidth || srcheight != dstheight) {
            if (0 != AndroidDevice_I420Scale(jyuvBuf, srcwidth, jyuvBuf + (srcwidth * srcheight),
                                             srcwidth / 2, jyuvBuf + (srcwidth * srcheight) +
                                                           (srcwidth / 2 * srcheight / 2),
                                             srcwidth / 2, srcwidth, srcheight,
                                             ptmpbuf, dstwidth, ptmpbuf + (dstwidth * dstheight),
                                             dstwidth / 2, ptmpbuf + (dstwidth * dstheight) +
                                                           (dstwidth / 2 * dstheight / 2),
                                             dstwidth / 2, dstwidth, dstheight)) {
                LOGI("-%s line:%d scale failed, w*h:%d*%d-->%d*%d!\n", __FUNCTION__, __LINE__,
                     srcwidth, srcheight, dstwidth, dstheight);
                break;
            }
            if (0 != AndroidDevice_I420ToNV12(ptmpbuf, dstwidth, ptmpbuf + (dstwidth * dstheight),
                                              dstwidth / 2, ptmpbuf + (dstwidth * dstheight) +
                                                            (dstwidth / 2 * dstheight / 2),
                                              dstwidth / 2,
                                              jyuvBuf, dstwidth, jyuvBuf + (dstwidth * dstheight),
                                              dstwidth, dstwidth, dstheight)) {
                LOGI("-%s line:%d scale convert failed!\n", __FUNCTION__, __LINE__);
                break;
            }
            ret = dstwidth * dstheight * 3 / 2;
        } else {
            if (0 != AndroidDevice_I420ToNV12(ptmpbuf, srcwidth, ptmpbuf + (srcwidth * srcheight),
                                              srcwidth / 2, ptmpbuf + (srcwidth * srcheight) +
                                                            (srcwidth / 2 * srcheight / 2),
                                              srcwidth / 2,
                                              jyuvBuf, srcwidth, jyuvBuf + (srcwidth * srcheight),
                                              srcwidth, srcwidth, srcheight)) {
                LOGI("-%s line:%d noscale convert failed!\n", __FUNCTION__, __LINE__);
                break;
            }
            ret = srcwidth * srcheight * 3 / 2;
        }
    } while (0);
    if (ptmpbuf)
        delete[] ptmpbuf;

    if (ret == 0) {
        if (jyuvBuf)
            delete[] jyuvBuf;
        return NULL;
    }

    jbyteArray barray = env->NewByteArray(ret);
    if (barray == NULL) {
        LOGI("-%s line:%d NewByteArray size:%d failed!\n", __FUNCTION__, __LINE__, ret);
    } else {
        env->SetByteArrayRegion(barray, 0, ret, (const jbyte *) jyuvBuf);
    }
    env->ReleaseByteArrayElements(pdata, (jbyte *) jBuf, 0);
    if (jyuvBuf)
        delete[] jyuvBuf;

    return barray;
}


int SwitchRGBPixfmt(int rgbmode)
{
    int infmt = -1;
    switch (rgbmode)
    {
        case 0://(abgr in memory)
            infmt = AD_PIX_FMT_ABGR;
            break;
        case 2://(argb in memory)
            infmt = AD_PIX_FMT_ARGB;
            break;
        case 3://(rgba in memory)
            infmt = AD_PIX_FMT_RGBA;
            break;
        case 4://(bgr in memory)
            infmt = AD_PIX_FMT_BGR24;
            break;
        case 5://(rgb in memory)
            infmt = AD_PIX_FMT_RGB24;
            break;
        default:
            infmt = -1;
            break;
    }
    return infmt;
}

extern "C" JNIEXPORT int JNICALL
Java_com_paperless_sdk_Call_FFmpegRGBToNV12(JNIEnv *env, jobject thiz,int rgbmode, jobject dbuf, jobject outdbuf,
                                                              jint srcwidth, jint srcheight, jint dstwidth, jint dstheight, jint rowslide)
{
    meetcore_crashtrace_reset();
    meetcore_crashtrace_smartpushex("%s", __FUNCTION__);

    void* ppixconvert = NULL;
    int ret = 0;
    int8u *jBuf = (int8u *)(env->GetDirectBufferAddress(dbuf));
    int8u *jyuvBuf = (int8u *)(env->GetDirectBufferAddress(outdbuf));
    do
    {
        if (dstheight <= 0 || dstwidth <= 0)
        {
            dstwidth = srcwidth;
            dstheight = srcheight;
        }
        if (jBuf == NULL || jyuvBuf == NULL)
        {
            LOGI("-%s line:%d param is empty!\n", __FUNCTION__, __LINE__);
            break;
        }

        if (rowslide <= 0)
            rowslide = srcwidth * 4;

        int infmt = SwitchRGBPixfmt(rgbmode), outfmt = AD_PIX_FMT_NV12;
        if (infmt == -1)
        {
            LOGI("-%s line:%d rgbmode:%d not support!\n", __FUNCTION__, __LINE__, rgbmode);
            break;
        }

        ppixconvert = AndroidDevice_video_convertinit(dstwidth, dstheight, outfmt, srcwidth, srcheight,infmt);
        if (ppixconvert == NULL)
        {
            LOGI("-%s line:%d Init ffmpeg convert failed,rgbmode:%d!\n", __FUNCTION__, __LINE__, rgbmode);
            break;
        }

        int8u*  inbuf[AD_MAX_PIX_LINESIZE] = { 0 };
        int*     ptmpoutlinesize = NULL;
        int8u**  ptmpout = NULL;
        int      inlinesize[AD_MAX_PIX_LINESIZE] = { 0 };

        inbuf[0] = jBuf;
        inlinesize[0] = rowslide;
        if (0 != AndroidDevice_video_convertex(ppixconvert, jyuvBuf, inbuf, inlinesize))
        {
            LOGI("-%s line:%d do ffmpeg convert failed,rgbmode:%d, rowslide:%d[%d,%d]-->[%d,%d]!\n", __FUNCTION__, __LINE__, rgbmode, rowslide, srcwidth, srcheight, dstwidth, dstheight);
            break;
        }

        ret = dstwidth * dstheight * 3 / 2;
    } while (0);

    if (ppixconvert)
        AndroidDevice_video_convertfree(ppixconvert);
    return ret;
}

//</editor-fold>