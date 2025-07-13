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
    suspend fun downloadFolder(@Path("id") id: Long): Response<ResponseBody>

    /**
     * retrieves a mapping of file id -> preview bytes from the server.
     *
     * preview byte array come in as [Short]s, this is because the server is sending in u8 bytes, but kotlin only recognizes i8.
     * Short is used over Int because there will be little if any math on these values, and will be immediately converted to a [Byte].
     * Short has a smaller memory footprint than Int, which is valuable on mobile platforms
     *
     * TODO, look into maybe using [UByteArray] (though it's been experimental since 1.3...maybe not use it if it hasn't been stabilized in this long)
     */
    @GET("/folders/preview/{id}")
    suspend fun getPreviewsForFolder(@Path("id") id: Long): Response<Map<Long, Array<Short>>>

    @POST("/folders")
    suspend fun createFolder(@Body req: CreateFolder): Response<FolderApi>

    @PUT("/folders")
    suspend fun updateFolder(@Body req: UpdateFolder): Response<FolderApi>

    @DELETE("/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: Long): Response<Unit>
}
