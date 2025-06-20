package dev.ploiu.file_server_ui_new

import dev.ploiu.file_server_ui_new.model.Attribute
import dev.ploiu.file_server_ui_new.model.EqualityOperator

enum class TokenTypes {
    /**
     * start of an attribute portion ("@")
     */
    ATTRIBUTE_START,

    /**
     * name of an attribute portion
     */
    ATTRIBUTE_NAME,

    /**
     * fragment or whole part of attribute operator see [dev.ploiu.file_server_ui_new.model.EqualityOperator]
     */
    ATTRIBUTE_OP,

    /**
     * fragment or whole part of the value of an attribute see [dev.ploiu.file_server_ui_new.model.Attribute]
     */
    ATTRIBUTE_VALUE,

    /**
     * start of a tag portion ("+")
     */
    TAG_START,

    /**
     * portion of a tag name
     */
    TAG_NAME,
    SPACE,

    /**
     * normal-mode text for search title
     */
    NORMAL,

    /**
     * catch-all
     */
    UNKNOWN
}

data class Token(val value: Char, val type: TokenTypes)

data class Search(val text: String, val tags: Collection<String>, val attributes: Collection<Attribute>)

/**
 * Parses a string input by the user into a search object that represents the type of search the backend can understand
 */
object SearchParser {
    private const val OP_GEX = "[=<>!]"
    private val MULTIPLE_SPACES = " +".toRegex()

    /**
     * parses the passed String into a [Search] object to be passed to the backend server
     *
     * @param search
     * @return
     */
    fun parse(search: String): Search {
        val tokens = tokenize(search.trim().replace(MULTIPLE_SPACES, " "))
        // searchText is always just going to be a concatenation of normal text so it's ok to have 1 dedicated to it
        val searchText = StringBuilder()
        val tags = mutableListOf<String>()
        val attributes = mutableListOf<Attribute>()
        var index = 0
        while (index < tokens.size) {
            val current = tokens[index]
            when (current.type) {
                TokenTypes.NORMAL -> index = handleNormalTokens(tokens, index, searchText)
                TokenTypes.TAG_START -> {
                    val builder = StringBuilder()
                    index = handleTagTokens(tokens, index, builder)
                    tags += builder.toString()
                }

                TokenTypes.ATTRIBUTE_START -> {
                    val builder = Attribute.builder()
                    index = handleAttributeTokens(tokens, index, builder)
                    attributes += builder.build()
                }

                else -> {
                    // unknown, skip
                    index++
                }
            }
        }
        val cleanedText = searchText.toString().trim().replace(MULTIPLE_SPACES, " ")
        return Search(cleanedText, tags, attributes)
    }

    /**
     * splits the text up into different token types for syntax highlighting and to aid searching
     *
     * @param search
     * @return a Token array. This array will have the same length as the input string in the same order of chars as the input string
     */
    fun tokenize(search: String): List<Token> {
        val tokens = mutableListOf<Token>()
        // stripping extra spaces will let us more easily tokenize and parse later
        val chars = search.trim().replace(MULTIPLE_SPACES, " ").toCharArray()
        var mode: Modes = Modes.UNSET
        chars.forEach { c ->
            val tokenAndMode = when (mode) {
                Modes.UNSET -> handleUnset(c)
                Modes.ATTRIBUTE_NAME -> handleAttributeName(c)
                Modes.FILE_NAME -> handleFileName(c)
                Modes.ATTRIBUTE_OP -> handleAttributeOp(c)
                Modes.ATTRIBUTE_VALUE -> handleAttributeValue(c)
                Modes.TAG_NAME -> handleTagName(c)
            }
            tokens += tokenAndMode.token
            mode = tokenAndMode.mode
        }
        return tokens
    }

    /**
     * parses out the text in `tokens` as a normal search, starting from `start` index. The passed StringBuilder is populated with the text
     *
     * @param tokens  tokens pulled from [.tokenize]
     * @param start   the start index to search through
     * @param builder this will have the contents of the tokens for normal text
     * @return the new index to iterate from in tokens
     */
    fun handleNormalTokens(tokens: List<Token>, start: Int, builder: StringBuilder): Int {
        var index = start
        while (index < tokens.size && (tokens[index].type === TokenTypes.NORMAL || tokens[index].type === TokenTypes.SPACE)) {
            builder.append(tokens[index].value)
            index++
        }
        return index
    }

    /**
     * parses out the text in `tokens` into a tag, starting from `start` + 1 (because the first index could be a `+`, which isn't actually part of the tag)
     *
     * @param tokens  the tokens to parse through
     * @param start   the index of the tag start (`+` character)
     * @param builder will be populated with the tag name
     * @return the index to continue iterating from
     */
    fun handleTagTokens(tokens: List<Token>, start: Int, builder: StringBuilder): Int {
        // first char is +, so skip it
        var index = start + 1
        while (index < tokens.size && tokens[index].type === TokenTypes.TAG_NAME) {
            builder.append(tokens[index].value)
            index++
        }
        return index
    }

