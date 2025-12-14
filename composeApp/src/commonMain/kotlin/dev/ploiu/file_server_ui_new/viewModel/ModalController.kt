package dev.ploiu.file_server_ui_new.viewModel

import androidx.lifecycle.ViewModel
import dev.ploiu.file_server_ui_new.components.dialog.ErrorModalProps
import dev.ploiu.file_server_ui_new.components.dialog.ModalProps
import dev.ploiu.file_server_ui_new.components.dialog.PromptModalProps
import dev.ploiu.file_server_ui_new.components.dialog.TextModalProps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

/**
 * represents modal state for the root application view, for things that aren't children of the main folder view or the side sheet
 */
sealed interface ApplicationModalState<T : ModalProps<*>>
object NoModal : ApplicationModalState<Nothing>

// all of these classes have a companion object for ease of use in opening modals (so that we don't need to wrap 3 functions every time to open a modal)
data class TextModal(val props: TextModalProps) : ApplicationModalState<TextModalProps> {
    companion object
}

data class ConfirmModal(val props: PromptModalProps) : ApplicationModalState<PromptModalProps> {
    companion object
}

data class ErrorModal(val props: ErrorModalProps) : ApplicationModalState<ErrorModalProps> {
    companion object
}

data class LoadingModal(val max: Int, val progress: Int) : ApplicationModalState<Nothing> {
    companion object
}

/**
 * represents modal state, which is used to actually build the composable modal during rendering
 *
 * @param modal the actual props and wrapper for the modal data
 * @param opener who opened the modal. Used to lock the modal and prevent other classes from closing or opening a new modal
 * @param dialogJustClosed used to control if the escape key should close opened elements that _aren't_ a dialog. If `true`, the other elements shouldn't be closed
 */
data class ModalState(
    val modal: ApplicationModalState<*>,
    val opener: KClass<out Any>?,
    val dialogJustClosed: Boolean,
) {
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
    val _state = MutableStateFlow(ModalState(NoModal, null, dialogJustClosed = false))
    val state = _state.asStateFlow()
    val isJustClosed: Boolean
        get() = _state.value.dialogJustClosed
    val isOpen: Boolean
        get() = _state.value.isOpen

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
        _state.update { ModalState(modal = modal, opener = opener::class, dialogJustClosed = false) }
        return true
    }

    /**
     * attempts to close the current modal.
     * @param closer the viewModel attempting to close the dialog. If it doesn't match the opener and `override` is not passed, the attempt is rejected
     * @param override whether to ignore the closer. Should never be used except in the atop level view model
     */
    fun close(closer: ViewModel, override: Boolean = false): Boolean {
        if (override || closer::class == _state.value.opener) {
            _state.update {
                ModalState(modal = NoModal, opener = null, dialogJustClosed = true)
            }
            return true
        } else {
            return false
        }
    }

    /**
     * used to reset the dialogJustClosed flag to false so that the escape key works properly
     */
    fun clearJustClosed() {
        _state.update {
            it.copy(dialogJustClosed = false)
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

    fun ConfirmModal.Companion.open(props: PromptModalProps) = openModal(ConfirmModal(props))

    fun TextModal.Companion.open(props: TextModalProps) = openModal(TextModal(props))

    fun LoadingModal.Companion.open(max: Int, progress: Int) = openModal(LoadingModal(max = max, progress = progress))

    fun ErrorModal.Companion.open(props: ErrorModalProps) = openModal(ErrorModal(props))
}
