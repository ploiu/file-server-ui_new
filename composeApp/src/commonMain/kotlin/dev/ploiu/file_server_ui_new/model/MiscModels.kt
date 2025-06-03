package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable

data class Metadata(val version: String)

data class DiskInfo (val name: String, val totalSpace: Long, val freeSpace: Long)

@Serializable
data class ErrorMessage(val message: String)
