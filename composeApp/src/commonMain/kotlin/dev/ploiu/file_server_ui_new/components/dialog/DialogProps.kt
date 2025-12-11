package dev.ploiu.file_server_ui_new.components.dialog

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface DialogProps<T> {
    val title: String
    val modifier: Modifier
    val bodyText: String?
    val icon: ImageVector?
    val iconColor: Color?
    val bodyColor: Color?
    val cancelText: String
    val confirmText: String
    val onCancel: () -> Unit
    val onConfirm: (T) -> Unit

    @Composable
    operator fun invoke()
}

data class PromptDialogProps(
    override val title: String,
    override val modifier: Modifier = Modifier,
    override val bodyText: String? = null,
    override val icon: ImageVector? = null,
    override val iconColor: Color? = null,
    override val bodyColor: Color? = null,
    override val cancelText: String = "Cancel",
    override val confirmText: String = "Confirm",
    override val onCancel: () -> Unit,
    override val onConfirm: (Unit) -> Unit,
) : DialogProps<Unit> {
    @Composable
    override operator fun invoke() = Dialog(
        title = title,
        text = bodyText,
        icon = icon,
        iconColor = iconColor ?: LocalContentColor.current,
        bodyColor = bodyColor ?: LocalContentColor.current,
        dismissText = cancelText,
        confirmText = confirmText,
        onDismissRequest = onCancel,
        onConfirm = { onConfirm(Unit) },
    )
}

data class TextDialogProps(
    override val title: String,
    override val modifier: Modifier = Modifier,
    override val bodyText: String? = null,
    override val icon: ImageVector? = null,
    override val iconColor: Color? = null,
    override val bodyColor: Color? = null,
    override val cancelText: String = "Cancel",
    override val confirmText: String = "Confirm",
    val defaultValue: String = "",
    override val onCancel: () -> Unit,
    override val onConfirm: (String) -> Unit,
) : DialogProps<String> {
    @Composable
    override fun invoke() = TextDialog(
        title = title,
        modifier = modifier,
        bodyText = bodyText,
        defaultValue = defaultValue,
        onCancel = onCancel,
        onConfirm = onConfirm,
        bodyColor = bodyColor,
        cancelText = cancelText,
        confirmText = confirmText,
    )
}


