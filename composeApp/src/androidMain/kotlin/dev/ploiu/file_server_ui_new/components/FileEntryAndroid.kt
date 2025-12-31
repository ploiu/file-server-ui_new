package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import dev.ploiu.file_server_ui_new.model.FileApi
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.application
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun PickFileImage(file: FileApi, preview: ByteArray?, modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.application),
        contentDescription = "file icon",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
