package com.capstone.zacharyverbeck.audiorecordtest.API;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Streaming;

/**
 * Created by zacharyverbeck on 4/25/15.
 */
public interface S3API {

    @GET("/loops/{filename}")
    @Streaming
    void getLoop(@Path("filename") String filename, Callback<Response> callback);

}
