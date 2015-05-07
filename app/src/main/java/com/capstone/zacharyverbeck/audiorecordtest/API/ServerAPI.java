package com.capstone.zacharyverbeck.audiorecordtest.API;

import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
import com.capstone.zacharyverbeck.audiorecordtest.Models.LoopFile;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Track;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

/**
 * Created by zacharyverbeck on 4/25/15.
 */
public interface ServerAPI {

    @GET("/me")
    public void getProfile(Callback<Data> callback);

    @POST("/signup")
    public void signup(@Body User user, Callback<Data> callback);

    @POST("/authenticate")
    public void authenticate(@Body User user, Callback<Data> callback);

    @Multipart
    @POST("/upload")
    public void upload(@Part("fileContent") TypedFile file, Callback<Endpoint> callback);

    @POST("/tracks")
    public void newTrack(@Body Track track, Callback<Data> callback);

    @GET("/tracks")
    public void getTracks(Callback<List<Track>> callback);

    @GET("/tracks/{track_id}/loops")
    public void getLoops(@Path("track_id") String trackId, Callback<List<LoopFile>> callback);

    @POST("/tracks/{track_id}/loops")
    public void addLoop(@Path("track_id") String trackId, @Body String endpoint, Callback<Data> callback);
}
