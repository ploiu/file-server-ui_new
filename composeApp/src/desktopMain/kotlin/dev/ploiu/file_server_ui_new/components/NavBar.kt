package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.CustomDataFlavors
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.model.FolderChild
import java.util.*

/**
 * represents the current state of folder history.
 *
 * the first element should _always_ be the root folder
 */
data class NavState(private val folders: LinkedList<FolderApi>) {
    val indices get() = folders.indices
    val size get() = folders.size
    val last get() = folders.last()

    operator fun plus(folder: FolderApi): NavState {
        val newFolders = LinkedList(folders)
        newFolders.add(folder)
        return this.copy(folders = newFolders)
    }

    fun pop(): NavState {
        return this.copy(folders = LinkedList(folders.subList(fromIndex = 0, toIndex = folders.size - 1)))
    }

    operator fun get(position: Int) = folders[position]

    operator fun get(item: FolderApi) = folders.indexOfFirst { it.id == item.id }

    operator fun get(range: UIntRange): MutableList<FolderApi> {
        return folders.subList(range.first.toInt(), range.last.toInt())
    }
}

// operator fun MutableState<NavState>.plusAssign(folder: FolderApi) {
//     value += folder
// }

fun MutableState<NavState>.pop() {
    value = value.pop()
}

@Composable
fun NavBar(
    state: NavState,
    clickEntry: (LinkedList<FolderApi>) -> Unit,
    onFolderChildDropped: (FolderApi, FolderChild) -> Unit,
) {
    FlowRow(modifier = Modifier.padding(start = 16.dp)) {
        for (index in state.indices) {
            val folder = state[index]
            NavEntry(
                folder = folder,
                state = state,
                onFolderChildDropped = onFolderChildDropped,
                clickEntry = clickEntry,
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (index < state.size - 1) {
                Text("/")
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NavEntry(
    folder: FolderApi,
    state: NavState,
    onFolderChildDropped: (FolderApi, FolderChild) -> Unit,
    clickEntry: (LinkedList<FolderApi>) -> Unit,
) {
    var isDraggedOver by remember { mutableStateOf(false) }
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDraggedOver = false
                return if (event.awtTransferable.isDataFlavorSupported(CustomDataFlavors.FOLDER_CHILD)) {
                    val child = event.awtTransferable.getTransferData(CustomDataFlavors.FOLDER_CHILD) as FolderChild
                    onFolderChildDropped(folder, child)
                    true
                } else {
                    false
                }
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDraggedOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDraggedOver = false
            }
        }
    }

    Text(
        folder.name,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.pointerHoverIcon(
            PointerIcon.Hand,
        ).clickable {
            val index = state[folder]
            if (index != -1) {
                val newFolders = state[0.toUInt()..(index + 1).toUInt()]
                clickEntry(LinkedList(newFolders))
            }
        }.dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget),
    )
}
