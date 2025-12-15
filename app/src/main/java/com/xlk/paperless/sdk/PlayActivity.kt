package com.xlk.paperless.sdk

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.paperless.player.PlayerController
import com.paperless.player.gl.VideoGLSurfaceView
import com.paperless.sdk.SdkVars

class PlayActivity : AppCompatActivity() {
    private var videoGLSurfaceView: VideoGLSurfaceView? = null
    private var playerController: PlayerController? = null
    private var surfaceView: SurfaceView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //<editor-fold desc="SurfaceView方式">

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)
        playerController = PlayerController(0, {

        })
        playerController!!.initialize(surfaceView!!)
        surfaceView?.setOnClickListener {
            capture(surfaceView!!)
        }

        //</editor-fold>

        //<editor-fold desc="VideoGLSurfaceView方式">

//        videoGLSurfaceView = findViewById(R.id.videoGLSurfaceView)
//        videoGLSurfaceView = VideoGLSurfaceView(this)
//        setContentView(videoGLSurfaceView)
//        playerController = PlayerController(0)
//        playerController?.initialize(videoGLSurfaceView!!)
//        videoGLSurfaceView?.setOnClickListener {
//            capture(videoGLSurfaceView!!)
//        }

        //</editor-fold>
    }

    private fun capture(surfaceView: SurfaceView) {
        val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(surfaceView, bitmap, object : PixelCopy.OnPixelCopyFinishedListener {
            override fun onPixelCopyFinished(copyResult: Int) {
                if (copyResult == PixelCopy.SUCCESS) {
                    showCapture(bitmap)
                }
            }
        }, Handler())
    }

    private fun showCapture(bitmap: Bitmap) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        val dialog = AlertDialog.Builder(this)
            .setView(imageView)
            .create()
        dialog.show()
        dialog.setOnDismissListener {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    override fun onDestroy() {
        LogUtils.i("onDestroy: ")
        playerController?.release()
        playerController = null
        surfaceView = null
        videoGLSurfaceView?.setOnClickListener(null)
        videoGLSurfaceView?.release()
        // 调用jni层结束本机播放
        Jni.stopResource(0, SdkVars.localDeviceId)
        super.onDestroy()
    }
}