package dev.ploiu.file_server_ui_new

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import dev.ploiu.file_server_ui_new.model.FolderChild
import kotlinx.coroutines.delay
import java.awt.MouseInfo
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

sealed interface DragStatus
object NotDragging : DragStatus
data class IsDragging(val isFilesystem: Boolean, val offset: Offset = Offset.Zero) : DragStatus

object CustomDataFlavors {
    val FOLDER_CHILD = DataFlavor(FolderChild::class.java, "application/x-ploiu-file-server-folder-child")
}

class FolderChildSelection(private val data: FolderChild) : Transferable {
    private companion object {
        val flavors: Array<DataFlavor> = arrayOf(CustomDataFlavors.FOLDER_CHILD)
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?> = flavors.clone()

    override fun isDataFlavorSupported(p0: DataFlavor?) = p0 in flavors

    override fun getTransferData(p0: DataFlavor?): Any {
        if (p0 !in flavors) {
            throw UnsupportedOperationException("flavor $p0 is not a supported flavor for FolderChildSelection")
        }
        return data
    }
}

/** Sugar class used to represent the distance from a screen edge the mouse cursor is */
data class MouseInsideWindow(
    val fromLeftEdge: UInt,
    val fromRightEdge: UInt,
    val fromTop: UInt,
    val fromBottom: UInt,
    private val windowWidth: UInt,
    private val windowHeight: UInt,
) {

    /** accounting for the screen width, how far the cursor is from the left of the screen in percentage */
    val percentFromLeft: Float
        get() = fromLeftEdge.toFloat() / windowWidth.toFloat()

    /** accounting for the screen width, how far the cursor is from the right of the screen in percentage */
    val percentFromRight: Float
        get() = fromRightEdge.toFloat() / windowWidth.toFloat()

    /** accounting for the screen height, how far the cursor is from the top of the screen in percentage */
    val percentFromTop: Float
        get() = fromTop.toFloat() / windowHeight.toFloat()

    /** accounting for the screen height, how far the cursor is from the bottom of the screen in percentage */
    val percentFromBottom: Float
        get() = fromBottom.toFloat() / windowHeight.toFloat()

    companion object {
        val Invalid = MouseInsideWindow(
            UInt.MAX_VALUE,
            UInt.MAX_VALUE,
            UInt.MAX_VALUE,
            UInt.MAX_VALUE,
            UInt.MAX_VALUE,
            UInt.MAX_VALUE,
        )
    }
}

@Composable
fun rememberMousePos(window: ComposeWindow): State<MouseInsideWindow> {
    val offset = remember { mutableStateOf(MouseInsideWindow.Invalid) }
    LaunchedEffect(window) {
        while (true) {
            if (window.isActive) {
                val mouse = MouseInfo.getPointerInfo().location
                val winLocation = window.locationOnScreen
                // if the mouse is offscreen, return zero
                if (mouse.x < winLocation.x || mouse.x > winLocation.x + window.width) {
                    offset.value = MouseInsideWindow.Invalid
                } else if (mouse.y < winLocation.y || mouse.y > winLocation.y + window.height) {
                    offset.value = MouseInsideWindow.Invalid
                } else {
                    val distanceFromLeft = mouse.x - winLocation.x
                    val distanceFromRight = (winLocation.x + window.width) - mouse.x
                    val distanceFromTop = mouse.y - winLocation.y
                    val distanceFromBottom = (winLocation.y + window.height) - mouse.y
                    offset.value = MouseInsideWindow(
                        fromLeftEdge = distanceFromLeft.toUInt(),
                        fromRightEdge = distanceFromRight.toUInt(),
                        fromTop = distanceFromTop.toUInt(),
                        fromBottom = distanceFromBottom.toUInt(),
                        windowWidth = window.width.toUInt(),
                        windowHeight = window.height.toUInt(),
                    )
                }
            }
            // live mouse tracking isn't super important, so give lots of time to do other things
            delay(100)
        }
    }
    return offset
}


