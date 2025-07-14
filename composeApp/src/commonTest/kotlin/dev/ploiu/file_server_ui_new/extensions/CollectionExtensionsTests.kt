package dev.ploiu.file_server_ui_new.extensions

import java.util.stream.IntStream
import kotlin.streams.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionExtensionsTests {
    @Test
    fun `uiCount shows the proper count if the size is under 1000`() {
        assertEquals("0", listOf<Any>().uiCount())
        assertEquals("500", IntStream.range(0, 500).toList().uiCount())
    }

    @Test
    fun `uiCount truncates to 1 sig fig when above 999`() {
        val firstSigFigOnly = IntStream.range(0, 1_502).toList()
        val secondSigFigOnly = IntStream.range(0, 1_050).toList()
        val bothSigFigs = IntStream.range(0, 1_333).toList()
        assertEquals("1.5k+", firstSigFigOnly.uiCount())
        assertEquals("1k+", secondSigFigOnly.uiCount())
        assertEquals("1.3k+", bothSigFigs.uiCount())
    }

    @Test
    fun `uiCount shows flat thousands count if under 1 sig fig for single thousands`() {
        val items = IntStream.range(0, 1_020).toList()
        assertEquals("1k+", items.uiCount())
    }

}
