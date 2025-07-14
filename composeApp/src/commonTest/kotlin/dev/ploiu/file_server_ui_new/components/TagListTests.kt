@file:OptIn(ExperimentalTestApi::class)

package dev.ploiu.file_server_ui_new.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.fail

class TagListTests {
    @Test
    fun `should not show any tag chips if no tags`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `should show a chip for each tag`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `should show the Add Tag Button`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `clicking add tag button shows a text dialog`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `should not call update callback when cancel is clicked on tag name dialog`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `should call update callback including the new tag when tag name is submitted`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `should no longer show dialog when tag name dialog is submitted`() = runComposeUiTest {
        fail()
    }

    @Test
    fun `clicking on a tag pill should call update callback without that tag`() = runComposeUiTest {
        fail()
    }

}
