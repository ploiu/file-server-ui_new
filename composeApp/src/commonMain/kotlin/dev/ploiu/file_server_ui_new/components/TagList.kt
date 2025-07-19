package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.TagApi
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * represents a panel of tags. The panel allows for adding and removing tags via the [onUpdate] parameter. When
 * invoked, [onUpdate] will be passed all the tags currently in this component
 */
@Composable
fun TagList(tags: Collection<TagApi>, onUpdate: (Collection<TagApi>) -> Unit) {
    val sorted = remember { tags.sortedBy { it.title } }
    var isAddingTag by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(Modifier.padding(start = 16.dp, end = 16.dp).fillMaxWidth()) {
                for (tag in sorted) {
                    Tag(tag = tag, requestDelete = { t -> onUpdate(sorted.filterNot { it == t }) })
                    Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.testTag("addTag"),
                onClick = { isAddingTag = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onTertiaryContainer),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag", tint = LocalContentColor.current)
                Text("Add Tag")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (isAddingTag) {
        TextDialog(title = "Add tag", onCancel = { isAddingTag = false }, confirmText = "Add", onConfirm = {
            val updateTags = tags.toMutableSet()
            updateTags.add(TagApi(id = null, title = it.lowercase()))
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
    TagList(
        listOf(
            TagApi(0, "Tag1"),
            TagApi(1, "tag with a really really long name"),
            TagApi(2, "Tag with medium name")
        )
    ) {}
}
