package com.xlk.paperless.sdk.service;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.util.Pools;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.paperless.sdk.SdkVars;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author : Administrator
 * created on 2025/12/19 17:06
 */
public class ScreenRecord {
    private static final String TAG = "ScreenRecord";
    /**
     * 每次用完一定要清空
     */
    public static Pools.SynchronizedPool<byte[]> framePoll = new Pools.SynchronizedPool<>(2);
    public static ArrayBlockingQueue<byte[]> decodeQueue = new ArrayBlockingQueue<>(2);

    protected boolean mIsRunning = true;
    private int screen_width, screen_height, dpi, rowStride;

    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private byte[] outData;
    private byte[] configbyte = null;
    private ByteBuffer srcBuff, dstBuff;

    @SuppressLint("WrongConstant")
    public void startRecorder(Context context, int resultCode, Intent resultData) {
        try {
            DisplayMetrics metric = new DisplayMetrics();
            WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            window.getDefaultDisplay().getMetrics(metric);
            screen_width = metric.widthPixels;
            screen_height = metric.heightPixels;
            dpi = metric.densityDpi;
            this.srcBuff = ByteBuffer.allocateDirect(screen_width * screen_height * 6);
            this.dstBuff = ByteBuffer.allocateDirect(screen_width * screen_height * 2);
            mImageReader = ImageReader.newInstance(screen_width, screen_height, 0x1, 2);//0x1
            Log.d(TAG, "ScreenRecord.startRecorder: size:" + screen_width + "," + screen_height + ",dpi:" + dpi + ",imageFormat:" + mImageReader.getImageFormat());
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-recorder",
                    screen_width, screen_height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
            mImageReader.setOnImageAvailableListener(listener, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                //Android 13系统需要调用一次才能正常回调 ImageReader.OnImageAvailableListener
                mImageReader.acquireLatestImage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            Log.d(TAG, "ScreenRecord.stop: ");
            mIsRunning = false;
            if (mMediaCodec != null) {
                mMediaCodec.signalEndOfInputStream();
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
            // 释放虚拟显示
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
            // 释放 ImageReader
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
            // 释放 MediaProjection
            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener listener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    // 处理采集到的图像
                    processImage(image);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    byte[] a = null;
    byte[] b = null;

    private void processImage(Image image) {
        //<editor-fold desc="画面处理">
        //平面数组，例如：YUV 420 格式的图像数据通常分为三个平面：一个 Y 平面和两个 UV 平面
        Image.Plane[] planes = image.getPlanes();
        Image.Plane plane = planes[0];
        //获取Image中的ByteBuffer，返回存储该平面数据的 ByteBuffer。该缓冲区包含实际的图像数据。
        ByteBuffer buffer = plane.getBuffer();
        //获取Image中每个像素的Byte数（像素间距）比如：RGBA 有4个通道，所以每个像素的间距是4。
        //返回像素步幅，即同一行中相邻两个像素之间的距离（以字节为单位）。对于不同的图像格式，像素步幅可能不同。
        //例如，对于 YUV 420 格式，Y 平面的像素步幅通常为 1，而 UV 平面的像素步幅通常为 2。
        int pixelStride = plane.getPixelStride();
        //获取Buffer中每行像素的字节宽度；
        //返回行步幅，即相邻两行之间的距离（以字节为单位）。这是因为图像的每行数据在缓冲区中可能不是连续存储的。
        int rowStride = plane.getRowStride();
        //获取Image的图片宽高
        int width = image.getWidth();
        int height = image.getHeight();
        //因为内存对齐的缘故，所以buffer的行宽度与上面获取到的 width*pixelStride 会有差异
        //内存对齐的padding字节数 = Buffer行宽 - Image中图片宽度*像素间距
        int rowPadding = rowStride - pixelStride * width;
        //</editor-fold>
        this.rowStride = rowStride;

        Log.d(TAG, "ScreenRecord.processImage format:" + image.getFormat() + ",buffer.capacity():" + buffer.capacity());
        if (a == null) {
            a = new byte[buffer.capacity()];
            LogUtils.e("ScreenRecord.processImage: 新建对象a");
            framePoll.release(a);
        }
        if (b == null) {
            b = new byte[buffer.capacity()];
            LogUtils.e("ScreenRecord.processImage: 新建对象b");
            framePoll.release(b);
        }

        byte[] acquire = framePoll.acquire();
        if (acquire != null) {
            buffer.get(acquire, 0, buffer.capacity());
            boolean offer = decodeQueue.offer(acquire);
            if (!offer) {
                LogUtils.e("ScreenRecord.processImage: 放入帧失败");
            }
        } else {
            byte[] oldFrame = decodeQueue.poll();
            if (oldFrame != null) {
                buffer.get(oldFrame, 0, buffer.capacity());
                boolean offer = decodeQueue.offer(oldFrame);
                if (!offer) {
                    LogUtils.e("ScreenRecord.processImage: 放入帧失败");
                }
            } else {
                LogUtils.i("ScreenRecord.processImage：进行丢帧...");
            }
        }
        if (mMediaCodec == null) {
            initMediaCodec();
        }
    }

    private void initMediaCodec() {
        try {
            String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, SdkVars.Companion.getRecord_width(), SdkVars.Companion.getRecord_height());
            format.setInteger(MediaFormat.KEY_FRAME_RATE, SdkVars.Companion.getFrameRate());
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, SdkVars.Companion.getIframeInterval());
            format.setInteger(MediaFormat.KEY_BIT_RATE, SdkVars.Companion.getBitrate());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);//COLOR_FormatYUV420SemiPlanar
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            LogUtils.e(TAG, "initMediaCodec: format=" + format);
            pushFrame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pushFrame() {
        LogUtils.e(TAG, "pushFrame: start");
        new Thread(() -> {
            try {
                LogUtils.e(TAG, "run: 开始推送数据");
                byte[] lastFramePacket = null;
                long frame_added_interval_setting = 1000 / SdkVars.Companion.getFrameRate();
                long lastTime = 0;
                long startTime = 0;
                int dstLength = 0;
                int normalFrameCount = 0;
                int secCount = 0;
                while (mIsRunning) {
                    byte[] frame = decodeQueue.poll();
                    //<editor-fold desc="丢帧与补帧">
                    if (frame == null && System.currentTimeMillis() - lastTime > frame_added_interval_setting) {
                        //LogUtils.e("pushFrame: 补帧");
                        frame = lastFramePacket;
                    } else if (frame != null && System.currentTimeMillis() - lastTime < frame_added_interval_setting) {
                        lastFramePacket = frame;
                        //LogUtils.e("pushFrame: 丢帧");
                        frame = null;
                    }
                    //</editor-fold>
                    if (frame != null) {
                        secCount++;
                        if (System.currentTimeMillis() - startTime >= 1000) {
                            startTime = System.currentTimeMillis();
                            LogUtils.e(TAG, "pushFrame: 处理帧：" + secCount + "f/s");
                            secCount = 0;
                        }
                        lastFramePacket = frame;
                        lastTime = System.currentTimeMillis();
                        srcBuff.clear();
                        dstBuff.clear();
                        srcBuff.put(frame);
                        try {
                            framePoll.release(frame);
                        } catch (IllegalStateException e) {

                        }

//                    ByteBuffer byteBuffer = RGBtoNV12Converter.convertRGB24ToNV12(srcBuff, screen_width, screen_height, width, height);
//                    dstLength = byteBuffer.capacity();
                        long l = System.currentTimeMillis();
//                        dstLength = Call.INSTANCE.FFmpegRGBToNV12(3, srcBuff, dstBuff, screen_width, screen_height, SdkVars.Companion.getRecord_width(), SdkVars.Companion.getRecord_height(), rowStride);
                        dstLength = Call.INSTANCE.RGBToNV12(3, srcBuff, dstBuff, screen_width, screen_height, SdkVars.Companion.getRecord_width(), SdkVars.Companion.getRecord_height(), rowStride);
                        LogUtils.e(TAG, "pushFrame:耗时= " + (System.currentTimeMillis() - l) + "," + dstBuff);
                        dstBuff.position(0);
                        dstBuff.limit(dstLength);

                        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            LogUtils.e(TAG, "pushFrame: " + inputBuffer);
                            inputBuffer.put(dstBuff);
                            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, dstBuff.limit(), System.nanoTime() / 1000L, 0);
                        }
                        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0L);
                        if (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                            outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                configbyte = outData;
                            } else {
                                boolean isKey = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME;
                                byte[] frameData = isKey
                                        ? combineKeyFrame(configbyte, outData)
                                        : outData;
                                if (isKey) {
                                    LogUtils.e(TAG, "pushFrame: 普通帧数量=" + normalFrameCount);
                                    normalFrameCount = 0;
                                } else {
                                    normalFrameCount++;
                                }
                                LogUtils.e(TAG, "pushFrame 推送" + (isKey ? "关键" : "普通") + "帧：" + frameData.length);
                                Call.INSTANCE.call(2, isKey ? 1 : 0, bufferInfo.presentationTimeUs, frameData);
                            }
                            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        LogUtils.e(TAG, "pushFrame: end");
    }

    private byte[] combineKeyFrame(byte[] configData, byte[] frameData) {
        byte[] keyframe = new byte[frameData.length + configData.length];
        System.arraycopy(configData, 0, keyframe, 0, configData.length);
        System.arraycopy(frameData, 0, keyframe, configData.length, frameData.length);
        return keyframe;
    }
}
