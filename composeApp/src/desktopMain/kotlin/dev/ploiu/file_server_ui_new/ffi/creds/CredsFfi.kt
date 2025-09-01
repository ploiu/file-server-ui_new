package dev.ploiu.file_server_ui_new.ffi.creds

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.Structure


/**
 * [documentation](https://ploiu.github.io/creds-ffi/rust_credential_manager/index.html)
 */
internal interface CredsFfi : Library {
    fun storeCredential(applicationName: Pointer, username: Pointer, password: Pointer): Byte
    fun retrieveCredential(applicationName: Pointer, outCreds: CredsByReference): Byte
    fun freeCreds(pointer: CredsByReference): Byte
}

@Structure.FieldOrder(value = ["username", "password"])
sealed class CredsInternal : Structure() {
    internal lateinit var username: String
    internal lateinit var password: String
}

class CredsByReference : CredsInternal(), Structure.ByReference
