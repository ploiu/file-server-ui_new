package dev.ploiu.file_server_ui_new.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
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
    return remember(flowTileWidth, latestContent) {
        // initial composition
        KindaLazyContent(flowTileWidth) {}
    }.apply {
        // and update internally whenever latestContent changes
        apply(latestContent.value)
    }
}

/**
 * allows controlling the scrolling of a [KindaLazyVerticalGrid]
 */
sealed interface KindaLazyScrollState {
    suspend fun animateScrollTo(location: Int)
    suspend fun stopScroll()
}

/**
 * Internal implementation of [KindaLazyScrollState]. This wraps the scroll trackers for the wrapper element and the internal lazy grid, and delegates scroll requests the caller sends accordingly
 */
private class KindaLazyScrollStateImpl : KindaLazyScrollState {
    /** the scroll state of the wrapper element */
    lateinit var rootScroll: ScrollState

    /** the scroll state of the lazy grid **/
    lateinit var gridScroll: LazyGridState

    /** the height of the permanent child container, used to know which element to hand scrolling off to */
    var permanentHeight: Int = 0

    /**
     * scrolls to the position in the list.
     *
     * Note that position does _not_ mean item position, but the position of the scroller in pixels
     */
    override suspend fun animateScrollTo(location: Int) {
        val currentPosition = rootScroll.value
        // are we scrolling towards the permanent items?
        if (location < currentPosition) {
            // if the lazy items can't scroll up any further, scroll our root scroller
            if (!gridScroll.canScrollBackward) {
                rootScroll.animateScrollTo(location)
            } else {
                gridScroll.animateScrollToItem(0)
            }
        }
    }

    override suspend fun stopScroll() {
        gridScroll.stopScroll()
        rootScroll.stopScroll()
    }
}

@Composable
fun rememberKindaLazyScrollState(): KindaLazyScrollState = remember { KindaLazyScrollStateImpl() }

/**
 * Combines a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] with a list of items that _always_ render
 */
@Composable
fun KindaLazyVerticalGrid(
    /** how big you want each column to be. The columns will never be smaller than this, but they may grow a bit */
    minColumnWidth: Dp,
    /** modifies the wrapper composable */
    modifier: Modifier = Modifier,
    /** modifies the internal [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] */
    lazyModifier: Modifier = Modifier,
    /** modifies the grid of items that always stays in the composition tree */
    activeModifier: Modifier = Modifier,
    /** used to programmatically control the scroll state of this component */
    scrollState: KindaLazyScrollState = rememberKindaLazyScrollState(),
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    /** The contents of this grid. The first param dictates the children that always render, the second param dictates how the lazy children render */
    content: KindaLazyScope.() -> Unit,
) {
    val columns = GridCells.Adaptive(minColumnWidth)
    val wrapperScrollState = rememberScrollState()
    val lazyState = rememberLazyGridState()
    // need to track the height of the permanent items wrapper so that we know when to enable / disable lazy scrolling
    var permanentHeight by remember { mutableIntStateOf(0) }

    // we're kind of dancing around ownership and how references get passed here. We can never re-init the scroll state passed here or else that severs the connection between us and the caller
    (scrollState as KindaLazyScrollStateImpl).apply {
        this.rootScroll = wrapperScrollState
        this.gridScroll = lazyState
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        val density = LocalDensity.current
        // the permanent items need to be sized and spaced the same as the lazy ones
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
        LaunchedEffect(permanentItems) {
            if (permanentItems == null) {
                permanentHeight = 0
            }
        }
        LaunchedEffect(permanentHeight) {
            scrollState.permanentHeight = permanentHeight
        }

        // not inside the above LaunchedEffect because we look at the scroll state which can change a lot
        val canScrollLazy = remember(permanentHeight) {
            derivedStateOf {
                wrapperScrollState.value > permanentHeight
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(wrapperScrollState) then modifier,
        ) {
            if (permanentItems != null) {
                // TODO use onGloballyPositioned to get the height when screen size changes
                FlowRow(
                    modifier = Modifier.fillMaxWidth().testTag("activeRow").padding(
                        start = horizontalArrangement.spacing,
                        // bottom is cut in half because the lazy grid will also have top padding due to contentPadding
                        bottom = verticalArrangement.spacing / 2,
                    ).onGloballyPositioned { permanentHeight = it.size.height } then activeModifier,
                    horizontalArrangement = horizontalArrangement,
                    verticalArrangement = verticalArrangement,
                    content = permanentItems,
                )
            }
            if (lazyItems != null) {
                LazyVerticalGrid(
                    userScrollEnabled = canScrollLazy.value,
                    columns = columns,
                    modifier = Modifier.fillMaxWidth().heightIn(max = viewportHeight) then lazyModifier,
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

/**
 * calculates and `remember`s how wide each cell needs to be in the row of permanent items
 */
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
            val containerWidthPx = maxWidth.toPx()
            val itemGapPx = arrangement.spacing.toPx()
            // only left because the parent does not apply right padding
            val containerPaddingPx = contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() + contentPadding
                .calculateRightPadding(LayoutDirection.Ltr)
                .toPx()
            val minItemWidthPx = minWidth.toPx()
            val usableContainerWidthPx = containerWidthPx - containerPaddingPx

            /** each item doesn't take up just its own width, but its width + the gap. both numerator and denominator
            having `+ itemGapPx` doesn't make sense to me intuitively, but when I do the math on paper I can see how the formula can be arranged to this
            I looked up how to do this math somewhere but can't find where...oh well, it's probably common knowledge
            basically, `W >= n * i + s(n - 1)`, where W = usable width, n = # of items, i = min width, s = gap size
            this becomes `W >= n * i + sn - s` -> `W + s >= n * i + sn` -> `(s/n) + (W/n) >= i + s` etc etc etc. We're only solving for n, we know all the other variables*/
            val itemsPerRow =
                max(1f, floor((usableContainerWidthPx + itemGapPx) / (minItemWidthPx + itemGapPx))).toInt()
            val sumRowGapSizePx = itemGapPx * (itemsPerRow - 1)
            val sumItemSizePx = usableContainerWidthPx - sumRowGapSizePx
            val realItemSizePx = sumItemSizePx / itemsPerRow
            realItemSizePx.dp
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
        modifier = Modifier.fillMaxSize(),
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
