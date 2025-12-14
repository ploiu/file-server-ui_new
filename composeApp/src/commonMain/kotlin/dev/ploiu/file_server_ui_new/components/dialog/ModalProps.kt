package dev.ploiu.file_server_ui_new.components.dialog

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent

typealias ComposableColor = @Composable () -> Color

sealed interface ModalProps<T> {
    val title: String
    val modifier: Modifier
    val text: String?
    val icon: ImageVector?
    val iconColorProvider: ComposableColor?
    val bodyColorProvider: ComposableColor?

    @Composable
    operator fun invoke()
}

data class PromptModalProps(
    override val title: String,
    override val modifier: Modifier = Modifier,
    override val text: String? = null,
    override val icon: ImageVector? = null,
    override val iconColorProvider: ComposableColor? = null,
    override val bodyColorProvider: ComposableColor? = null,
    val cancelText: String = "Cancel",
    val confirmText: String = "Confirm",
    val onCancel: () -> Unit,
    val onConfirm: (Unit) -> Unit,
) : ModalProps<Unit> {
    @Composable
    override operator fun invoke() = Dialog(
        title = title,
        text = text,
        icon = icon,
        modifier = modifier.onPreviewKeyEvent { k ->
            if (k.key == Key.Escape) {
                onCancel()
                true
            } else {
                false
            }
        },
        iconColor = iconColorProvider?.invoke() ?: LocalContentColor.current,
        bodyColor = bodyColorProvider?.invoke() ?: LocalContentColor.current,
        dismissText = cancelText,
        confirmText = confirmText,
        onDismissRequest = onCancel,
        onConfirm = { onConfirm(Unit) },
    )
}

data class ErrorModalProps(
    override val title: String,
    override val modifier: Modifier = Modifier,
    override val text: String?,
    override val icon: ImageVector? = null,
    override val iconColorProvider: ComposableColor? = null,
    override val bodyColorProvider: ComposableColor? = null,
    val closeText: String = "Dismiss",
    val onClose: () -> Unit,
) : ModalProps<Nothing> {
    @Composable
    override fun invoke() {
        Dialog(
            title = title,
            modifier = modifier.onPreviewKeyEvent { k ->
                if (k.key == Key.Escape) {
                    onClose()
                    true
                } else {
                    false
                }
            },
            text = text,
            icon = icon,
            iconColor = iconColorProvider?.invoke() ?: LocalContentColor.current,
            bodyColor = bodyColorProvider?.invoke() ?: LocalContentColor.current,
            dismissText = closeText,
            onDismissRequest = onClose,
        )
    }
}

data class TextModalProps(
    override val title: String,
    override val modifier: Modifier = Modifier,
    override val text: String? = null,
    override val icon: ImageVector? = null,
    override val iconColorProvider: ComposableColor? = null,
    override val bodyColorProvider: ComposableColor? = null,
    val defaultValue: String = "",
    val cancelText: String = "Cancel",
    val confirmText: String = "Confirm",
    val onCancel: () -> Unit,
    val onConfirm: (String) -> Unit,
) : ModalProps<String> {
    @Composable
    override fun invoke() = TextDialog(
        title = title,
        modifier = modifier.onPreviewKeyEvent { k ->
            if (k.key == Key.Escape) {
                onCancel()
                true
            } else {
                false
            }
        },
        bodyText = text,
        defaultValue = defaultValue,
        onCancel = onCancel,
        onConfirm = onConfirm,
        bodyColor = bodyColorProvider?.invoke() ?: LocalContentColor.current,
        cancelText = cancelText,
        confirmText = confirmText,
    )
}


