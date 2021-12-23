package com.jsikmc15.fcmapptest;


import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface TokenService {

    @FormUrlEncoded
    @POST("/token")
    Call<String> postToken(@Field("token")String token);

}
