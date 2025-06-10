package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.FileClient
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File
import kotlin.test.*

class PreviewServiceTest {
    @get:Rule
    val currentTestName = TestName()

    private lateinit var directoryService: DirectoryService

    private lateinit var previewService: PreviewService
    private lateinit var cacheDir: File

    private fun getTestName(): String {
        return currentTestName.methodName.replace("()", "")
    }

    @BeforeTest
    fun setUp() {
        directoryService = mockk()

        val testName = getTestName()
        val rootDir = File("./testDirs/$testName")
        every { directoryService.getRootDirectory() } returns rootDir
        cacheDir = File(rootDir, "/cache")
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
        val folderService: FolderService = mockk()
        val fileClient: FileClient = mockk()
        val previewService = PreviewService(folderService, fileClient, directoryService)
        previewService.getFolderPreview(FolderApi(1, 0, "./test", "test", emptyList(), emptyList(), emptyList()))
        assertTrue { File("./testDirs/${getTestName()}/cache/1").exists() }
    }

    @Test
    fun `when the folder is retrieved from folderService, any previews stored to the disk that aren't in the resulting FolderAPI get removed from the disk`() {
        fail()
    }

    @Test
    fun `when the folder is retrieved from folderService, if fewer than 21 previews aren't cached on disk, download each preview individually in chunks of 5 and store them in the cache`() {
        fail()
    }

    @Test
    fun `when more than 20 previews aren't cached to the disk, it should call the endpoint to folderService to download all cached previews and store them in the disk`() {
        fail()
    }

    @Test
    fun `when more than 20 previews aren't cached to the disk, the pre-existing cache dir should be deleted and re-built`() {
        fail()
    }
}
