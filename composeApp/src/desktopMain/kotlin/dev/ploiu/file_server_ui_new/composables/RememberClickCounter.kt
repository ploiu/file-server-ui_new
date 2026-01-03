package dev.ploiu.file_server_ui_new.composables

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

/**
 * Used to detect single vs double-click on something.
 *
 * Usage:
 * ```kt
 *  val clickCounter = rememberClickCounter(onSingleClick = {/*...*/}, onDoubleClick = {/*...*/})
 * // ...
 * SomeComposable(onClick = clickCounter)
 * ```
 */
@Composable
fun rememberClickCounter(onSingleClick: () -> Unit, onDoubleClick: () -> Unit): () -> Unit {
    val singleCallback = rememberUpdatedState(onSingleClick)
    val doubleCallback = rememberUpdatedState(onDoubleClick)

    var clickTimer by remember { mutableLongStateOf(-1) }
    var clickCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(clickTimer) {
        when (clickTimer) {
            -1L -> { // initial load, don't do anything
                return@LaunchedEffect
            }

            0L -> { // timer ended
                if (clickCount == 1) {
                    singleCallback.value.invoke()
                } else if (clickCount > 1) {
                    doubleCallback.value.invoke()
                }
                clickCount = 0
            }

            else -> { // timer started
                delay(clickTimer)
                clickTimer = 0
            }
        }
    }

    return remember(singleCallback, doubleCallback) {
        {
            clickCount += 1
            // make sure to only start the timer if it's not started
            if (clickTimer <= 0L) {
                clickTimer = 200L
            }
        }
    }
}
