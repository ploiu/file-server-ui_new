package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.ploiu.file_server_ui_new.model.FileApi
import dev.ploiu.file_server_ui_new.model.FolderApi
import kotlin.math.floor
import kotlin.math.max

sealed class KindaLazyScope(val columnWidth: Dp) {
    var permanentItems: (@Composable FlowRowScope.() -> Unit)? = null
    var lazyItems: (LazyGridScope.() -> Unit)? = null
}

class KindaLazyContent(tileWidth: Dp, content: KindaLazyScope.() -> Unit) : KindaLazyScope(tileWidth) {
    init {
        apply(content)
    }
}

/**
 * adapted from [androidx.compose.foundation.lazy.grid.LazyGridItemProvider]
 */
@Composable
fun rememberKindaLazyGridItemProviderLambda(flowTileWidth: Dp, content: KindaLazyScope.() -> Unit): KindaLazyContent {
    // update the content lambda whenever it changes items
    val latestContent = rememberUpdatedState(content)
    return remember {
        // initial composition
        KindaLazyContent(flowTileWidth) {}
    }.apply {
        // and update internally whenever latestContent changes
        apply(latestContent.value)
    }
}

/**
 * Combines a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] with a list of items that _always_ render
 */
@Composable
fun KindaLazyVerticalGrid(
    minColumnWidth: Dp,
    /** modifies the wrapper composable */
    modifier: Modifier = Modifier,
    /** modifies the internal [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] */
    lazyModifier: Modifier = Modifier,
    /** modifies the grid of items that always stays in the compose tree */
    activeModifier: Modifier = Modifier,
    lazyState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    /** The contents of this grid. The first param dictates the children that always render, the second param dictates how the lazy children render */
    content: KindaLazyScope.() -> Unit,
) {
    val columns = GridCells.Adaptive(minColumnWidth)
    val wrapperScroll = rememberScrollState(0)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                val wrapperCanScroll =
                    (deltaY < 0 && wrapperScroll.canScrollForward) || (deltaY > 0 && wrapperScroll.canScrollBackward)
                return if (wrapperCanScroll) {
                    val consumed = wrapperScroll.dispatchRawDelta(deltaY)
                    Offset(0f, consumed)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                val consumedByGrid = lazyState.dispatchRawDelta(deltaY)
                return Offset(0f, consumedByGrid)
            }
        }
    }

    /*
        Scroll rules:
        1. permanent items need to scroll first
            a. while permanent items are not scrolled off the screen, lazy items cannot scroll
        2. lazy items have scrolling enabled when permanent items are done scrolling
     */
    BoxWithConstraints(modifier = Modifier.fillMaxSize().border(width = 2.dp, color = Color.Red)) {
        val viewportHeight = maxHeight
        val density = LocalDensity.current
        // the permanent items need to be sized and spaced the same as the lazy ones
        // TODO cleanup and put inside a remember
        val cellWidth = rememberPermanentFlowCellWidth(
            density = density,
            minWidth = minColumnWidth,
            contentPadding = contentPadding,
            maxWidth = maxWidth,
            arrangement = horizontalArrangement,
        )

        val memoizedContent = rememberKindaLazyGridItemProviderLambda(cellWidth, content)
        val permanentItems = memoizedContent.permanentItems
        val lazyItems = memoizedContent.lazyItems

        Column(
            modifier = Modifier.fillMaxSize()
                .border(width = 4.dp, color = Color.Yellow)
                    // TODO this can scroll even if there's only 1 screen's worth of content, period
                .verticalScroll(wrapperScroll) then modifier,
        ) {
            if (permanentItems != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag("activeRow").padding(
                        start = horizontalArrangement.spacing,
                        // bottom is cut in half because the lazy grid will also have top padding due to contentPadding
                        bottom = verticalArrangement.spacing / 2,
                    ).border(width = 8.dp, color = Color.White) then activeModifier,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                    content = permanentItems,
                )
            }
            if (lazyItems != null) {
                LazyVerticalGrid(
                    userScrollEnabled = false,
                    columns = columns,
                    modifier = Modifier.fillMaxWidth().height(viewportHeight) then lazyModifier,
                    state = lazyState,
                    contentPadding = contentPadding,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement,
                    content = lazyItems,
                )
            }
        }
    }
}

@Composable
private fun rememberPermanentFlowCellWidth(
    density: Density,
    minWidth: Dp,
    contentPadding: PaddingValues,
    maxWidth: Dp,
    arrangement: Arrangement.Horizontal,
): Dp {
    return remember(density, minWidth, contentPadding, maxWidth, arrangement) {
        with(density) {
            val maxWidthPx = maxWidth.toPx()
            val horizontalSpacing = arrangement.spacing.toPx()
            val paddingPx = contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() + contentPadding
                .calculateRightPadding(LayoutDirection.Ltr)
                .toPx()
            val minWidthPx = minWidth.toPx()
            // account for our min width and in-between spacing to determine how many cells we can have (rounded down)
            val numberOfItemsPerRow =
                max(floor((maxWidthPx + horizontalSpacing) / (minWidthPx + horizontalSpacing)).toInt(), 1)
            val numberOfGaps = numberOfItemsPerRow - 1
            val totalHorizontalSpacing = horizontalSpacing * numberOfGaps
            ((maxWidthPx - paddingPx - totalHorizontalSpacing) / numberOfItemsPerRow).dp
        }
    }
}

@Preview
@Composable
private fun WithPermanentItems() {
    KindaLazyVerticalGrid(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
        ),
        minColumnWidth = 150.dp,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().border(width = 2.dp, color = Color.Green),
    ) {
        val baseFolder = FolderApi(
            id = 0L,
            parentId = 0L,
            path = "/whatever",
            name = "some folder",
            folders = listOf(),
            files = listOf(),
            tags = listOf(),
        )
        val folders = (1..5L).map { baseFolder.copy(id = it) }
        val baseFile = FileApi(
            id = 0L,
            folderId = 0L,
            name = "some file",
            tags = listOf(),
            size = 0L,
            dateCreated = "",
            fileType = "application",
        )
        val files = (1..100L).map { baseFile.copy(id = it) }
        permanentItems = @Composable {
            for (folder in folders) {
                FolderEntry(
                    folder = folder,
                    onClick = {},
                )
            }
        }

        lazyItems = {
            items(files) {
                FileEntry(
                    file = it,
                )
            }
        }
    }
}
