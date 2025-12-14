package dev.ploiu.file_server_ui_new.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.InputChipDefaults.inputChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.ploiu.file_server_ui_new.model.TaggedItemApi

class TagChipColors(
    val containerColor: Color,
    val labelColor: Color,
    val iconColor: Color,
)

@Composable
private fun TagChipColors.into() = inputChipColors(
    containerColor = containerColor,
    labelColor = labelColor,
    leadingIconColor = iconColor,
)

// TODO this needs to be expect because Android will need a press and hold for deleting
@Composable
fun Tag(
    tag: TaggedItemApi,
    modifier: Modifier = Modifier,
    colors: TagChipColors = TagChipColors(
        containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .12f),
        labelColor = LocalContentColor.current,
        iconColor = LocalContentColor.current,
    ),
    requestDelete: (TaggedItemApi) -> Unit,
) {
    val deletable = remember { tag.implicitFrom == null }
    InputChip(
        // TODO for android this needs to be pulled out into something that gets pressed and held to be deleted. Use expect fun
        onClick = {
            if (deletable) {
                requestDelete(tag)
            }
        },
        colors = colors.into(),
        leadingIcon = {
            if (deletable) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Tag Icon",
                    modifier = Modifier.testTag("tag_${tag.title}_delete_icon"),
                )
            }
        },
        selected = false,
        enabled = deletable,
        label = { Text(tag.title.lowercase()) },
        modifier = modifier then Modifier.testTag("tag_${tag.title}").semantics { contentDescription = "Tag Chip" },
    )
}

@Preview
@Composable
private fun RegularTagPreview() {
    Surface {
        Tag(tag = TaggedItemApi(id = 1, title = "Explicit Tag", implicitFrom = null), requestDelete = {})
    }
}

@Preview
@Composable
private fun ImplicatedTagPreview() {
    Surface {
        Tag(tag = TaggedItemApi(id = 1, title = "Implicit Tag", implicitFrom = 2), requestDelete = { })
    }
}
