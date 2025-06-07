package dev.ploiu.file_server_ui_new.client

import dev.ploiu.file_server_ui_new.model.Attribute
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FileRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface FileClient {
    @Streaming
    @GET("/files/preview/{id}")
    suspend fun getFilePreview(@Path("id") id: Long): Response<ResponseBody>

    @GET("/files/metadata")
    suspend fun search(
        @Query("search") search: String,
        @Query("tags") tags: Collection<String>,
        @Query("attributes") attributes: Collection<Attribute>
    ): Collection<FileApi>

    @Multipart
    @POST("/files")
    suspend fun createFile(
        @Part file: MultipartBody.Part,
        @Part extension: MultipartBody.Part,
        @Part folderId: MultipartBody.Part
    ): FileApi

    @GET("/files/metadata/{id}")
    suspend fun getMetadata(@Path("id") id: Long): FileApi?

    @Streaming
    @GET("/files/{id}")
    fun getFileContents(@Path("id") id: Long): ResponseBody

    @PUT("/files")
    suspend fun updateFile(@Body file: FileRequest): FileApi

    @DELETE("/files/{id}")
    suspend fun deleteFile(@Path("id") id: Long)
}
