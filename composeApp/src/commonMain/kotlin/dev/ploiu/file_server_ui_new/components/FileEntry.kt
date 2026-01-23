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
import file_server_ui_new.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

/** a mapping of all drawables directly bundled with the app, paired to their resource drawable */
val internalDrawables = mapOf(
    "application" to Res.drawable.application,
    "archive" to Res.drawable.archive,
    "audio" to Res.drawable.audio,
    "cad" to Res.drawable.cad,
    "code" to Res.drawable.code,
    "configuration" to Res.drawable.configuration,
    "diagram" to Res.drawable.diagram,
    "document" to Res.drawable.document,
    "font" to Res.drawable.font,
    "rom" to Res.drawable.rom,
    "image" to Res.drawable.image,
    "material" to Res.drawable.material,
    "model" to Res.drawable.model,
    "object" to Res.drawable.`object`,
    "presentation" to Res.drawable.presentation,
    "savefile" to Res.drawable.savefile,
    "spreadsheet" to Res.drawable.spreadsheet,
    "text" to Res.drawable.text,
    "video" to Res.drawable.video,
    "unknown" to Res.drawable.unknown,
    "folder" to Res.drawable.folder,
)

fun determineDrawableIcon(file: FileApi): DrawableResource {
    return internalDrawables[file.fileType.lowercase()] ?: Res.drawable.unknown
}

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
        modifier = Modifier.fillMaxWidth() then modifier,
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
