package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.net.URI

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun UploadFileBox(
    height: Dp,
    shouldShow: Boolean,
    modifier: Modifier = Modifier,
    onDrop: (Collection<PlatformFile>) -> Unit,
) {
    var isHovering by remember { mutableStateOf(false) }
    val composedCallback by rememberUpdatedState(onDrop)
    val dragAndDropHandler = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                println("dropped!")
                // this should only even show if the drag type is a file system drag, so we can just blindly assume
                val files = (event.dragData() as DragData.FilesList).readFiles()
                val platformFiles = files.map { URI.create(it) }.map(::File).map(::PlatformFile)
                composedCallback(platformFiles)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) {
                super.onEntered(event)
                isHovering = true
            }

            override fun onExited(event: DragAndDropEvent) {
                super.onExited(event)
                isHovering = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isHovering = false
            }
        }
    }

    Box(
        modifier = modifier
            .alpha(if (shouldShow) 1f else 0f)
            .fillMaxWidth()
            .height(if (shouldShow) height else 0.dp)
            .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            .pointerInput(Unit) {}
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropHandler,
            ) then if (isHovering) {
            Modifier.background(
                MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            )
        } else {
            Modifier
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = "Upload file")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload to current folder")
        }
    }
}
