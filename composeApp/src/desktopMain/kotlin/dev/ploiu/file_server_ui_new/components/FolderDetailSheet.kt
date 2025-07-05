package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.extensions.uiCount
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailErrored
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailViewModel
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.draft
import file_server_ui_new.composeapp.generated.resources.folder
import org.jetbrains.compose.resources.painterResource

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun FolderDetailSheet(viewModel: FolderDetailViewModel, modifier: Modifier = Modifier) {
    val (pageState) = viewModel.state.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadFolder()
    }

    when (pageState) {
        is FolderDetailLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        is FolderDetailLoaded -> {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // image and title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(Res.drawable.folder),
                        contentDescription = "folder icon",
                        Modifier.width(108.dp).height(108.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(pageState.folder.name, style = MaterialTheme.typography.headlineSmall)
                }
                // folder stats FIXME this will need to be rearranged and better thought out as screen sizes change
                Spacer(Modifier.height(16.dp))
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Pill(
                            text = pageState.folder.folders.uiCount(),
                            icon = Icons.Default.Folder,
                            color = MaterialTheme.colorScheme.background,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Pill(
                            text = pageState.folder.files.uiCount(),
                            icon = Res.drawable.draft,
                            color = MaterialTheme.colorScheme.background
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Pill(
                            "~/" + pageState.folder.path,
                            color = MaterialTheme.colorScheme.background,
                            textStyle = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // tags TODO pull into shared component
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    val chipColors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        leadingIconContentColor = MaterialTheme.colorScheme.onTertiary
                    )
                    FlowRow(Modifier.padding(start = 16.dp, end = 16.dp)) {
                        for (tag in pageState.folder.tags) {
                            Chip(onClick = { println(tag) }, colors = chipColors, leadingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "close icon"
                                )
                            }) {
                                Text(tag.title)
                            }
                        }
                    }
                }
            }
        }

        is FolderDetailErrored -> TODO()
    }
}
