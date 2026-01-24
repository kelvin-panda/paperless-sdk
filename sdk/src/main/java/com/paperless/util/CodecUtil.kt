package com.paperless.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import java.math.BigDecimal
import java.math.RoundingMode

/**
 *  @author : Administrator
 *  created on 2025/12/20 17:07
 */
object CodecUtil {
    fun getEncodeSize( width: Int, height: Int,type: Int): Size {
        val ratio = "${calcRation(height, width)}"
        var tempW = width
        var tempH = height
        when (type) {
            0 -> {
                if (isSizeSupported(width, height)) {
                    return Size(width, height)
                }
            }

            1 -> {
                if (checkSize(width, height, 1920, 1080)) return Size(1920, 1080)
                if (ratio == "0.8") if (checkSize(width, height, 1920, 1530)) return Size(1920, 1536)
                if (ratio == "0.75") if (checkSize(width, height, 1920, 1400)) return Size(1920, 1400)
                if (ratio == "0.625") if (checkSize(width, height, 1920, 1200)) return Size(1920, 1200)
                if (ratio == "0.6") if (checkSize(width, height, 1920, 1152)) return Size(1920, 1152)
                if (tempW > 1920) {
                    tempW = 1920
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    return Size(tempW, getSupportHeight(tempW, tempH))
                }
            }

            2 -> {
                if (checkSize(width, height, 1280, 720)) return Size(1280, 720)
                if (ratio == "0.8") if (checkSize(width, height, 1280, 1024)) return Size(1280, 1024)
                if (ratio == "0.75") if (checkSize(width, height, 1280, 960)) return Size(1280, 960)
                if (ratio == "0.625") if (checkSize(width, height, 1280, 800)) return Size(1280, 800)
                if (ratio == "0.6") if (checkSize(width, height, 1280, 768)) return Size(1280, 768)
                if (tempW > 1280) {
                    tempW = 1280
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    return Size(tempW, getSupportHeight(tempW, tempH))
                }
            }

            3 -> {
                if (checkSize(width, height, 720, 480)) return Size(720, 480)
                if (tempW > 720) {
                    tempW = 720
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    return Size(tempW, getSupportHeight(tempW, tempH))
                }
            }
        }
        val size = adjustSize(tempW, tempH)
        if (isSizeSupported(size.width, size.height)) {
            return size
        }
        return getSupportSize(width, height)
    }

    private fun getSupportSize(width: Int, height: Int): Size {
        var w = width
        var h = height
        if (isSizeSupported(w, h)) {
            return Size(w, h)
        }
        val vcb = getSupportedMediaCodecInfo()?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities
        val supWidths = vcb!!.supportedWidths
        w = supWidths.clamp(w)
        if (w % vcb.widthAlignment != 0) {
            w -= 1
            return getSupportSize(w, h)
        }
        val supHeights = vcb.getSupportedHeightsFor(w)
        h = supHeights.clamp(h)
        if (h % vcb.heightAlignment != 0) {
            h -= 1
            return getSupportSize(w, h)
        }
        if (isSizeSupported(w, h)) {
            return Size(w, h)
        }
        h -= vcb.widthAlignment
        return getSupportSize(w, h)
    }

    private fun adjustSize(width: Int, height: Int): Size {
        //用于将是奇数的宽高改成偶数
        val w = if ((width and 1) == 1) width - 1 else width
        val h = if ((height and 1) == 1) height - 1 else height
        return Size(w, h)
    }

    private fun getSupportHeight(width: Int, height: Int): Int {
        val vcb = getSupportedMediaCodecInfo()?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities
        var tempW = vcb!!.supportedWidths?.clamp(width) ?: 0
        if (tempW % vcb.widthAlignment != 0) {
            tempW -= 1
            return getSupportHeight(tempW, height)
        }
        val supHeights = vcb.getSupportedHeightsFor(tempW)
        var h = supHeights.clamp(height)
        if (vcb.isSizeSupported(tempW, h)) {
            return h
        } else {
            h -= vcb.widthAlignment
            return getSupportHeight(tempW, h)
        }
    }

    private fun calcRation(a: Int, b: Int): Double {
        val bgA = BigDecimal(a.toString())
        val bgB = BigDecimal(b.toString())
        val result = try {
            bgA.divide(bgB)
        } catch (e: ArithmeticException) { // 防止无限循环而报错  采用四舍五入保留3位有效数字
            bgA.divide(bgB, 3, RoundingMode.HALF_DOWN)
        }.apply {
            setScale(4, RoundingMode.HALF_UP)
        }
        return result.toDouble()
    }

    private fun checkSize(width: Int, height: Int, dstW: Int, dstH: Int): Boolean {
        if (width >= dstW && height >= dstH) {
            if (isSizeSupported(dstW, dstH)) {
                return true
            }
        }
        return false
    }

    private fun isSizeSupported(width: Int, height: Int): Boolean {
        return getSupportedMediaCodecInfo()?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities?.isSizeSupported(
            width,
            height
        ) ?: false
    }

    private fun getSupportedMediaCodecInfo(): MediaCodecInfo? {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = mediaCodecList.codecInfos
        codecInfos.filter { it.isEncoder }.forEach { info ->
            info.supportedTypes.find { it == MediaFormat.MIMETYPE_VIDEO_AVC }?.let {
                return info
            }
        }
        try {
            val mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            return mediaCodec.codecInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}