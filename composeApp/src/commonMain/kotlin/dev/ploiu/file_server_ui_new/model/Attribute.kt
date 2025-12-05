package dev.ploiu.file_server_ui_new.model

import java.util.*
import java.util.regex.Pattern

class Attribute(field: String, private val op: EqualityOperator, value: String) {
    private val field: String
    private val value: String

    init {
        this.field = mapField(field)
        this.value = mapValue(value)
    }

    /**
     * maps the passed field to one of the valid field names for the backend server:
     * <dl>
     * <dt>dateCreated</dt>
     * <dd>
     *
     *  * dateCreated
     *  * createDate
     *  * date
     *
    </dd> *
     * <dt>fileSize</dt>
     * <dd>
     *
     *  * fileSize
     *  * size
     *  * length
     *
    </dd> *
     * <dt>fileType</dt>
     * <dd>
     *
     *  * fileType
     *  * type
     *
    </dd> *
    </dl> *
     *
     * @param field
     * @return the mapped field name listed above. If no mapping can be matched, the field itself is returned
     */
    private fun mapField(field: String): String {
        return when (field.lowercase(Locale.getDefault())) {
            "datecreated", "createdate", "date" -> "dateCreated"
            "filesize", "size", "length" -> "fileSize"
            "filetype", "type" -> "fileType"
            else -> field
        }
    }

    private fun mapValue(value: String): String {
        if ("fileSize" == this.field) {
            return handleFileSizeByteAlias(value)
        }
        return value.lowercase(Locale.getDefault())
    }

    private fun handleFileSizeByteAlias(value: String): String {
        val matcher = BYTE_MULT_PATTERN.matcher(value)
        if (!matcher.find()) {
            return value
        }
        // matcher found something, we are guaranteed to have a number and a mult (even if number is an empty string)
        val number = matcher.group("number")
        val mult = matcher.group("mult")
        val parsedNum = if ("" == number) 1 else number!!.toInt()
        val byteMult = when (mult!!.lowercase(Locale.getDefault())) {
            "kb" -> 1000
            "kib" -> 1024
            "mb" -> 1_000_000
            "mib" -> 1_048_576
            "gb" -> 1_000_000_000
            "gib" -> 1_073_741_824
            "tb" -> 1_000_000_000_000L
            "tib" -> 1_099_511_627_776L
            "pb" -> 1_000_000_000_000_000L
            "pib" -> 1_125_899_906_842_624L
            else -> 1
        }
        return (parsedNum * byteMult).toString()
    }

    override fun toString(): String {
        return "$field.$op;$value"
    }

    companion object {
        val BYTE_MULT_PATTERN: Pattern =
            Pattern.compile(
                "^(?<number>[0-9]*)(?<mult>ki?b|mi?b|gi?b|ti?b|pi?b)$",
                Pattern.CASE_INSENSITIVE,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Attribute) return false
        return other.field == this.field && other.op == this.op && other.value == this.value
    }

    override fun hashCode(): Int {
        return Objects.hash(field, op, value)
    }
}
