package com.paperless.sdk

import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.bus.Bus
import com.paperless.bus.SdkBusType
import com.paperless.data.FrameData
import com.paperless.data.YuvData
import com.paperless.player.DecodeQueue
import com.paperless.player.Fps
import com.paperless.player.FrameDataPool
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 *  @author : Administrator
 *  created on 2025/7/5 16:49
 */
object Call {

    lateinit var m_dbuf: ByteBuffer
    lateinit var m_dexbuf: ByteBuffer
    fun initSetDirectBuf() {
        m_dbuf = ByteBuffer.allocateDirect(SdkVars.frame_size)
        m_dexbuf = ByteBuffer.allocateDirect(SdkVars.frame_codec_size)
        initDirectBuf(m_dbuf, m_dexbuf)
    }

    /**
     * 初始化之前调用，切换议题3模式
     *
     * @param enable =1 强制开启议题3
     */
    external fun switchAgendav3(enable: Int)

    external fun initDirectBuf(dbuf: ByteBuffer, dexbuf: ByteBuffer)

    //初始化无纸化接口
    //data 参考无纸化接口对照表
    //成功返回0 失败返回-1
    external fun initWalletSys(data: ByteArray): Int

    /**
     * 判断缓存是否好了
     * @param type 类型
     * @param id 二级数据时传入的id， e.g  缓存某个目录的文件时传目录id
     * @param cacheflag =1 表示强制缓存
     * @return = 0;  //操作成功，或者数据为空，视操作请求定 [InterfaceMacro.Pb_Error]
     */
    external fun checkcache(type: Int, id: Int, cacheflag: Int): Int

    //无纸化功能接口中调用
    //type 功能类型
    //method 功能类型的方法
    //data  方法需要的参数 参考无纸化接口对照表
    //成功返回对应的数组 失败返回null数组
    external fun callMethod(type: Int, method: Int, data: ByteArray?): ByteArray?

    external fun enablebackgroud(type: Int)

    /**
     * 初始化桌面、摄像头采集
     *
     * @param type         流类型
     * @param channelindex 流通道索引值
     * @return 成功返回0 失败返回-1
     */
    external fun initAndCapture(type: Int, channelindex: Int): Int

    /**
     * @param channel 通道 4
     * @param count   个数
     */
    external fun initRtspcapture(channel: Int, count: Int): Int

    //初始化桌面、摄像头采集
    //type 流类型
    //data 采集数据
    //成功返回0 失败返回-1
    external fun call(type: Int, iskeyframe: Int, pts: Long, data: ByteArray?): Int


    external fun bytebuffercall(
        type: Int,
        iskeyframe: Int,
        pts: Long,
        buffer: ByteBuffer?,
        length: Int
    ): Int

    external fun NV21ToI420(src: ByteBuffer?, dst: ByteBuffer?, w: Int, h: Int): Int

    external fun NV21ToNV12(src: ByteBuffer?, dst: ByteBuffer?, w: Int, h: Int): Int

    external fun I420ToNV12(src: ByteBuffer?, dst: ByteBuffer?, w: Int, h: Int): Int

    /**
     * //rgbmode= 0://(abgr in memory)
     * //rgbmode= 1://(bgra in memory)
     * //rgbmode= 2://(argb in memory)
     * //rgbmode= 3://(rgba in memory)
     * //rgbmode= 4://(bgr in memory)
     * //rgbmode= 5://(rgb in memory)
     */
    external fun RGBToI420(mode: Int, src: ByteBuffer?, dst: ByteBuffer?, w: Int, h: Int): Int

    external fun RGBToNV12BA(
        mode: Int,
        src: ByteArray,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): ByteArray

    external fun RGBToNV12(
        mode: Int,
        src: ByteBuffer,
        dst: ByteBuffer,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int,
        rowStride: Int
    ): Int

//    external fun FFmpegRGBToNV12(
//        mode: Int,
//        src: ByteBuffer,
//        dst: ByteBuffer,
//        srcW: Int,
//        srcH: Int,
//        dstW: Int,
//        dstH: Int,
//        rowStride: Int
//    ): Int

