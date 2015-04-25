package com.capstone.zacharyverbeck.audiorecordtest.API;

import com.capstone.zacharyverbeck.audiorecordtest.Models.Data;
import com.capstone.zacharyverbeck.audiorecordtest.Models.User;
import com.capstone.zacharyverbeck.audiorecordtest.Models.Endpoint;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
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

}
