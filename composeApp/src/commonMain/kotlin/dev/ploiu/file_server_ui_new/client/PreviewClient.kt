package dev.ploiu.file_server_ui_new.client

import dev.ploiu.file_server_ui_new.config.ServerConfig
import dev.ploiu.file_server_ui_new.model.FilePreview
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.io.encoding.Base64

class PreviewClient(private val client: OkHttpClient, private val serverConfig: ServerConfig) {
    private val log = KotlinLogging.logger { }

    /**
     * Downloads previews for all files in the given folder
     * This uses the new /previews/folder/{folderId} endpoint that streams previews
     * using server-sent events (SSE)
     *
     * @param folderId the ID of the folder to download previews for
     * @return a Flow that emits FilePreview objects as they are received from the server
     */
    fun downloadFolderPreviews(folderId: Long): Flow<FilePreview> = flow {
        val url = serverConfig.baseUrl.trimEnd('/') + "/previews/folder/$folderId"
        val request = Request.Builder().url(url).addHeader("accept", "text/event-stream").build()
        val call = client.newCall(request)
        val res = try {
            call.execute()
        } catch (e: Exception) {
            log.error { "Failed to retrieve folder previews! $e" }
            return@flow
        }
        if (!res.isSuccessful) {
            log.error { "Retrieving folder previews returned ${res.code}" }
            return@flow
        }
        // event is the file id, data is the base64 encoded preview
        res.body.charStream().useLines {
            var eventId = 0L
            var contents = byteArrayOf()
            for (line in it) {
                when {
                    line.startsWith("id:") -> {
                        eventId = line.removePrefix("id:").trim().toLong()
                    }

                    line.startsWith("data:") -> {
                        contents = Base64.decode(line.removePrefix("data:").trim())
                    }
                    // empty line means end of event and we need to emit
                    line.isBlank() -> {
                        emit(eventId to contents)
                    }
                }
            }
        }
    }
}
