package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
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

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TrackCreateActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_create);
        init();





    }


    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }




    private void init() {
        setUpRestAdapter();
        setUpViews();
        buildGoogleApiClient();
        mGoogleApiClient.connect();

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

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();

            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocation(
                        latitude, longitude, 1);
                if (addressList != null && addressList.size() > 0) {
                    Address address = addressList.get(0);
                    city = address.getLocality();
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable connect to Geocoder", e);
            }
            Log.d(TAG, String.valueOf(mLastLocation.getLatitude()));
        }



    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();


    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());

    }
}
