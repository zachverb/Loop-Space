package com.capstone.zacharyverbeck.loopspace.Java;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.capstone.zacharyverbeck.loopspace.Models.Comment;
import com.capstone.zacharyverbeck.loopspace.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by zacharyverbeck on 5/28/15.
 */
public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.ViewHolder> {

    private List<Comment> mCommentsList;
    private Context mContext;
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView comment;
        public TextView owner;
        public TextView timeStamp;
        public RelativeLayout mContainer;

        public ViewHolder(View v) {
            super(v);
            comment = (TextView) v.findViewById(R.id.commentText);
            owner = (TextView) v.findViewById(R.id.owner);
            mContainer = (RelativeLayout) v.findViewById(R.id.row);
            timeStamp = (TextView) v.findViewById(R.id.timeStamp);
        }
    }

    public CommentListAdapter(Context context, List<Comment> comments) {
        mContext = context;
        mCommentsList = comments;
    }
    @Override
    public CommentListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_row_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        //...
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final int index = position;
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Comment comment = mCommentsList.get(index);
        // final int commentId = comment.id;
        String username = comment.User.name;
        holder.comment.setText(comment.comment.substring(0, 1).toUpperCase() + comment.comment.substring(1));
        holder.owner.setText(username);
        String date = mCommentsList.get(index).createdAt;
        Date formatted = null;
        try {
            formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(date.replaceAll("Z$", "+0000"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat outputFormatter = new SimpleDateFormat("MMM d, h:mm a");
        holder.timeStamp.setText(outputFormatter.format(formatted));
    }

    @Override
    public int getItemCount() {
        return mCommentsList.size();
    }
}
