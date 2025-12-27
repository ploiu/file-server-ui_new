@file:OptIn(ExperimentalTestApi::class)

package dev.ploiu.file_server_ui_new.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import dev.ploiu.file_server_ui_new.model.TaggedItemApi
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

private typealias TagCallback = (Collection<TaggedItemApi>) -> Unit

class TagListTests {

    @Test
    fun `should not show any tag chips if no tags`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        setContent {
            TagList(tags = listOf(), onAddClick = {}, onDelete = callback)
        }

        val tagMatcher = SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Tag Chip"))
        onAllNodes(tagMatcher).assertCountEquals(0)
    }

    @Test
    fun `should show a chip for each tag`() = runComposeUiTest {
        val callback = mockk<TagCallback>(relaxed = true)
        val tags = listOf(
            TaggedItemApi(id = null, title = "tag1", implicitFrom = null),
            TaggedItemApi(id = null, title = "tag2", implicitFrom = null),
        )
        setContent {
            TagList(tags = tags, onAddClick = {}, onDelete = callback)
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
            TagList(tags = tags, onAddClick = {}, onDelete = callback)
        }
        val matcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        onAllNodes(matcher).onFirst().assertTextEquals("Add Tag")
    }

    @Test
    fun `clicking add tag button calls the add tag click callback`() = runComposeUiTest {
        val callback = mockk<() -> Unit>(relaxed = true)
        setContent {
            TagList(tags = listOf(), onAddClick = callback, onDelete = {})
        }
        onNodeWithTag("addTag").performClick()
        verify(exactly = 1) {
            callback()
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
            TagList(tags = tags, onAddClick = {}, onDelete = callback)
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
