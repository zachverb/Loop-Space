package com.capstone.zacharyverbeck.audiorecordtest.Global;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by zacharyverbeck on 4/28/15.
 */
public class PreferencesGlobals {

    Context mContext;

    public PreferencesGlobals(Context context) {
        mContext = context;
    }

    public void saveToken(String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("token", token);
        editor.commit();
    }

    public void saveUserId(int id) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userId", id + "");
        editor.commit();
    }
}
