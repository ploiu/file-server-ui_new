package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.FolderClient
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.UpdateFolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.Response

class FolderServiceTests {

    private val folderClient = mockk<FolderClient>()
    private val folderService = FolderService(folderClient)

    @Test
    fun `deleteFolder should not allow deleting root folder`() {
        runBlocking {
            val result = folderService.deleteFolder(0)
            assertTrue(result.isErr)
            assertEquals("id (0) must be > 0", result.error)
        }
    }

    @Test
    fun `getFolder should return an Err if id is less than 0`() {
        runBlocking {
            val result = folderService.getFolder(-1)
            assertTrue(result.isErr)
            assertEquals("id (-1) must be >= 0", result.error)
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

        runBlocking {
            folderService.getFolder(id)
        }

        coVerify { folderClient.getFolder(id) }
    }

    @Test
    fun `downloadFolder should return an Err if id is less than 1`() {
        runBlocking {
            val result = folderService.downloadFolder(0)
            assertTrue(result.isErr)
            assertEquals("id (0) must be > 0", result.error)
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
    fun `getPreviewsForFolder should return an Err if passed id is less than 0`() {
        runBlocking {
            val result = folderService.getPreviewsForFolder(-1)
            assertTrue(result.isErr)
            assertEquals("id (-1) must be >= 0", result.error)
        }
    }

    @Test
    fun `getPreviewsForFolder should call client getPreviewsForFolder with passed id`() {
        val id = 123L
        val previewAsShorts = "preview".toCharArray().map { it.code.toShort() }.toTypedArray()
        val expectedPreviews = mapOf(1L to previewAsShorts)
        coEvery { folderClient.getPreviewsForFolder(id) } returns Response.success(expectedPreviews)

        runBlocking { folderService.getPreviewsForFolder(id) }

        coVerify { folderClient.getPreviewsForFolder(id) }
    }

    @Test
    fun `createFolder should return an Err if the parentId is less than 0`() {
        val createFolder = CreateFolder(name = "Test Folder", parentId = -1L, tags = emptyList())
        runBlocking {
            val result = folderService.createFolder(createFolder)
            assertTrue(result.isErr)
            assertEquals("parentId (-1) must be >= 0", result.error)
        }
    }

    @Test
    fun `createFolder should return a Err if the name is a blank String`() {
        val createFolder = CreateFolder(name = " ", parentId = 1L, tags = emptyList())
        runBlocking {
            val result = folderService.createFolder(createFolder)
            assertTrue(result.isErr)
            assertEquals("name cannot be blank", result.error)
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
    fun `updateFolder should return an Err if the passed id is less than 1`() {
        val updateFolder = UpdateFolder(id = 0, name = "Test Folder", parentId = 0L, tags = emptyList())
        runBlocking {
            val result = folderService.updateFolder(updateFolder)
            assertTrue(result.isErr)
            assertEquals("id (0) must be > 0", result.error)
        }
    }

    @Test
    fun `updateFolder should return a Err if the passed parentId is less than 0`() {
        val updateFolder = UpdateFolder(id = 1, name = "Test Folder", parentId = -1, tags = emptyList())
        runBlocking {
            val result = folderService.updateFolder(updateFolder)
            assertTrue(result.isErr)
            assertEquals("parentId (-1) must be >= 0", result.error)
        }
    }

    @Test
    fun `updateFolder should return a Err if the passed name is a blank String`() {
        val updateFolder = UpdateFolder(id = 1, name = " ", parentId = 1L, tags = emptyList())
        runBlocking {
            val result = folderService.updateFolder(updateFolder)
            assertTrue(result.isErr)
            assertEquals("name cannot be empty", result.error)
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
    fun `deleteFolder should return an Err if the passed id is less than 1`() {
        runBlocking {
            val result = folderService.deleteFolder(0)
            assertTrue(result.isErr)
            assertEquals("id (0) must be > 0", result.error)
        }
    }

    @Test
    fun `deleteFolder should call client deleteFolder with the passed id`() {
        val id = 123L
        coEvery { folderClient.deleteFolder(id) } returns Response.success(Unit)

        runBlocking { folderService.deleteFolder(id) }

        coVerify { folderClient.deleteFolder(id) }
    }
}
