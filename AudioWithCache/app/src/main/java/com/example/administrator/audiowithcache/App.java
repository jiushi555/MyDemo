package com.example.administrator.audiowithcache;

import android.app.Application;
import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * Created by Administrator on 2017/2/22.
 */

public class App extends Application {

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context){
        App app= (App) context.getApplicationContext();
        return app.proxy==null?(app.proxy=app.newProxy()):app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer(this);
    }
}
