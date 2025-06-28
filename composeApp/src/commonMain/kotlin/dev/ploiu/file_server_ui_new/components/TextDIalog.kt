package dev.ploiu.file_server_ui_new.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun TextDialog(
    title: String,
    bodyText: String? = null,
    defaultValue: String = "",
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
    bodyColor: Color? = null,
) {
    Dialog(onDismissRequest = { onCancel() }) {
        var inputText by remember { mutableStateOf(defaultValue) }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (bodyText != null) {
                    val baseStyle = MaterialTheme.typography.bodyLarge
                    val style = if (bodyColor != null) {
                        baseStyle.copy(color = bodyColor)
                    } else {
                        baseStyle
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = bodyText,
                            modifier = Modifier.fillMaxWidth(),
                            style = style,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // TODO cancel and confirm buttons, enter key submit on text field
            }
        }
    }
}
