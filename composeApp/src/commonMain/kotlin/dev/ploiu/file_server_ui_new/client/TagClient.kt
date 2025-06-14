package dev.ploiu.file_server_ui_new.client

import retrofit2.http.*

interface TagClient {

    @GET("/tags/{id}")
    suspend fun getTag(@Path("id") id: Int): Tag

    @PUT("/tags")
    suspend fun updateTag(@Body tag: Tag): Tag

    @DELETE("/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Int)

    @POST("/tags")
    suspend fun createTag(@Body tag: Tag): Tag
}
