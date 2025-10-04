package dev.ploiu.file_server_ui_new.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name


class DesktopFolderUploadService(private val folderService: FolderService, private val fileService: FileService): FolderUploadService {
    override suspend fun uploadFolder(
        folder: PlatformFile,
        parentFolder: FolderApi
    ): Result<Unit, String> {
        if (parentFolder.containsFolder(folder.name)) {
            return Err("A folder with the name ${folder.name} already exists in this folder")
        }
        // the issue here is that there could be an arbitrary number of nested folders and an arbitrary number of files in each of those folders.
        // This presents multiple issues:
        // 1. stack overflow from too much recursion
        // 2. overloading the server with too many requests at once
        // ignoring #1 for now (how likely is it _really_ to upload a folder with so many nested folders that it overflows the stack?)
        // #2 is still a concern; we still need to chunk requests and delay between them
        TODO("this problem was likely solved already, look at DragNDropService, FolderApproximator, and FolderApproximation in the old ui (https://github.com/ploiu/file-server-ui/blob/main/src/main/java/ploiu/service/DragNDropService.java)")
    }
}
