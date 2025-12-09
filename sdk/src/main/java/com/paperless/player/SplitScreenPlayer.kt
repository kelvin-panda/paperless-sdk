package com.paperless.player

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.blankj.utilcode.util.LogUtils
import com.paperless.data.YuvData
import com.paperless.player.gl.VideoGLSurfaceView
import com.paperless.sdk.R

/**
 *  @author : Administrator
 *  created on 2025/9/11 14:36
 */
class SplitScreenPlayer(cxt: Context, attrs: AttributeSet?) :
    ViewGroup(cxt, attrs) {

    private val controllerMap = mutableMapOf<Int, PlayerController?>()

    private var widthMeasureSpec = 0
    private var heightMeasureSpec = 0

    private var largeResId = -1
    private var isEnlarge = false//当前是否是最大的界面
    private val resIds = mutableListOf<Int>()
    var listener: SplitScreenPlayerClickListener? = null

    interface SplitScreenPlayerClickListener {
        fun onClick(resId: Int)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        this.widthMeasureSpec = widthMeasureSpec
        this.heightMeasureSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        measureChild(width, height)
    }

    private fun measureChild(parentWidth: Int, parentHeight: Int) {
        LogUtils.e("measureChild -->viewGroup的宽高：$parentWidth, $parentHeight")
        val videoView1 = getChildAt(0)
        val videoView2 = getChildAt(1)
        val videoView3 = getChildAt(2)
        val videoView4 = getChildAt(3)
        val params = LayoutParams(parentWidth, parentHeight)
        val params2_1 = LayoutParams(parentWidth / 2, parentHeight / 2)
        val params1_1 = LayoutParams(1, 1)
        when (childCount) {
            1 -> videoView1.layoutParams = params
            4 -> if (largeResId == -1) {
                videoView1.layoutParams = params2_1
                videoView2.layoutParams = params2_1
                videoView3.layoutParams = params2_1
                videoView4.layoutParams = params2_1
            } else if (largeResId == 1) {
                videoView1.layoutParams = params
                videoView2.layoutParams = params1_1
                videoView3.layoutParams = params1_1
                videoView4.layoutParams = params1_1
            } else if (largeResId == 2) {
                videoView1.layoutParams = params1_1
                videoView2.layoutParams = params
                videoView3.layoutParams = params1_1
                videoView4.layoutParams = params1_1
            } else if (largeResId == 3) {
                videoView1.layoutParams = params1_1
                videoView2.layoutParams = params1_1
                videoView3.layoutParams = params
                videoView4.layoutParams = params1_1
            } else if (largeResId == 4) {
                videoView1.layoutParams = params1_1
                videoView2.layoutParams = params1_1
                videoView3.layoutParams = params1_1
                videoView4.layoutParams = params
            }

            else -> {}
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 1) {
            layout1()
        } else {
            layout4()
        }
    }

    private fun layout1() {
        val videoGLSurfaceView = getChildAt(0) as VideoGLSurfaceView
        videoGLSurfaceView.layout(0, 0, videoGLSurfaceView.measuredWidth, videoGLSurfaceView.measuredHeight)
        videoGLSurfaceView.setOnClickListener { listener?.onClick(videoGLSurfaceView.getResId()) }
    }

    private fun layout4() {
        val p1 = getChildAt(0) as VideoGLSurfaceView
        val w1 = p1.measuredWidth
        val h1 = p1.measuredHeight
        p1.layout(0, 0, w1, h1)
        p1.setOnClickListener { listener?.onClick(p1.getResId()) }

        val p2 = getChildAt(1) as VideoGLSurfaceView
        val w2 = p2.measuredWidth
        val h2 = p2.measuredHeight
        p2.layout(w1, 0, w1 + w2, h2)
        p2.setOnClickListener { listener?.onClick(p2.getResId()) }

        val p3 = getChildAt(2) as VideoGLSurfaceView
        val w3 = p3.measuredWidth
        val h3 = p3.measuredHeight
        p3.layout(0, h1, w3, h1 + h3)
        p3.setOnClickListener { listener?.onClick(p3.getResId()) }

        val p4 = getChildAt(3) as VideoGLSurfaceView
        val w4 = p4.measuredWidth
        val h4 = p4.measuredHeight
        p4.layout(w3, h2, w3 + w4, h2 + h4)
        p4.setOnClickListener { listener?.onClick(p4.getResId()) }
    }

    fun createView(ids: MutableList<Int>) {
        clearAll()
        resIds.clear()
        resIds.addAll(ids)
        ids.forEach {
            val p = VideoGLSurfaceView(context, null)
            p.setResId(it)
            p.setBackgroundResource(R.drawable.player_select_bg)
            p.isClickable = true
            addView(p)
            val controller = PlayerController(it)
            controllerMap[it] = controller
            controller.initialize(p)
        }
        reDraw()
    }

    fun setSelectResId(resId: Int) {
        for (i in 0 until childCount) {
            val p = getChildAt(i)
            if (p is VideoGLSurfaceView) {
                p.isSelected = p.getResId() == resId
            }
        }
    }

    fun getSelectResId(): Int {
        for (i in 0 until childCount) {
            val p = getChildAt(i)
            if (p is VideoGLSurfaceView) {
                if (p.isSelected) return p.getResId()
            }
        }
        return -1
    }

    fun updateYuvData(yuvData: YuvData) {
        if (!resIds.contains(yuvData.res)) return
        for (i in 0 until childCount) {
            val p = getChildAt(i)
            if (p is VideoGLSurfaceView) {
                if (p.getResId() == yuvData.res) {
                    p.setFrameData(yuvData.w, yuvData.h, yuvData.y, yuvData.u, yuvData.v)
                    return
                }
            }
        }
    }

    fun stopResWork(resId: Int) {
        var index = -1
        for (i in 0 until childCount) {
            val p = getChildAt(i)
            if (p is VideoGLSurfaceView) {
                if (p.getResId() == resId) {
                    controllerMap[resId]?.release()
                    removeView(p)
                    index = i
                    break
                }
            }
        }
        if (index != -1) {
            insertView(index, resId)
        }
    }

    private fun insertView(index: Int, resId: Int) {
        val p = VideoGLSurfaceView(context, null)
        p.setResId(resId)
        p.setBackgroundResource(R.drawable.player_select_bg)
        p.isClickable = true
        addView(p, index)
        reDraw()

        val controller = PlayerController(resId)
        controllerMap[resId] = controller
        controller.initialize(p)
    }

    fun zoom(resId: Int) {
        largeResId = if (isEnlarge) -1 else resId
        isEnlarge = !isEnlarge
        reDraw()
    }

    private fun reDraw() {
        invalidate()
        measureChild(width, height)
        requestLayout()
    }

    fun clearAll() {
        for (i in 0 until childCount) {
            val childAt = getChildAt(i)
            if (childAt is VideoGLSurfaceView) {
                val resId = childAt.getResId()
                controllerMap[resId]?.release()
                controllerMap.remove(resId)
            }
        }
        removeAllViews()
    }

}