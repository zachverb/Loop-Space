package com.capstone.zacharyverbeck.loopspace.Java;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by zacharyverbeck on 4/28/15.
 */
public class GlobalFunctions {

    Context mContext;

    public GlobalFunctions(Context context) {
        mContext = context;
    }

    public void saveToken(String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("token", token);
        editor.commit();
    }

    public void saveUserId(int id) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userId", id + "");
        editor.commit();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }


    public void setupUI(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard((Activity) mContext);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}
