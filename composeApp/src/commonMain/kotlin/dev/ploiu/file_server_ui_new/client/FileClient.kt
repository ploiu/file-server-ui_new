package dev.ploiu.file_server_ui_new.client

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface FileClient {
    @Streaming
    @GET("/files/preview/{id}")
    suspend fun getFilePreview(@Path("id") id: Long): ResponseBody

    @GET("/files/metadata")
    suspend fun search(@Query("search") search: String, @Query("tags") tags: Collection<String>, @Query("attributes") attributes: Collection<String> /* TODO attribute class */)
}
