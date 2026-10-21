package com.shrivatsav.monomail.core.data.repository

import java.text.SimpleDateFormat
import java.util.Locale

internal data class ParsedSearchQuery(
    val ftsQuery: String,
    val dateFrom: Long?,
    val dateTo: Long?,
    val hasAttachments: Boolean,
    val isRead: Boolean?,
    val isStarred: Boolean?,
    val folder: String?,
    val cc: String?,
    val bcc: String?
)

internal fun parseSearchQuery(input: String, selectedField: SearchField): ParsedSearchQuery {
    val tokens = Regex("""(?:[^\s"]+|"[^"]*")+""").findAll(input.trim()).map { it.value }.toList()
    val ftsParts = mutableListOf<String>()
    var dateFrom: Long? = null
    var dateTo: Long? = null
    var hasAttachments = false
    var isRead: Boolean? = null
    var isStarred: Boolean? = null
    var folder: String? = null
    var cc: String? = null
    var bcc: String? = null

    fun ftsTerm(value: String, field: String? = null): String {
        val cleaned = value.trim().trim('"').replace(Regex("""[^\p{L}\p{N}@._+-]+"""), " ")
        val words = cleaned.split(Regex("""\s+""")).filter(String::isNotBlank)
        if (words.isEmpty()) return ""
        val terms = words.joinToString(" AND ") { "${it}*" }
        return if (field == null) terms else "$field:($terms)"
    }

    tokens.forEach { token ->
        val negative = token.startsWith("-") && token.length > 1
        val raw = token.removePrefix("-")
        val operator = raw.substringBefore(':', "").lowercase(Locale.ROOT)
        val value = raw.substringAfter(':', "")
        if (operator.isNotEmpty() && value.isNotEmpty()) {
            when (operator) {
                "from", "sender" -> {
                    val name = ftsTerm(value, "fromName")
                    val email = ftsTerm(value, "fromEmail")
                    ftsParts += if (negative) "NOT ($name OR $email)" else "($name OR $email)"
                }
                "to" -> ftsParts += if (negative) "NOT (${ftsTerm(value, "toEmail")})" else ftsTerm(value, "toEmail")
                "cc" -> cc = value.takeUnless { negative }
                "bcc" -> bcc = value.takeUnless { negative }
                "subject" -> ftsParts += if (negative) "NOT (${ftsTerm(value, "subject")})" else ftsTerm(value, "subject")
                "body", "text" -> ftsParts += if (negative) "NOT (${ftsTerm(value, "body")})" else ftsTerm(value, "body")
                "has" -> if (value.equals("attachment", true) || value.equals("attachments", true)) hasAttachments = !negative
                "is" -> when (value.lowercase(Locale.ROOT)) {
                    "read" -> isRead = !negative
                    "unread" -> isRead = negative
                    "starred", "flagged" -> isStarred = !negative
                    "unstarred" -> isStarred = negative
                }
                "in", "folder" -> folder = value.lowercase(Locale.ROOT)
                "after", "since" -> dateFrom = parseDate(value)
                "before" -> dateTo = parseDate(value)?.plus(86_399_999L)
                else -> ftsTerm(token)?.takeIf(String::isNotBlank)?.let { ftsParts += it }
            }
        } else {
            val term = if (selectedField == SearchField.FROM) {
                val name = ftsTerm(raw, "fromName")
                val email = ftsTerm(raw, "fromEmail")
                "($name OR $email)"
            } else {
                ftsTerm(raw, selectedField.ftsColumn())
            }
            if (term.isNotBlank()) ftsParts += if (negative) "NOT ($term)" else term
        }
    }

    return ParsedSearchQuery(
        ftsQuery = ftsParts.filter(String::isNotBlank).joinToString(" AND ").ifBlank { "fromName:*" },
        dateFrom = dateFrom,
        dateTo = dateTo,
        hasAttachments = hasAttachments,
        isRead = isRead,
        isStarred = isStarred,
        folder = folder,
        cc = cc,
        bcc = bcc
    )
}

private fun SearchField.ftsColumn(): String? = when (this) {
    SearchField.ALL -> null
    SearchField.SUBJECT -> "subject"
    SearchField.BODY -> "body"
    SearchField.FROM -> "fromName"
    SearchField.TO -> "toEmail"
}

private fun parseDate(value: String): Long? {
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(value)?.time
    }.getOrNull()
}
