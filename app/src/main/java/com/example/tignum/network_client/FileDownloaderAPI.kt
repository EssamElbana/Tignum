package com.example.tignum.network_client

import retrofit2.Call
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FileDownloaderAPI {
        @Streaming
        @GET
        fun downloadFile(
            @Url fileUrl: String,
            @HeaderMap headers:Map<String,String>
        ): Call<ResponseBody>
}