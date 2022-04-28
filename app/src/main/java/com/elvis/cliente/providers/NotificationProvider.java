package com.elvis.cliente.providers;

import com.elvis.cliente.models.FCMBody;
import com.elvis.cliente.models.FCMResponse;
import com.elvis.cliente.retrofit.IFCMApi;
import com.elvis.cliente.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {

    private String url = "https://fcm.googleapis.com";

    public NotificationProvider() {
    }
    
    public Call<FCMResponse> sendNotification(FCMBody body) {
        return RetrofitClient.getClientObject(url).create(IFCMApi.class).send(body);
    }
}
