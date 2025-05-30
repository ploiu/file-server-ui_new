package dev.ploiu.file_server_ui_new.model

data class Metadata(val version: String)

data class DiskInfo (val name: String, val totalSpace: Long, val freeSpace: Long)

data class ErrorMessage(val message: String)
