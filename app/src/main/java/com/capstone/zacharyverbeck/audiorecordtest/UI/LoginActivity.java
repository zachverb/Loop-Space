package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.R;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends ActionBarActivity {

    public Button mSignUpButton;
    public Button mLoginButton;

    public EditText mUsernameField;
    public EditText mPasswordField;

    public ProgressBar mLoadingBar;

    public ServerAPI service;

    private String username;
    private String password;

    public String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mSignUpButton = (Button)findViewById(R.id.signUpButton);
        mLoginButton = (Button)findViewById(R.id.logInButton);

        mUsernameField = (EditText)findViewById(R.id.usernameField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);

        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .build();

        service = restAdapter.create(ServerAPI.class);

        mSignUpButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                username = mUsernameField.getText().toString();
                password = mPasswordField.getText().toString();

                mLoadingBar.setVisibility(View.VISIBLE);

                service.signup(new User(username, password), new Callback<Data>() {
                    @Override
                    public void success(Data data, Response response) {
                        Log.d("HttpTest", data.type + data.token);
                        mLoadingBar.setVisibility(View.GONE);
                        if(data.error == null && data.type == true) {
                            saveToken(data.token);
                            Intent intent = new Intent(LoginActivity.this, LoopActivity.class);
                            startActivity(intent);
                        } else {
                            //errorMessage(data.error);
                            Log.d(TAG, "Error signing in.");
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        retrofitError.printStackTrace();
                    }
                });
            }
        });

        mLoginButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                username = mUsernameField.getText().toString();
                password = mPasswordField.getText().toString();

                mLoadingBar.setVisibility(View.VISIBLE);

                service.authenticate(new User(username, password), new Callback<Data>() {
                    @Override
                    public void success(Data data, Response response) {
                        Log.d("HttpTest", data.type + data.token);
                        mLoadingBar.setVisibility(View.GONE);
                        if(data.error == null && data.type == true) {
                            saveToken(data.token);
                            saveUserId(data.data.id);
                            Intent intent = new Intent(LoginActivity.this, LoopActivity.class);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        retrofitError.printStackTrace();
                    }
                });
            }
        });
    }

    public void saveToken(String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("token", token);
        editor.commit();
    }

    public void saveUserId(int id) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userId", id + "");
        editor.commit();
    }


}
