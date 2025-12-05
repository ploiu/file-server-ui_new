package dev.ploiu.file_server_ui_new.ffi.creds

import com.sun.jna.Memory
import com.sun.jna.Native
import java.nio.charset.StandardCharsets

object CredsLib {
    private val LIB: CredsFfi = Native.load("rust_credential_manager", CredsFfi::class.java)

    fun storeCredential(
        applicationName: String,
        username: String,
        password: String,
    ): SavePasswordResult {
        val applicationBytes = applicationName.nullTerminate()
        val usernameBytes = username.nullTerminate()
        val passwordBytes = password.nullTerminate()
        return Memory(applicationBytes.size.toLong()).use { applicationMem ->
            Memory(usernameBytes.size.toLong()).use { usernameMem ->
                Memory(passwordBytes.size.toLong()).use { passwordMem ->
                    applicationMem.write(0, applicationBytes, 0, applicationBytes.size)
                    usernameMem.write(0, usernameBytes, 0, usernameBytes.size)
                    passwordMem.write(0, passwordBytes, 0, passwordBytes.size)
                    SavePasswordResult.from(LIB.storeCredential(applicationMem, usernameMem, passwordMem))
                }
            }
        }
    }

    fun retrieveCredential(applicationName: String): RetrievePasswordResult {
        val applicationBytes = applicationName.nullTerminate()
        return Memory(applicationBytes.size.toLong()).use { mem ->
            mem.write(0, applicationBytes, 0, applicationBytes.size)
            val ref = CredsByReference()
            try {
                val res = parseRetrieveResult(LIB.retrieveCredential(mem, ref), ref)
                if (res is Success) {
                    // data is already moved into the jvm, but we still need to have the original pointers freed
                    releaseMemory(ref)
                }
                res
            } catch (e: Exception) {
                System.err.println("catastrophic error attempting to retrieve credential from keystore: ${e.message}")
                e.printStackTrace()
                GenericError()
            }
        }
    }

    private fun releaseMemory(credsPointer: CredsByReference) {
        LIB.freeCreds(credsPointer)
    }

    private fun String.nullTerminate(): ByteArray {
        // we're removing all null bytes from the string first, just in case
        val thisBytes = this.replace("\u0000", "").toByteArray(charset = StandardCharsets.UTF_8)
        val copiedBytes = ByteArray(thisBytes.size + 1)
        System.arraycopy(thisBytes, 0, copiedBytes, 0, thisBytes.size)
        copiedBytes[copiedBytes.size - 1] = 0
        return copiedBytes
    }
}

fun parseRetrieveResult(code: Byte, creds: CredsByReference): RetrievePasswordResult {
    creds.read()
    return when (code.toInt() and 0xFF) {
        // 0x00 means creds were retrieved, so username and password will never be null unless something goes horrifically wrong
        0x00 -> Success(creds.username, creds.password)
        0x03 -> OSError()
        0x04 -> NotFoundError()
        0x05 -> InvalidFormatError()
        else -> GenericError()
    }
}
