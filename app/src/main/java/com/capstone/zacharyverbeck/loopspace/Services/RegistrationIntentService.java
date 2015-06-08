package com.capstone.zacharyverbeck.loopspace.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Models.User;
import com.capstone.zacharyverbeck.loopspace.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by zacharyverbeck on 6/4/15.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};


    public void onCreate() {
        super.onCreate();
        Log.d("Server", ">>>onCreate()");
    }

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "Yup");
        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                // [START get_token]
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local.
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.i(TAG, "GCM Registration Token: " + token);

                // TODO: Implement this method to send any registration to your app's servers.
                //if(sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false)) {
                sendRegistrationToServer(token);
                //}
                // Subscribe to topic channels
                //subscribeTopics(token);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                // [END get_token]
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        final String userToken = settings.getString("token", "");

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("gcmToken", token);
        if(token != "") {
            // setup heroku connection
            RequestInterceptor interceptor = new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Accept", "multipart/form-data");
                    request.addHeader("Authorization", userToken);
                }
            };
            RestAdapter serverRestAdapter = new RestAdapter.Builder()
                    .setEndpoint(this.getResources().getString(R.string.server_addr))
                    .setRequestInterceptor(interceptor)
                    .build();
            ServerAPI service = serverRestAdapter.create(ServerAPI.class);
            service.gcmRegistration(new User(token), new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    Log.d(TAG, "WORKED");
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.d(TAG, "DID NOT WORK");
                }
            });

            editor.putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
        } else {
            editor.putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
//    private void subscribeTopics(String token) throws IOException {
//        for (String topic : TOPICS) {
//            GcmPubSub pubSub = GcmPubSub.getInstance(this);
//            pubSub.subscribe(token, "/topics/" + topic, null);
//        }
//    }
    // [END subscribe_topics]

}
