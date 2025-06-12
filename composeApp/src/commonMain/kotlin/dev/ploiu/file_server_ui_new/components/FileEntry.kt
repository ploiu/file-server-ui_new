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
import file_server_ui_new.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

fun determineIcon(file: FileApi): DrawableResource {
    return when (file.fileType.lowercase()) {
        "application" -> Res.drawable.application
        "archive" -> Res.drawable.archive
        "audio" -> Res.drawable.audio
        "cad" -> Res.drawable.cad
        "code" -> Res.drawable.code
        "configuration" -> Res.drawable.configuration
        "diagram" -> Res.drawable.diagram
        "document" -> Res.drawable.document
        "font" -> Res.drawable.font
        "rom" -> Res.drawable.rom
        "image" -> Res.drawable.image
        "material" -> Res.drawable.material
        "model" -> Res.drawable.model
        "object" -> Res.drawable.`object`
        "presentation" -> Res.drawable.presentation
        "savefile" -> Res.drawable.savefile
        "spreadsheet" -> Res.drawable.spreadsheet
        "text" -> Res.drawable.text
        "video" -> Res.drawable.video
        else -> Res.drawable.unknown
    }
}

/**
 * picks which image is used to represent the file - the file type image or the preview.
 *
 * if [preview] is null, the result of [determineIcon] is used
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
