package com.example.tignum.network_client

import com.example.tignum.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    fun buildRetrofitObject() : Retrofit {
        //--- OkHttp client ---//
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)

        //--- Add authentication headers ---//
        okHttpClient.addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .addHeader("Connection", "keep-alive")
                .header("User-Agent", "downloader")

            val request = requestBuilder.build()
            chain.proceed(request)
        }

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC
            okHttpClient.addInterceptor(logging)
        }

        return Retrofit.Builder()
            .client(okHttpClient.build())
            .baseUrl("https://www.google.com/")
            .build()
    }
}