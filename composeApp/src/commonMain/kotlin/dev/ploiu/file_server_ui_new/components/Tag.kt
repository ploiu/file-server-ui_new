package dev.ploiu.file_server_ui_new.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.InputChipDefaults.inputChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.ploiu.file_server_ui_new.model.TagApi

class TagChipColors(
    val containerColor: Color,
    val labelColor: Color,
    val iconColor: Color,
)

@Composable
private fun TagChipColors.into() = inputChipColors(
    containerColor = containerColor,
    labelColor = labelColor,
    leadingIconColor = iconColor
)

// TODO this needs to be expect because Android will need a press and hold for deleting
@Composable
fun Tag(
    tag: TagApi,
    modifier: Modifier = Modifier,
    colors: TagChipColors = TagChipColors(
        containerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .12f),
        labelColor = LocalContentColor.current,
        iconColor = LocalContentColor.current
    ),
    requestDelete: (TagApi) -> Unit,
) {
    InputChip(
        // TODO for android this needs to be pulled out into something that gets pressed and held to be deleted. Use expect fun
        onClick = { requestDelete(tag) },
        colors = colors.into(),
        leadingIcon = { Icon(Icons.Default.Close, contentDescription = "close icon") },
        selected = false,
        label = { Text(tag.title.lowercase()) },
        modifier = modifier then Modifier.testTag("tag_${tag.title}").semantics { contentDescription = "Tag Chip" }
    )
}
