package com.capstone.zacharyverbeck.audiorecordtest.Java;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.R;
import com.capstone.zacharyverbeck.audiorecordtest.UI.LoopActivity;

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

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            owner = (TextView) v.findViewById(R.id.owner);
            mContainer = (RelativeLayout) v.findViewById(R.id.row);
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
        final int trackId = mDataset.get(index).id;
        final int bpm = mDataset.get(index).bpm;
        holder.title.setText(mDataset.get(index).title);
        holder.owner.setText(mDataset.get(index).User.name);
        holder.mLoopButton.setText("3", 50f, Color.WHITE);
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
