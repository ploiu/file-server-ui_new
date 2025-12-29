package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp


@Composable
private fun FileServerSearchPlaceholder(focused: Boolean, text: String) {
    if (!focused && text.isBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Search")
            Spacer(modifier = Modifier.weight(1f))
            Text("Ctrl + K")
            Spacer(modifier = Modifier.width(8.dp))
        }
    } else {
        Text("Press Enter to Search")
    }
}

@Composable
fun FileServerSearchBar(focusRequester: FocusRequester, modifier: Modifier = Modifier, onSearch: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
        },
        label = { FileServerSearchPlaceholder(focused, text) },
        singleLine = true,
        modifier = Modifier.onFocusChanged { focused = it.isFocused }.focusRequester(focusRequester).onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyUp) {
                false
            } else {
                when (it.key) {
                    Key.Enter -> {
                        if (!text.isBlank()) {
                            onSearch(text)
                        }
                        true
                    }

                    else -> false
                }
            }
        } then modifier,
    )
}
