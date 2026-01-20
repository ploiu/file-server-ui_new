package dev.ploiu.file_server_ui_new.components.kindalazygrid

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.floor
import kotlin.math.max


/**
 * allows controlling the scrolling of a [KindaLazyVerticalGrid]
 */
sealed interface KindaLazyScrollState {
    suspend fun scrollToTop(speed: Float)

    suspend fun jumpToTop()

    suspend fun jumpToBottom()

    suspend fun scrollToBottom(speed: Float)

    suspend fun stopScroll()
}

sealed class KindaLazyScope<P, L>(val columnWidth: Dp) {
    var permanentTemplate: (@Composable (P) -> Unit)? = null
    var lazyTemplate: (@Composable (L) -> Unit)? = null
}

@Composable
fun rememberKindaLazyScrollState(vararg keys: Any): KindaLazyScrollState = remember(keys) { KindaLazyScrollStateImpl() }

/**
 * Combines a [LazyVerticalGrid] with a list of items that _always_ render
 */
@Composable
fun <P, L> KindaLazyVerticalGrid(
    /** how big you want each column to be. The columns will never be smaller than this, but they may grow a bit */
    minColumnWidth: Dp,
    /** modifies the wrapper composable */
    modifier: Modifier = Modifier,
    /** modifies the internal [LazyVerticalGrid] */
    lazyModifier: Modifier = Modifier,
    /** modifies the grid of items that always stays in the composition tree */
    activeModifier: Modifier = Modifier,
    /** used to programmatically control the scroll state of this component */
    scrollState: KindaLazyScrollState,
    contentPadding: PaddingValues = PaddingValues.Zero,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    /** The children that should _always_ be permanent items. These children are _guaranteed_ to always remain in the composition tree until this composable leaves the tree */
    permanentChildren: List<P> = listOf(),
    /** The children that are _desired_ to be lazily-loaded. In order to make items flow better, some of these children _may_ be made permanent, depending on screen size, [minColumnWidth], [contentPadding], and [horizontalArrangement] */
    lazyChildren: List<L> = listOf(),
    /** The contents of this grid. The first param dictates the children that always render, the second param dictates how the lazy children render */
    content: KindaLazyScope<P, L>.() -> Unit,
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

    // not inside the above LaunchedEffect because we look at the scroll state which can change a lot
    val canScrollLazy = remember(permanentHeight) {
        derivedStateOf {
            wrapperScrollState.value >= permanentHeight
        }
    }

    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val atBottom = wrapperScrollState.value >= permanentHeight
                // if the flowRow is done scrolling, there's more scroll left to be done, _and_ the grid can scroll more, pass that scroll to the grid
                if (atBottom && available.y < 0 && lazyState.canScrollForward) {
                    runBlocking {
                        lazyState.scroll {
                            scrollBy(-available.y)
                        }
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero

            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        val density = LocalDensity.current
        // the permanent items need to be sized and spaced the same as the lazy ones
        val (cellWidth, cellsPerRow) = rememberPermanentFlowCellLayout(
            density = density,
            minWidth = minColumnWidth,
            contentPadding = contentPadding,
            maxWidth = maxWidth,
            arrangement = horizontalArrangement,
        )
        val memoizedContent = rememberKindaLazyGridItemProviderLambda(
            flowTileWidth = cellWidth,
            containerWidth = maxWidth,
            content = content,
            tilesPerRow = cellsPerRow,
            permanentChildren = permanentChildren,
            lazyChildren = lazyChildren.toMutableList(),
        )
        val permanentItems = memoizedContent.wrappedPermanentChildren
        val lazyItems = memoizedContent.wrappedLazyChildren
        LaunchedEffect(permanentItems) {
            if (permanentItems == null) {
                permanentHeight = 0
            }
        }
        LaunchedEffect(permanentHeight) {
            scrollState.permanentHeight = permanentHeight
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .nestedScroll(scrollConnection)
                .verticalScroll(wrapperScrollState) then modifier,
        ) {
            if (permanentItems != null) {
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
private fun rememberPermanentFlowCellLayout(
    density: Density,
    minWidth: Dp,
    contentPadding: PaddingValues,
    maxWidth: Dp,
    arrangement: Arrangement.Horizontal,
): Pair<Dp, Int> {
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
            Pair(realItemSizePx.toDp(), itemsPerRow)
        }
    }
}

/**
 * adapted from [LazyGridItemProvider]
 */
@Composable
private fun <P, L> rememberKindaLazyGridItemProviderLambda(
    flowTileWidth: Dp,
    tilesPerRow: Int,
    containerWidth: Dp,
    permanentChildren: List<P>,
    lazyChildren: MutableList<L>,
    content: KindaLazyScope<P, L>.() -> Unit,
): KindaLazyContent<P, L> {
    // update the content lambda whenever it changes items
    val latestContent = rememberUpdatedState(content)
    return remember(tilesPerRow, permanentChildren, lazyChildren, flowTileWidth, latestContent) {
        // if our permanent children isn't evenly divisible by tilesPerRow, we need to move some of the lazy children from the _beginning_ of the lazyChildren list
        val itemsToMove = tilesPerRow - (permanentChildren.size % tilesPerRow)
        val movedLazyChildren = lazyChildren.take(itemsToMove)
        val updatedLazyChildren = lazyChildren.drop(itemsToMove)
        // initial composition
        KindaLazyContent(
            tileWidth = flowTileWidth,
            permanentChildren = permanentChildren,
            movedLazyChildren = movedLazyChildren,
            lazyChildren = updatedLazyChildren,
            // initial composition has no children, because it stops errors on the first render (can't recompose a composable, which is what we'd be doing down below)
            // this is all to stop re-initializing a brand new KindaLazyContent on every recompose
            content = {},
        )
    }.apply {
        // and update internally whenever latestContent changes
        update(latestContent.value)
    }
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

    override suspend fun scrollToTop(speed: Float) = coroutineScope {
        gridScroll.scroll {
            while (gridScroll.canScrollBackward) {
                scrollBy(-speed)
                delay(25)
            }
        }
        rootScroll.scroll {
            while (rootScroll.canScrollBackward) {
                scrollBy(-speed)
                delay(25)
            }
        }
    }

    override suspend fun jumpToTop() {
        gridScroll.scrollToItem(0)
        rootScroll.animateScrollTo(0)
    }

    override suspend fun jumpToBottom() {
        val lazyItems = gridScroll.layoutInfo.totalItemsCount
        gridScroll.scrollToItem(lazyItems)
        rootScroll.animateScrollTo(rootScroll.maxValue)
    }

    override suspend fun scrollToBottom(speed: Float) {
        rootScroll.scroll {
            while (rootScroll.canScrollForward) {
                scrollBy(speed)
                delay(25)
            }
        }
        gridScroll.scroll {
            while (gridScroll.canScrollForward) {
                scrollBy(speed)
                delay(25)
            }
        }
    }

    override suspend fun stopScroll() {
        gridScroll.stopScroll()
        rootScroll.stopScroll()
    }
}

/**
 * the transformed version of [KindaLazyScope]
 */
private class KindaLazyContent<P, L>(
    /** how wide each flow row tile needs to be */
    tileWidth: Dp,
    /** The children that are _always_ permanent */
    private val permanentChildren: List<P>,
    /** The lazy children that, for layout reasons, are shifted to the permanent section */
    private val movedLazyChildren: List<L>,
    /** the adjusted items to render in the lazy grid */
    private val lazyChildren: List<L>,
    content: KindaLazyScope<P, L>.() -> Unit,
) : KindaLazyScope<P, L>(tileWidth) {
    var wrappedPermanentChildren: (@Composable FlowRowScope.() -> Unit)? = null
    var wrappedLazyChildren: (LazyGridScope.() -> Unit)? = null

    init {
        apply(content)
    }

    fun update(content: KindaLazyScope<P, L>.() -> Unit) {
        apply(content)
        require(lazyTemplate != null) {
            "lazy template required for KindaLazyVerticalGrid"
        }
        require(permanentTemplate != null) {
            "permanent template required for KindaLazyVerticalGrid"
        }

        wrappedPermanentChildren = @Composable {
            for (child in permanentChildren) {
                Box(modifier = Modifier.requiredWidth(columnWidth)) {
                    permanentTemplate!!.invoke(child)
                }
            }
            for (moved in movedLazyChildren) {
                // need to wrap in a box here
                Box(modifier = Modifier.requiredWidth(columnWidth)) {
                    lazyTemplate!!.invoke(moved)
                }
            }
        }

        wrappedLazyChildren = {
            items(lazyChildren) {
                lazyTemplate!!.invoke(it)
            }
        }
    }
}
