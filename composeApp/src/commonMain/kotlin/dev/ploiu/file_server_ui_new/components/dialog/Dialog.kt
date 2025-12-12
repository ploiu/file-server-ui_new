package dev.ploiu.file_server_ui_new.components.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun Dialog(
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector?,
    iconColor: Color = LocalContentColor.current,
    bodyColor: Color = LocalContentColor.current,
    onDismissRequest: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = "Confirm",
    dismissText: String = "Dismiss",
) {
    AlertDialog(
        modifier = modifier,
        icon = {
            if (icon != null) {
                Icon(icon, tint = iconColor, contentDescription = "Header icon for modal")
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            if (text != null) {
                val style = MaterialTheme.typography.bodyLarge.copy(color = bodyColor)
                Text(text = text, style = style)
            }
        },
        onDismissRequest = {
            if (onDismissRequest != null) {
                onDismissRequest()
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                TextButton(
                    onClick = {
                        onConfirm()
                    },
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            if (onDismissRequest != null) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                    },
                ) {
                    Text(dismissText)
                }
            }
        },
    )
}
