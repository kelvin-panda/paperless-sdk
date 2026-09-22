package com.paperless.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import com.blankj.utilcode.util.LogUtils
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
                    val s = Size(width, height)
                    diag("type0-原尺寸可用", width, height, s)
                    return s
                }
            }

            1 -> {
                if (checkSize(width, height, 1920, 1080)) {
                    val s = Size(1920, 1080)
                    diag("type1-预设 16:9 (1920x1080)", width, height, s)
                    return s
                }
                if (ratio == "0.8") if (checkSize(width, height, 1920, 1530)) {
                    val s = Size(1920, 1536)
                    diag("type1-预设 5:4 (1920x1536)", width, height, s)
                    return s
                }
                if (ratio == "0.75") if (checkSize(width, height, 1920, 1400)) {
                    val s = Size(1920, 1400)
                    diag("type1-预设 4:3 (1920x1400)", width, height, s)
                    return s
                }
                if (ratio == "0.625") if (checkSize(width, height, 1920, 1200)) {
                    val s = Size(1920, 1200)
                    diag("type1-预设 16:10 (1920x1200)", width, height, s)
                    return s
                }
                if (ratio == "0.6") if (checkSize(width, height, 1920, 1152)) {
                    val s = Size(1920, 1152)
                    diag("type1-预设 (1920x1152)", width, height, s)
                    return s
                }
                if (tempW > 1920) {
                    tempW = 1920
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    val s = Size(tempW, getSupportHeight(tempW, tempH))
                    diag("type1-能力兜底", width, height, s)
                    return s
                }
            }

            2 -> {
                if (checkSize(width, height, 1280, 720)) {
                    val s = Size(1280, 720)
                    diag("type2-预设 16:9 (1280x720)", width, height, s)
                    return s
                }
                if (ratio == "0.8") if (checkSize(width, height, 1280, 1024)) {
                    val s = Size(1280, 1024)
                    diag("type2-预设 5:4 (1280x1024)", width, height, s)
                    return s
                }
                if (ratio == "0.75") if (checkSize(width, height, 1280, 960)) {
                    val s = Size(1280, 960)
                    diag("type2-预设 4:3 (1280x960)", width, height, s)
                    return s
                }
                if (ratio == "0.625") if (checkSize(width, height, 1280, 800)) {
                    val s = Size(1280, 800)
                    diag("type2-预设 16:10 (1280x800)", width, height, s)
                    return s
                }
                if (ratio == "0.6") if (checkSize(width, height, 1280, 768)) {
                    val s = Size(1280, 768)
                    diag("type2-预设 (1280x768)", width, height, s)
                    return s
                }
                if (tempW > 1280) {
                    tempW = 1280
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    val s = Size(tempW, getSupportHeight(tempW, tempH))
                    diag("type2-能力兜底", width, height, s)
                    return s
                }
            }

            3 -> {
                if (checkSize(width, height, 720, 480)) {
                    val s = Size(720, 480)
                    diag("type3-预设 (720x480)", width, height, s)
                    return s
                }
                if (tempW > 720) {
                    tempW = 720
                    tempH = (tempH * calcRation(tempW, width)).toInt()
                }
                if (!isSizeSupported(tempW, tempH)) {
                    val s = Size(tempW, getSupportHeight(tempW, tempH))
                    diag("type3-能力兜底", width, height, s)
                    return s
                }
            }
        }
        val size = adjustSize(tempW, tempH)
        if (isSizeSupported(size.width, size.height)) {
            diag("adjustSize 分支", width, height, size)
            return size
        }
        val supportSize = getSupportSize(width, height)
        diag("getSupportSize 分支", width, height, supportSize)
        return supportSize
    }

    //<editor-fold desc="诊断：打印采集尺寸决策结果（临时，可整块删除）">
    /**
     * 打印"屏幕宽高 -> 最终采集宽高"，用于定位同屏画面比例变形
     * 注意：这里只打日志，不参与任何判断
     */
    private fun diag(branch: String, screenW: Int, screenH: Int, size: Size) {
        LogUtils.e(
            "DIAG-CodecUtil",
            "[DIAG-ENC] 命中分支=$branch 屏幕=${screenW}x$screenH" +
                    " 屏幕比例=${ratioOf(screenW, screenH)}" +
                    " 采集=${size.width}x${size.height}" +
                    " 采集比例=${ratioOf(size.width, size.height)}" +
                    " 比例是否一致=${ratioOf(screenW, screenH) == ratioOf(size.width, size.height)}"
        )
    }

    private fun ratioOf(w: Int, h: Int): String =
        if (w <= 0 || h <= 0) "n/a" else String.format(java.util.Locale.US, "%.4f", w.toDouble() / h.toDouble())
    //</editor-fold>

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