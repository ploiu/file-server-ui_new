package dev.ploiu.file_server_ui_new.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.AwtWindow
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

@Composable
fun OpenFileDialog(
    parent: Frame? = null,
    onCloseRequest: (Array<File>) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            init {
                isMultipleMode = true
            }

            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                // this might look unintuitive, but the above setVisible call is blocking until the window is closed
                if (value) {
                    onCloseRequest(files)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
fun SaveFileDialog(
    file: FileApi,
    onClosed: (PlatformFile?) -> Unit
) {
  val launcher = rememberFileSaverLauncher(onResult = onClosed)
    launcher.launch(suggestedName = file.nameWithoutExtension, extension = file.extension)
    TODO(
        """
        Look at Filekit:
        - https://filekit.mintlify.app/core/setup
        - https://filekit.mintlify.app/core/platform-file
        - Kotlin IO? (https://github.com/Kotlin/kotlinx-io)
        - 
    """.trimIndent()
    )
}

@Composable
fun SaveFolderDialog(
    onClosed: (PlatformFile?) -> Unit
) {
    val launcher = rememberDirectoryPickerLauncher(onResult = onClosed)
    launcher.launch()
}
