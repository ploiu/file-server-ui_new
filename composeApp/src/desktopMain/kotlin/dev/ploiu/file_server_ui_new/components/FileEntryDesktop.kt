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

private object CachedResources {
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
        "video" to Res.drawable.video_svg,
        "unknown" to Res.drawable.unknown,
        "folder" to Res.drawable.folder,
    )

    /** similar to [CachedResources.internalDrawables], but for [ImageBitmap]s */
    val internalBitmaps = mutableMapOf<String, ImageBitmap>()
}

fun determineDrawableIcon(file: FileApi): DrawableResource {
    return CachedResources.internalDrawables[file.fileType.lowercase()] ?: Res.drawable.unknown
}

/**
 * reads all needed resources for the application and caches them for use with [determineBitmapIcon]
 */
fun loadResources() = runBlocking {
    val keysPathMapping = CachedResources.internalDrawables.mapValues { "drawable/${it.key}.png" }
    for ((key, path) in keysPathMapping) {
        CachedResources.internalBitmaps[key] = Res.readBytes(path).toImageBitmap()
    }
}

fun determineBitmapIcon(file: FileApi): ImageBitmap =
    CachedResources.internalBitmaps[file.fileType.lowercase()] ?: CachedResources.internalBitmaps["unknown"]!!

fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
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
