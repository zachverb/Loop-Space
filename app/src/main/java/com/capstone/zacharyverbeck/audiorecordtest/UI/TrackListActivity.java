package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.TrackListAdapter;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.melnykov.fab.FloatingActionButton;
import com.rey.material.app.Dialog;
import com.rey.material.widget.EditText;

import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TrackListActivity extends ActionBarActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public ServerAPI service;
    public String TAG = "TrackListActivity";
    public FloatingActionButton mNewTrack;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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
        mRecyclerView = (RecyclerView) findViewById(R.id.trackList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        // specify an adapter (see also next example)

        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                getTracks();
            }
        });

        getTracks();

        mNewTrack = (FloatingActionButton)findViewById(R.id.newTrack);
        mNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "YO");


                final Dialog mDialog = new Dialog(TrackListActivity.this, R.style.Material_App_Dialog_Simple);
                final boolean[] isText = new boolean[]{false};

                final EditText input = new EditText(TrackListActivity.this);
                input.setTextColor(Color.WHITE);
                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        Log.d(TAG, "Before Text Changed");
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if(s.length() == 0) {
                            isText[0] = false;
                        } else {
                            isText[0] = true;
                        }
                        Log.d(TAG, "On Text Changed");
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        Log.d(TAG, "After Text Changed");
                    }
                });
                FrameLayout container = new FrameLayout(TrackListActivity.this);
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                params.leftMargin = (int) convertDpToPx(15, TrackListActivity.this);
                params.rightMargin = (int) convertDpToPx(15, TrackListActivity.this);
                input.setLayoutParams(params);
                input.setSingleLine();
                container.addView(input);
                mDialog.title("Track Name")
                        .positiveAction("OK")
                        .negativeAction("CANCEL")
                        .contentView(container)
                        .contentMargin(50)
                        .show();


                mDialog.positiveActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isText[0] == false) {
                            input.setHelper("Can't be empty!");
                        } else {
                            service.newTrack(new Track(input.getText().toString()), new Callback<Data>() {
                                @Override
                                public void success(Data data, Response response) {
                                    Log.d(TAG, "SUCCESS!");
                                    if (data.type == true) {
                                        Intent loopIntent = new Intent(getApplicationContext(), LoopActivity.class);
                                        loopIntent.putExtra("trackId", data.id);
                                        //loopIntent.putExtra("title", );
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
                            mDialog.dismiss();
                        }
                    }
                });

                mDialog.negativeActionClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.dismiss();
                    }
                });

            }
        });
    }

    public void getTracks() {
        service.getTracks(new Callback<List<Track>>() {
            @Override
            public void success(List<Track> tracks, Response response) {
                mAdapter = new TrackListAdapter(getApplicationContext(), tracks);
                mRecyclerView.setAdapter(mAdapter);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Failed to retrieve tracklist");
                error.printStackTrace();
            }
        });
    }

    public static float convertDpToPx(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }
}
