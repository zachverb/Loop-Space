package com.capstone.zacharyverbeck.audiorecordtest.API;

import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;
import com.capstone.zacharyverbeck.audiorecordtest.Models.LoopFile;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;

import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
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

    @FormUrlEncoded
    @POST("/authenticate")
    public void authenticate(@Field("username") String username, @Field("password") String password, Callback<Data> callback);

    @Multipart
    @POST("/upload")
    public void upload(@Part("fileContent") TypedFile file, Callback<Endpoint> callback);

    @GET("/tracks/{track_id}/loops")
    public void getLoops(@Path("track_id") int trackId, Callback<List<LoopFile>> callback);

    @POST("/tracks/{track_id}/loops")
    public void addLoop(@Path("track_id") int track_id, @Body String endpoint, Callback<Data> callback);
}
