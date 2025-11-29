package com.example.smartnovelreader.network

import com.example.smartnovelreader.model.SimpleSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SimpleApiService {
    @GET("api/novels")
    suspend fun searchNovels(
        @Query("q") query: String?
    ): Response<SimpleSearchResponse>

    companion object {
        const val BASE_URL = "https://novel-downloader-production.up.railway.app/"
    }
}