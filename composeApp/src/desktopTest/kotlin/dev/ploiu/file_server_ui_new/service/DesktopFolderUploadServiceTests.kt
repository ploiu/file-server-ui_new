package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderApproximator
import io.github.vinceglb.filekit.PlatformFile
import io.mockk.coEvery
import io.mockk.coVerifyCount
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.*

@MockKExtension.CheckUnnecessaryStub
@MockKExtension.ConfirmVerification
class DesktopFolderUploadServiceTests {

    val fileService = mockk<FileService>()
    val folderService = mockk<FolderService>()
    val approximator = mockkObject(FolderApproximator)

    val service = DesktopFolderUploadService(folderService, fileService)

    @BeforeTest
    fun setup() {

    }

    @Test
    fun `uploadFolder works for an empty folder`() = runTest {
        val tempFile = createTempDirectory("whatever").toFile()
        tempFile.deleteOnExit()
        val file = mockk<PlatformFile> {
            every { file } returns tempFile
        }
        val resultFolder = FolderApi(
            id = 2L,
            parentId = 1L,
            path = "",
            name = "",
            folders = listOf(),
            files = listOf(),
            tags = listOf(),
        )
        coEvery { folderService.getFolder(any()) } returns Ok(FolderApi(1L, 0L, "", "", listOf(), listOf(), listOf()))
        coEvery { folderService.createFolder(any()) } returns Ok(resultFolder)
        val res = service.uploadFolder(file, 0L).first()
        assertTrue { res.isSuccess }
        assertTrue { res is BatchFolderUploadResult }
        assertEquals((res as BatchFolderUploadResult).folder, resultFolder)
    }

    @Test
    fun `uploadFolder returns err of folder already exists`() = runTest {
        fail()
    }

    @Test
    fun `uploadFolder works for a flat folder (only files)`() = runTest {
        fail()
    }

    @Test
    fun `uploadFolder works for a folder with empty folders`() = runTest {
        fail()
    }

    @Test
    fun `uploadFolder works for nested folder structure`() = runTest {
        fail()
    }

    @Test
    fun `uploadFolder should error without service calls if getting the parent folder fails`() = runTest {
        coEvery { folderService.getFolder(any()) } returns Err("bad folder")
        val folder = mockk<PlatformFile> {
            every { file } returns File("")
        }
        val res = service.uploadFolder(folder = folder, parentFolderId = -1L).first()
        assertFalse { res.isSuccess }
        assertEquals("bad folder", res.errorMessage)
        coVerifyCount {
            0 * { folderService.createFolder(any()) }
            0 * { fileService.createFile(any()) }
        }
    }

}
