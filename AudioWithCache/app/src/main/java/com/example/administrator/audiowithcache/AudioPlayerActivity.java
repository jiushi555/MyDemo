package com.example.administrator.audiowithcache;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.ContactsContract;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Administrator on 2017/2/21.
 */

public class AudioPlayerActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener {

//    String uri = "http://abv.cn/music/光辉岁月.mp3";   //网络音频资源

    String uri = "http://yjx-szrw.oss-cn-shanghai.aliyuncs.com/wKgJMViuc9ayTk3wADBg-4wkuec084.m4a";
    //    String uri="http://yjx-szrw.oss-cn-shanghai.aliyuncs.com/%E5%90%8C%E6%A0%B9%E4%B8%8D%E5%90%8C%E5%91%BD.mp3?Expires=1487835142&OSSAccessKeyId=TMP.AQFX35SChXwqbyZEX4K2cV77i0HUMDJajkn3OntQhQ0efikS-o2sUqVT3op4MC4CFQCM7XADChLKFkQN-Enkd0sBY0nawwIVAIjEeH7Jwov-_1k6r8AwGdtOPJQN&Signature=%2B2qXC9RW%2FhC8QRyyJ2CBsuY7l3c%3D";
    private static String TAG = "AudioPlayerActivity---";
    private ImageView mBtnLast;
    private ImageView mBtnPlayOrPause;
    private ImageView mBtnNext;
    private SeekBar mSeekBar;

//    private MediaPlayer mMediaPlayer = null;

    private boolean isPrepared = false;

    private TextView mTextViewTime;
    private TextView mTextViewNum;

    private int mNumTime;   //标记音频总时长
    private int mPlayTime = 0;   //标记现在播放的时间

    //标题栏上的控制
    private ImageView mBtnNoticePOP;   //标题栏栏上的播放、暂停按钮
    private NotificationManager mManager;
    private RemoteViews mRemoteViews;
    private Notification mNotification;


    //缓存
    private HttpProxyCacheServer mProxy;
    private String mProxyUrl;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            mTextViewTime.setText(Utils.formatTime(mPlayTime));
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            if (mPlayTime <= mNumTime) {
                mTextViewTime.setText(Utils.formatTime(mPlayTime));
                mSeekBar.setProgress(mPlayTime);
                mHandler.postDelayed(mRunnable, 1000);
            }

            mPlayTime = mPlayTime + 1000;
        }
    };

    private AudioPlayerService.MyBinder mMyBinder;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mMyBinder = (AudioPlayerService.MyBinder) iBinder;
            mService = mMyBinder.getService();

            mMyBinder.initPlayer(uri, new PlaySecondListener() {
                @Override
                public void setTextSecond(int time, int type) {
                    if (type == 2) {
                        mNumTime = time;
                        mTextViewNum.setText(Utils.formatTime(time));
                        mSeekBar.setMax(time);
                    } else {
                        mSeekBar.setProgress(time);
                        mTextViewTime.setText(Utils.formatTime(time));
                    }
                    changeBtn();
                }
            }, new MediaPlayer.OnBufferingUpdateListener() {

                @Override
                public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                    Log.e(TAG, "缓存进度" + i);
                    mSeekBar.setSecondaryProgress(mSeekBar.getMax() / 100 * i);
                }

            });

            if (mMyBinder.isCached(uri)) {
                mSeekBar.setSecondaryProgress(100);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private AudioPlayerService mService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audioplayer);

        findView();

        initCache();

        bindService();

        setListener();

        //禁止截屏
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    /**
     * 绑定service
     */
    private void bindService() {
        Intent it = new Intent(this, AudioPlayerService.class);
        bindService(it, mConnection, BIND_AUTO_CREATE);
    }

    private void initCache() {
        mProxy = getProxy();
        mProxyUrl = mProxy.getProxyUrl(uri);
    }

    private HttpProxyCacheServer getProxy() {
        return App.getProxy(this);
    }

    private void findView() {
        mBtnLast = (ImageView) findViewById(R.id.id_btn_last);
        mBtnNext = (ImageView) findViewById(R.id.id_btn_next);
        mBtnPlayOrPause = (ImageView) findViewById(R.id.id_btn_playorpause);
        mSeekBar = (SeekBar) findViewById(R.id.id_seekbar);

        mTextViewNum = (TextView) findViewById(R.id.id_text_num);
        mTextViewTime = (TextView) findViewById(R.id.id_text_time);
    }

    private void setListener() {
        mBtnPlayOrPause.setOnClickListener(new PlayOrPauseListener());


        /*mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener());
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener());
        mMediaPlayer.setOnPreparedListener(this);*/

        mSeekBar.setOnSeekBarChangeListener(new SeekChangeListener());

        mProxy.registerCacheListener(new MyCacheListener(), uri);
    }

    private class MyCacheListener implements com.danikula.videocache.CacheListener {

        @Override
        public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
            Log.e(TAG, percentsAvailable + "");
        }
    }

    /**
     * 播放、暂停按钮点击监听
     */
    private class PlayOrPauseListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (mService.mMediaPlayer == null) {
                return;
            }

            if (mService.mMediaPlayer.isPlaying()) {
                mService.pause();
            } else {
                mService.play();
            }

            changeBtn();
        }
    }

    /**
     * 改变按钮状态
     */
    private void changeBtn() {

        if (mService.mMediaPlayer == null) {
            return;
        }

        if (mService.mMediaPlayer.isPlaying()) {
            mBtnPlayOrPause.setImageResource(R.drawable.ic_pause);
        } else {
            mBtnPlayOrPause.setImageResource(R.drawable.ic_play);
        }

    }

    /**
     * 进度条滑动监听
     */
    public class SeekChangeListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            mTextViewTime.setText(Utils.formatTime(i));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mService.seekTo(seekBar.getProgress());
            mPlayTime = seekBar.getProgress();
//            mMediaPlayer.seekTo(mPlayTime);
        }
    }


    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        mediaPlayer.start();
        isPrepared = true; //标记状态，MediaPlayer准备好了
        Log.e(TAG, "处理后的地址：" + mProxyUrl);
//        mNumTime = mMediaPlayer.getDuration();
        mTextViewNum.setText(Utils.formatTime(mNumTime));
        mSeekBar.setMax(mNumTime);

        mHandler.post(mRunnable);

        changeBtn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unbindService(mConnection);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //点击返回按钮是回退到上级，而非销毁
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



}
