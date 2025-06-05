package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FolderApi
import file_server_ui_new.composeapp.generated.resources.Res
import file_server_ui_new.composeapp.generated.resources.folder
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.Modifier



@Composable
fun FolderEntry(folder: FolderApi, onClick: (f: FolderApi) -> Unit) {
    Surface(tonalElevation = 5.dp, modifier = Modifier.padding(8.dp), onClick = { onClick(folder) }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(Res.drawable.folder), contentDescription = "folder icon")
            Text(folder.name, textAlign = TextAlign.Center)
        }
    }
}
