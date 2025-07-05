package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.extensions.uiCount
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.draft
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.util.stream.IntStream
import kotlin.streams.toList


@Composable
private fun PillRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.background(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.extraSmall
        ).then(modifier).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        content()
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    icon: ImageVector,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    PillRow(modifier) {
        Icon(imageVector = icon, contentDescription = "pill icon", tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall + textStyle, color = color)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    icon: DrawableResource,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    PillRow(modifier) {
        Icon(painter = painterResource(icon), contentDescription = "pill icon", tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall + textStyle, color = color)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall
) {
    PillRow(modifier) {
        Text(text, style = MaterialTheme.typography.labelSmall + textStyle, color = color)
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
    Pill(text = "with icon", icon = Icons.Default.Preview)
}


@Preview
@Composable
private fun PillWithColoredText() {
    Pill(text = "colored text", color = MaterialTheme.colorScheme.tertiary)
}

@Preview
@Composable
private fun PillWithIconAndColoredText() {
    Pill(text = "with colored icon and text", icon = Icons.Default.Close, color = MaterialTheme.colorScheme.surface)
}


@Preview
@Composable
private fun PillsWithLargeNumber() {
    val small = (0 until 1_000).toList()
    val med = (0 until 1_500).toList()
    val lg = (0 until 11_000).toList()
    Row {
        Pill(text = small.uiCount(), icon = Icons.Default.Folder)
        Pill(text = med.uiCount())
        Pill(text = lg.uiCount())
    }
}
