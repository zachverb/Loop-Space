package com.capstone.zacharyverbeck.audiorecordtest.UI;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.capstone.zacharyverbeck.audiorecordtest.R;

public class TrackViewActivity extends Activity {

    public RecyclerView mTrackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_view);
        init();
    }

    public void init() {
        mTrackList = (RecyclerView)findViewById(R.id.trackList);
    }
}
