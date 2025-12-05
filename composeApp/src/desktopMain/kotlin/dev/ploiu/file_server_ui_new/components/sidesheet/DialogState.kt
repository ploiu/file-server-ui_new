package dev.ploiu.file_server_ui_new.components.sidesheet

import dev.ploiu.file_server_ui_new.model.FolderChild

/**
 * used to control state for extra elements such as rename/delete dialogs and folder info dialogs
 */
sealed interface DialogState

// actions that alter the state of this page directly
class NoDialogState : DialogState
data class RenameDialogState(val item: FolderChild) : DialogState
data class DeleteDialogState(val item: FolderChild) : DialogState

