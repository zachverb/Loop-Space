package com.capstone.zacharyverbeck.loopspace.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.capstone.zacharyverbeck.loopspace.API.ServerAPI;
import com.capstone.zacharyverbeck.loopspace.Java.CommentListAdapter;
import com.capstone.zacharyverbeck.loopspace.Java.GlobalFunctions;
import com.capstone.zacharyverbeck.loopspace.Models.Comment;
import com.capstone.zacharyverbeck.loopspace.R;
import com.gc.materialdesign.views.ButtonRectangle;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.List;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CommentActivity extends ActionBarActivity {

    private String TAG = "CommentActivity";
    private RecyclerView mCommentList;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Toolbar toolbar;
    private ServerAPI service;
    private String trackId;
    private String city;
    private EditText mCommentBox;
    private ButtonRectangle mSubmitButton;
    private GlobalFunctions mGlobal;
    private MaterialMenuDrawable materialMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        setUpRestAdapter();
        setUpToolbar();
        init();
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        materialMenu = new MaterialMenuDrawable(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.setIconState(MaterialMenuDrawable.IconState.ARROW);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "home selected");
                Intent loopIntent = new Intent(CommentActivity.this, LoopActivity.class);
                loopIntent.putExtra("trackId", Integer.parseInt(trackId));
                startActivity(loopIntent);
            }
        });
        toolbar.setNavigationIcon(materialMenu);
        getSupportActionBar().setTitle("Comments");
    }

    private void init() {
        trackId = getIntent().getIntExtra("trackId", -1) + "";

        mGlobal = new GlobalFunctions(this);
        mGlobal.setupUI(findViewById(R.id.container));

        mCommentList = (RecyclerView) findViewById(R.id.commentlist);


        mCommentList.addItemDecoration(new HorizontalDividerItemDecoration.Builder(CommentActivity.this)
                .color(R.color.divider)
                .showLastDivider()
                .build());

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mCommentList.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mCommentList.setLayoutManager(mLayoutManager);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mCommentBox = (EditText) findViewById(R.id.commentBox);
        mSubmitButton = (ButtonRectangle) findViewById(R.id.submitButton);

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCommentBox.getText().length() != 0) {
                    newComment(mCommentBox.getText().toString());
                    mGlobal.hideSoftKeyboard(CommentActivity.this);
                    mCommentBox.setText("");
                }
            }
        });
        getComments();
    }

    private void setUpRestAdapter() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        final String token = settings.getString("token", "");
        city = settings.getString("city", "");

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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_collaborate) {
//            Intent loopIntent = new Intent(CommentActivity.this, LoopActivity.class);
//            loopIntent.putExtra("trackId", Integer.parseInt(trackId));
//            loopIntent.putExtra("BPM", getIntent().getIntExtra("BPM", -1));
//            startActivity(loopIntent);
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void getComments() {
        service.getComments(trackId, new Callback<List<Comment>>() {
            @Override
            public void success(List<Comment> comments, Response response) {
                mAdapter = new CommentListAdapter(getApplicationContext(), comments);

                mCommentList.setAdapter(mAdapter);
                mCommentList.setVisibility(View.VISIBLE);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Failed to retrieve tracklist");
                error.printStackTrace();
            }
        });
    }

    public void newComment(String comment) {
        service.newComment(trackId, new Comment(comment, city), new Callback<Comment>() {
            @Override
            public void success(Comment comment, Response response) {
                getComments();
            }

            @Override
            public void failure(RetrofitError error) {
                Log.d(TAG, "Failed to retrieve tracklist");
                error.printStackTrace();
            }
        });
    }
}