    fun handleAttributeTokens(tokens: List<Token>, start: Int, builder: Attribute.Builder): Int {
        // first char is @ which we don't need, so skip it
        var index = start + 1
        val nameBuilder = StringBuilder()
        val opBuilder = StringBuilder()
        val valueBuilder = StringBuilder()
        while (index < tokens.size && tokens[index].type === TokenTypes.ATTRIBUTE_NAME) {
            nameBuilder.append(tokens[index].value)
            index++
        }
        // there could be spaces in between the name and operator, so we need to skip those
        while (index < tokens.size && (tokens[index]).type !== TokenTypes.ATTRIBUTE_OP) {
            index++
        }
        while (index < tokens.size && tokens[index].type === TokenTypes.ATTRIBUTE_OP) {
            opBuilder.append(tokens[index].value)
            index++
        }
        // there could be spaces in between the operator and value, so we need to skip those
        while (index < tokens.size && tokens[index].type !== TokenTypes.ATTRIBUTE_VALUE) {
            index++
        }
        while (index < tokens.size && tokens[index].type === TokenTypes.ATTRIBUTE_VALUE) {
            valueBuilder.append(tokens[index].value)
            index++
        }
        builder
            .field(nameBuilder.toString())
            .op(EqualityOperator.parse(opBuilder.toString()))
            .value(valueBuilder.toString())
        return index
    }

    private fun handleUnset(c: Char): TokenAndMode {
        return when (c) {
            '+' -> TokenAndMode(Token(c, TokenTypes.TAG_START), Modes.TAG_NAME)
            '@' -> TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_START), Modes.ATTRIBUTE_NAME)
            ' ' -> TokenAndMode(Token(c, TokenTypes.SPACE), Modes.UNSET)
            else -> TokenAndMode(Token(c, TokenTypes.NORMAL), Modes.FILE_NAME)
        }
    }

    private fun handleAttributeName(c: Char): TokenAndMode {
        return if (c == ' ') {
            TokenAndMode(Token(c, TokenTypes.SPACE), Modes.ATTRIBUTE_OP)
        } else if (c.toString().matches(OP_GEX.toRegex())) {
            TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_OP), Modes.ATTRIBUTE_OP)
        } else if (c.toString().matches("[a-zA-Z]".toRegex())) {
            TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_NAME), Modes.ATTRIBUTE_NAME)
        } else {
            TokenAndMode(Token(c, TokenTypes.UNKNOWN), Modes.ATTRIBUTE_NAME)
        }
    }

    private fun handleFileName(c: Char): TokenAndMode {
        return if (c == ' ') {
            TokenAndMode(Token(c, TokenTypes.SPACE), Modes.UNSET)
        } else {
            TokenAndMode(Token(c, TokenTypes.NORMAL), Modes.FILE_NAME)
        }
    }

    private fun handleAttributeOp(c: Char): TokenAndMode {
        return if (c == ' ') {
            TokenAndMode(Token(c, TokenTypes.SPACE), Modes.ATTRIBUTE_VALUE)
        } else if (c.toString().matches(OP_GEX.toRegex())) {
            TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_OP), Modes.ATTRIBUTE_OP)
        } else {
            TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_VALUE), Modes.ATTRIBUTE_VALUE)
        }
    }

    private fun handleAttributeValue(c: Char): TokenAndMode {
        return if (c == ' ') {
            TokenAndMode(Token(c, TokenTypes.SPACE), Modes.UNSET)
        } else {
            TokenAndMode(Token(c, TokenTypes.ATTRIBUTE_VALUE), Modes.ATTRIBUTE_VALUE)
        }
    }

    private fun handleTagName(c: Char): TokenAndMode {
        return if (c == ' ') {
            TokenAndMode(Token(c, TokenTypes.SPACE), Modes.UNSET)
        } else {
            TokenAndMode(Token(c, TokenTypes.TAG_NAME), Modes.TAG_NAME)
        }
    }

    /**
     * the current mode when parsing chars into tokens
     */
    internal enum class Modes {
        FILE_NAME,
        ATTRIBUTE_NAME,
        ATTRIBUTE_OP,
        ATTRIBUTE_VALUE,
        TAG_NAME,

        /**
         * the default mode, set whenever we encounter a space and are either in "FILE_NAME", "ATTRIBUTE_VALUE", or "TAG_NAME"
         */
        UNSET
    }

    internal data class TokenAndMode(val token: Token, val mode: Modes)
}
