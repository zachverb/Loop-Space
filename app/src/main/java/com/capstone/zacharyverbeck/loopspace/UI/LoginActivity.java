package com.capstone.zacharyverbeck.loopspace.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.loopspace.Models.Data;
import com.capstone.zacharyverbeck.loopspace.Models.User;
import com.capstone.zacharyverbeck.loopspace.R;
import com.capstone.zacharyverbeck.loopspace.Services.QuickstartPreferences;
import com.capstone.zacharyverbeck.loopspace.Services.RegistrationIntentService;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

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

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiveToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private void receiveToken() {
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.d(TAG, "Token sent!");
                } else {
                    Log.d(TAG, "Token didn't send");
                }
                setupRestAdapter();
            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Log.d(TAG, "Yup");
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        } else {
            setupRestAdapter();
        }
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
                .setEndpoint(this.getResources().getString(R.string.server_addr))
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
        } else {
            setContentView(R.layout.activity_login);
            init();
        }
    }

    public void init() {
        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));

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

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        9000).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}
