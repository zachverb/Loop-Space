package com.capstone.zacharyverbeck.loopspace.Java;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by zacharyverbeck on 5/31/15.
 */
public class LoopSingleton {

    private static LoopSingleton sLoopSingleton;
    private Context mAppContext;
    public SimpleDiskCache mSimpleDiskCache;

    private LoopSingleton(Context appContext) {
        mAppContext = appContext;
        String cacheLocation = Environment.getExternalStorageDirectory() + "/loopcache";
        if(mSimpleDiskCache == null) {
            try {
                // creates a new cache in the folder /loopspacecache. The size is 4MiB
                mSimpleDiskCache = SimpleDiskCache.open(new File(cacheLocation), 1, (20 * 1024 * 1024));
            } catch (IOException e) {
                Log.d("Singleton", "Whoops");
            }
        }
    }

    public static LoopSingleton get(Context c) {
        if(sLoopSingleton == null) {
            sLoopSingleton = new LoopSingleton(c.getApplicationContext());
        }
        return sLoopSingleton;
    }

    public SimpleDiskCache getSimpleDiskCache() {
        return mSimpleDiskCache;
    }
}
