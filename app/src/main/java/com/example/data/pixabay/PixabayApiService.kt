package com.example.data.pixabay

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PixabayApiService {
    @GET("api/videos/")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("video_type") videoType: String = "all", // "all", "film", "animation"
        @Query("category") category: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("safesearch") safeSearch: Boolean = true,
        @Query("order") order: String = "popular" // "popular", "latest"
    ): Response<PixabayVideoResponse>

    @GET("api/")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("category") category: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("safesearch") safeSearch: Boolean = true,
        @Query("orientation") orientation: String = "vertical"
    ): Response<PixabayImageResponse>
}
