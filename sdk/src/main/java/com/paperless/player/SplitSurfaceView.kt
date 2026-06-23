package com.paperless.player

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.blankj.utilcode.util.LogUtils
import com.paperless.sdk.R

/**
 *  @author : Administrator
 *  created on 2025/9/11 14:36
 */
class SplitSurfaceView(cxt: Context, attrs: AttributeSet?) :
    ViewGroup(cxt, attrs) {

    private val controllerMap = mutableMapOf<Int, PlayerController?>()

    private var widthMeasureSpec = 0
    private var heightMeasureSpec = 0

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
        val params2_1 = LayoutParams(parentWidth / 2, parentHeight / 2)
        getChildAt(0).layoutParams = params2_1
        getChildAt(1).layoutParams = params2_1
        getChildAt(2).layoutParams = params2_1
        getChildAt(3).layoutParams = params2_1
        measureChildren(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val p1 = getChildAt(0) as SurfaceView
        val w1 = p1.measuredWidth
        val h1 = p1.measuredHeight
        p1.layout(0, 0, w1, h1)

        val p2 = getChildAt(1) as SurfaceView
        val w2 = p2.measuredWidth
        val h2 = p2.measuredHeight
        p2.layout(w1, 0, w1 + w2, h2)

        val p3 = getChildAt(2) as SurfaceView
        val w3 = p3.measuredWidth
        val h3 = p3.measuredHeight
        p3.layout(0, h1, w3, h1 + h3)

        val p4 = getChildAt(3) as SurfaceView
        val w4 = p4.measuredWidth
        val h4 = p4.measuredHeight
        p4.layout(w3, h2, w3 + w4, h2 + h4)
    }

    fun createView(ids: MutableList<Int>) {
        clearAll()
        resIds.clear()
        resIds.addAll(ids)
        ids.forEach {
            val p = SurfaceView(context)
            val controller = PlayerController(it)
            controller.initialize(p)
            controllerMap[it] = controller
//            p.holder.addCallback(object : SurfaceHolder.Callback {
//                override fun surfaceCreated(holder: SurfaceHolder) {
//                    LogUtils.i("surfaceCreated: $it")
//                    val controller = PlayerController(it)
//                    controllerMap[it] = controller
//                    controller.initialize(holder.surface)
//                }
//
//                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                    LogUtils.i("surfaceChanged: $it")
//                }
//
//                override fun surfaceDestroyed(holder: SurfaceHolder) {
//                    LogUtils.i("surfaceDestroyed: $it")
//                }
//            })
            val id = it
            p.setOnClickListener { listener?.onClick(id) }
            p.setBackgroundResource(R.drawable.player_select_bg)
            p.isClickable = true
            addView(p)
        }
        reDraw()
    }

    fun setSelectResId(resId: Int) {
        for (i in 0 until childCount) {
            getChildAt(i).isSelected = i + 1 == resId
        }
    }

    fun getSelectResId(): Int {
        for (i in 0 until childCount) {
            if (getChildAt(i).isSelected) return i + 1
        }
        return -1
    }

    fun stopResWork(resId: Int) {
        var index = -1
        for (i in 0 until childCount) {
            if (i + 1 == resId) {
                controllerMap[resId]?.release()
                removeView(getChildAt(i))
                index = i
                break
            }
        }
        if (index != -1) {
            insertView(index, resId)
        }
    }

    private fun insertView(index: Int, resId: Int) {
        val p = SurfaceView(context)
        val controller = PlayerController(resId)
        controllerMap[resId] = controller
        controller.initialize(p)
//        p.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                LogUtils.i("surfaceCreated: $resId")
//                val controller = PlayerController(resId)
//                controllerMap[resId] = controller
//                controller.initialize(holder.surface)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                LogUtils.i("surfaceChanged: $resId")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                LogUtils.i("surfaceDestroyed: $resId")
//            }
//        })
        p.setOnClickListener { listener?.onClick(resId) }
        p.setBackgroundResource(R.drawable.player_select_bg)
        p.isClickable = true
        addView(p, index)
        reDraw()
    }

    private fun reDraw() {
        invalidate()
        measureChild(width, height)
        requestLayout()
    }

    fun clearAll() {
        for (i in 0 until childCount) {
            val resId = i + 1
            controllerMap[resId]?.release()
            controllerMap.remove(resId)
        }
        removeAllViews()
    }
}