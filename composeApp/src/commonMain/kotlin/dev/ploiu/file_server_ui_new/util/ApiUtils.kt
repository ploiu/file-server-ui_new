package dev.ploiu.file_server_ui_new.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.model.ErrorMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import retrofit2.Response

private val log = KotlinLogging.logger("ApiUtils")

/**
 * Meant to give both the human-readable message from the server and the actual status code a response returned,
 * for when the server returns a non-2xx status code
 *
 * @see [ErrorMessage] for the raw format the server returns response bodies as
 */
data class ApiError(val statusCode: Int, val message: String)

/**
 * Processes a Retrofit [Response] and returns the body if the response is successful.
 *
 * @param response The Retrofit response to process.
 * @return The response body of type [T] if the response is successful.
 * @throws RuntimeException if the response is not successful and an error message is parsed.
 * @throws Exception if parsing the error message fails.
 */
fun <T> processResponse(response: Response<out T>): Result<T, ApiError> {
    return if (response.isSuccessful) {
        Ok(response.body()!!)
    } else {
        val errorMessage = parseErrorFromResponse(response)
        Err(ApiError(response.code(), errorMessage.message))
    }
}

fun processResponseUnit(response: Response<Unit>): Result<Unit, ApiError> {
    return if (response.isSuccessful) {
        Ok(Unit)
    } else {
        val errorMessage = parseErrorFromResponse(response)
        Err(ApiError(response.code(), errorMessage.message))
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
