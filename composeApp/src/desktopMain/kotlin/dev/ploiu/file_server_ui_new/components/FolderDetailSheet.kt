package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailErrored
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailLoaded
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailLoading
import dev.ploiu.file_server_ui_new.viewModel.FolderDetailViewModel
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.draft
import file_server_ui_new.composeapp.generated.resources.folder
import org.jetbrains.compose.resources.painterResource

@Composable
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
            Row(modifier = Modifier.fillMaxWidth()) {
                // image and title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(Res.drawable.folder),
                        contentDescription = "folder icon",
                        Modifier.width(120.dp).height(120.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(pageState.folder.name, style = MaterialTheme.typography.headlineMedium)
                }
                // folder stats FIXME this will need to be rearranged and better thought out as screen sizes change
                Column {
                    Spacer(modifier = Modifier.height(30.dp))
                    Row {
                        // TODO this isn't what I want, and chip is supposedly interactive
                        BadgedBox(badge = { Badge { Text(pageState.folder.files.size.toString()) } }) {
                            Icon(painter = painterResource(Res.drawable.draft), contentDescription = "files")
                        }
                    }
                    Text("Number of folders: " + pageState.folder.folders.size)
                    Text("Path: ~/" + pageState.folder.path)
                }
            }
        }

        is FolderDetailErrored -> TODO()
    }
}
