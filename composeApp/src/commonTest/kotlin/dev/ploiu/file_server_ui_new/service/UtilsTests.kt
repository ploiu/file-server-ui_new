package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import dev.ploiu.file_server_ui_new.model.ErrorMessage
import dev.ploiu.file_server_ui_new.processResponse
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import retrofit2.Response
import kotlin.test.Test

class UtilsTests {

    @Test
    fun `processResponse should return the response body if the response is successful`() {
        val expectedBody = "Success"
        val response = Response.success(expectedBody)

        val result = processResponse(response)

        assertEquals(expectedBody, result.unwrap())
    }

    @Test
    fun `processResponse should return a failure Result with the errorBody's message value`() {
        val errorMessage = ErrorMessage("Test Error Message")
        val errorBody = Json.encodeToString(errorMessage).toResponseBody()
        val response: Response<String> = Response.error(400, errorBody)

        val (actualCode, actualMessage) = processResponse(response).unwrapError()

        assertEquals(errorMessage.message, actualMessage)
        assertEquals(400, actualCode)
    }
}
