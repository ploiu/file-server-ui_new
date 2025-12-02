package dev.ploiu.file_server_ui_new.util

import io.github.vinceglb.filekit.PlatformFile
import java.io.InputStream

/**
 * Handles writing a file to the disk via an InputStream
 */
expect fun saveFile(res: InputStream, saveLocation: PlatformFile, fileName: String)
