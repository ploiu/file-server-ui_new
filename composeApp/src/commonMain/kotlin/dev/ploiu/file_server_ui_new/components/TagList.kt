package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.InputChipDefaults.inputChipColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.Tag
import org.jetbrains.compose.ui.tooling.preview.Preview

// TODO tests
@Composable
fun TagList(tags: Collection<Tag>, onUpdate: (Collection<Tag>) -> Unit) {
    val sorted = remember { tags.sortedBy { it.title } }
    var isAddingTag by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        val chipColors = inputChipColors(
            containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .12f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(Modifier.padding(start = 16.dp, end = 16.dp).fillMaxWidth()) {
                for (tag in sorted) {
                    // TODO for android this needs to be pulled out into something that gets pressed and held to be deleted. Use expect fun
                    InputChip(
                        onClick = { onUpdate(sorted.filterNot { it == tag }) },
                        colors = chipColors,
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = "close icon") },
                        selected = false,
                        label = { Text(tag.title.lowercase()) })
                    Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { isAddingTag = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
                Text("Add Tag")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (isAddingTag) {
        TextDialog(title = "Add tag", onCancel = { isAddingTag = false }, confirmText = "Add", onConfirm = {
            val updateTags = tags.toMutableSet()
            updateTags.add(Tag(id = null, title = it.lowercase()))
            onUpdate(updateTags)
            isAddingTag = false
        })
    }
}

@Preview
@Composable
private fun EmptyTagList() {
    TagList(listOf()) {}
}

@Preview
@Composable
private fun TagListWithTags() {
    TagList(listOf(Tag(0, "Tag1"), Tag(1, "tag with a really really long name"), Tag(2, "Tag with medium name"))) {}
}
