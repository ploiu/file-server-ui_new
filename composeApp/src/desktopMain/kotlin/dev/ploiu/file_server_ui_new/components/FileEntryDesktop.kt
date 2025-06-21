package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FileApi
import file_server_ui_new.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image

fun ByteArray.toImageBitmap() = Image.makeFromEncoded(this).toComposeImageBitmap()

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
        "video" -> Res.drawable.video_svg
        else -> Res.drawable.unknown
    }
}

@Composable
actual fun PickFileImage(file: FileApi, preview: ByteArray?) {
    // TODO I might have to migrate all of these assets to svg for desktop. No clue how they'll look on android though
    if (preview == null) {
        Image(
            painter = painterResource(determineIcon(file)),
            contentDescription = "file icon",
            Modifier.width(64.dp).height(64.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Image(
            bitmap = preview.toImageBitmap(),
            contentDescription = "file preview",
            modifier = Modifier.width(64.dp).height(64.dp),
            contentScale = ContentScale.Fit
        )
    }
}
