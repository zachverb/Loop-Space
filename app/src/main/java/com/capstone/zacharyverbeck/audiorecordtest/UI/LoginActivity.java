package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity {

    public ButtonRectangle mSignUpButton;
    public ButtonRectangle mLoginButton;

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
        mSignUpButton = (ButtonRectangle)findViewById(R.id.signUpButton);
        mLoginButton = (ButtonRectangle)findViewById(R.id.logInButton);

        mSignUpButton.setRippleSpeed(100f);
        mLoginButton.setRippleSpeed(100f);

        mEmailField = (EditText)findViewById(R.id.usernameField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .build();

        service = restAdapter.create(ServerAPI.class);

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));


        //toolbar.inflateMenu(R.menu.menu_login);

    }

    public View.OnClickListener logIn = new ButtonRectangle.OnClickListener() {
        public void onClick(View v) {
            String email = mEmailField.getText().toString();
            String password = mPasswordField.getText().toString();
            final ProgressDialog loginDialog = new ProgressDialog(LoginActivity.this);
            loginDialog.setIndeterminate(true);
            loginDialog.setTitle("Please Wait");
            loginDialog.setMessage("Logging in");
            loginDialog.show();


            service.authenticate(new User(email, password), new Callback<Data>() {
                @Override
                public void success(Data data, Response response) {
                    if(loginDialog.isShowing()) {
                        loginDialog.dismiss();
                    }
                    Log.d(TAG, data.type + data.token);
                    if(data.error == null && data.type) {
                        mGlobal.saveToken(data.token);
                        mGlobal.saveUserId(data.id);
                        Intent intent = new Intent(LoginActivity.this, TrackListActivity.class);
                        startActivity(intent);
                    } else {
                        Dialog dialog = new Dialog(LoginActivity.this, "Error!", data.error);
                        dialog.show();
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    if(loginDialog.isShowing()) {
                        loginDialog.dismiss();
                    }
                    Dialog dialog = new Dialog(LoginActivity.this , "Error!", "Network error!");
                    dialog.show();

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
