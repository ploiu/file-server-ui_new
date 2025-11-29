package dev.ploiu.file_server_ui_new.model

import kotlinx.serialization.Serializable

@Serializable
data class TagApi(val id: Long?, val title: String)

@Serializable
data class TaggedItemApi(val id: Long?, val title: String, val implicitFrom: Long?)