    external fun RGBToNV12EX(
        src: ByteBuffer,
        dst: ByteBuffer,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): Int

    external fun I420Scale(
        src: ByteBuffer,
        dst: ByteBuffer,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): Int

    external fun ARGBScale(
        src: ByteBuffer,
        dst: ByteBuffer,
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): Int

    external fun ARGBToXRGB(
        src: ByteBuffer,
        dst: ByteBuffer,
        mode: Int,
        srcW: Int,
        srcH: Int
    ): Int

    external fun ARGBToNV21(src: ByteBuffer, dst: ByteBuffer, srcW: Int, srcH: Int): Int


    /**
     * 初始化一个下载会话
     *
     * @param mediaId 文件媒体id
     * @return 成功返回一个会议索引，失败返回-1
     */
    external fun userDownloadinit(mediaId: Long): Int

    /**
     * 下载文件数据
     *
     * @param opindex  会话索引
     * @param dbuf     用来接收数据的ByteBuffer
     * @param readsize 需要的数据大小
     * @return 返回读取的大小
     */
    external fun userDownloadread(opindex: Int, dbuf: ByteBuffer?, readsize: Int): Int

    /**
     * 设置下载的偏移
     *
     * @param opindex 会议索引
     * @param offset  偏移量
     * @return 成功返回0，失败返回-1
     */
    external fun userDownloadseek(opindex: Int, offset: Long): Int

    /**
     * 关闭会话
     *
     * @param opindex 会话索引
     */
    external fun userDownloadclose(opindex: Int)

    /**
     * @param mode =0记录方法 =1注册线程 =2重置
     * @param name 方法/线程名称,不能超过128个字符
     */
    external fun crashinit(mode: Int, name: String?)

    var COLOR_FORMAT = 1
    fun callback(type: Int, oper: Int): Int {
        when (oper) {
            //pixel format
            1 -> {
                return if (type == 2) 1 else 0
            }
            //width
            2 -> {
                return if (type == 2) {
                    LogUtils.e("SdkVars.record_width: " + SdkVars.record_width);
                    SdkVars.record_width
                } else {
                    SdkVars.camera_width
                }
            }
            //height
            3 -> {
                return if (type == 2) {
                    LogUtils.e("SdkVars.record_height: " + SdkVars.record_height);
                    SdkVars.record_height
                } else SdkVars.camera_height
            }
            // start capture
            4 -> {
                LogUtils.e("通知开始采集 type:$type")
                Bus.postObj(type = SdkBusType.capture_start, type)
                return 0
            }
            // stop carture
            5 -> {
                LogUtils.e("通知停止采集 type:$type")
                Bus.postObj(type = SdkBusType.capture_stop, type)
                return 0
            }

            else -> {
                return 0
            }
        }
    }

    fun callback_yuvdisplay(
        res: Int,
        w: Int,
        h: Int,
        y: ByteArray,
        u: ByteArray,
        v: ByteArray
    ): Int {
        Bus.postObj(SdkBusType.yuv_data, YuvData(res, w, h, y, u, v))
        return 0
    }

    fun callback_videodecode(
        isKeyframe: Int,
        res: Int,
        codecid: Int,
        w: Int,
        h: Int,
        packet: ByteArray,
        pts: Long,
        codecdata: ByteArray
    ): Int {
        if (SdkConfig.logEnable) LogUtils.e("后台接收 datalen：${packet.size}")
        // 优化内存使用对象池
        val frameData = FrameDataPool.obtain()
        frameData.isKeyFrame = isKeyframe
        frameData.res = res
        frameData.codecid = codecid
        frameData.w = w
        frameData.h = h
        frameData.pts = pts
        frameData.setPacketBytes(packet)
        frameData.setCodecDataBytes(codecdata)
        Fps.add(res)
        // 放入到待处理解码的集合中
        if (!DecodeQueue.offer(res, frameData)) {
            // 队列已满，尝试移除最旧的非关键帧
            DecodeQueue.remove(frameData)
        }
        return 0
    }

