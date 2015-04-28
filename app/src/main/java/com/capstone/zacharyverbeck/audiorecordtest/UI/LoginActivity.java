package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Global.PreferencesGlobals;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
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

    public String TAG = "LoginActivity";

    public PreferencesGlobals mGlobal;

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

        mGlobal = new PreferencesGlobals(getApplicationContext());

        mLoginButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String username = mUsernameField.getText().toString();
                String password = mPasswordField.getText().toString();

                mLoadingBar.setVisibility(View.VISIBLE);

                service.authenticate(username, password, new Callback<Data>() {
                    @Override
                    public void success(Data data, Response response) {
                        Log.d("HttpTest", data.type + data.token);
                        mLoadingBar.setVisibility(View.GONE);
                        if(data.error == null && data.type == true) {
                            mGlobal.saveToken(data.token);
                            mGlobal.saveUserId(data.id);
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

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }



}
