package chat.oi.core.domain.model

object PlainTextRules {
    const val MAX_STATUS_CODE_POINTS = 140
    const val MAX_MESSAGE_CODE_POINTS = 4096

    private val htmlTag = Regex("</?[A-Za-z][^>]*>")
    private val markdownLink = Regex("""\[[^\]]+]\([^)]+\)""")
    private val url = Regex("""(?i)(https?://|www\.)\S+""")

    fun isValidStatus(text: String): Boolean =
        text.isNotBlank() &&
            text.codePointCount() <= MAX_STATUS_CODE_POINTS &&
            isPlainTextStatus(text)

    fun isPlainTextStatus(text: String): Boolean =
        !htmlTag.containsMatchIn(text) &&
            !markdownLink.containsMatchIn(text) &&
            !url.containsMatchIn(text)

    fun isValidMessage(text: String): Boolean =
        text.isNotBlank() && text.codePointCount() <= MAX_MESSAGE_CODE_POINTS

    private fun String.codePointCount(): Int =
        codePointCount(0, length)
}
