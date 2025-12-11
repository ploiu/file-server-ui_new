package dev.ploiu.file_server_ui_new.components.dialog

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.ploiu.file_server_ui_new.viewModel.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurrentDialog(controller: ModalController = koinViewModel()) {
    val (currentModal) = controller.state.collectAsState().value
    when (currentModal) {
        is ConfirmModal -> currentModal.props()
        is ErrorModal -> {
            val actualProps = currentModal.props.copy(
                iconColor = MaterialTheme.colorScheme.error,
            )
            actualProps()
        }

        is LoadingModal -> LoadingModalDialog(currentModal)
        NoModal -> {
            /*no op*/
        }

        is TextModal -> currentModal.props()
    }
}