    fun callback_startdisplay(res: Int): Int {
        return 0
    }

    fun callback_stopdisplay(res: Int): Int {
        return 0
    }

    fun error_ret(type: Int, method: Int, ret: Int) {
        if (SdkConfig.logEnable) LogUtils.e("error_ret：type=$type,method=$method,ret=$ret")
    }

    fun callback_method(type: Int, method: Int, data: ByteArray?, datalen: Int): Int {
        if (type != 1) {
            if (SdkConfig.logEnable) LogUtils.e("callback_method：type=$type,method=$method,datalen=$datalen")
        }
        Bus.post(type = type, method = method, data = data)
        return 0
    }

    fun callback_directvideodecode(
        isKeyframe: Int,
        res: Int,
        codecid: Int,
        w: Int,
        h: Int,
        datalen: Int,
        pts: Long,
        codecdatalen: Int
    ): Int {
        if (SdkConfig.isUseSdkPlayer) {
            sdkPlayer(isKeyframe, res, codecid, w, h, datalen, pts, codecdatalen)
        } else {
            m_dbuf.position(0)
            m_dbuf.limit(datalen)
            m_dexbuf.position(0)
            m_dexbuf.limit(codecdatalen)
            if (res == Protocol.resource_id_0) {
                SdkVars.frame_count++
            }
            var decodeQueue = SdkVars.decodeMap[res]
            if (decodeQueue == null) {
                if (SdkConfig.logEnable) LogUtils.e("player_log", "新建 LinkedBlockingQueue<FrameData>")
                val value = LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY)
                SdkVars.decodeMap.put(res, value)
                decodeQueue = value
            }
            var frameData = SdkVars.frameDataPool.poll()
            if (frameData == null) {
                frameData = FrameData()
                if (SdkConfig.logEnable) LogUtils.e("player_log", "新建对象 size=${decodeQueue.size}")
            }
            frameData.isKeyFrame = isKeyframe
            frameData.res = res
            frameData.codecid = codecid
            frameData.w = w
            frameData.h = h
            frameData.pts = pts
            frameData.setPacketBuffer(m_dbuf)
            frameData.setCodecDataBuffer(m_dexbuf)
            if (!decodeQueue.offer(frameData)) {
                //添加失败就把最旧的数据删除后再添加
                if (decodeQueue.poll() != null) {
                    val offer = decodeQueue.offer(frameData)
                    if (SdkConfig.logEnable) LogUtils.e("player_log", "添加失败就把最旧的数据删除后再添加，offer=$offer")
                }
            }
        }
        return 0
    }

    fun sdkPlayer(
        isKeyframe: Int,
        res: Int,
        codecid: Int,
        w: Int,
        h: Int,
        datalen: Int,
        pts: Long,
        codecdatalen: Int
    ) {
        m_dbuf.position(0)
        m_dbuf.limit(datalen)
        m_dexbuf.position(0)
        m_dexbuf.limit(codecdatalen)
        // 优化内存使用对象池
        val frameData = FrameDataPool.obtain()
        frameData.isKeyFrame = isKeyframe
        frameData.res = res
        frameData.codecid = codecid
        frameData.w = w
        frameData.h = h
        frameData.pts = pts
        frameData.setPacketBuffer(m_dbuf)
        frameData.setCodecDataBuffer(m_dexbuf)
        Fps.add(res)
        // 放入到待处理解码的集合中
        if (!DecodeQueue.offer(res, frameData)) {
            // 队列已满，尝试移除最旧的非关键帧
            if (SdkConfig.logEnable) Log.d("", "sdkPlayer: 队列已满，尝试移除最旧的非关键帧")
            DecodeQueue.remove(frameData)
        }
    }
}