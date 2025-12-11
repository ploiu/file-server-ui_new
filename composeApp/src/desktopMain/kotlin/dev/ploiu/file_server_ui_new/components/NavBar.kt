package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FolderApi
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

    operator fun get(position: Int) = folders[position]

    operator fun get(item: FolderApi) = folders.indexOfFirst { it.id == item.id }

    operator fun get(range: UIntRange): MutableList<FolderApi> {
        return folders.subList(range.first.toInt(), range.last.toInt())
    }
}

operator fun MutableState<NavState>.plusAssign(folder: FolderApi) {
    value += folder
}

@Composable
fun NavBar(state: NavState, clickEntry: (LinkedList<FolderApi>) -> Unit) {
    // TODO support dragging + dropping folders + files to an entry in the navbar
    FlowRow(modifier = Modifier.padding(start = 16.dp)) {
        for (index in state.indices) {
            val folder = state[index]
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
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (index < state.size - 1) {
                Text("/")
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
