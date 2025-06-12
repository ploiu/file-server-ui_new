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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image

// TODO android doesn't like this
fun ByteArray.toImageBitmap() = Image.makeFromEncoded(this).toComposeImageBitmap()

@Composable
actual fun PickFileImage(file: FileApi, preview: ByteArray?) {
    // TODO I might have to migrate all of these assets to svg for desktop. No clue how they'll look on android though
    if (preview == null) {
        Image(
            painter = painterResource(determineIcon(file)),
            contentDescription = "file icon",
            Modifier.width(96.dp).height(96.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            bitmap = preview.toImageBitmap(),
            contentDescription = "file preview",
            modifier = Modifier.width(96.dp).height(96.dp),
            contentScale = ContentScale.Fit
        )
    }
}
