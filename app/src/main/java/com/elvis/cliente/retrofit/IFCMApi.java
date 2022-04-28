package com.elvis.cliente.retrofit;

import com.elvis.cliente.models.FCMBody;
import com.elvis.cliente.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAbzxuGQ8:APA91bGZdYiy-atIH0DrcsCbzL3bBuMiA_LZ-Bdp5At8-uV9wAStByKL75ivYOV4oX-89vJ9_08CiqOGo6Q5HanOZerQTGz3egFlUnemmIsyHBiQZTijrhFBbGVQiUSxxBaPAaXIIxje"
    })
    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);

}
