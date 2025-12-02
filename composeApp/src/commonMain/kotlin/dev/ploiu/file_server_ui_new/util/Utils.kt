package dev.ploiu.file_server_ui_new.util

import io.github.oshai.kotlinlogging.KotlinLogging

val log = KotlinLogging.logger("Utils")

fun trimToSize(name: String, maxLength: Int = 25): String {
    if (name.length <= maxLength) {
        return name
    }
    val extensionIndex = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
    val extension = name.substring(extensionIndex)
    val extensionLength = extension.length
    val nameWithoutExtension = name.substring(0, extensionIndex)
    // use ... to show that there's more to the title
    val mainNameLength = maxLength - (extensionLength + 3)
    // having ... right in front of the extension doesn't look good, so put it in the middle of the word
    val halfName = nameWithoutExtension.substring(0, mainNameLength / 2)
    val otherHalfName = nameWithoutExtension.takeLast(mainNameLength / 2)
    return "$halfName...$otherHalfName$extension"
}

/**
 * formats a file or folder name for display on the ui.
 *
 * It trims the name down to an acceptable size while also reversing any name changes done to the file on upload (such as replacing "leftParenthese" and "rightParenthese" with ( and ) respectively)
 */
fun formatFileOrFolderName(name: String) =
    trimToSize(name.replace("leftParenthese", "(").replace("rightParenthese", ")"))

/**
 * Converts a raw byte value to a human-readable format.
 *
 * For example:
 * ```kt
 * 1024.toShortHandBytes // "1KiB"
 * 1536.toShortHandBytes // "1.5KiB"
 * ```
 * @param bytes the byte value to format
 * @returns a string representation with an appropriate unit (Bytes, KiB, MiB, GiB, or TiB)
 */
fun Long.toShortHandBytes(): String {
    if (this < 0) {
        return "Invalid Byte Value"
    }
    if (this < 1024) {
        return "$this Bytes"
    }
    val units = listOf("KiB", "MiB", "GiB", "TiB")
    var value = this.toFloat()
    var unitIndex = -1

    // using a loop like this saves a lot of if/else branches and repeated logic
    while (value >= 1024 && unitIndex < units.size) {
        value /= 1024
        unitIndex++
    }

    // if the decimal part is less than .5, we can round down
    val wholeValue = value.toInt()
    return if (value - .5 < wholeValue) {
        "$wholeValue ${units[unitIndex]}"
    } else {
        "%.1f ${units[unitIndex]}".format(value)
    }
}

fun Long.getFileSizeAlias(): String {
    val kib = 1024
    val mib = kib * 1024
    val gib = mib * 1024

    return if (this < 500 * kib) {
        "Tiny"
    } else if (this < 10 * mib) {
        "Small"
    } else if (this < 100 * mib) {
        "Medium"
    } else if (this < gib) {
        "Large"
    } else {
        "ExtraLarge"
    }
}
