package com.capstone.zacharyverbeck.audiorecordtest.Java;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.capstone.zacharyverbeck.audiorecordtest.UI.LoopActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by zacharyverbeck on 5/6/15.
 */
public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder> {

    private List<Track> mDataset;
    private Context mContext;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView title;
        public TextView owner;
        public RelativeLayout mContainer;
        public LoopButton mLoopButton;
        public TextView timeStamp;
        public TextView city;

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            owner = (TextView) v.findViewById(R.id.owner);
            mContainer = (RelativeLayout) v.findViewById(R.id.row);
            timeStamp = (TextView) v.findViewById(R.id.timeStamp);
            city = (TextView) v.findViewById(R.id.city);
            mLoopButton = (LoopButton) v.findViewById(R.id.circle);
            mLoopButton.setClickable(false);
            mLoopButton.setOnClickListener(null);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TrackListAdapter(Context context, List<Track> myDataset) {
        mDataset = myDataset;
        mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TrackListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        //...
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        final int index = position;
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Track track = mDataset.get(index);
        final int trackId = track.id;
        final int bpm = track.bpm;
        String username = track.User.name;
        holder.title.setText(track.title.substring(0, 1).toUpperCase() + track.title.substring(1));
        holder.owner.setText(username);
        holder.mLoopButton.setText(username.substring(0,1).toUpperCase(), 60f, Color.WHITE);
        String date = mDataset.get(index).createdAt;
        Date formatted = null;
        try {
            formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(date.replaceAll("Z$", "+0000"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat outputFormatter = new SimpleDateFormat("MMM d, h:mm a");
        holder.timeStamp.setText(outputFormatter.format(formatted));
        holder.city.setText(track.city);
        Log.d("TrackListAdapter", formatted.toString());
        //Log.d("WHY", mDataset.get(index).user.email);
        holder.mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loopIntent = new Intent(mContext, LoopActivity.class);
                loopIntent.putExtra("trackId", trackId);
                loopIntent.putExtra("BPM", bpm);
                loopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(loopIntent);
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
