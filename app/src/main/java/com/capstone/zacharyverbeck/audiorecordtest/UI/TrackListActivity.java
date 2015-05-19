package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.capstone.zacharyverbeck.audiorecordtest.API.ServerAPI;
import com.capstone.zacharyverbeck.audiorecordtest.Java.TrackListAdapter;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.melnykov.fab.FloatingActionButton;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

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
    private ProgressBarCircularIndeterminate mProgressBar;
    public ServerAPI service;
    public String TAG = "TrackListActivity";
    public FloatingActionButton mNewTrack;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Spinner mFiltersSpinner;
    private boolean filtersShowing = false;

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

        // specify an adapter (see also next example)

        mSwipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
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

        mFiltersSpinner = (Spinner)findViewById(R.id.filter_spinner);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Global Tracks");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(filtersShowing);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filters_array, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mFiltersSpinner.setAdapter(adapter);




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
            case R.id.action_filter:
                //filtersShowing = !filtersShowing;
                //getSupportActionBar().setDisplayShowTitleEnabled(filtersShowing);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getTracks() {
        service.getTracks(new Callback<List<Track>>() {
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
        });
    }
}
