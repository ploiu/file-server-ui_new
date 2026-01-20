package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.ploiu.file_server_ui_new.model.FileApi
import file_server_ui_new.composeapp.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skia.Image

object CachedResources {
    /** similar to [internalDrawables], but for [ImageBitmap]s */
    val internalBitmaps = mutableMapOf<String, ImageBitmap>()
}

/**
 * reads all needed resources for the application and caches them for use with [determineBitmapIcon]
 */
fun loadResources() = runBlocking {
    val keysPathMapping = internalDrawables.mapValues { "drawable/${it.key}.png" }
    for ((key, path) in keysPathMapping) {
        CachedResources.internalBitmaps[key] = Res.readBytes(path).toImageBitmap()
    }
}

fun determineBitmapIcon(file: FileApi): ImageBitmap =
    CachedResources.internalBitmaps[file.fileType.lowercase()] ?: CachedResources.internalBitmaps["unknown"]!!

fun ByteArray.toImageBitmap(): ImageBitmap {
    // sometimes this fails when running a proxy server to check reqs and responses. Something something byte order I can't tell
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        CachedResources.internalBitmaps["unknown"]!!
    }
}

@Composable
actual fun PickFileImage(file: FileApi, preview: ByteArray?, modifier: Modifier) {
    // TODO I might have to migrate all of these assets to svg for desktop. No clue how they'll look on android though
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
