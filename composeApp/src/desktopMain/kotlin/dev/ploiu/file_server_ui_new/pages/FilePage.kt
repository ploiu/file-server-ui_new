package dev.ploiu.file_server_ui_new.pages

import dev.ploiu.file_server_ui_new.model.FileApi
import kotlinx.serialization.Serializable

@Serializable
data class FileRoute(val id: Long)

sealed interface FileViewState
private class LoadingFileView : FileViewState
private class LoadedFileView : FileViewState
private data class ErroredFileView(val message: String) : FileViewState

data class FileState(val pageState: FileViewState, val file: FileApi?, val preview: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileState

        if (pageState != other.pageState) return false
        if (file != other.file) return false
        if (!preview.contentEquals(other.preview)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageState.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        result = 31 * result + preview.contentHashCode()
        return result
    }
}
