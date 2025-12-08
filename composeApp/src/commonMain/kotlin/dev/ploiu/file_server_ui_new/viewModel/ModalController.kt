package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.components.dialog.PromptDialogProps
import dev.ploiu.file_server_ui_new.components.dialog.DialogProps
import dev.ploiu.file_server_ui_new.components.dialog.TextDialogProps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

/**
 * represents modal state for the root application view, for things that aren't children of the main folder view or the side sheet
 */
sealed interface ApplicationModalState<T: DialogProps<*>>
object NoModal : ApplicationModalState<Nothing>
data class TextModal(val props: TextDialogProps): ApplicationModalState<TextDialogProps>
data class ConfirmModal(val props: PromptDialogProps): ApplicationModalState<PromptDialogProps>
data class ApplicationErrorModal(val message: String) : ApplicationModalState<PromptDialogProps>
data class LoadingModal(val max: Int, val progress: Int) : ApplicationModalState<Nothing>


private data class ModalState(val modal: ApplicationModalState<*>, val opener: KClass<out ViewModel>?) {
    val isOpen: Boolean
        get() = modal !is NoModal
}

/**
 * A global controller for application modals.
 *
 * This does not handle rendering the modal at all, only which modal should be open. Modals should be rendered in the main application shell
 * TODO register in module, figure out how to make globally available.
 *   Find everywhere dialogs are used, determine how the states are controlled, and consolidate in here
 *
 * TODO instead of just checking which modal type is open, also check _who_ opened it to know who's allowed to close it
 */
class ModalController : ViewModel() {

    private val _state = MutableStateFlow(ModalState(NoModal, null))

    val isOpen: StateFlow<Boolean> = _state.map {it.isOpen}
        .stateIn(
            scope = viewModelScope,
            initialValue = _state.value.isOpen,
            started = SharingStarted.Eagerly
        )

    /**
     * attempts to change the currently-opened modal.
     * Modal change will be rejected if any modal is already open.
     *
     * @return `true` if the modal was opened, `false` otherwise
     */
    fun open(modal: ApplicationModalState<*>, opener: ViewModel): Boolean {
        val currentState = _state.value
        if (currentState.isOpen) {
            return false
        }
        _state.update { ModalState(modal = modal, opener = opener::class) }
        return true
    }

    /**
     * attempts to close the current modal
     */
    fun close(closer: ViewModel): Boolean {
        if (closer::class == _state.value.opener) {
            _state.update {
                ModalState(modal = NoModal, opener = null)
            }
            return true
        } else {
            return false
        }
    }
}
