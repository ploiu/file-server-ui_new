package dev.ploiu.file_server_ui_new.model

import java.io.File
import java.nio.file.Files

/**
 * represents a "middle ground" between a file system {@link File} and a {@link FolderApi}
 */
data class FolderApproximation(
    private val self: File,
    private val childFiles: Collection<File>,
    private val childFolders: Collection<FolderApproximation>
) {

    val size: Int
        get() = 1 + childFiles.size + childFolders.sumOf { it.size }

    init {
        if (!self.isDirectory) {
            throw UnsupportedOperationException("Self must be a directory")
        }
        detectSymLinks()
        detectChildDirs()
    }

    /** symlinks can cause infinite loops, so we need to avoid them */
    private fun detectSymLinks() {
        if (Files.isSymbolicLink(self.toPath())) {
            throw UnsupportedOperationException("Cannot upload symbolic links, fix these files paths: \n\t${self.toPath()}")
        }
        val symPaths =
            childFiles.map { it.toPath() }.filter { Files.isSymbolicLink(it) }
                .joinToString("\n\t") { it.toAbsolutePath().toString() }
        if (symPaths.isNotEmpty()) {
            throw UnsupportedOperationException("Cannot upload symbolic links, fix these files paths: \n\t$symPaths")
        }
    }

    /**
     * will throw an exception if anything in childFiles is a directory.
     * This represents an issue with the code, rather than an issue with the user
     */
    private fun detectChildDirs() {
        val childDirs = childFiles
            .filter { it.isDirectory }
            .map { it.toPath() }.joinToString("\n\t") { it.toAbsolutePath().toString() }
        if (!childDirs.isBlank()) {
            throw java.lang.UnsupportedOperationException("Your code is broken: directories are in the list of child files: \n\t$childDirs")
        }
    }
}

/**
 * Used to handle transforming File system files and directories into a "middle ground" between the file system and the api.
 * This allows us an easier way to upload entire directories to the server
 */
object FolderApproximator {
    fun convertDir(root: File, currentDepth: Int = 1): FolderApproximation {
        if (currentDepth > 50) {
            throw UnsupportedOperationException("Possible recursive symlinks: cannot go past depth of 50.")
        }
        if (!root.isDirectory) {
            throw UnsupportedOperationException("Can only read directories.")
        }
        val childFiles = root.listFiles { it.isFile }
            ?: throw NullPointerException("Could not list files in directory: ${root.path}")
        val childFolders = root.listFiles { it.isDirectory && !Files.isSymbolicLink(it.toPath().toAbsolutePath()) }
            ?: throw NullPointerException("Could not list directories in directory: ${root.path}")
        val approximations = mutableListOf<FolderApproximation>()
        for (childFolder in childFolders) {
            approximations += convertDir(childFolder, currentDepth + 1)
        }
        return FolderApproximation(root, childFiles.toList(), approximations)
    }
}
