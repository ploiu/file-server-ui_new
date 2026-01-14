package dev.ploiu.file_server_ui_new.model

import dev.ploiu.file_server_ui_new.FolderApproximation
import dev.ploiu.file_server_ui_new.FolderApproximator.convertDir
import org.junit.jupiter.api.Assertions.assertThrows
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val dir = helper.createDir("dir")
        val file = helper.createFile("test.txt")
        file.createNewFile()
        dir.mkdir()
        val res = convertDir(root)
        assertEquals(
            FolderApproximation(
                root, listOf(file), listOf(FolderApproximation(dir, listOf(), listOf())),
            ),
            res,
        )
    }

    @Test
    fun testConvertEmptyDir() {
        val res = convertDir(root)
        assertEquals(FolderApproximation(root, mutableListOf(), mutableListOf()), res)
    }

    @Test
    @Throws(IOException::class)
    fun testConvertRecursiveNoFiles() {
        val top = helper.createDir("top")
        val middle = helper.createDir("top/middle")
        val bottom = helper.createDir("top/middle/bottom")
        val expected = FolderApproximation(
            root, mutableListOf(),
            listOf(
                FolderApproximation(
                    top, mutableListOf(),
                    listOf(
                        FolderApproximation(
                            middle, mutableListOf(),
                            listOf(
                                FolderApproximation(
                                    bottom, mutableListOf(), mutableListOf(),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val res = convertDir(root)
        assertEquals(expected, res)
    }

    @Test
    @Throws(IOException::class)
    fun testConvertRecursiveWithFiles() {
        val top = helper.createDir("top")
        val rootFile = helper.createFile("root.txt")
        val topFile = helper.createFile("top/top.txt")
        val expected = FolderApproximation(
            root, listOf(rootFile),
            listOf(
                FolderApproximation(
                    top, listOf(topFile), mutableListOf(),
                ),
            ),
        )
        val res = convertDir(root)
        assertEquals(expected, res)
    }

    @Test
    @Throws(IOException::class)
    fun testConvertRecursivePastLimit() {
        // the point of this test is to reject cases with symbolic links, but we can't create symbolic link directories in java sooooo
        val builder = StringBuilder("0")
        for (i in 1..50) {
            builder.append("/").append(i)
        }
        helper.createDir(builder.toString())
        val exception = assertThrows(
            UnsupportedOperationException::class.java,
        ) { convertDir(root) }
        assertEquals("Possible recursive symlinks: cannot go past depth of 50.", exception.message)
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
