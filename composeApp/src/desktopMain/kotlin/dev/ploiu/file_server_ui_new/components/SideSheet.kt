package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** attempts to adhere to a modified (read, for my purposes)
 * [Material Standard Side Sheet Specification] (https://m3.material .io/components/side-sheets/overview),
 * since it is currently not implemented in compose multiplatform */
// TODO escape key close?, somehow sending title to this thing from a child component (probably a data class tbh)
@Composable
fun StandardSideSheet(
    title: String,
    modifier: Modifier = Modifier,
    onCloseAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(tonalElevation = 1.dp, modifier = modifier.then(Modifier.fillMaxHeight())) {
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp)) {
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCloseAction) {
                    Icon(Icons.Default.Close, contentDescription = "Close Button")
                }
            }
            content.invoke()
        }
    }
}
