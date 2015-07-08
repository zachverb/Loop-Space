package com.capstone.zacharyverbeck.loopspace.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.balysv.materialmenu.MaterialMenuDrawable;
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
        setUpToolbar();
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
                .setEndpoint(this.getResources().getString(R.string.server_addr))
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
                if (mTrackName.getText().length() != 0) {
                    service.newTrack(new Track(mTrackName.getText().toString(), mBpmPicker.getValue(), longitude, latitude, city), new Callback<Track>() {
                        @Override
                        public void success(Track track, Response response) {
                            Log.d(TAG, "SUCCESS!");
                            Intent loopIntent = new Intent(getApplicationContext(), LoopActivity.class);
                            loopIntent.putExtra("trackId", track.id);
                            Log.d(TAG, String.valueOf(track.latitude));
                            Log.d(TAG, track.city + "");
                            startActivity(loopIntent);
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

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        MaterialMenuDrawable materialMenu = new MaterialMenuDrawable(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.setIconState(MaterialMenuDrawable.IconState.ARROW);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "home selected");
                TrackCreateActivity.this.finish();
            }
        });
        toolbar.setNavigationIcon(materialMenu);
        getSupportActionBar().setTitle("New Track");
    }


}
