package dev.ploiu.file_server_ui_new.model

import java.util.*

enum class EqualityOperator {
    // string values are equivalent to the backend server
    LT, GT, EQ, NEQ, UNKNOWN;

    override fun toString(): String {
        return this.name.lowercase(Locale.getDefault())
    }

    companion object {
        fun parse(op: String): EqualityOperator {
            return when (op.trim { it <= ' ' }) {
                "<" -> LT
                ">" -> GT
                "=", "==", "===" -> EQ
                "!=", "!==", "<>" -> NEQ
                else -> UNKNOWN
            }
        }
    }
}
