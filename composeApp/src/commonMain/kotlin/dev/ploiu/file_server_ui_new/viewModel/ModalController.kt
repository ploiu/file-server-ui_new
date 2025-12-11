package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ploiu.file_server_ui_new.components.dialog.DialogProps
import dev.ploiu.file_server_ui_new.components.dialog.PromptDialogProps
import dev.ploiu.file_server_ui_new.components.dialog.TextDialogProps
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

/**
 * represents modal state for the root application view, for things that aren't children of the main folder view or the side sheet
 */
sealed interface ApplicationModalState<T : DialogProps<*>>
object NoModal : ApplicationModalState<Nothing>
data class TextModal(val props: TextDialogProps) : ApplicationModalState<TextDialogProps>
data class ConfirmModal(val props: PromptDialogProps) : ApplicationModalState<PromptDialogProps>
data class ErrorModal(val props: PromptDialogProps) : ApplicationModalState<PromptDialogProps>
data class LoadingModal(val max: Int, val progress: Int) : ApplicationModalState<Nothing>


data class ModalState(val modal: ApplicationModalState<*>, val opener: KClass<out ViewModel>?) {
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
@Suppress("DEPRECATION")
class ModalController : ViewModel() {

    @Deprecated(level = DeprecationLevel.WARNING, message = "don't ever use this externally")
    val _state = MutableStateFlow(ModalState(NoModal, null))
    val state = _state.asStateFlow()

    val isOpen: StateFlow<Boolean> = _state.map { it.isOpen }
        .stateIn(
            scope = viewModelScope,
            initialValue = _state.value.isOpen,
            started = SharingStarted.Eagerly,
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

    /**
     * attempts to update the current modal without closing and re-opening it.
     *
     * Modal changes will be rejected if the `updater` does not match the same class that opened the modal initially
     *
     * Example:
     * ```kt
     * modalController.open(LoadingModal(val max: Int, val progress: Int)
     * ```
     *
     * @param updater the object performing the update
     * @param updateFn a function that takes the current state as input and produces a new state as output. This function
     * is guaranteed to be type safe. If the current state is not the same class as the parameter specified in `updateFn`,
     * the function is not called and false is returned
     *
     * @return `true` if the update succeeded, false otherwise
     */
    inline fun <reified T : ApplicationModalState<*>> updateModalState(
        updater: ViewModel,
        crossinline updateFn: (T) -> T,
    ): Boolean {
        val (current, opener) = _state.value
        if (current is T && updater::class == opener) {
            _state.update { it.copy(modal = updateFn(current)) }
            return true
        } else {
            return false
        }
    }
}

/**
 * used to provide helper functions for ease of use to child classes
 */
abstract class ViewModelWithModal(protected val modalController: ModalController) : ViewModel() {
    protected fun openModal(state: ApplicationModalState<*>) {
        modalController.open(modal = state, opener = this)
    }

    fun closeModal() = modalController.close(this)

    protected inline fun <reified T : ApplicationModalState<*>> updateModal(crossinline updateFn: (T) -> T) =
        modalController.updateModalState(updater = this, updateFn = updateFn)
}
