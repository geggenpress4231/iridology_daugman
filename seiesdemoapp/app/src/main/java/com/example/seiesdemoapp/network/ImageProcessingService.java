package com.example.seiesdemoapp.network;

import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;

public interface ImageProcessingService {

    @Multipart
    @POST("/api/process")
    Call<ResponseBody> processImage(@Part MultipartBody.Part file);
}
