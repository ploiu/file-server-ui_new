package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.formatFileOrFolderName
import dev.ploiu.file_server_ui_new.model.FileApi

/**
 * picks which image is used to represent the file - the file type image or the preview.
 *
 * if [preview] is null, a default is used
 */
@Composable
expect fun PickFileImage(file: FileApi, preview: ByteArray?)

@Composable
fun FileEntry(file: FileApi, preview: ByteArray? = null) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            PickFileImage(file, preview)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatFileOrFolderName(file.name),
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )
        }
    }
}
