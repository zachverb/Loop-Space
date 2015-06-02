package com.capstone.zacharyverbeck.loopspace.Java;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by zacharyverbeck on 5/31/15.
 */
public class LoopApplication extends Application {

    public SimpleDiskCache mSimpleDiskCache;
    public LoopApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        String cacheLocation = Environment.getExternalStorageDirectory() + "/loopspacecache";
        Log.d("LoopActivity", "it was put in here man + " + cacheLocation);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 8);
        Log.d("LoopActivity", maxMemory + " memory");
        Log.d("LoopActivity", (4 * 1024 * 1024) + "");
        try {
            mSimpleDiskCache = SimpleDiskCache.open(new File(cacheLocation), 1, (4 * 1024 * 1024));
        } catch(IOException e) {
            Log.d("Singleton", "Whoops");
        }
    }

    public LoopApplication getInstance(){
        return instance;
    }

    public SimpleDiskCache getSimpleDiskCache() {
        return mSimpleDiskCache;
    }
}
