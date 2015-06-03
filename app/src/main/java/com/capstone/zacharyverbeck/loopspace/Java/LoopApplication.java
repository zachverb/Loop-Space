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
        try {
            // creates a new cache in the folder /loopspacecache. The size is 4MiB
            mSimpleDiskCache = SimpleDiskCache.open(new File(cacheLocation), 1, (20 * 1024 * 1024));
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
