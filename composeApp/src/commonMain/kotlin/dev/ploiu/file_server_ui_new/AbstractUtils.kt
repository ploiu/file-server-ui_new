package dev.ploiu.file_server_ui_new

import io.github.vinceglb.filekit.PlatformFile
import java.io.InputStream

expect fun saveFile(res: InputStream, saveLocation: PlatformFile, fileName: String)
