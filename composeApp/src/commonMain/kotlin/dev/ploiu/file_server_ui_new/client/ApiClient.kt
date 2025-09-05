package dev.ploiu.file_server_ui_new.client

import dev.ploiu.file_server_ui_new.model.DiskInfo
import dev.ploiu.file_server_ui_new.model.Metadata
import retrofit2.Response
import retrofit2.http.GET

interface ApiClient {
    @GET("/api/version")
    suspend fun getApiInfo(): Metadata

    @GET("/api/disk")
    suspend fun getStorageInfo(): Response<DiskInfo>
}
