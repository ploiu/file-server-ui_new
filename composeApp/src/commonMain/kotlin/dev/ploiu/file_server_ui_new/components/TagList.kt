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
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * represents a panel of tags. The panel allows for adding and removing tags via the [onUpdate] parameter. When
 * invoked, [onUpdate] will be passed all the tags currently in this component
 */
@Composable
fun TagList(tags: Collection<TaggedItemApi>, onUpdate: (Collection<TaggedItemApi>) -> Unit) {
    val sorted = remember {
        // inherited tags should be listed last
        tags.sortedWith { a, b ->
            if (a.implicitFrom == null && b.implicitFrom != null) {
                -1
            } else if (b.implicitFrom == null && a.implicitFrom != null) {
                1
            } else {
                a.title.compareTo(b.title)
            }
        }
    }
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
            updateTags.add(TaggedItemApi(id = null, title = it.lowercase(), implicitFrom = null))
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
            TaggedItemApi(0, "Tag1", null),
            TaggedItemApi(1, "tag with a really really long name", null),
            TaggedItemApi(2, "Tag with medium name", null)
        )
    ) {}
}


@Preview
@Composable
private fun TagListWithInheritedTags() {
    TagList(
        listOf(
            TaggedItemApi(0, "Tag1", null),
            TaggedItemApi(1, "tag with a really really long name", null),
            TaggedItemApi(id = 3, title = "Inherited Tag", implicitFrom = 5),
            TaggedItemApi(2, "Tag with medium name", null)
        )
    ) {}
}
