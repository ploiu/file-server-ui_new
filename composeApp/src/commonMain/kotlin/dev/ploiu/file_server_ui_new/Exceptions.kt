package dev.ploiu.file_server_ui_new

class BadRequestException(message: String): RuntimeException(message)

class ApiException(message: String) : RuntimeException(message)
