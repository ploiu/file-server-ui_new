package dev.ploiu.file_server_ui_new

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

fun formatFileOrFolderName(name: String) =
    trimToSize(name.replace("leftParenthese", "(").replace("rightParenthese", ")"))
