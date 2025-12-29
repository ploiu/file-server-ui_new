package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.util.formatFileOrFolderName
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.folder
import org.jetbrains.compose.resources.painterResource


@Composable
fun FolderEntry(
    folder: FolderApi,
    modifier: Modifier = Modifier,
    surfaceColor: Color? = null,
    onClick: (f: FolderApi) -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        color = surfaceColor ?: MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().then(modifier),
        shape = MaterialTheme.shapes.small,
        onClick = { onClick(folder) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(Res.drawable.folder),
                contentDescription = "folder icon",
                Modifier.width(96.dp).height(96.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatFileOrFolderName(folder.name),
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
            )
        }
    }
}
