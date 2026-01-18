package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.unwrap
import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.client.PreviewClient
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.vinceglb.filekit.PlatformFile
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.rules.TestName
import retrofit2.Response
import java.io.File
import kotlin.test.*

class PreviewServiceTest {
    @get:Rule
    val currentTestName = TestName()

    private lateinit var cacheDir: File

    private fun getTestName(): String {
        return currentTestName.methodName.replace("()", "")
    }

    @BeforeTest
    fun setUp() {
        mockkObject(AppSettings)

        val testName = getTestName()
        val rootDir = File("./testDirs/$testName")
        cacheDir = File(rootDir, "/cache")
        every { AppSettings.getCacheDir() } returns PlatformFile(cacheDir)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }

    @AfterTest
    fun tearDown() {
        val testName = getTestName()
        File("./testDirs/$testName").deleteRecursively()
    }

    @Test
    fun `getFolderPreview creates the folder cache directory if it doesn't exist`() = runTest {
        val previewClient: PreviewClient = mockk()
        val fileClient: FileClient = mockk()
        val previewService = DesktopPreviewService(fileClient, previewClient)
        previewService.getFolderPreview(
            FolderApi(
                1,
                0,
                "./test",
                "test",
                emptyList(),
                emptyList(),
                emptyList(),
            ),
        ).collect()
        assertTrue { File("./testDirs/${getTestName()}/cache/1").exists() }
    }

    @Test
    fun `when the folder is retrieved from folderService, any previews stored to the disk that aren't in the resulting FolderAPI get removed from the disk`() =
        runTest {
            val previewClient: PreviewClient = mockk()
            val fileClient: FileClient = mockk()
            val folder = FolderApi(1, 0, "./test", "test", emptyList(), emptyList(), emptyList())
            val dir = File("./testDirs/${getTestName()}/cache/${folder.id}")
            dir.mkdirs()
            // create a new file that should not exist at the end of this test
            val oldFile = File(dir, "1.png")
            oldFile.createNewFile()
            assertTrue { oldFile.exists() }
            val previewService = DesktopPreviewService(
                fileClient = fileClient,
                previewClient = previewClient,
            )
            previewService.getFolderPreview(folder).collect()
            assertFalse { oldFile.exists() }
        }

    @Test
    @Suppress("UnusedFlow") // we're asserting on a method call and don't need to consume it
    fun `getFolderPreview should not clean up previews the folder still has files for`() = runTest {
        val previewClient: PreviewClient = mockk()
        val fileClient: FileClient = mockk()
        // FolderApi contains file with id 2
        val folder = FolderApi(
            1, 0, "./test", "test", emptyList(),
            listOf(
                FileApi(2, 1, "file2", emptyList(), 0, "", "Image"),
            ),
            emptyList(),
        )
        val dir = File("./testDirs/${getTestName()}/cache/${folder.id}")
        dir.mkdirs()
        // create a file that should remain after the test
        val validFile = File(dir, "2.png")
        validFile.createNewFile()
        assertTrue { validFile.exists() }
        val previewService = DesktopPreviewService(
            fileClient = fileClient,
            previewClient = previewClient,
        )
        previewService.getFolderPreview(folder).collect()
        assertTrue { validFile.exists() }
        // since all the files on the disk are in the folder api, we shouldn't attempt to download them
        verify(exactly = 0) { previewClient.downloadFolderPreviews(any()) }
    }

    @Suppress("UnusedFlow")
    @Test
    fun `when the folder is retrieved from folderService, if fewer than 21 previews aren't cached on disk, download each preview individually in chunks of 5 and store them in the cache`() =
        runTest {
            val previewClient: PreviewClient = mockk()
            val fileClient: FileClient = mockk()
            // Create 10 files missing from disk
            val fileIds = (1L..10L).toList()
            val files = fileIds.map { FileApi(it, 1, "file$it", emptyList(), 0, "", "Image") }
            val folder = FolderApi(1, 0, "./test", "test", emptyList(), files, emptyList())

            // No files exist in cache dir
            val previewBytes = ByteArray(10) { 42 }
            coEvery { fileClient.getFilePreview(any()) } answers {
                Response.success(200, previewBytes.toResponseBody(null))
            }
            // Should not call getPreviewsForFolder
            every { previewClient.downloadFolderPreviews(any()) } returns flowOf()

            val previewService = DesktopPreviewService(
                fileClient = fileClient,
                previewClient = previewClient,
            )
            val result = previewService.getFolderPreview(folder).map { it.first }.toSet()

            // All previews should be downloaded and stored
            assertEquals(fileIds.toSet(), result)
            fileIds.forEach {
                val file = File("./testDirs/${getTestName()}/cache/1/$it.png")
                assertTrue(file.exists())
                assertContentEquals(previewBytes, file.readBytes())
            }
            // Should call downloadPreview for each file
            coVerify(exactly = fileIds.size) { fileClient.getFilePreview(any()) }
            // Should not call getPreviewsForFolder
            coVerify(exactly = 0) { previewClient.downloadFolderPreviews(any()) }
        }

    @Test
    fun `when attempting to download an individual preview, if the client returns null, that file should not be cached`() =
        runTest {
            val previewClient: PreviewClient = mockk()
            val fileClient: FileClient = mockk()
            // FolderApi contains one file with id 1
            val fileId = 1L
            val folder = FolderApi(
                1, 0, "./test", "test", emptyList(),
                listOf(
                    FileApi(fileId, 1, "file1", emptyList(), 0, "", "Image"),
                ),
                emptyList(),
            )
            // Simulate no preview available (404)
            coEvery { fileClient.getFilePreview(fileId) } returns Response.error(
                404,
                "".toResponseBody(null),
            )
            coEvery { previewClient.downloadFolderPreviews(any()) } returns flowOf()
            val previewService = DesktopPreviewService(
                fileClient = fileClient,
                previewClient = previewClient,
            )
            val result = previewService.getFolderPreview(folder).toList()
            // Should not cache the file
            val cachedFile = File("./testDirs/${getTestName()}/cache/1/$fileId.png")
            assertFalse(cachedFile.exists())
            assertTrue(result.isEmpty())
        }

    @Test
    fun `downloadPreview should return response body bytes if response is successful`() = runTest {
        val fileClient: FileClient = mockk()
        val previewClient: PreviewClient = mockk()
        val previewService = DesktopPreviewService(
            fileClient = fileClient,
            previewClient = previewClient,
        )
        val bytes = byteArrayOf(1, 2, 3)
        coEvery { fileClient.getFilePreview(any()) } returns Response.success(
            bytes.toResponseBody(null),
        )
        val result = previewService.downloadPreview(123L).unwrap()
        assertNotNull(result)
        assertContentEquals(bytes, result)
    }

    @Test
    fun `downloadPreview should return null if response code is 404`() = runTest {
        val fileClient: FileClient = mockk()
        val previewClient: PreviewClient = mockk()
        val previewService = DesktopPreviewService(
            fileClient = fileClient,
            previewClient = previewClient,
        )
        coEvery { fileClient.getFilePreview(any()) } returns Response.error(
            404,
            "".toResponseBody(null),
        )
        val result = previewService.downloadPreview(123L).unwrap()
        assertNull(result)
    }

}
