package dev.ploiu.file_server_ui_new.ffi.creds

import dev.ploiu.file_server_ui_new.service.RetrieveCredsSuccess

sealed interface RetrievePasswordResult

class GenericError : RetrievePasswordResult
class InvalidFormatError : RetrievePasswordResult
class NotFoundError : RetrievePasswordResult
class OSError : RetrievePasswordResult
data class Success(val username: String, val password: String) : RetrievePasswordResult {
    fun into() = RetrieveCredsSuccess(username, password)
}
