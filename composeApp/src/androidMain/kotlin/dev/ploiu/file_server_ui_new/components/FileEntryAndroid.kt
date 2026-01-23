package dev.ploiu.file_server_ui_new.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.ploiu.file_server_ui_new.model.FileApi
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun PickFileImage(file: FileApi, preview: ByteArray?, modifier: Modifier) {
    if (preview == null) {
        Image(
            painter = painterResource(determineDrawableIcon(file)),
            contentDescription = "file icon",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Image(
            bitmap = preview.toImageBitmap(),
            contentDescription = "file preview",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

fun ByteArray.toImageBitmap(): ImageBitmap = BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()

