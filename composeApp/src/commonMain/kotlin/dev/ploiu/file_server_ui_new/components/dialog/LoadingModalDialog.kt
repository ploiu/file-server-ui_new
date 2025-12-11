package dev.ploiu.file_server_ui_new.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.ploiu.file_server_ui_new.viewModel.LoadingModal

@Composable
fun LoadingModalDialog(loadingModal: LoadingModal) {
    Dialog(onDismissRequest = {}) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(
                    progress = { loadingModal.progress / loadingModal.max.toFloat() },
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Uploading... ${loadingModal.progress} / ${loadingModal.max}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
