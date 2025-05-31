package dev.ploiu.file_server_ui_new.client

import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

interface FolderClient {

    @GET("/folders/metadata/{id}")
    suspend fun getFolder(@Path("id") id: Long): FolderApi

    @Streaming
    @GET("/folders/{id}")
    fun downloadFolder(@Path("id") id: Long): ResponseBody

    @GET("/folders/preview/{id}")
    suspend fun getPreviewsForFolder(@Path("id") id: Long): Map<Long, ByteArray>

    @POST("/folders")
    suspend fun createFolder(@Body req: CreateFolder): FolderApi

    @PUT("/folders")
    suspend fun updateFolder(@Body req: UpdateFolder): FolderApi

    @DELETE("/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: Long)
}
