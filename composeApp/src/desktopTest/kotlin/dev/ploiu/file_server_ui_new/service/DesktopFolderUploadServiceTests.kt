package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import dev.ploiu.file_server_ui_new.model.*
import io.github.vinceglb.filekit.PlatformFile
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.test.*

@MockKExtension.CheckUnnecessaryStub
@MockKExtension.ConfirmVerification
class DesktopFolderUploadServiceTests {

    val fileService = mockk<FileService>()
    val folderService = mockk<FolderService>()

    val service = DesktopFolderUploadService(folderService, fileService)

    @BeforeTest
    fun setup() {
        mockkObject(FolderApproximator)
    }

    @Test
    fun `uploadFolder works for an empty folder`() = runTest {
        val tempFile = createTempDirectory("whatever").toFile()
        tempFile.deleteOnExit()
        val file = mockk<PlatformFile> {
            every { file } returns tempFile
        }
        every { FolderApproximator.convertDir(any(), any()) } returns FolderApproximation(
            tempFile,
            childFiles = listOf(),
            childFolders = listOf(),
        )
        coEvery { folderService.getFolder(any()) } returns Ok(FolderApi(1L, 0L, "", "", listOf(), listOf(), listOf()))
        coEvery { folderService.createFolder(any()) } returns Ok(
            FolderApi(
                0L,
                null,
                "",
                "",
                listOf(),
                listOf(),
                listOf(),
            ),
        )
        val res = service.uploadFolder(file, 0L).toList()
        // should be no elements, since each emit is for an uploaded child file / folder
        assertTrue { res.isEmpty() }
        coVerify { folderService.createFolder(CreateFolder(name = tempFile.name, parentId = 1L, tags = listOf())) }
    }

    @Test
    fun `uploadFolder returns err of folder already exists`() = runTest {
        val tempFile = createTempDirectory("whatever").toFile()
        tempFile.deleteOnExit()
        val file = mockk<PlatformFile> {
            every { file } returns tempFile
        }
        val parentFolder = FolderApi(
            id = 0L,
            parentId = null,
            path = "",
            name = "",
            folders = listOf(
                FolderApi(
                    id = 1L,
                    parentId = 0L,
                    path = "",
                    name = tempFile.name,
                    folders = listOf(),
                    files = listOf(),
                    tags = listOf(),
                ),
            ),
            files = listOf(),
            tags = listOf(),
        )
        coEvery { folderService.getFolder(0L) } returns Ok(parentFolder)
        val res = service.uploadFolder(file, 0L).first()
        assertFalse { res.isSuccess }
        assertEquals("A folder with the name ${tempFile.name} already exists in this folder", res.errorMessage)
        coVerifyCount {
            0 * { folderService.createFolder(any()) }
            0 * { fileService.createFile(any()) }
            0 * { FolderApproximator.convertDir(any(), any()) }
        }
    }

    @Test
    fun `uploadFolder works for a flat folder (only files)`() = runTest {
        val tempFile = createTempDirectory("whatever").toFile()
        val childFiles = (1..5).map { createTempFile(directory = tempFile.toPath(), prefix = it.toString()).toFile() }
        tempFile.deleteOnExit()
        val file = mockk<PlatformFile> {
            every { file } returns tempFile
        }
        coEvery { folderService.getFolder(any()) } returns Ok(FolderApi(0L, null, "", "", listOf(), listOf(), listOf()))
        coEvery { folderService.createFolder(any()) } returns Ok(
            FolderApi(
                0L,
                null,
                "",
                "",
                listOf(),
                listOf(),
                listOf(),
            ),
        )
        every { FolderApproximator.convertDir(any(), any()) } returns FolderApproximation(
            tempFile,
            childFiles = childFiles,
            childFolders = listOf(),
        )
        coEvery { fileService.createFile(any()) } coAnswers {
            val f = firstArg<CreateFileRequest>()
            Ok(
                FileApi(
                    id = 0L,
                    folderId = 0L,
                    name = f.file.name,
                    tags = listOf(),
                    size = 0L,
                    dateCreated = "",
                    fileType = "",
                ),
            )
        }
        val expected = childFiles.map { it.name }.toSet()
        val actual = service.uploadFolder(file, 0L).map { it as BatchUploadFileResult }.map { it.file!!.name }.toSet()
        assertEquals(expected, actual)
        coVerifyCount {
            1 * { folderService.createFolder(any()) }
            5 * { fileService.createFile(any()) }
        }
    }

    @Test
    fun `uploadFolder works for a folder with empty folders`() = runTest {
        val tempFile = createTempDirectory("whatever").toFile()
        val childFolders = (1..5).map {
            FolderApproximation(
                self = createTempDirectory(
                    directory = tempFile.toPath(),
                    prefix = it.toString(),
                ).toFile(),
                childFiles = listOf(), childFolders = listOf(),
            )
        }
        tempFile.deleteOnExit()
        val file = mockk<PlatformFile> {
            every { file } returns tempFile
        }
        coEvery { folderService.getFolder(any()) } returns Ok(
            FolderApi(
                0L,
                null,
                "",
                "",
                listOf(),
                listOf(),
                listOf(),
            ),
        )
        coEvery { folderService.createFolder(any()) } coAnswers {
            val f = firstArg<CreateFolder>()
            Ok(
                FolderApi(
                    0L,
                    null,
                    "",
                    f.name,
                    listOf(),
                    listOf(),
                    listOf(),
                ),
            )
        }
        every { FolderApproximator.convertDir(any(), any()) } coAnswers {
            val f = firstArg<File>()
            if (f == tempFile) {
                FolderApproximation(
                    tempFile,
                    childFolders = childFolders,
                    childFiles = listOf(),
                )
            } else {
                FolderApproximation(
                    f,
                    listOf(),
                    listOf()
                )
            }
        }
        service.uploadFolder(file, 0L).toSet()
        coVerifyCount {
            // 1 for root + 5 for extras
            6 * { folderService.createFolder(any()) }
        }
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
