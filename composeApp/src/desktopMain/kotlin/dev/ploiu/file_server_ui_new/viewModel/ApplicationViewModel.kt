package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.model.CreateFolder
import dev.ploiu.file_server_ui_new.service.FileService
import dev.ploiu.file_server_ui_new.service.FolderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the current state of the application. Not intended to replace routing functionality, but there are some things
 * that can't be represented with a route (such as side sheet status). This also serves as a central way to manage things
 * like event handlers sent from child components where necessary (such as the strip of action buttons in the app header)
 */
sealed interface ApplicationViewModelState


// I actually still don't like this but it's cleaner to handle it this way than a bunch of random handlers all over the place.
// However, this is only intended for the "root" application state. Things like the folder page, folder side sheet, etc should
// handle api calls in their own view models. This isn't a "catch all", this serves a small scope of the application
// (that just so happens to be top level)
class ApplicationViewModel(private val folderService: FolderService, private val fileService: FileService) :
    ViewModel() {
    // TODO exception handler (look at folder detail view model)
    fun addEmptyFolder(name: String) = viewModelScope.launch(Dispatchers.IO) {
        folderService.createFolder(CreateFolder(name, 0L, listOf()))
    }
}
