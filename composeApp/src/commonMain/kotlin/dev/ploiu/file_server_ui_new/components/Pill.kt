package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.extensions.uiCount
import org.jetbrains.compose.ui.tooling.preview.Preview


/** implemented this way after seeing how colors were implemented for Buttons And Chips*/
class PillColors(
    val backgroundColor: Color,
    val contentColor: Color,
    val iconColor: Color,
)

@Composable
fun pillColors(
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
        .compositeOver(MaterialTheme.colorScheme.surface),
    contentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = .87f),
    iconColor: Color = contentColor.copy(alpha = .54f)
) = PillColors(backgroundColor, contentColor, iconColor)

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    colors: PillColors = pillColors(),
    icon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
        Row(
            modifier = Modifier.background(color = colors.backgroundColor, shape = MaterialTheme.shapes.extraSmall)
                .then(modifier).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.iconColor, content = icon)
            }
            Spacer(Modifier.width(3.dp))
            Text(text = text, style = textStyle)
        }

    }
}

@Preview
@Composable
private fun DefaultPill() {
    Pill("test")
}

@Preview
@Composable
private fun PillWithIcon() {
    Pill(text = "with icon", icon = { Icon(Icons.Default.Preview, "Preview") })
}


@Preview
@Composable
private fun PillWithColoredText() {
    Pill(
        text = "colored text",
        colors = pillColors(
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}

@Preview
@Composable
private fun PillWithIconAndColoredText() {
    Pill(
        text = "with colored icon and text",
        icon = { Icon(Icons.Default.Close, "close") },
        colors = pillColors(
            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}


@Preview
@Composable
private fun PillsWithLargeNumber() {
    val small = (0 until 1_000).toList()
    val med = (0 until 1_500).toList()
    val lg = (0 until 11_000).toList()
    Row {
        Pill(text = small.uiCount(), icon = { Icon(Icons.Default.Folder, "Folder") })
        Pill(text = med.uiCount())
        Pill(text = lg.uiCount())
    }
}
