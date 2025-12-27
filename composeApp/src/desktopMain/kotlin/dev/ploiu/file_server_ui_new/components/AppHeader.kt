package dev.ploiu.file_server_ui_new.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.material3.IconButtonDefaults.iconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.ploiu.file_server_ui_new.viewModel.SearchResultsRoute

// TODO probably should pull this out into its own general purpose tooltip
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ButtonTooltip(tooltipText: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            Surface(
                tonalElevation = 10.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(tooltipText, modifier = Modifier.padding(3.dp))
            }
        },
    ) {
        content()
    }
}

@Composable
fun AppHeader(
    searchBarFocuser: FocusRequester,
    navController: NavController,
    sideSheetActive: Boolean,
    onCreateFolderClick: () -> Unit,
    onUploadFolderClick: () -> Unit,
    onUploadFileClick: () -> Unit,
    onSettingsClick: () -> Unit = { TODO("settings button not implemented") },
) {
    // setting some crazy high value but also using it for a max size will make sure we can still animate without a weird gap
    val actionMenuMaxWidth = animateIntAsState(if (sideSheetActive) 0 else 300, animationSpec = tween())
    val buttonOpacity = animateFloatAsState(targetValue = if (sideSheetActive) 0f else 1f)
    val buttonColors = iconButtonColors()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            // TODO button events
            Row(modifier = Modifier.alpha(buttonOpacity.value).widthIn(max = actionMenuMaxWidth.value.dp)) {
                ButtonTooltip("Create empty folder") {
                    IconButton(onClick = onCreateFolderClick, colors = buttonColors) {
                        Icon(Icons.Default.CreateNewFolder, "create new folder")
                    }
                }
                ButtonTooltip("Upload folder") {
                    IconButton(onClick = onUploadFolderClick, colors = buttonColors) {
                        Icon(Icons.Default.DriveFolderUpload, "upload folder")
                    }
                }
                ButtonTooltip("Upload file") {
                    IconButton(onClick = onUploadFileClick, colors = buttonColors) {
                        Icon(Icons.Default.UploadFile, "upload file")
                    }
                }
                // TODO remove this once the settings button is implemented
                ButtonTooltip("Settings (not implemented)") {
                    IconButton(onClick = onSettingsClick, colors = buttonColors, enabled = false) {
                        Icon(Icons.Default.Settings, "settings")
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) {
            FileServerSearchBar(
                focusRequester = searchBarFocuser,
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                navController.navigate(SearchResultsRoute(it))
            }
        }
    }
}
