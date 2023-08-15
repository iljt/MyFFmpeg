package com.example.player;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MyFFmpegPlayer implements SurfaceHolder.Callback {

    /* TODO 第二次新增 --- start */

    // 错误代码 ================ 如下
    // 打不开视频
    // #define FFMPEG_CAN_NOT_OPEN_URL 1
    private static final int FFMPEG_CAN_NOT_OPEN_URL = 1;

    // 找不到流媒体
    // #define FFMPEG_CAN_NOT_FIND_STREAMS 2
    private static final int FFMPEG_CAN_NOT_FIND_STREAMS = 2;

    // 找不到解码器
    // #define FFMPEG_FIND_DECODER_FAIL 3
    private static final int FFMPEG_FIND_DECODER_FAIL = 3;

    // 无法根据解码器创建上下文
    // #define FFMPEG_ALLOC_CODEC_CONTEXT_FAIL 4
    private static final int FFMPEG_ALLOC_CODEC_CONTEXT_FAIL = 4;

    //  根据流信息 配置上下文参数失败
    // #define FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL 6
    private static final int FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL = 6;

    // 打开解码器失败
    // #define FFMPEG_OPEN_DECODER_FAIL 7
    private static final int FFMPEG_OPEN_DECODER_FAIL = 7;

    // 没有音视频
    // #define FFMPEG_NOMEDIA 8
    private static final int FFMPEG_NOMEDIA = 8;
    /* TODO 第二次新增 --- end */

    static {
        System.loadLibrary("native-lib");
    }

    private OnPreparedListener onPreparedListener; // C++层准备情况的接口
    private OnErrorListener onErrorListener; // TODO 第二次新增
    private SurfaceHolder surfaceHolder; // TODO 第三次新增

    public MyFFmpegPlayer() {
    }

    // 媒体源（文件路径， 直播地址rtmp）
    private String dataSource;

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
        double aspRatio=getAspRatio(dataSource);
        Log.e("aspRatio= "," aspRatio= "+aspRatio);
    }

    /**
     * 播放前的 准备工作
     */
    public void prepare() {
        prepareNative(dataSource);
    }

    /**
     * 获取视频宽高比
     */
    public double getAspRatio(String dataSource_) {
       double aspectRatio=getAspectRatio(dataSource_);
       return aspectRatio;
    }


    /**
     * 开始播放
     */
    public void start() {
        startNative();
    }

    /**
     * 停止播放
     */
    public void stop() {
        stopNative();
    }

    /**
     * 释放资源
     */
    public void release() {
        releaseNative();
    }

    /**
     * 给jni反射调用的  准备成功
     */
    public void onPrepared() {
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared();
        }
    }

    /**
     * 设置准备OK的监听方法
     */
    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    /**
     * 获取总时长
     * @return
     */
    public int getDuration() {
        return getDurationNative();
    }

    public void seek(int playProgress) {
        seekNative(playProgress);
    }

    /**
     * 准备OK的监听
     */
    public interface OnPreparedListener {
         void onPrepared();
    }

    /* TODO 第二次新增 --- start */

    /**
     * 给jni反射调用的 准备错误了
     */
    public void onError(int errorCode) {
        if (null != this.onErrorListener) {
            String msg = null;
            switch (errorCode) {
                case FFMPEG_CAN_NOT_OPEN_URL:
                    msg = "打不开视频";
                    break;
                case FFMPEG_CAN_NOT_FIND_STREAMS:
                    msg = "找不到流媒体";
                    break;
                case FFMPEG_FIND_DECODER_FAIL:
                    msg = "找不到解码器";
                    break;
                case FFMPEG_ALLOC_CODEC_CONTEXT_FAIL:
                    msg = "无法根据解码器创建上下文";
                    break;
                case FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL:
                    msg = "根据流信息 配置上下文参数失败";
                    break;
                case FFMPEG_OPEN_DECODER_FAIL:
                    msg = "打开解码器失败";
                    break;
                case FFMPEG_NOMEDIA:
                    msg = "没有音视频";
                    break;
            }
            onErrorListener.onError(msg);
        }
    }

    interface OnErrorListener {
        void onError(String errorCode);
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    /* TODO 第二次新增 --- end */

    /**
     * set SurfaceView
     * @param surfaceView
     */
    public void setSurfaceView(SurfaceView surfaceView) {
        if (this.surfaceHolder != null) {
            surfaceHolder.removeCallback(this); // 清除上一次的
        }
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this); // 监听
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // 之前开发是：写这里的
    }

    @Override // 界面发生了改变
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        setSurfaceNative(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    // TODO 第七次增加 2.1
    /**
     * 给jni反射调用的  准备成功
     */
    public void onProgress(int progress) {
        if (onProgressListener != null) {
            onProgressListener.onProgress(progress);
        }
    }

    private OnProgressListener onProgressListener;

    public interface OnProgressListener {

        void onProgress(int progress);
    }

    /**
     * 设置准备播放时进度的监听
     */
    public void setOnOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    // TODO >>>>>>>>>>> 下面是native函数区域
    private native void prepareNative(String dataSource);
    private native double getAspectRatio(String dataSource);
    private native void startNative();
    private native void stopNative();
    private native void releaseNative();
    private native void setSurfaceNative(Surface surface); //第3次增加
    private native int getDurationNative(); //  第七次增加 获取总时长
    private native void seekNative(int playValue); // 第七次增加 3.1

}
