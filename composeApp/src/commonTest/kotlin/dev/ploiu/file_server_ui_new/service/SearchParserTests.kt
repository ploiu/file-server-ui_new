package dev.ploiu.file_server_ui_new.service

import dev.ploiu.file_server_ui_new.Search
import dev.ploiu.file_server_ui_new.SearchParser
import dev.ploiu.file_server_ui_new.model.Attribute
import dev.ploiu.file_server_ui_new.model.EqualityOperator
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class SearchParserTests {
    @Test
    fun `parse handles regular search`() {
        val input = "test"
        val expected = "test"
        val actual = SearchParser.parse(input).text
        assertEquals(expected, actual)
    }

    @Test
    fun `parse handles no search`() {
        val input = "@size > small"
        val expected = ""
        val actual = SearchParser.parse(input).text
        assertEquals(expected, actual)
    }


    @Test
    fun `parse strips extra spaces from search`() {
        val input = "\ttest      test "
        val expected = "test test"
        val actual = SearchParser.parse(input).text
        assertEquals(expected, actual)
    }

    @Test
    fun `parse brings all regular search parts together when separated`() {
        val input =
            "test @size > small .gif" // there is a space after test and a space before .gif, so we get a space
        val expected = "test .gif"
        val actual = SearchParser.parse(input).text
        assertEquals(expected, actual)
    }

    @Test
    fun `parse properly parses single tag`() {
        val input = "+tag1"
        val expected = ArrayList(mutableListOf("tag1"))
        val actual = SearchParser.parse(input).tags
        assertEquals(expected, actual)
    }

    @Test
    fun `parse properly parses multiple tags`() {
        val input = "+tag1 +tag2"
        val expected = ArrayList(mutableListOf("tag1", "tag2"))
        val actual = SearchParser.parse(input).tags
        assertEquals(expected, actual)
    }

    @Test
    fun `parse properly parses attributes properly`() {
        val simple = "@size > small"
        val simpleExpected = listOf(Attribute("fileSize", EqualityOperator.GT, "small"))
        val complex = "@date <> 2025-02-01 @size < Large"
        val complexExpected = listOf(
            Attribute("dateCreated", EqualityOperator.NEQ, "2025-02-01"),
            Attribute("fileSize", EqualityOperator.LT, "Large")
        )
        assertEquals(simpleExpected, SearchParser.parse(simple).attributes)
        assertEquals(complexExpected, SearchParser.parse(complex).attributes)
    }

    @Test
    fun `parse properly parses full search text`() {
        val simple = "blah blah blah +tag1 @size > small +tag2"
        val simpleExpected = Search(
            "blah blah blah",
            listOf("tag1", "tag2"),
            listOf(Attribute("fileSize", EqualityOperator.GT, "small"))
        )
        val complex = "\nblah blah blah +tag1 @size>small  +tag2 blah"
        val complexExpected = Search(
            "blah blah blah blah",
            listOf("tag1", "tag2"),
            listOf(Attribute("fileSize", EqualityOperator.GT, "small"))
        )
        assertEquals(simpleExpected, SearchParser.parse(simple))
        assertEquals(complexExpected, SearchParser.parse(complex))
    }
}
