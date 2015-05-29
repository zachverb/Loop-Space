package com.capstone.zacharyverbeck.loopspace.UI;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.zacharyverbeck.loopspace.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class CommentActivityFragment extends Fragment {

    public CommentActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comment_list, container, false);
    }
}
