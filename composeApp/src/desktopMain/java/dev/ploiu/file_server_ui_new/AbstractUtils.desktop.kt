package dev.ploiu.file_server_ui_new

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.div

actual fun saveFile(
    res: InputStream,
    saveLocation: io.github.vinceglb.filekit.PlatformFile,
    archiveName: String
) {
    Files.copy(res, saveLocation.file.toPath() / archiveName, StandardCopyOption.REPLACE_EXISTING)
}
