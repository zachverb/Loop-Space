package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.views.ButtonFlat;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SignUpActivity extends Activity {

    public ButtonRectangle mSignUpButton;
    public ButtonFlat mCancelButton;

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
        mSignUpButton = (ButtonRectangle)findViewById(R.id.signUpButton);
        mCancelButton = (ButtonFlat)findViewById(R.id.cancelButton);

        mSignUpButton.setRippleSpeed(50f);
        mCancelButton.setRippleSpeed(50f);

        mSignUpButton.setOnClickListener(signUp);
        mCancelButton.setOnClickListener(cancel);

        mErrorMessage = (TextView)findViewById(R.id.errorMessage);

        mUsernameField = (EditText)findViewById(R.id.usernameField);
        mPasswordField = (EditText)findViewById(R.id.passwordField);
        mEmailField = (EditText)findViewById(R.id.emailField);
        mNumberField = (EditText)findViewById(R.id.phoneNumberField);

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));
    }

    public void errorMessage(String error) {
        mErrorMessage = (TextView)findViewById(R.id.errorMessage);
        mErrorMessage.setText(error);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    public View.OnClickListener signUp = new View.OnClickListener() {
        public void onClick(View v) {
            String username = mUsernameField.getText().toString();
            String password = mPasswordField.getText().toString();
            String email = mEmailField.getText().toString();
            String number = mNumberField.getText().toString();

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                    .build();

            service = restAdapter.create(ServerAPI.class);
            final ProgressDialog signupDialog = new ProgressDialog(SignUpActivity.this);
            signupDialog.setIndeterminate(true);
            signupDialog.setTitle("Please Wait");
            signupDialog.setMessage("Signing up");
            signupDialog.show();

            service.signup(new User(email, password, username, number), new Callback<Data>() {
                @Override
                public void success(Data data, Response response) {
                    signupDialog.dismiss();
                    Log.d(TAG, data.type + data.token);
                    //mLoadingBar.setVisibility(View.GONE);
                    if(data.error == null && data.type == true) {
                        mGlobal.saveToken(data.token);
                        mGlobal.saveUserId(data.id);
                        Intent intent = new Intent(SignUpActivity.this, TrackListActivity.class);
                        startActivity(intent);
                    } else {
                        Dialog dialog = new Dialog(SignUpActivity.this, "Error!", "Signup Error!");
                        dialog.show();
                        Log.d(TAG, data.error);
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    signupDialog.dismiss();
                    Dialog dialog = new Dialog(getApplicationContext() , "Error!", "Network error!");
                    dialog.show();
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
