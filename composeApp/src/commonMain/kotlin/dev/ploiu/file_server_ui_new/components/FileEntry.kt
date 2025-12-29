package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.util.formatFileOrFolderName

/**
 * picks which image is used to represent the file - the file type image or the preview.
 *
 * if [preview] is null, a default is used
 */
@Composable
expect fun PickFileImage(file: FileApi, preview: ByteArray?, modifier: Modifier = Modifier)

@Composable
fun FileEntry(
    file: FileApi,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    preview: ByteArray? = null,
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().then(modifier),
        onClick = { TODO("fileEntry click") },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            PickFileImage(file, preview, Modifier.width(96.dp).height(96.dp).then(imageModifier))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatFileOrFolderName(file.name),
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
            )
        }
    }
}
