package com.capstone.zacharyverbeck.loopspace.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.loopspace.Models.Track;
import com.capstone.zacharyverbeck.loopspace.R;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.common.api.GoogleApiClient;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TrackCreateActivity extends ActionBarActivity {

    public String TAG = "TrackListActivity";
    public ServerAPI service;
    public ButtonRectangle mTrackCreateButton;
    public EditText mTrackName;
    public NumberPicker mBpmPicker;
    public GlobalFunctions mGlobal;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private double latitude;
    private double longitude;
    private String city;
    private SharedPreferences settings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_create);
        init();

    }

    private void init() {
        setUpVars();
        setUpRestAdapter();
        setUpViews();
    }

    private void setUpVars() {
        settings = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        latitude = settings.getLong("latitude", 0);
        longitude = settings.getLong("longitude", 0);
        city = settings.getString("city", "");
    }

    private void setUpRestAdapter() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        final String token = settings.getString("token", "");

        // setup heroku connection
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Accept", "application/json");
                request.addHeader("Authorization", token);
            }
        };
        RestAdapter serverRestAdapter = new RestAdapter.Builder()
                .setEndpoint("https://secret-spire-6485.herokuapp.com/")
                .setRequestInterceptor(interceptor)
                .build();
        service = serverRestAdapter.create(ServerAPI.class);
    }

    private void setUpViews() {
        mTrackName = (EditText)findViewById(R.id.trackName);

        mBpmPicker = (NumberPicker)findViewById(R.id.bpmPicker);
        mBpmPicker.setMinValue(60);
        mBpmPicker.setMaxValue(200);
        mBpmPicker.setValue(80);

        mTrackCreateButton = (ButtonRectangle) findViewById(R.id.createTrackButton);
        mTrackCreateButton.setRippleSpeed(100f);
        mTrackCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTrackName.getText().length() != 0) {
                    service.newTrack(new Track(mTrackName.getText().toString(), mBpmPicker.getValue(), longitude, latitude, city), new Callback<Track>() {
                        @Override
                        public void success(Track track, Response response) {
                            Log.d(TAG, "SUCCESS!");
                            if (track.type == true) {
                                Intent loopIntent = new Intent(getApplicationContext(), LoopActivity.class);
                                loopIntent.putExtra("trackId", track.id);
                                loopIntent.putExtra("BPM", mBpmPicker.getValue());
                                Log.d(TAG, String.valueOf(track.latitude));
                                Log.d(TAG, track.city + "");

                                startActivity(loopIntent);
                            } else {
                                Log.d(TAG, "JK, failure");
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.d(TAG, "failed to create track");
                            error.printStackTrace();
                        }
                    });
                } else {
                    Dialog dialog = new Dialog(TrackCreateActivity.this, "Error", "Please enter a Track title!");
                    dialog.show();
                }
            }
        });

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.parent));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
