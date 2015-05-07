package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.R;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity {

    public Button mSignUpButton;
    public Button mLoginButton;

    public EditText mEmailField;
    public EditText mPasswordField;

    public ProgressBar mLoadingBar;

    public ServerAPI service;

    public String TAG = "LoginActivity";

    public GlobalFunctions mGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();

        mLoginButton.setOnClickListener(logIn);
        mSignUpButton.setOnClickListener(goToSignUpActivity);
    }

    public void init() {
        mSignUpButton = (Button)findViewById(R.id.signUpButton);
        mLoginButton = (Button)findViewById(R.id.logInButton);

        mEmailField = (EditText)findViewById(R.id.usernameField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);

        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .build();

        service = restAdapter.create(ServerAPI.class);

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));
    }

    public View.OnClickListener logIn = new Button.OnClickListener() {
        public void onClick(View v) {
            String email = mEmailField.getText().toString();
            String password = mPasswordField.getText().toString();

            mLoadingBar.setVisibility(View.VISIBLE);

            service.authenticate(new User(email, password), new Callback<Data>() {
                @Override
                public void success(Data data, Response response) {
                    Log.d(TAG, data.type + data.token);
                    mLoadingBar.setVisibility(View.GONE);
                    if(data.error == null && data.type == true) {
                        mGlobal.saveToken(data.token);
                        mGlobal.saveUserId(data.id);
                        Intent intent = new Intent(LoginActivity.this, TrackListActivity.class);
                        startActivity(intent);
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    retrofitError.printStackTrace();
                }
            });
        }
    };

    public View.OnClickListener goToSignUpActivity = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        }
    };

}
