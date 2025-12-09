package com.paperless.player.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLException
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *  @author : Administrator
 *  created on 2025/9/9 15:46
 */
class VideoRenderer : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private val TAG = "VideoRenderer"

    // 添加旋转相关变量
    private var rotation = 0 // 旋转角度（0, 90, 180, 270）

    // 添加标志位，标识是否需要旋转
    private var needRotation = false
    private val projectionMatrix = FloatArray(16) // 投影矩阵
    private val viewMatrix = FloatArray(16) // 视图矩阵
    private val modelMatrix = FloatArray(16) // 模型矩阵
    private val mvpMatrix = FloatArray(16) // MVP矩阵

    private var uMVPMatrixHandle_mediacodec = 0
    private var uMVPMatrixHandle_yuv = 0
    private var uMVPMatrixHandle_stop = 0

    private val vertexBuffer: FloatBuffer

    // 修正顶点数据（确保全屏）
    private val vertexData = floatArrayOf(
        -1f, -1f, 0f,  // 左下
        1f, -1f, 0f,  // 右下
        -1f, 1f, 0f,  // 左上
        1f, 1f, 0f // 右上
    )

    private val textureBuffer: FloatBuffer

    // 修正纹理坐标（匹配新顶点顺序）
    private val textureVertexData = floatArrayOf(
        0f, 1f,  // 左下
        1f, 1f,  // 右下
        0f, 0f,  // 左上
        1f, 0f // 右上
    )

    private val vertexShader = "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 av_Position;\n" +
            "attribute vec2 af_Position;\n" +
            "varying vec2 v_texPo;\n" +
            "void main() {\n" +
            "    v_texPo = af_Position;\n" +
            "    gl_Position = uMVPMatrix * av_Position;\n" +
            "}"

    private val mediaCodeShader = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 v_texPo;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor=texture2D(sTexture, v_texPo);\n" +
            "}"

    private val yuvShader = "precision mediump float;\n" +
            "varying vec2 v_texPo;\n" +
            "uniform sampler2D sampler_y;\n" +
            "uniform sampler2D sampler_u;\n" +
            "uniform sampler2D sampler_v;\n" +
            "void main() {\n" +
            "    float y,u,v;\n" +
            "    y = texture2D(sampler_y,v_texPo).x;\n" +
            "    u = texture2D(sampler_u,v_texPo).x- 128./255.;\n" +
            "    v = texture2D(sampler_v,v_texPo).x- 128./255.;\n" +
            "    vec3 rgb;\n" +
            "    rgb.r = y + 1.403 * v;\n" +
            "    rgb.g = y - 0.344 * u - 0.714 * v;\n" +
            "    rgb.b = y + 1.770 * u;\n" +
            "    gl_FragColor = vec4(rgb,1);\n" +
            "}"

    /**
     * mediacodec
     */
    private var programId_mediacodec = 0
    private var aPositionHandle_mediacodec = 0
    private var textureid_mediacodec = 0
    private var uTextureSamplerHandle_mediacodec = 0
    private var aTextureCoordHandle_mediacodec = 0

    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    /**
     * yuv
     */
    private var programId_yuv = 0
    private var aPositionHandle_yuv = 0
    private var aTextureCoordHandle_yuv = 0
    private var sampler_y = 0
    private var sampler_u = 0
    private var sampler_v = 0
    private lateinit var textureid_yuv: IntArray

    var w: Int = 0
    var h: Int = 0

    var y: Buffer? = null
    var u: Buffer? = null
    var v: Buffer? = null


    enum class RenderMode {
        RENDER_TYPE_STOP,
        RENDER_TYPE_DECODE,
        RENDER_TYPE_YUV
    }

    private var mRenderMode: RenderMode = RenderMode.RENDER_TYPE_DECODE

    private var captureEnable = false
    private var sWidth = 0
    private var sHeight = 0

    init {
        vertexBuffer = createFloatBuffer(vertexData)
        //设置此缓冲区的位置。如果标记已定义且大于新的位置，则丢弃该标记
        //新位置值；必须为 非负 且 不大于当前限制
        vertexBuffer.position(0)

        textureBuffer = createFloatBuffer(textureVertexData)
        textureBuffer.position(0)


        // 初始化矩阵
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    private var mSurfaceReadyListener: SurfaceReadyListener? = null
    private var mRequestRenderListener: RequestRenderListener? = null
    private var mCaptureListener: CaptureListener? = null

    interface SurfaceReadyListener {
        fun onSurfaceReady(surfaceTexture: SurfaceTexture)
    }

    interface RequestRenderListener {
        fun onRequestRender()
    }

    interface CaptureListener {
        fun onCapture(bitmap: Bitmap)
    }

    fun setOnSurfaceReadyListener(listener: SurfaceReadyListener) {
        mSurfaceReadyListener = listener
        // 如果Surface已经就绪，立即通知
        surfaceTexture?.let {
            listener.onSurfaceReady(it)
        }
    }

    fun setOnRequestRenderListener(listener: RequestRenderListener) {
        mRequestRenderListener = listener
    }

    fun setOnCaptureListener(listener: CaptureListener) {
        mCaptureListener = listener
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4) //分配新的直接字节缓冲区
            .order(ByteOrder.nativeOrder()) //修改此缓冲区的字节顺序
            .asFloatBuffer() //创建此字节缓冲区的视图，作为 float 缓冲区
            .put(array)//将4个包含给定float值的字节按照当前的字节顺序写入到此缓冲区的当前位置，然后然后将该位置增加 4。
    }

    fun setRotation(rotation: Int, needRotation: Boolean) {
        this.rotation = rotation
        this.needRotation = needRotation
        updateMatrix()
    }

    private fun updateMatrix() {
        Matrix.setIdentityM(modelMatrix, 0)

        // 1. 应用旋转
        if (needRotation) {
            // 应用旋转（绕Z轴）
            Matrix.rotateM(modelMatrix, 0, rotation.toFloat(), 0f, 0f, 1f)
        }

        // 2. 缩放适配屏幕（保持完整视频内容）
        if (sWidth > 0 && sHeight > 0 && w > 0 && h > 0) {
            val viewRatio = sWidth.toFloat() / sHeight
            var videoRatio = w.toFloat() / h

            // 考虑旋转后的宽高比
            if (needRotation && (rotation % 180 == 90)) {
                videoRatio = h.toFloat() / w
            }

            // 计算缩放比例，确保完整显示视频内容
            var scaleX = 1.0f
            var scaleY = 1.0f

            if (viewRatio > videoRatio) {
                // 视图更宽，视频需要按高度缩放
                scaleX = videoRatio / viewRatio
            } else {
                // 视图更高，视频需要按宽度缩放
                scaleY = viewRatio / videoRatio
            }

            // 应用缩放
            Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f)
        }

        // 3. 组合MVP矩阵: Projection * View * Model
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
    }

    fun setFrameData(w: Int, h: Int, by: ByteArray, bu: ByteArray, bv: ByteArray) {
        this.w = w
        this.h = h
        y = ByteBuffer.wrap(by)
        u = ByteBuffer.wrap(bu)
        v = ByteBuffer.wrap(bv)
        updateMatrix() // 添加矩阵更新
    }

    fun switchToExternalTextureMode() {
        mRenderMode = RenderMode.RENDER_TYPE_DECODE
        mRequestRenderListener?.onRequestRender()
    }

    fun switchToYUVTextureMode() {
        mRenderMode = RenderMode.RENDER_TYPE_YUV
        mRequestRenderListener?.onRequestRender()
    }

    fun switchToClearTextureMode() {
        mRenderMode = if (mRenderMode == RenderMode.RENDER_TYPE_STOP) {
            RenderMode.RENDER_TYPE_DECODE
        } else {
            RenderMode.RENDER_TYPE_STOP
        }
        mRequestRenderListener?.onRequestRender()
    }

    private fun glClear() {
        GLES20.glClearDepthf(1f)
        GLES20.glClearStencil(1)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_STENCIL_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(1f, 0f, 0f, 1f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        initMediacodecShader()

        textureid_mediacodec = createExternalTexture()
        surfaceTexture = SurfaceTexture(textureid_mediacodec)
        surfaceTexture!!.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)
        mSurfaceReadyListener?.onSurfaceReady(surfaceTexture!!)

        initYuvShader()

        // 设置清屏颜色为黑色 可使用 GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) 进行清屏
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    private fun initMediacodecShader() {
        val externalVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val externalFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mediaCodeShader)
        programId_mediacodec = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId_mediacodec, externalVertexShader)
        GLES20.glAttachShader(programId_mediacodec, externalFragmentShader)
        GLES20.glLinkProgram(programId_mediacodec)

        aPositionHandle_mediacodec = GLES20.glGetAttribLocation(programId_mediacodec, "av_Position")
        aTextureCoordHandle_mediacodec = GLES20.glGetAttribLocation(programId_mediacodec, "af_Position")
        uTextureSamplerHandle_mediacodec = GLES20.glGetUniformLocation(programId_mediacodec, "sTexture")

        // 获取MVP矩阵uniform位置
        uMVPMatrixHandle_mediacodec = GLES20.glGetUniformLocation(programId_mediacodec, "uMVPMatrix")
    }

    private fun renderMediacodec() {
        GLES20.glUseProgram(programId_mediacodec)
        surfaceTexture!!.updateTexImage()

        // 传递MVP矩阵
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle_mediacodec, 1, false, mvpMatrix, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle_mediacodec)
        GLES20.glVertexAttribPointer(
            aPositionHandle_mediacodec, 3, GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )
        textureBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle_mediacodec)
        GLES20.glVertexAttribPointer(aTextureCoordHandle_mediacodec, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureid_mediacodec)
        GLES20.glUniform1i(uTextureSamplerHandle_mediacodec, 0)
    }

    private fun initYuvShader() {
        val yuvVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val yuvFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, yuvShader)
        programId_yuv = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId_yuv, yuvVertexShader)
        GLES20.glAttachShader(programId_yuv, yuvFragmentShader)
        GLES20.glLinkProgram(programId_yuv)

        aPositionHandle_yuv = GLES20.glGetAttribLocation(programId_yuv, "av_Position")
        aTextureCoordHandle_yuv = GLES20.glGetAttribLocation(programId_yuv, "af_Position")
        sampler_y = GLES20.glGetUniformLocation(programId_yuv, "sampler_y")
        sampler_u = GLES20.glGetUniformLocation(programId_yuv, "sampler_u")
        sampler_v = GLES20.glGetUniformLocation(programId_yuv, "sampler_v")

        // 获取MVP矩阵uniform位置
        uMVPMatrixHandle_yuv = GLES20.glGetUniformLocation(programId_yuv, "uMVPMatrix")

        // 创建YUV纹理
        textureid_yuv = IntArray(3)
        GLES20.glGenTextures(3, textureid_yuv, 0)
        for (i in 0..2) {
            // 绑定纹理空间
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[i])
            //设置属性 当显示的纹理比加载的纹理大时 使用纹理坐标中最接近的若干个颜色 通过加权算法获得绘制颜色
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            // 比加载的小
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            // 如果纹理坐标超出范围 0,0-1,1 坐标会被截断在范围内
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
    }

    private fun renderYuv() {
        if (w > 0 && h > 0 && y != null && u != null && v != null) {
            GLES20.glUseProgram(programId_yuv)

            // 传递MVP矩阵
            GLES20.glUniformMatrix4fv(uMVPMatrixHandle_yuv, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(aPositionHandle_yuv)
            GLES20.glVertexAttribPointer(
                aPositionHandle_yuv, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer
            )
            textureBuffer.position(0)
            GLES20.glEnableVertexAttribArray(aTextureCoordHandle_yuv)
            GLES20.glVertexAttribPointer(aTextureCoordHandle_yuv, 2, GLES20.GL_FLOAT, false, 8, textureBuffer)


            //使 GL_TEXTURE0 单元 活跃 opengl最多支持16个纹理
            //纹理单元是显卡中所有的可用于在shader中进行纹理采样的显存 数量与显卡类型相关，至少16个
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

            //绑定纹理空间 下面的操作就会作用在这个空间中
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[0])

            //创建一个2d纹理 使用亮度颜色模型并且纹理数据也是亮度颜色模型
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                w,
                h,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                y
            )

            //绑定采样器与纹理单元
            GLES20.glUniform1i(sampler_y, 0)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[1])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, w / 2, h / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                u
            )
            GLES20.glUniform1i(sampler_u, 1)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid_yuv[2])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, w / 2, h / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                v
            )
            GLES20.glUniform1i(sampler_v, 2)
            y!!.clear()
            u!!.clear()
            v!!.clear()
            y = null
            u = null
            v = null
        }
    }

    private fun cutBitmap(w: Int, h: Int): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            GLES20.glReadPixels(
                0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                intBuffer
            )
            var offset1: Int
            var offset2: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            return null
        }
        val bitmap = Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
        intBuffer.clear()
        return bitmap
    }

    fun release() {
        glClear()
        vertexBuffer.clear()
        textureBuffer.clear()
        if (surfaceTexture != null) {
            surfaceTexture!!.setOnFrameAvailableListener(null)
            surfaceTexture!!.release()
        }
        mSurfaceReadyListener = null
        mRequestRenderListener = null
        mCaptureListener = null
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        sWidth = width
        sHeight = height
        //设置展示窗口
        GLES20.glViewport(0, 0, width, height)
        // 修改1: 设置正交投影为固定范围（-1,1），避免宽高比影响
        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        // 更新视图矩阵 - 简化为单位矩阵
        Matrix.setIdentityM(viewMatrix, 0)
        // 更新变换矩阵
        updateMatrix()
    }

    override fun onDrawFrame(gl: GL10?) {
        if (mRenderMode == RenderMode.RENDER_TYPE_STOP) {
            // 清除屏幕 清除颜色缓冲区和深度缓冲区
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT /*or GLES20.GL_DEPTH_BUFFER_BIT*/)
            return
        }

        if (mRenderMode == RenderMode.RENDER_TYPE_DECODE) {
            renderMediacodec()
        } else if (mRenderMode == RenderMode.RENDER_TYPE_YUV) {
            renderYuv()
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        if (captureEnable) {
            captureEnable = false
            val bitmap = cutBitmap(sWidth, sHeight)
            mCaptureListener?.onCapture(bitmap!!)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        mRequestRenderListener?.onRequestRender()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // 检查编译状态
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textures[0]
    }
}