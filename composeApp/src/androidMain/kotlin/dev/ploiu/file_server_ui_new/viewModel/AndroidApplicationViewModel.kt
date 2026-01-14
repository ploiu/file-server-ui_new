package dev.ploiu.file_server_ui_new.viewModel

import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job

class AndroidApplicationViewModel(
    folderService: FolderService,
    fileService: FileService,
    modalController: ModalController,
) : ApplicationViewModel(folderService = folderService, fileService = fileService, modalController = modalController) {
    override fun uploadBulk(
        bulk: Collection<PlatformFile>,
        currentFolderId: Long,
    ): Job {
        TODO("Not yet implemented")
    }
}
