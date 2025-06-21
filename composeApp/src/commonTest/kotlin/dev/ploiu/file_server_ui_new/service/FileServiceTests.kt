package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.client.FileClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class FileServiceTests {
    private val fileClient = mockk<FileClient>()
    private val fileService = FileService(fileClient)

    @Test
    fun `splitFileName splits regular file`() {
        val input = "test.jpg"
        val expected = SplitName("test", "jpg")
        assertEquals(expected, fileService.splitFileName(input))
    }

    @Test
    fun `splitFileName splits file with multiple dots in name`() {
        // created from default android camera app sometimes
        val input = "test.MP.jpg"
        val expected = SplitName("test", "MP.jpg")
        assertEquals(expected, fileService.splitFileName(input))
    }

    @Test
    fun `splitFileName doesn't split file names without extensions`() {
        val input = "LICENSE"
        val expected = SplitName("LICENSE", null)
        assertEquals(expected, fileService.splitFileName(input))
    }

    @Test
    fun `splitFileName treats leading dot files as only having an extension`() {
        val input = ".bashrc"
        val expected = SplitName("", "bashrc")
        assertEquals(expected, fileService.splitFileName(input))
    }
}
