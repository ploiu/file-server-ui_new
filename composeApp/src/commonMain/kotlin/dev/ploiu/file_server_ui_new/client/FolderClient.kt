package dev.ploiu.file_server_ui_new.client

import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FolderClient {

    @GET("/folders/metadata/{id}")
    suspend fun getFolder(@Path("id") id: Long): Response<FolderApi>

    @Streaming
    @GET("/folders/{id}")
    fun downloadFolder(@Path("id") id: Long): Response<ResponseBody>

    @GET("/folders/preview/{id}")
    suspend fun getPreviewsForFolder(@Path("id") id: Long): Response<Map<Long, ByteArray>>

    @POST("/folders")
    suspend fun createFolder(@Body req: CreateFolder): Response<FolderApi>

    @PUT("/folders")
    suspend fun updateFolder(@Body req: UpdateFolder): Response<FolderApi>

    @DELETE("/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: Long): Response<Unit>
}
