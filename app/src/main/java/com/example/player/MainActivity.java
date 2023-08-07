package com.example.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener,EasyPermissions.PermissionCallbacks{

    private MyFFmpegPlayer player;
    private TextView tv_state;
    private SurfaceView surfaceView;

    // TODO 第七次增加
    // 直播视频是没有拖动条的（直播是没有总时长）（隐藏），  若非直播的视频文件才有拖动条（视频文件是有总时长）（显示）
    private SeekBar seekBar;
    private TextView tv_time; // 显示播放时间
    private boolean isTouch; // 用户是否拖拽了 拖动条，（默认是没有拖动false）
    private int duration; // 获取native层的总时长
    //读写权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static final int RC_READ_EXTERNAL_STORAGE = 1; // requestCode


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        tv_state = findViewById(R.id.tv_state);
        surfaceView = findViewById(R.id.surfaceView);

        // 第7次增加
        tv_time = findViewById(R.id.tv_time);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        player = new MyFFmpegPlayer();
        player.setSurfaceView(surfaceView);
        //requestStoragePermission();

        //  测试播放特殊的视频，会有bug ： 声音是OK的, 视频只能显示第一帧 然后自动回到首页了
        //试了一下可以播放
        /*player.setDataSource(
                new File(Environment.getExternalStorageDirectory() + File.separator + "chengdu.mp4")
                        .getAbsolutePath());*/

        //  湖南卫视直播地址播放  地址失效
        // rtmp://58.200.131.2:1935/livetv/hunantv
        //m3u8地址可以播放
         player.setDataSource("http://recordcdn.quklive.com/upload/vod/user1462960877450854/1527512379701708/3/video.m3u8");

        // 还不支持 rtsp
        // rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov
        // player.setDataSource("rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov");

        // https://stream7.iqilu.com/10339/upload_transcode/202002/18/20200218114723HDu3hhxqIT.mp4
        // player.setDataSource("https://stream7.iqilu.com/10339/upload_transcode/202002/18/20200218114723HDu3hhxqIT.mp4");

        // 准备成功的回调处    <----  C++ 子线程调用的
        player.setOnPreparedListener(new MyFFmpegPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {

                // TODO 第七次增加 1.1 拖动条默认隐藏，如果播放视频有总时长，就显示所以拖动条控件
                // 得到视频总时长： 直播：duration=0，  非直播-视频：duration=有值的
                duration = player.getDuration();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //  第七次增加
                        if (duration != 0) {

                            // duration == 119 转换成  01:59

                            // 非直播-视频
                            // tv_time.setText("00:00/" + "01:59");
                            tv_time.setText("00:00/" + getMinutes(duration) + ":" + getSeconds(duration));
                            tv_time.setVisibility(View.VISIBLE); // 显示
                            seekBar.setVisibility(View.VISIBLE); // 显示
                        }

                        // Toast.makeText(MainActivity.this, "准备成功，即将开始播放", Toast.LENGTH_SHORT).show();
                        tv_state.setTextColor(Color.GREEN); // 绿色
                        tv_state.setText("初始化成功");
                    }
                });
                player.start(); // 调用 C++ 开始播放
            }
        });

        // 准备过程中，发送了错误
        /*  第2次新增 --- start */
        player.setOnErrorListener(new MyFFmpegPlayer.OnErrorListener() {
            @Override
            public void onError(final String errorInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Toast.makeText(MainActivity.this, "出错了，错误详情是:" + errorInfo, Toast.LENGTH_SHORT).show();
                        tv_state.setTextColor(Color.RED); // 红色
                        tv_state.setText("哎呀,错误啦，错误:" + errorInfo);
                    }
                });
            }
        });
        /* TODO 第2次新增 --- end */

        player.setOnOnProgressListener(new MyFFmpegPlayer.OnProgressListener() {
            @Override
            public void onProgress(final int progress) {

                // TODO C++层吧audio_time时间搓传递上来 --> 被动？
                // 【如果是人为拖动的，不能干预我们计算】 否则会混乱
                if (!isTouch) {

                    // C++层是异步线程调用上来的，小心，UI
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            if (duration != 0) {
                                // TODO 播放信息 动起来
                                // progress:C++层 ffmpeg获取的当前播放【时间（单位是秒 80秒都有，肯定不符合界面的显示） -> 1分20秒】
                                tv_time.setText(getMinutes(progress) + ":" + getSeconds(progress)
                                        + "/" +
                                        getMinutes(duration) + ":" + getSeconds(duration));

                                // TODO 拖动条 动起来 seekBar相对于总时长的百分比
                                // progress == C++层的 音频时间搓  ----> seekBar的百分比
                                // seekBar.setProgress(progress * 100 / duration 以秒计算seekBar相对总时长的百分比);
                                seekBar.setProgress(progress * 100 / duration);
                            }
                        }
                    });
                }
            }
        });
    }




    @Override // ActivityThread.java Handler
    protected void onResume() { // 我们的准备工作：触发
        super.onResume();
        player.prepare();
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    // TODO >>>>>>>>>>>>>>>>>>>>>>>>>>>

    // TODO 第七次增加 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    // 119 ---> 1.多一点点
    private String getMinutes(int duration) { // 给我一个duration，转换成xxx分钟
        int minutes = duration / 60;
        if (minutes <= 9) {
            return "0" + minutes;
        }
        return "" + minutes;
    }

    // 119 ---> 60 59
    private String getSeconds(int duration) { // 给我一个duration，转换成xxx秒
        int seconds = duration % 60;
        if (seconds <= 9) {
            return "0" + seconds;
        }
        return "" + seconds;
    }

    /**
     * 当前拖动条进度发送了改变 回调此函数
     * @param seekBar 控件
     * @param progress 1~100
     * @param fromUser 是否用户拖拽导致的改变
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            // progress 是进度条的进度 （0 - 100） ------>   秒 分 的效果
            tv_time.setText(getMinutes(progress * duration / 100)
                    + ":" +
                    getSeconds(progress * duration / 100) + "/" +
                    getMinutes(duration) + ":" + getSeconds(duration));
        }
    }

    // 手按下去，回调此函数
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isTouch = true;
    }

    // TODO 第七次增加 3
    // 手松开（SeekBar当前值 ---> C++层），回调此函数
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isTouch = false;

        int seekBarProgress = seekBar.getProgress(); // 获取当前seekbar当前进度

        // SeekBar1~100  -- 转换 -->  C++播放的时间（61.546565）
        int playProgress = seekBarProgress * duration / 100;

        player.seek(playProgress);
    }

    private void readFromSdCard() {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "bbb.mp4");
        if(file.exists()){
            Log.e("MainActivity","file path= "+file.getAbsolutePath());
            player.setDataSource(file.getAbsolutePath());
        }else{
            Log.e("MainActivity","file not exists");
        }
    }

    // 请求权限的方法
    private void requestStoragePermission() {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // 已获取权限
          readFromSdCard();
        } else {
            // 没有权限，现在去获取
            EasyPermissions.requestPermissions(this, "申请内存权限",
                    RC_READ_EXTERNAL_STORAGE, perms);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将返回结果转给EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        // 一些权限被授予
        Toast.makeText(this, "允许", Toast.LENGTH_SHORT).show();
        readFromSdCard();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        // 一些权限被禁止
        Toast.makeText(this, "禁止", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
            //弹出个对话框 可以自定义
        }
    }


}
