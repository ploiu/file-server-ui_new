package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.model.ErrorMessage
import dev.ploiu.file_server_ui_new.processResponse
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UtilsTests {
    @Test
    fun `processResponse should return the response body if the response is successful`() {
        val expectedBody = "Success"
        val response = Response.success(expectedBody)

        val actualBody = processResponse(response)

        assertEquals(expectedBody, actualBody)
    }

    @Test
    fun `processResponse should throw a RuntimeException with the errorBody's message value`() {
        val errorMessage = ErrorMessage("Test Error Message")
        val errorBody = Json.encodeToString(errorMessage).toResponseBody()
        val response: Response<String> = Response.error(400, errorBody)

        val exception = assertFailsWith<RuntimeException> {
            processResponse(response)
        }

        assertEquals(errorMessage.message, exception.message)
    }
}
