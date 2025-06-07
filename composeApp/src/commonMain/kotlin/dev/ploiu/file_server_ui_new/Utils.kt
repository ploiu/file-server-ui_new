package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.model.ErrorMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import retrofit2.Response

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

fun formatFileOrFolderName(name: String) =
    trimToSize(name.replace("leftParenthese", "(").replace("rightParenthese", ")"))

/**
 * Processes a Retrofit [Response] and returns the body if the response is successful.
 *
 * @param response The Retrofit response to process.
 * @return The response body of type [T] if the response is successful.
 * @throws RuntimeException if the response is not successful and an error message is parsed.
 * @throws Exception if parsing the error message fails.
 */
fun <T> processResponse(response: Response<out T>): T {
    if (response.isSuccessful) {
        return response.body()!!
    } else {
        val errorMessage = parseErrorFromResponse(response)
        throw RuntimeException(errorMessage.message)
    }
}

fun parseErrorFromResponse(response: Response<out Any>): ErrorMessage {
    if (response.isSuccessful) {
        throw UnsupportedOperationException("Can only call parseErrorFromResponse if response isn't successful")
    }
    val errorBody = response.errorBody()?.string()
    val errorMessage = try {
        Json.decodeFromString<ErrorMessage>(errorBody ?: "")
    } catch (e: Exception) {
        log.error(e) { "Failed to parse error message" }
        throw e
    }
    return errorMessage
}
