@file:OptIn(ExperimentalTestApi::class)

package dev.ploiu.file_server_ui_new.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import io.mockk.called
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

private typealias TagCallback = (Collection<TaggedItemApi>) -> Unit

class TagListTests {

    @Test
    fun `should not show any tag chips if no tags`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        setContent {
            TagList(tags = listOf(), onUpdate = callback)
        }

        val tagMatcher = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Tag Chip"))
        onAllNodes(tagMatcher)
            .assertCountEquals(0)
    }

    @Test
    fun `should show a chip for each tag`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        val tags = listOf(
            TaggedItemApi(id = null, title = "tag1", implicitFrom = null),
            TaggedItemApi(id = null, title = "tag2", implicitFrom = null),
        )
        setContent {
            TagList(tags = tags, onUpdate = callback)
        }

        for ((_, title) in tags) {
            val tagNode = onNodeWithTag("tag_$title")
            tagNode.assertContentDescriptionContains("Tag Chip")
            tagNode.assertIsDisplayed()
            tagNode.assertTextEquals(title)
        }
    }

    @Test
    fun `should show the Add Tag Button`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        val tags = listOf(
            TaggedItemApi(id = null, title = "tag1", implicitFrom = null),
            TaggedItemApi(id = null, title = "tag2", implicitFrom = null),
        )
        setContent {
            TagList(tags = tags, onUpdate = callback)
        }
        val matcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        onAllNodes(matcher).onFirst().assertTextEquals("Add Tag")
    }

    @Test
    fun `clicking add tag button shows a text dialog`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        setContent {
            TagList(tags = listOf(), onUpdate = callback)
        }
        onNodeWithTag("addTag").performClick()
        val dialog = onNodeWithTag("textDialog")
        val dialogTitle = onNodeWithText("Add tag")
        dialog.assertIsDisplayed()
        dialogTitle.assertIsDisplayed()
    }

    @Test
    fun `should not call update callback when cancel is clicked on tag name dialog`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        setContent {
            TagList(tags = listOf(), onUpdate = callback)
        }
        onNodeWithTag("addTag").performClick()
        val cancelButton = onNodeWithTag("textDialogCancelButton")
        cancelButton.performClick()
        verify { callback wasNot called }
    }

    @Test
    fun `should call update callback including the new tag when tag name is submitted`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        val tags = listOf(TaggedItemApi(id = 1, title = "original", implicitFrom = null))
        setContent {
            TagList(tags = tags, onUpdate = callback)
        }
        onNodeWithTag("addTag").performClick()
        onNodeWithTag("textDialogInput").performTextInput("new tag name")
        onNodeWithTag("textDialogConfirmButton").performClick()
        verify {
            callback(
                match {
                    it.contains(TaggedItemApi(id = 1, title = "original", implicitFrom = null))
                            && it.contains(TaggedItemApi(id = null, title = "new tag name", implicitFrom = null))
                },
            )
        }
    }

    // TODO this should be moved to desktop, with android getting a press and hold
    @Test
    fun `clicking on a tag pill should call update callback without that tag`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        val tags = listOf(
            TaggedItemApi(id = 1, title = "whatever", implicitFrom = null),
            TaggedItemApi(id = 2, title = "whatever2", implicitFrom = null),
        )
        setContent {
            TagList(tags = tags, onUpdate = callback)
        }
        onNodeWithTag("tag_whatever").performClick()
        verify(exactly = 1) {
            callback(
                match {
                    it.size == 1 && it.contains(
                        TaggedItemApi(
                            id = 2,
                            title = "whatever2",
                            implicitFrom = null,
                        ),
                    )
                },
            )
        }
    }

}
