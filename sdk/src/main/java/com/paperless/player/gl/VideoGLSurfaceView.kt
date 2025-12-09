package com.paperless.player.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class VideoGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private val TAG = "VideoGLSurfaceView"

    private var resId: Int = 0

    // 外部纹理（用于MediaCodec）
    private var textureId = -1
    private var surfaceTexture: SurfaceTexture? = null
    private var updateSurface = false

    // YUV纹理（用于直接渲染YUV数据）
    private var yuvTextureIds = IntArray(3) // Y, U, V 纹理
    private var yuvWidth = 0
    private var yuvHeight = 0
    private var yuvDataAvailable = false

    // 渲染程序 - 外部纹理
    private var externalProgram: Int = 0
    private var externalPositionHandle: Int = 0
    private var externalTextureHandle: Int = 0
    private var externalTextureCoordHandle: Int = 0
    private var externalMvpMatrixHandle: Int = 0
    private var externalStMatrixHandle: Int = 0

    // 渲染程序 - YUV纹理
    private var yuvProgram: Int = 0
    private var yuvPositionHandle: Int = 0
    private var yuvTextureCoordHandle: Int = 0
    private var yuvMvpMatrixHandle: Int = 0
    private var yuvYTextureHandle: Int = 0
    private var yuvUTextureHandle: Int = 0
    private var yuvVTextureHandle: Int = 0

    private val mvpMatrix = FloatArray(16) // MVP矩阵
    private val projectionMatrix = FloatArray(16) // 投影矩阵
    private val stMatrix = FloatArray(16) // SurfaceTexture的变换矩阵
    private val modelMatrix = FloatArray(16) // 模型矩阵

    // 顶点和纹理坐标
    private val vertices = floatArrayOf(
        -1.0f, -1.0f,  // 左下
        1.0f, -1.0f,   // 右下
        -1.0f, 1.0f,   // 左上
        1.0f, 1.0f     // 右上
    )

    private val textureCoords = floatArrayOf(
        0.0f, 1.0f,    // 左下
        1.0f, 1.0f,    // 右下
        0.0f, 0.0f,    // 左上
        1.0f, 0.0f     // 右上
    )

    private val vertexBuffer: FloatBuffer
    private val textureCoordBuffer: FloatBuffer

    // 视频源的旋转角度，应用场景只有EXTERNAL_TEXTURE才可能有旋转需求
    private var mVideoRotation: Int = 0 // 旋转角度（0, 90, 180, 270）
    private var mVideoWidth: Int = 0 // 视频源宽
    private var mVideoHeight: Int = 0 // 视频源高
    private var mNeedRotation: Boolean = false // 添加标志位，标识是否需要旋转
    private var mViewWidth: Int = 0 // 视图宽
    private var mViewHeight: Int = 0 //视图高

    // 监听器
    private var surfaceReadyListener: OnSurfaceReadyListener? = null

    // 渲染模式
    private var mRenderMode: RenderMode = RenderMode.EXTERNAL_TEXTURE

    enum class RenderMode {
        EXTERNAL_TEXTURE, // 使用外部纹理（MediaCodec）
        YUV_TEXTURE,       // 使用YUV纹理（直接YUV数据）
        CLEAR_TEXTURE       // 清理屏幕（黑屏）
    }

    init {
        // 初始化顶点和纹理坐标缓冲区
        vertexBuffer = createFloatBuffer(vertices)
        textureCoordBuffer = createFloatBuffer(textureCoords)
        // 设置使用 OpenGL ES 2.0
        setEGLContextClientVersion(2)
        // 设置渲染器
        setRenderer(this)
        // 设置为按需渲染模式（如果内容静止，可以节省资源）
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(array.size * 4) //分配新的直接字节缓冲区
            .order(ByteOrder.nativeOrder()) //修改此缓冲区的字节顺序
            .asFloatBuffer() //创建此字节缓冲区的视图，作为 float 缓冲区
            .put(array)//将4个包含给定float值的字节按照当前的字节顺序写入到此缓冲区的当前位置，然后然后将该位置增加 4。
            .apply {
                position(0)
            }
    }

    interface OnSurfaceReadyListener {
        fun onSurfaceReady(surfaceTexture: SurfaceTexture)
    }

    fun setOnSurfaceReadyListener(listener: OnSurfaceReadyListener?) {
        this.surfaceReadyListener = listener
        // 如果Surface已经就绪，立即通知
        surfaceTexture?.let {
            listener?.onSurfaceReady(it)
        }
    }

    fun setResId(id: Int) {
        resId = id
    }

    fun getResId() = resId

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 初始化OpenGL
        initGL()

        // 创建外部纹理
        textureId = createExternalTexture()

        // 创建SurfaceTexture
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture?.setOnFrameAvailableListener(this)

        // 创建YUV纹理
        createYUVTextures()

        // 通知Surface已就绪
        surfaceReadyListener?.onSurfaceReady(surfaceTexture!!)

        Log.d(TAG, "OpenGL surface created")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mViewWidth = width
        mViewHeight = height
        // 设置视口，告诉OpenGL可用于渲染的区域
        GLES20.glViewport(0, 0, width, height)
        // 如果需要，可以在这里计算和设置投影矩阵（例如使用 glFrustumf 或 Matrix.frustumM）
        // float ratio = (float) width / height;
        // gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7); // 例如设置透视投影:cite[10]
        Log.d(TAG, "Surface changed: $width x $height")
        // 修改1: 设置正交投影为固定范围（-1,1），避免宽高比影响
        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f)
        // 更新视图矩阵 - 简化为单位矩阵
        Matrix.setIdentityM(stMatrix, 0)
        // 更新变换矩阵
        updateMatrix()
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清除屏幕 清除颜色缓冲区和深度缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT /*or GLES20.GL_DEPTH_BUFFER_BIT*/)

        when (mRenderMode) {
            RenderMode.EXTERNAL_TEXTURE -> renderExternalTexture()
            RenderMode.YUV_TEXTURE -> renderYUVTexture()
            RenderMode.CLEAR_TEXTURE -> {
                // 清理屏幕，不做其它处理
            }
        }
    }

    private fun renderExternalTexture() {
        synchronized(this) {
            if (updateSurface) {
                surfaceTexture?.updateTexImage()
                surfaceTexture?.getTransformMatrix(stMatrix)
                updateSurface = false
            }
        }

        // 使用外部纹理程序
        GLES20.glUseProgram(externalProgram)

        // 设置顶点坐标
        GLES20.glVertexAttribPointer(externalPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(externalPositionHandle)

        // 设置纹理坐标
        GLES20.glVertexAttribPointer(externalTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureCoordBuffer)
        GLES20.glEnableVertexAttribArray(externalTextureCoordHandle)

        // 设置矩阵
        GLES20.glUniformMatrix4fv(externalMvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(externalStMatrixHandle, 1, false, stMatrix, 0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(externalTextureHandle, 0)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点数组
        GLES20.glDisableVertexAttribArray(externalPositionHandle)
        GLES20.glDisableVertexAttribArray(externalTextureCoordHandle)
    }

    private fun renderYUVTexture() {
        if (!yuvDataAvailable) return

        // 使用YUV纹理程序
        GLES20.glUseProgram(yuvProgram)

        // 设置顶点坐标
        GLES20.glVertexAttribPointer(yuvPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(yuvPositionHandle)

        // 设置纹理坐标
        GLES20.glVertexAttribPointer(yuvTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureCoordBuffer)
        GLES20.glEnableVertexAttribArray(yuvTextureCoordHandle)

        // 设置矩阵
        GLES20.glUniformMatrix4fv(yuvMvpMatrixHandle, 1, false, mvpMatrix, 0)

        // 绑定Y纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glUniform1i(yuvYTextureHandle, 0)

        // 绑定U纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glUniform1i(yuvUTextureHandle, 1)

        // 绑定V纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[2])
        GLES20.glUniform1i(yuvVTextureHandle, 2)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点数组
        GLES20.glDisableVertexAttribArray(yuvPositionHandle)
        GLES20.glDisableVertexAttribArray(yuvTextureCoordHandle)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
        requestRender()
    }

    private fun initGL() {
        // 初始化外部纹理着色器
        val externalVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, EXTERNAL_VERTEX_SHADER)
        val externalFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, EXTERNAL_FRAGMENT_SHADER)

        externalProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(externalProgram, externalVertexShader)
        GLES20.glAttachShader(externalProgram, externalFragmentShader)
        GLES20.glLinkProgram(externalProgram)

        // 获取外部纹理属性位置
        externalPositionHandle = GLES20.glGetAttribLocation(externalProgram, "aPosition")
        externalTextureCoordHandle = GLES20.glGetAttribLocation(externalProgram, "aTextureCoord")
        externalTextureHandle = GLES20.glGetUniformLocation(externalProgram, "sTexture")
        externalMvpMatrixHandle = GLES20.glGetUniformLocation(externalProgram, "uMVPMatrix")
        externalStMatrixHandle = GLES20.glGetUniformLocation(externalProgram, "uSTMatrix")

        // 初始化YUV纹理着色器
        val yuvVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, YUV_VERTEX_SHADER)
        val yuvFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, YUV_FRAGMENT_SHADER)

        yuvProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(yuvProgram, yuvVertexShader)
        GLES20.glAttachShader(yuvProgram, yuvFragmentShader)
        GLES20.glLinkProgram(yuvProgram)

        // 获取YUV纹理属性位置
        yuvPositionHandle = GLES20.glGetAttribLocation(yuvProgram, "aPosition")
        yuvTextureCoordHandle = GLES20.glGetAttribLocation(yuvProgram, "aTextureCoord")
        yuvMvpMatrixHandle = GLES20.glGetUniformLocation(yuvProgram, "uMVPMatrix")
        yuvYTextureHandle = GLES20.glGetUniformLocation(yuvProgram, "yTexture")
        yuvUTextureHandle = GLES20.glGetUniformLocation(yuvProgram, "uTexture")
        yuvVTextureHandle = GLES20.glGetUniformLocation(yuvProgram, "vTexture")

        // 设置默认矩阵
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)

        // 设置清屏颜色为黑色 可使用 GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) 进行清屏
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
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

    private fun createYUVTextures() {
        GLES20.glGenTextures(3, yuvTextureIds, 0)

        for (i in 0 until 3) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
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

    /**
     * 设置YUV帧数据
     * @param w 图像宽度
     * @param h 图像高度
     * @param y Y分量数据
     * @param u U分量数据
     * @param v V分量数据
     */
    fun setFrameData(w: Int, h: Int, y: ByteArray, u: ByteArray, v: ByteArray) {
        mVideoWidth = w
        mVideoHeight = h
        if (w <= 0 || h <= 0 || y.isEmpty() || u.isEmpty() || v.isEmpty()) {
            Log.e(TAG, "Invalid YUV data")
            return
        }

        // 切换到YUV渲染模式
        mRenderMode = RenderMode.YUV_TEXTURE
        yuvWidth = w
        yuvHeight = h

        // 在OpenGL线程中更新纹理
        queueEvent {
            updateYUVTextures(y, u, v, w, h)
            yuvDataAvailable = true
            requestRender()
        }
        updateMatrix() // 添加矩阵更新
    }

    private fun updateYUVTextures(y: ByteArray, u: ByteArray, v: ByteArray, width: Int, height: Int) {
        // 更新Y纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            width, height, 0,
            GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(y)
        )

        // 更新U纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            width / 2, height / 2, 0,
            GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(u)
        )

        // 更新V纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[2])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            width / 2, height / 2, 0,
            GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(v)
        )
    }

    fun setVideoRotation(rotation: Int, width: Int, height: Int) {
        mVideoRotation = rotation
        mVideoWidth = width
        mVideoHeight = height
        mNeedRotation = rotation != 0
        updateMatrix()
    }

    private fun updateMatrix() {
        Matrix.setIdentityM(modelMatrix, 0)

        // 1. 应用旋转
        if (mNeedRotation) {
            // 应用旋转（绕Z轴）
            Matrix.rotateM(modelMatrix, 0, mVideoRotation.toFloat(), 0f, 0f, 1f)
        }

        // 2. 缩放适配屏幕（保持完整视频内容）
//        if (mViewWidth > 0 && mViewHeight > 0 && mVideoWidth > 0 && mVideoHeight > 0) {
//            val viewRatio = mViewWidth.toFloat() / mViewHeight
//            var videoRatio = mVideoWidth.toFloat() / mVideoHeight
//
//            // 考虑旋转后的宽高比
//            if (mNeedRotation && (mVideoRotation % 180 == 90)) {
//                videoRatio = mVideoHeight.toFloat() / mVideoWidth
//            }
//
//            // 计算缩放比例，确保完整显示视频内容
//            var scaleX = 1.0f
//            var scaleY = 1.0f
//
//            if (viewRatio > videoRatio) {
//                // 视图更宽，视频需要按高度缩放
//                scaleX = videoRatio / viewRatio
//            } else {
//                // 视图更高，视频需要按宽度缩放
//                scaleY = viewRatio / videoRatio
//            }
//
//            // 应用缩放
//            Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, 1f)
//        }

        // 3. 组合MVP矩阵: Projection * View * Model
        Matrix.multiplyMM(mvpMatrix, 0, stMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
    }

    /**
     * 切换到外部纹理渲染模式（MediaCodec）
     */
    fun switchToExternalTextureMode() {
        mRenderMode = RenderMode.EXTERNAL_TEXTURE
        requestRender()
    }

    /**
     * 切换到YUV纹理渲染模式
     */
    fun switchToYUVTextureMode() {
        mRenderMode = RenderMode.YUV_TEXTURE
        requestRender()
    }

    /**
     * 清理屏幕（黑屏）
     */
    fun switchToClearTextureMode() {
        mRenderMode = if (mRenderMode == RenderMode.CLEAR_TEXTURE) {
            RenderMode.EXTERNAL_TEXTURE
        } else {
            RenderMode.CLEAR_TEXTURE
        }
        requestRender()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 若 Surface 未正常销毁可能导致泄漏，所以在此处也调用release方法
        release()
    }

    fun release() {
        LogUtils.i("release")
        surfaceTexture?.release()

        // 删除纹理
        if (textureId != -1) {
            val textures = IntArray(1)
            textures[0] = textureId
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = -1
        }

        // 删除YUV纹理
        if (yuvTextureIds.isNotEmpty()) {
            GLES20.glDeleteTextures(3, yuvTextureIds, 0)
            yuvTextureIds = IntArray(3)
        }

        // 删除程序
        if (externalProgram != 0) {
            GLES20.glDeleteProgram(externalProgram)
            externalProgram = 0
        }

        if (yuvProgram != 0) {
            GLES20.glDeleteProgram(yuvProgram)
            yuvProgram = 0
        }
    }

    companion object {
        // 外部纹理顶点着色器
        private const val EXTERNAL_VERTEX_SHADER = """ 
            attribute vec4 aPosition; 
            attribute vec4 aTextureCoord; 
            varying vec2 vTextureCoord; 
            uniform mat4 uMVPMatrix; 
            uniform mat4 uSTMatrix; 
            void main() { 
                gl_Position = uMVPMatrix * aPosition; 
                // 修正纹理坐标以解决垂直和水平颠倒问题
                vTextureCoord = (uSTMatrix * vec4(aTextureCoord.x, 1.0 - aTextureCoord.y, 0.0, 1.0)).xy; // 垂直翻转修正
                // 如果水平也颠倒，则使用：vTextureCoord = (uSTMatrix * vec4(1.0 - aTextureCoord.x, 1.0 - aTextureCoord.y, 0.0, 1.0)).xy;
            } 
        """

        // 外部纹理片段着色器 - 用于外部纹理(OES)
        private const val EXTERNAL_FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTextureCoord);
            }
        """

        // YUV纹理顶点着色器
        private const val YUV_VERTEX_SHADER = """ 
            attribute vec4 aPosition; 
            attribute vec4 aTextureCoord; 
            varying vec2 vTextureCoord; 
            uniform mat4 uMVPMatrix; 
            void main() { 
                gl_Position = uMVPMatrix * aPosition; 
                // 修正纹理坐标以解决垂直颠倒问题（假设YUV数据本身没有水平颠倒）
                vTextureCoord = vec2(aTextureCoord.x, 1.0 - aTextureCoord.y); // 仅垂直翻转
                // 如果YUV数据也水平颠倒，则使用：vTextureCoord = vec2(1.0 - aTextureCoord.x, 1.0 - aTextureCoord.y);
            } 
        """

        // YUV纹理片段着色器 - 用于YUV转RGB
        private const val YUV_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D yTexture;
            uniform sampler2D uTexture;
            uniform sampler2D vTexture;
            
            void main() {
                float y, u, v, r, g, b;
                y = texture2D(yTexture, vTextureCoord).r;
                u = texture2D(uTexture, vTextureCoord).r;
                v = texture2D(vTexture, vTextureCoord).r;
                
                // YUV to RGB conversion
                y = 1.1643 * (y - 0.0625);
                u = u - 0.5;
                v = v - 0.5;
                
                r = y + 1.5958 * v;
                g = y - 0.39173 * u - 0.81290 * v;
                b = y + 2.017 * u;
                
                gl_FragColor = vec4(r, g, b, 1.0);
            }
        """
    }
}