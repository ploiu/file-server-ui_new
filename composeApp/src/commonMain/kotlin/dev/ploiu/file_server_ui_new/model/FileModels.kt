package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FileApi(
    val id: Long,
    val folderId: Long = 0,
    val name: String,
    val tags: Collection<Tag>,
    val size: Long,
    val dateCreated: String,
    val fileType: String,
) {
    val extension: String
        get() {
            // fun fact, files can end with . on linux!
            return if (name.endsWith('.') || !name.contains('.')) {
                ""
            } else {
                name.substring(name.lastIndexOf('.') + 1)
            }
        }
    val nameWithoutExtension
        get() = if (!name.contains('.')) {
            name
        } else {
            name.substring(0, name.lastIndexOf('.'))
        }
}

@Serializable
data class FileRequest(
    val id: Long,
    val folderId: Long,
    val name: String,
    val tags: Collection<Tag>,
)

data class CreateFileRequest(
    val folderId: Long,
    val file: File,
    val force: Boolean,
)
