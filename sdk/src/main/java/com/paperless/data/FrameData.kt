package com.paperless.data

import java.nio.ByteBuffer

/**
 *  @author : Administrator
 *  created on 2025/7/7 9:28
 */
class FrameData(
    var isKeyFrame: Int = 0,
    var res: Int = 0,
    var codecid: Int = 0,
    var w: Int = 0,
    var h: Int = 0,
    var pts: Long = 0,
    var packet: ByteArray = ByteArray(0),
    var packetSize: Int = 0,
    var codecData: ByteArray = ByteArray(0),
    var codecDataSize: Int = 0
) {

    /**
     * 判断是否是关键帧
     */
    fun checkKeyFrame(): Boolean {
        return isKeyFrame and 4 == 4
    }

    /**
     * 获取视频的旋转角度
     */
    fun getRotation(): Int {
        if (isKeyFrame and 32 == 32) return 90
        if (isKeyFrame and 64 == 64) return 180
        if (isKeyFrame and 128 == 128) return 270
        return 0
    }

    fun setPacketBuffer(buf: ByteBuffer) {
        this.packetSize = buf.remaining()
        if (packet.size < packetSize) {
            packet = ByteArray(packetSize)
        }
        buf.get(packet, 0, this.packetSize)
    }

    fun setPacketBytes(bytes: ByteArray) {
        if (packet.size < bytes.size) {
            packet = ByteArray(bytes.size)
        }
        System.arraycopy(bytes, 0, packet, 0, bytes.size)
        packetSize = bytes.size
    }

    fun setCodecDataBuffer(buf: ByteBuffer) {
        this.codecDataSize = buf.remaining()
        if (codecData.size < codecDataSize) {
            codecData = ByteArray(codecDataSize)
        }
        buf.get(codecData, 0, this.codecDataSize)
    }

    fun setCodecDataBytes(bytes: ByteArray) {
        if (codecData.size < bytes.size) {
            codecData = ByteArray(bytes.size)
        }
        System.arraycopy(bytes, 0, codecData, 0, bytes.size)
        codecDataSize = bytes.size
    }

    fun clear() {
        isKeyFrame = 0
        res = 0
        codecid = 0
        w = 0
        h = 0
        pts = 0
        packetSize = 0
        codecDataSize = 0
        // 不需要清空字节数组内容，下次使用时会被覆盖
        packet = ByteArray(0)
        codecData = ByteArray(0)
    }

    override fun toString(): String {
        return "FrameData(isKeyFrame=$isKeyFrame, checkKeyFrame=${checkKeyFrame()} res=$res, codecid=$codecid, w=$w, h=$h, pts=$pts, packetSize=$packetSize, codecdataSize=$codecDataSize)"
    }
}
