package com.example.administrator.audiowithcache;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 2017/2/21.
 */

public class Utils {
    /**
     * 格式化时间
     * @param time  毫秒
     * @return
     */
    public static String formatTime(int time){
        SimpleDateFormat sdf=new SimpleDateFormat("mm:ss");
        Date date=new Date(time);

        String formatTime=sdf.format(date);

        return formatTime;
    }

    /**
     * 判断某个Activity或service是否存在
     * @param context
     * @param className
     * @return
     */
    public static boolean isWorked(Context context,String className) {
        ActivityManager manager = (ActivityManager) context.getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        ArrayList<ActivityManager.RunningServiceInfo> runningService =
                (ArrayList<ActivityManager.RunningServiceInfo>) manager.getRunningServices(30);

        for (int i = 0; i < runningService.size(); i++) {
            Log.e("AudioPlayerActivity---",runningService.get(i).service.getClassName().toString());
            if (runningService.get(i).service.getClassName().toString().equals(className)) {
                return true;
            }
        }

        return false;

    }
}
