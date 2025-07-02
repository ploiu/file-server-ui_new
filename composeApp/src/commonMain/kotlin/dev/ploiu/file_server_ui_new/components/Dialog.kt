package dev.ploiu.file_server_ui_new.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun Dialog(
    title: String,
    text: String? = null,
    icon: ImageVector,
    iconColor: Color = LocalContentColor.current,
    onDismissRequest: (() -> Unit)? = null,
    onConfirmation: (() -> Unit)? = null,
) {
    AlertDialog(
        icon = {
            Icon(icon, tint = iconColor, contentDescription = "Header icon for modal")
        },
        title = {
            Text(text = title)
        },
        text = {
            if (text != null) {
                Text(text = text)
            }
        },
        onDismissRequest = {
            if (onDismissRequest != null) {
                onDismissRequest()
            }
        },
        confirmButton = {
            if (onConfirmation != null) {
                TextButton(
                    onClick = {
                        onConfirmation()
                    }
                ) {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            if (onDismissRequest != null) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }
                ) {
                    Text("Dismiss")
                }
            }
        }
    )
}
