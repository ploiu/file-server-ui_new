package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import kotlinx.serialization.json.Json
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

object CustomDataFlavors {
    val FOLDER_CHILD = DataFlavor(FolderChild::class.java, "application/x-ploiu-file-server-folder-child")
}

fun FolderChild.toJson() = Json.encodeToString(this)

fun FileApi.fromJson(str: String) = Json.decodeFromString<FileApi>(str)
fun FolderApi.fromJson(str: String) = Json.decodeFromString<FolderApi>(str)

class FolderChildSelection(private val data: FolderChild) : Transferable {
    private companion object {
        val flavors: Array<DataFlavor> = arrayOf(CustomDataFlavors.FOLDER_CHILD)
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?> = flavors.clone()

    override fun isDataFlavorSupported(p0: DataFlavor?) = p0 in flavors

    override fun getTransferData(p0: DataFlavor?): Any {
        if (p0 !in flavors) {
            throw UnsupportedOperationException("flavor $p0 is not a supported flavor for FolderChildSelection")
        }
        return data
    }

}
