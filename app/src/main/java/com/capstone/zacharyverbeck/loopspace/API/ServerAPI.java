package com.capstone.zacharyverbeck.loopspace.API;

import com.capstone.zacharyverbeck.loopspace.Models.Comment;
import com.capstone.zacharyverbeck.loopspace.Models.Data;
import com.capstone.zacharyverbeck.loopspace.Models.Endpoint;
import com.capstone.zacharyverbeck.loopspace.Models.LoopFile;
import com.capstone.zacharyverbeck.loopspace.Models.Track;
import com.capstone.zacharyverbeck.loopspace.Models.User;

import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
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

    @POST("/authorization")
    public void authorization(Callback<Response> callback);

    @Multipart
    @POST("/upload")
    public void upload(@Part("fileContent") TypedFile file, Callback<Endpoint> callback);

    @POST("/tracks")
    public void newTrack(@Body Track track, Callback<Track> callback);

    @GET("/tracks")
    public void getTracks(Callback<List<Track>> callback);

    @GET("/tracks/{city}")
    public void getTracksByCity(@Path("city") String city, Callback<List<Track>> callback);

    @GET("/tracks/{track_id}/loops")
    public void getLoops(@Path("track_id") String trackId, Callback<List<LoopFile>> callback);

    @GET("/tracks/{track_id}/comments")
    public void getComments(@Path("track_id") String trackId, Callback<List<Comment>> callback);

    @POST("/tracks/{track_id}/comments")
    public void newComment(@Path("track_id") String trackId, @Body Comment comment, Callback<Comment> callback);

    @DELETE("/tracks/{track_id}/loops/{loop_id}")
    public void deleteLoop(@Path("track_id") String trackId, @Path("loop_id") String loopId, Callback<Response> callback);
}
