package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.BadRequestException
import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.ErrorMessage
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertThrows
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertFailsWith

@MockKExtension.CheckUnnecessaryStub
class FolderServiceTests {

    private val folderClient = mockk<FolderClient>()
    private val folderService = FolderService(folderClient)

    @Test
    fun `deleteFolder should not allow deleting root folder`() {
        assertThrows(BadRequestException::class.java) {
            runBlocking { folderService.deleteFolder(0) }
        }
    }

    @Test
    fun `getFolder should throw an exception if id is less than 0`() {
        assertThrows<BadRequestException> {
            runBlocking { folderService.getFolder(-1) }
        }
    }

    @Test
    fun `getFolder should call client getFolder with the passed id`() {
        val id = 123L
        val expectedFolder = FolderApi(
            id = id,
            name = "Test Folder",
            parentId = 0L,
            path = "",
            folders = emptyList(),
            files = emptyList(),
            tags = emptyList()
        )
        coEvery { folderClient.getFolder(id) } returns Response.success(expectedFolder)

        runBlocking { folderService.getFolder(id) }

        coVerify { folderClient.getFolder(id) }
    }

    @Test
    fun `downloadFolder should throw an exception if id is less than 1`() {
        assertThrows<BadRequestException> {
            runBlocking { folderService.downloadFolder(0) }
        }
    }

    @Test
    fun `downloadFolder should call client downloadFolder with passed id`() {
        val id = 123L
        coEvery { folderClient.downloadFolder(id) } returns Response.success("".toResponseBody())

        runBlocking { folderService.downloadFolder(id) }

        coVerify { folderClient.downloadFolder(id) }
    }

    @Test
    fun `getPreviewsForFolder should throw an exception if passed id is less than 0`() {
        assertThrows<BadRequestException> {
            runBlocking { folderService.getPreviewsForFolder(-1) }
        }
    }

    @Test
    fun `getPreviewsForFolder should call client getPreviewsForFolder with passed id`() {
        val id = 123L
        val expectedPreviews = mapOf(1L to "preview".encodeToByteArray())
        coEvery { folderClient.getPreviewsForFolder(id) } returns Response.success(expectedPreviews)

        runBlocking { folderService.getPreviewsForFolder(id) }

        coVerify { folderClient.getPreviewsForFolder(id) }
    }

    @Test
    fun `createFolder should throw an exception if the parentId is less than 0`() {
        val createFolder = CreateFolder(name = "Test Folder", parentId = -1L, tags = emptyList())
        assertThrows<BadRequestException> {
            runBlocking { folderService.createFolder(createFolder) }
        }
    }

    @Test
    fun `createFolder should throw a BadRequestException if the name is a blank String`() {
        val createFolder = CreateFolder(name = " ", parentId = 1L, tags = emptyList())
        assertThrows<BadRequestException> {
            runBlocking { folderService.createFolder(createFolder) }
        }
    }

    @Test
    fun `createFolder should call client createFolder with the passed request`() {
        val createFolder = CreateFolder(name = "Test Folder", parentId = 0L, tags = emptyList())
        val expectedFolder = FolderApi(
            id = 1L,
            name = "Test Folder",
            parentId = 0L,
            path = "",
            folders = emptyList(),
            files = emptyList(),
            tags = emptyList()
        )
        coEvery { folderClient.createFolder(createFolder) } returns Response.success(expectedFolder)

        runBlocking { folderService.createFolder(createFolder) }

        coVerify { folderClient.createFolder(createFolder) }
    }

    @Test
    fun `updateFolder should throw an exception if the passed id is less than 1`() {
        val updateFolder = UpdateFolder(id = 0, name = "Test Folder", parentId = 0L, tags = emptyList())
        assertThrows<BadRequestException> {
            runBlocking { folderService.updateFolder(updateFolder) }
        }
    }

    @Test
    fun `updateFolder should throw a BadRequestException if the passed parentId is less than 0`() {
        val updateFolder = UpdateFolder(id = 1, name = "Test Folder", parentId = -1, tags = emptyList())
        assertThrows<BadRequestException> {
            runBlocking { folderService.updateFolder(updateFolder) }
        }
    }

    @Test
    fun `updateFolder should throw a BadRequestException if the passed name is a blank String`() {
        val updateFolder = UpdateFolder(id = 1, name = " ", parentId = 1L, tags = emptyList())
        assertThrows<BadRequestException> {
            runBlocking { folderService.updateFolder(updateFolder) }
        }
    }

    @Test
    fun `updateFolder should call client updateFolder with the passed request`() {
        val updateFolder = UpdateFolder(id = 1, name = "Test Folder", parentId = 1L, tags = emptyList())
        val expectedFolder = FolderApi(
            id = 1L,
            name = "Test Folder",
            parentId = 0L,
            path = "",
            folders = emptyList(),
            files = emptyList(),
            tags = emptyList()
        )
        coEvery { folderClient.updateFolder(updateFolder) } returns Response.success(expectedFolder)

        runBlocking { folderService.updateFolder(updateFolder) }

        coVerify { folderClient.updateFolder(updateFolder) }
    }

    @Test
    fun `deleteFolder should throw an exception if the passed id is less than 1`() {
        assertThrows<BadRequestException> {
            runBlocking { folderService.deleteFolder(0) }
        }
    }

    @Test
    fun `deleteFolder should call client deleteFolder with the passed id`() {
        val id = 123L
        coEvery { folderClient.deleteFolder(id) } returns Response.success(Unit)

        runBlocking { folderService.deleteFolder(id) }

        coVerify { folderClient.deleteFolder(id) }
    }

    @Test
    fun `processResponse should return the response body if the response is successful`() {
        val expectedBody = "Success"
        val response = Response.success(expectedBody)

        val actualBody = folderService.processResponse(response)

        assertEquals(expectedBody, actualBody)
    }

    @Test
    fun `processResponse should throw a RuntimeException with the errorBody's message value`() {
        val errorMessage = ErrorMessage("Test Error Message")
        val errorBody = Json.encodeToString(errorMessage).toResponseBody()
        val response: Response<String> = Response.error(400, errorBody)

        val exception = assertFailsWith<RuntimeException> {
            folderService.processResponse(response)
        }

        assertEquals(errorMessage.message, exception.message)
    }
}
