package dev.ploiu.file_server_ui_new.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    modifier: Modifier = Modifier,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
) {
    Dialog(onDismissRequest = { onCancel() }) {
        var inputText by remember { mutableStateOf(defaultValue) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth() then modifier) {
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
                    horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        maxLines = 1,
                        modifier = Modifier.onPreviewKeyEvent {
                            when (it.key) {
                                Key.Enter -> {
                                    onConfirm(inputText.trim())
                                    true
                                }

                                else -> false
                            }
                        }.focusRequester(focusRequester)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp)
                ) {
                    TextButton(onClick = onCancel) {
                        Text(cancelText)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { onConfirm(inputText.trim()) }) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}
