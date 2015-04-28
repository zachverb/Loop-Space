package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.R;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SignUpActivity extends Activity {

    public Button mSignUpButton;
    public Button mCancelButton;

    public ProgressBar mLoadingBar;

    public TextView mErrorMessage;

    public EditText mUsernameField;
    public EditText mPasswordField;
    public EditText mEmailField;
    public EditText mNumberField;

    public ServerAPI service;

    public GlobalFunctions mGlobal;

    private final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        init();
    }

    public void init() {
        mSignUpButton = (Button)findViewById(R.id.signUpButton);
        mCancelButton = (Button)findViewById(R.id.cancelButton);

        mSignUpButton.setOnClickListener(signUp);
        mCancelButton.setOnClickListener(cancel);

        mErrorMessage = (TextView)findViewById(R.id.errorMessage);

        mUsernameField = (EditText)findViewById(R.id.usernameField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);
        mEmailField = (EditText)findViewById(R.id.emailField);
        mNumberField = (EditText)findViewById(R.id.phoneNumberField);

        mLoadingBar = (ProgressBar)findViewById(R.id.loadingBar);

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));
    }

    public void errorMessage(String error) {
        mErrorMessage = (TextView)findViewById(R.id.errorMessage);
        mErrorMessage.setText(error);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    public View.OnClickListener signUp = new Button.OnClickListener() {
        public void onClick(View v) {
            String username = mUsernameField.getText().toString();
            String password = mPasswordField.getText().toString();
            String email = mEmailField.getText().toString();
            String number = mNumberField.getText().toString();


            mLoadingBar.setVisibility(View.VISIBLE);

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                    .build();

            service = restAdapter.create(ServerAPI.class);

            service.signup(new User(email, password, username, number), new Callback<Data>() {
                @Override
                public void success(Data data, Response response) {
                    Log.d(TAG, data.type + data.token);
                    mLoadingBar.setVisibility(View.GONE);
                    if(data.error == null && data.type == true) {
                        mGlobal.saveToken(data.token);
                        mGlobal.saveUserId(data.id);
                        Intent intent = new Intent(SignUpActivity.this, TrackViewActivity.class);
                        startActivity(intent);
                    } else {
                        errorMessage(data.error);
                        Log.d(TAG, data.error);
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    retrofitError.printStackTrace();
                }
            });
        }
    };

    public View.OnClickListener cancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    };



}
