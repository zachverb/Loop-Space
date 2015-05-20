package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.TrackListAdapter;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.melnykov.fab.FloatingActionButton;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TrackListActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBarCircularIndeterminate mProgressBar;
    public ServerAPI service;
    public String TAG = "TrackListActivity";
    public FloatingActionButton mNewTrack;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Spinner mFiltersSpinner;
    private boolean filtersShowing = false;
    private double latitude;
    private double longitude;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private String city = "";
    private int filterIndex = 0;
    private Toolbar toolbar;
    private List<String> filterStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);
        setUpRestAdapter();
        init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        getTracks();
    }

    public double countDistance(double lat1, double lng1, double lat2, double lng2) {
        Location locationUser = new Location("point A");
        Location locationPlace = new Location("point B");
        locationUser.setLatitude(lat1);
        locationUser.setLongitude(lng1);
        locationPlace.setLatitude(lat2);
        locationPlace.setLongitude(lng2);

        double distance = locationUser.distanceTo(locationPlace);

        return distance;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
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

    private void init() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        mProgressBar = (ProgressBarCircularIndeterminate) findViewById(R.id.refreshProgress);

        mRecyclerView = (RecyclerView) findViewById(R.id.trackList);


        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(TrackListActivity.this)
                .color(R.color.divider)
                .showLastDivider()
                .build());

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTracks();
            }
        });

        getTracks();

        mNewTrack = (FloatingActionButton) findViewById(R.id.newTrack);
        mNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "YO");
                Intent trackIntent = new Intent(getApplicationContext(), TrackCreateActivity.class);
                startActivity(trackIntent);
            }
        });

        mFiltersSpinner = (Spinner) findViewById(R.id.filter_spinner);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        filterStrings = new ArrayList<String>(Arrays.asList(new String[]{"Filter Nearby", "Filter by City: ", "Global Filter"}));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                 android.R.layout.simple_spinner_item, filterStrings);

        adapter.setNotifyOnChange(true);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mFiltersSpinner.setAdapter(adapter);
        mFiltersSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        filterIndex = pos;
        getTracks();
        Log.d(TAG, String.valueOf(parent.getItemAtPosition(pos)));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                mProgressBar.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                getTracks();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getTracks() {
        switch(filterIndex) {
            case 0:
                getNearby();
                break;
            case 1:
                getTracksByCity();
                break;
            case 2:
                getTracksGlobal();
                break;
        }
    }

    public void getTracksGlobal() {
        service.getTracks(getTracksCallback);
    }

    public void getTracksByCity() {
        service.getTracksByCity(city, getTracksCallback);
    }

    public void getNearby() {
        service.getTracks(new Callback<List<Track>>() {
            @Override
            public void success(List<Track> tracks, Response response) {
                Collections.sort(tracks, new Comparator<Track>() {

                    @Override
                    public int compare(Track a, Track b) {

                        double lat = latitude;
                        double lng = longitude;

                        double lat1 = a.latitude;
                        double lng1 = a.longitude;

                        double lat2 = b.latitude;
                        double lng2 = b.longitude;

                        double lhsDistance = countDistance(lat, lng, lat1, lng1);
                        double rhsDistance = countDistance(lat, lng, lat2, lng2);
                        Log.i(TAG, String.valueOf(lhsDistance));
                        if (lhsDistance < rhsDistance)
                            return -1;
                        else if (lhsDistance > rhsDistance)
                            return 1;
                        else return 0;
                    }
                });
                mAdapter = new TrackListAdapter(getApplicationContext(), tracks);

                mRecyclerView.setAdapter(mAdapter);
                mSwipeRefreshLayout.setRefreshing(false);
                mRecyclerView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Failed to retrieve tracklist");
                error.printStackTrace();
            }
        });

    }

    public Callback<List<Track>> getTracksCallback = new Callback<List<Track>>() {
        @Override
        public void success(List<Track> tracks, Response response) {
            mAdapter = new TrackListAdapter(getApplicationContext(), tracks);
            mRecyclerView.setAdapter(mAdapter);
            mSwipeRefreshLayout.setRefreshing(false);
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        }

        @Override
        public void failure(RetrofitError error) {
            Log.d(TAG, "Failed to retrieve tracklist");
            error.printStackTrace();
        }
    };

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
                    filterStrings.set(1, filterStrings.get(1) + city);
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable connect to Geocoder", e);
            }
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
