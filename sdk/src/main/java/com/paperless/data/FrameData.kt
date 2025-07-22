package com.paperless.data

import com.paperless.sdk.SdkVars
import java.nio.ByteBuffer

/**
 *  @author : Administrator
 *  created on 2025/7/7 9:28
 */
data class FrameData(
    var isKeyFrame: Int = 0,
    var res: Int = 0,
    var codecid: Int = 0,
    var w: Int = 0,
    var h: Int = 0,
    var pts: Long = 0,
    var packet: ByteArray = ByteArray(SdkVars.frame_size),
    var packetSize: Int = 0,
    var codecData: ByteArray = ByteArray(SdkVars.frame_codec_size),
    var codecDataSize: Int = 0
) {

    fun getRotation(): Int {
        if (isKeyFrame and 32 == 32) return 360 - 90
        if (isKeyFrame and 64 == 64) return 360 - 180
        if (isKeyFrame and 128 == 128) return 360 - 270
        return 0
    }

    fun setPacketBuffer(buf: ByteBuffer) {
        this.packetSize = buf.remaining()
        buf.get(packet, 0, this.packetSize)
    }

    fun setCodecDataBuffer(buf: ByteBuffer) {
        this.codecDataSize = buf.remaining()
        buf.get(codecData, 0, this.packetSize)
    }

    override fun toString(): String {
        return "FrameData(isKeyFrame=$isKeyFrame, res=$res, codecid=$codecid, w=$w, h=$h, pts=$pts, packetSize=$packetSize, codecdataSize=$codecDataSize)"
    }
}
