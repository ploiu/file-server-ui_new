package dev.ploiu.file_server_ui_new.ffi.creds

/**
 * Indicates the result of attempting to save a password.
 */
enum class SavePasswordResult {
    /**
     * Password successfully saved
     */
    SUCCESS,

    /**
     * An underlying error with the OS occurred. Check console logs.
     */
    OS_ERROR,

    /**
     * application name, username, or password was too long for the OS to handle. Check console logs.
     */
    LENGTH_ERROR,

    /**
     * catch-all error. Check console logs.
     *
     * Note: this library attempts to eliminate the NullPointerError Utf8Error responses from the ffi library.
     * UTF-8 is handled internally to the library, and passing `null` to [CredsLib.storeCredential]
     * will throw a NullPointerException
     */
    GENERIC_ERROR;

    internal companion object {
        fun from(value: Byte) = when (value.toInt() and 0xFF) {
            0x00 -> SUCCESS
            0x03 -> OS_ERROR
            0x04 -> LENGTH_ERROR
            else -> GENERIC_ERROR
        }
    }
}
