package dev.ploiu.file_server_ui_new

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform