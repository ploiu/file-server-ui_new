package dev.ploiu.file_server_ui_new.model

import dev.ploiu.file_server_ui_new.model.FolderApproximator.convertDir
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class FolderApproximatorTests {
    companion object {
        val root = File("src/test/resources/FolderApproximatorTests")
        val helper = TestHelper(root)
    }

    @BeforeTest
    fun setup() {
        if (root.exists()) {
            helper.deleteDirectory(root)
        }
        root.mkdirs()
    }

    @AfterTest
    fun teardown() {
        if (root.exists()) {
            helper.deleteDirectory(root)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testConvertFlatDir() {
        val dir =
            helper.createDir("dir")
        val file =
            helper.createFile("test.txt")
        file.createNewFile()
        dir.mkdir()
        val res = convertDir(root)
        Assertions.assertEquals(
            FolderApproximation(
                root,
                listOf(file),
                listOf(FolderApproximation(dir, listOf(), listOf()))
            ), res
        )
    }

}


class TestHelper(private val root: File) {
    fun deleteDirectory(directoryToBeDeleted: File) = directoryToBeDeleted.deleteRecursively()

    @Throws(IOException::class)
    fun createDir(path: String?): File {
        val f = File(root.path + "/" + path)
        f.mkdirs()
        return f
    }

    @Throws(IOException::class)
    fun createFile(path: String?): File {
        val f = File(root.path + "/" + path)
        f.createNewFile()
        return f
    }
}
