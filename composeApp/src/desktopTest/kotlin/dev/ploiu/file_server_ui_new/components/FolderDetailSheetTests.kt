@file:OptIn(ExperimentalTestApi::class)

package dev.ploiu.file_server_ui_new.components

import androidx.compose.ui.test.*
import dev.ploiu.file_server_ui_new.components.sidesheet.FolderDetailSheet
import dev.ploiu.file_server_ui_new.model.FolderApi
import dev.ploiu.file_server_ui_new.viewModel.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

class FolderDetailSheetTests {
    @Test
    fun `should show a loading spinner when state is loading`() = runComposeUiTest {
        val viewModel: FolderDetailViewModel = mockk()
        every { viewModel.state } returns MutableStateFlow(FolderDetailUiModel(FolderDetailLoading())).asStateFlow()
        every { viewModel.folderId } returns 1
        every { viewModel.loadFolder() } returns Job()
        setContent {
            FolderDetailSheet(viewModel, "", {}, {})
        }
        onNodeWithTag("spinner").assertIsDisplayed()
        onNodeWithTag("loadedRoot").assertDoesNotExist()
        onNodeWithTag("renameDialog").assertDoesNotExist()
        onNodeWithTag("deleteDialog").assertDoesNotExist()
        onNodeWithTag("message").assertDoesNotExist()
    }

    @Test
    fun `should show main body when folder is loaded`() = runComposeUiTest {
        val viewModel: FolderDetailViewModel = mockk()
        every { viewModel.state } returns MutableStateFlow(
            FolderDetailUiModel(
                FolderDetailLoaded(
                    FolderApi(
                        1,
                        0,
                        "/test",
                        "whatever",
                        listOf(),
                        listOf(),
                        listOf(),
                    ),
                ),
            ),
        ).asStateFlow()
        every { viewModel.folderId } returns 1
        every { viewModel.loadFolder() } returns Job()
        setContent {
            FolderDetailSheet(viewModel, "", {}, {})
        }

        onNodeWithTag("spinner").assertDoesNotExist()
        onNodeWithTag("loadedRoot").assertIsDisplayed()
        onNodeWithTag("message").assertDoesNotExist()
    }

    @Test
    fun `should show toast message and main folder body when there is a message`() = runComposeUiTest {
        val viewModel: FolderDetailViewModel = mockk()
        every { viewModel.state } returns MutableStateFlow(
            FolderDetailUiModel(
                FolderDetailMessage(
                    FolderApi(
                        1,
                        0,
                        "/test",
                        "whatever",
                        listOf(),
                        listOf(),
                        listOf(),
                    ),
                    "test message",
                ),
            ),
        ).asStateFlow()
        every { viewModel.folderId } returns 1
        every { viewModel.loadFolder() } returns Job()
        setContent {
            FolderDetailSheet(viewModel, "", {}, {})
        }
        val messageNode = onNodeWithTag("message")
        onNodeWithTag("loadedRoot").assertIsDisplayed()
        messageNode.assertIsDisplayed()
        messageNode.assertTextEquals("test message")
    }

    @Test
    fun `should show folder image and title when loaded`() = runComposeUiTest {
        val viewModel: FolderDetailViewModel = mockk()
        every { viewModel.state } returns MutableStateFlow(
            FolderDetailUiModel(
                FolderDetailLoaded(
                    FolderApi(
                        1,
                        0,
                        "/test",
                        "whatever",
                        listOf(),
                        listOf(),
                        listOf(),
                    ),
                ),
            ),
        ).asStateFlow()
        every { viewModel.folderId } returns 1
        every { viewModel.loadFolder() } returns Job()
        setContent {
            FolderDetailSheet(viewModel, "", {}, {})
        }
        val image = onNodeWithTag("folderImage", useUnmergedTree = true)
        val folderName = onNodeWithTag("folderName", useUnmergedTree = true)

        image.assertIsDisplayed()
        folderName.assertIsDisplayed()
        folderName.assertTextEquals("whatever")
    }
}

class FolderDetailSheetStatsTests

class FolderDetailSheetTagsTests

class FolderDetailSheetActionsPanelTests
