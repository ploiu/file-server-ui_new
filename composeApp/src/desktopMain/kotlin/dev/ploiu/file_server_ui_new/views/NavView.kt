package dev.ploiu.file_server_ui_new.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
data class NavState(val folders: LinkedList<FolderApi>)

@Composable
fun NavBar(state: NavState, clickEntry: (FolderApi) -> Unit) {
    // TODO support dragging + dropping folders + files to an entry in the navbar
    FlowRow(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
        for (index in state.folders.indices) {
            val folder = state.folders[index]
            Text(
                folder.name,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.pointerHoverIcon(
                    PointerIcon.Hand
                )
                    .clickable {
                        // only navigate if they don't click the current folder
                        if (index < state.folders.size - 1) {
                            // always backward here because we never show child folders in this bar
                            clickEntry(folder)
                        }
                    },
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (index < state.folders.size - 1) {
                Text("/")
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
