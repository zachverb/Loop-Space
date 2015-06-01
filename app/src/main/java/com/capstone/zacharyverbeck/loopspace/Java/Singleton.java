package com.capstone.zacharyverbeck.loopspace.Java;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by zacharyverbeck on 5/31/15.
 */
public class Singleton extends Application {

    public SimpleDiskCache mSimpleDiskCache;
    public Context mContext;
    public Singleton instance;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mSimpleDiskCache = new SimpleDiskCache(new File(Environment.getExternalStorageDirectory(), "/downloads"), 1, 20);
        } catch(IOException e) {
            Log.d("Singleton", "Whoops");
        }
    }

    public void init(Context context){
        if(mContext == null){
            mContext = context;
        }
    }

    public Singleton getInstance(){
        return instance == null ?
                (instance = new Singleton()):
                instance;
    }
}
