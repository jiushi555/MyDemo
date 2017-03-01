package com.example.administrator.audiowithcache;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.danikula.videocache.HttpProxyCacheServer;

import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2017/2/24.
 */

public class AudioPlayerService extends Service implements MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener{

    private static String TAG="AudioPlayerService---";

    public MediaPlayer mMediaPlayer;

    private String mUri; //音频播放地址

    private MyBinder mMyBinder=new MyBinder();

    private int mIntNumTime;   //播放总时长

    private int mIntNowTime=0;   //播放当前时间

    private PlaySecondListener mListener;

    private MediaPlayer.OnBufferingUpdateListener mBufferListener;

    private static int TYPE1=1;   //更新时间
    private static int TYPE2=2;   //其他消息

    //缓存
    private HttpProxyCacheServer mProxy;
    private String mProxyUrl;

    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
            if(mIntNowTime<=mIntNumTime){
                mHandler.postDelayed(mRunnable,1000);
            }

            Log.e(TAG,mIntNowTime+"");

            mListener.setTextSecond(mIntNowTime,TYPE1);

            mIntNowTime=mIntNowTime+1000;
        }
    };

    //标题栏上的控制
    private ImageView mBtnNoticePOP;   //标题栏栏上的播放、暂停按钮
    private NotificationManager mManager;
    private RemoteViews mRemoteViews;
    private Notification mNotification;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMyBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    class MyBinder extends Binder{
        public void initPlayer(String uri, PlaySecondListener listener,
                               MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener){
            Log.e(TAG,"initPlayer");

            //初始化缓存
            mProxy = getProxy();
            mProxyUrl = mProxy.getProxyUrl(uri);
            mProxy.registerCacheListener(new MyCacheListener(), uri);

            mListener=listener;
            mBufferListener=onBufferingUpdateListener;
            mMediaPlayer=new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(AudioPlayerService.this);
            mMediaPlayer.setOnPreparedListener(AudioPlayerService.this);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferListener);

            try {
                mMediaPlayer.setDataSource(mProxyUrl);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public AudioPlayerService getService(){
            return AudioPlayerService.this;
        }

        public boolean isCached(String uri){
            if(mProxy==null){
                Log.e(TAG,"null");
                return false;
            }
            if(mProxy.isCached(uri)){
                Log.e(TAG,"Cached");
                return true;
            }
            Log.e(TAG,"NotCached");
            return false;
        }
    }

    private class MyCacheListener implements com.danikula.videocache.CacheListener {

        @Override
        public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
            Log.e(TAG, percentsAvailable + "");
        }
    }

    public void play(){
        if(mMediaPlayer!=null){
            mHandler.post(mRunnable);
            mMediaPlayer.start();
        }
    }

    public void pause(){
        if(mMediaPlayer!=null){
            mHandler.removeCallbacks(mRunnable);
            mMediaPlayer.pause();
        }
    }

    public void seekTo(int i){
        if(mMediaPlayer!=null){
            mMediaPlayer.seekTo(i);
            mIntNowTime=i;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mHandler.removeCallbacks(mRunnable);

        mIntNowTime=0;

        mListener.setTextSecond(mIntNowTime,TYPE1);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        registerBroadcastReceiver();

        initNotification();

        mediaPlayer.start();

        mIntNumTime=mediaPlayer.getDuration();

        mListener.setTextSecond(mIntNumTime,TYPE2);

        Log.e(TAG,mIntNumTime+"");

        mHandler.post(mRunnable);
    }

    /**
     * 检查是否播放
     * @return
     */
    public boolean checkPlaying(){
        if(mMediaPlayer.isPlaying()){
            return true;
        }else{
            return false;
        }
    }

    private HttpProxyCacheServer getProxy() {
        return App.getProxy(this);
    }
    /**
     * 初始化通知栏音乐播放器控制
     * 使用广播，控制音乐播放
     */
    private void initNotification(){
        Notification.Builder mNotifyBuilder=new Notification.Builder(this);
        mNotification=mNotifyBuilder.setContentTitle("音乐播放器")
                .setContentText("音乐播放器")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        mManager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //通知栏一直显示
        mNotification.flags=Notification.FLAG_ONGOING_EVENT;

        //标题栏显示布局
        mRemoteViews=new RemoteViews(getPackageName(),R.layout.notice_musiccontrol);

        if(checkPlaying()){
            mRemoteViews.setImageViewResource(R.id.id_notice_playorpause,R.drawable.ic_pause);
        }else{
            mRemoteViews.setImageViewResource(R.id.id_notice_playorpause,R.drawable.ic_play);
        }

        //点击返回主界面
        Intent it_go=new Intent();
        it_go.setAction(String.valueOf(what.broadcast.open));
        PendingIntent intent_go = PendingIntent.getBroadcast(this, 0, it_go,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.id_notice,intent_go);

        //点击暂停
        if(checkPlaying()){
            Intent it_pause=new Intent();
            it_pause.setAction(String.valueOf(what.broadcast.pause));
            PendingIntent intent_pause=PendingIntent.getBroadcast(this,1,it_pause,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteViews.setOnClickPendingIntent(R.id.id_notice_playorpause,intent_pause);
        }

        //点击播放
        if(checkPlaying()){
            Intent it_play=new Intent();
            it_play.setAction(String.valueOf(what.broadcast.play));
            PendingIntent intent_play=PendingIntent.getBroadcast(this,2,it_play,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteViews.setOnClickPendingIntent(R.id.id_notice_playorpause,intent_play);
        }

        mNotification.bigContentView=mRemoteViews;



        mManager.notify(0,mNotification);
    }

    /**
     * 注册广播
     */
    private void registerBroadcastReceiver(){
        IntentFilter intentFilterOpen=new IntentFilter();
        intentFilterOpen.addAction(String.valueOf(what.broadcast.open));
        registerReceiver(mBroadcastReceiver,intentFilterOpen);

        IntentFilter intentFilterPlay=new IntentFilter();
        intentFilterOpen.addAction(String.valueOf(what.broadcast.play));
        registerReceiver(mBroadcastReceiver,intentFilterPlay);

        IntentFilter intentFilterPause=new IntentFilter();
        intentFilterPause.addAction(String.valueOf(what.broadcast.pause));
        registerReceiver(mBroadcastReceiver,intentFilterPause);
    }

    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(String.valueOf(what.broadcast.open))){
                //TODO
            }else if(action.equals(String.valueOf(what.broadcast.play))){
                play();
                mRemoteViews.setImageViewResource(R.id.id_notice_playorpause,R.drawable.ic_pause);
            }else if(action.equals(String.valueOf(what.broadcast.pause))){
                pause();
                mRemoteViews.setImageViewResource(R.id.id_notice_playorpause,R.drawable.ic_play);
            }
        }
    };
}
