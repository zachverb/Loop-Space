package com.capstone.zacharyverbeck.loopspace.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.loopspace.Models.Data;
import com.capstone.zacharyverbeck.loopspace.Models.User;
import com.capstone.zacharyverbeck.loopspace.R;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginActivity extends Activity {

    public ButtonRectangle mSignUpButton;
    public ButtonRectangle mLoginButton;

    public EditText mEmailField;
    public EditText mPasswordField;

    public ServerAPI service;

    public String TAG = "LoginActivity";

    public GlobalFunctions mGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupRestAdapter();


    }

    private void setupRestAdapter() {
        final String token = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext()).getString("token", "");
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", token);
            }
        };
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .setRequestInterceptor(interceptor)
                .build();
        service = restAdapter.create(ServerAPI.class);

        if(token != "") {
            service.authorization(new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    Intent intent = new Intent(LoginActivity.this, TrackListActivity.class);
                    startActivity(intent);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d(TAG, "Not already logged in." + token);
                    setContentView(R.layout.activity_login);
                    init();
                }
            });
        }
    }

    public void init() {
        mSignUpButton = (ButtonRectangle) findViewById(R.id.signUpButton);
        mLoginButton = (ButtonRectangle) findViewById(R.id.logInButton);

        mSignUpButton.setRippleSpeed(100f);
        mLoginButton.setRippleSpeed(100f);

        mEmailField = (EditText) findViewById(R.id.usernameField);
        mPasswordField = (EditText) findViewById(R.id.passwordField);
        mPasswordField.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    login();
                    return true;
                }
                return false;
            }
        });



        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailField.getText().toString();
                String password = mPasswordField.getText().toString();
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                startActivity(intent);
            }
        });

    }

    public void login() {
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
                if (loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                Log.d(TAG, data.type + data.token);
                if (data.error == null && data.type) {
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
                if (loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
                Dialog dialog = new Dialog(LoginActivity.this, "Error!", "Network error!");
                dialog.show();

                retrofitError.printStackTrace();
            }
        });
    }

}
