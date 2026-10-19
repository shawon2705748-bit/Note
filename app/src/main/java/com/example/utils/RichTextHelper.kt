package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.util.Patterns
import android.widget.Toast

object RichTextHelper {

    fun toHtml(editable: Editable): String {
        return Html.toHtml(editable, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL).trim()
    }

    fun fromHtml(html: String?): SpannableStringBuilder {
        if (html.isNullOrBlank()) {
            return SpannableStringBuilder("")
        }
        val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        val ssb = SpannableStringBuilder(spanned)
        // Trim trailing newlines often appended by Html.fromHtml
        while (ssb.isNotEmpty() && ssb[ssb.length - 1] == '\n') {
            ssb.delete(ssb.length - 1, ssb.length)
        }
        return ssb
    }

    fun isBoldActive(editable: Editable, start: Int, end: Int): Boolean {
        if (start >= end) return false
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        return spans.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
    }

    fun isItalicActive(editable: Editable, start: Int, end: Int): Boolean {
        if (start >= end) return false
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        return spans.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
    }

    fun getExistingUrl(editable: Editable, start: Int, end: Int): String? {
        if (start > end) return null
        val spans = editable.getSpans(start, end, URLSpan::class.java)
        return spans.firstOrNull()?.url
    }

    fun toggleBold(editable: Editable, selStart: Int, selEnd: Int): Boolean {
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        if (start == end) return false

        val isBold = isBoldActive(editable, start, end)
        if (isBold) {
            removeStyle(editable, start, end, Typeface.BOLD)
            return false
        } else {
            editable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return true
        }
    }

    fun toggleItalic(editable: Editable, selStart: Int, selEnd: Int): Boolean {
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        if (start == end) return false

        val isItalic = isItalicActive(editable, start, end)
        if (isItalic) {
            removeStyle(editable, start, end, Typeface.ITALIC)
            return false
        } else {
            editable.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return true
        }
    }

    private fun removeStyle(editable: Editable, start: Int, end: Int, targetStyle: Int) {
        val spans = editable.getSpans(start, end, StyleSpan::class.java)
        for (span in spans) {
            if (span.style == targetStyle) {
                val spanStart = editable.getSpanStart(span)
                val spanEnd = editable.getSpanEnd(span)
                editable.removeSpan(span)

                if (spanStart < start) {
                    editable.setSpan(
                        StyleSpan(targetStyle),
                        spanStart,
                        start,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (spanEnd > end) {
                    editable.setSpan(
                        StyleSpan(targetStyle),
                        end,
                        spanEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else if (span.style == Typeface.BOLD_ITALIC) {
                val spanStart = editable.getSpanStart(span)
                val spanEnd = editable.getSpanEnd(span)
                editable.removeSpan(span)

                val remainingStyle = if (targetStyle == Typeface.BOLD) Typeface.ITALIC else Typeface.BOLD
                // Preserve outside parts
                if (spanStart < start) {
                    editable.setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        spanStart,
                        start,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (spanEnd > end) {
                    editable.setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        end,
                        spanEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Selection gets remaining style
                editable.setSpan(
                    StyleSpan(remainingStyle),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    fun applyLink(editable: Editable, selStart: Int, selEnd: Int, rawUrl: String) {
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        if (start == end) return

        removeLink(editable, start, end)

        var sanitizedUrl = rawUrl.trim()
        if (!sanitizedUrl.startsWith("http://", ignoreCase = true) &&
            !sanitizedUrl.startsWith("https://", ignoreCase = true)
        ) {
            sanitizedUrl = "https://$sanitizedUrl"
        }

        editable.setSpan(
            URLSpan(sanitizedUrl),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    fun removeLink(editable: Editable, selStart: Int, selEnd: Int) {
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        val spans = editable.getSpans(start, end, URLSpan::class.java)
        for (span in spans) {
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val url = span.url
            editable.removeSpan(span)

            if (spanStart < start) {
                editable.setSpan(URLSpan(url), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                editable.setSpan(URLSpan(url), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun clearFormatting(editable: Editable, selStart: Int, selEnd: Int) {
        val start = minOf(selStart, selEnd)
        val end = maxOf(selStart, selEnd)
        if (start == end) return

        // Remove style spans
        val styleSpans = editable.getSpans(start, end, StyleSpan::class.java)
        for (span in styleSpans) {
            val spanStart = editable.getSpanStart(span)
            val spanEnd = editable.getSpanEnd(span)
            val style = span.style
            editable.removeSpan(span)

            if (spanStart < start) {
                editable.setSpan(StyleSpan(style), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                editable.setSpan(StyleSpan(style), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Remove URL spans
        removeLink(editable, start, end)
    }

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        var testUrl = url.trim()
        if (!testUrl.startsWith("http://", ignoreCase = true) &&
            !testUrl.startsWith("https://", ignoreCase = true)
        ) {
            testUrl = "https://$testUrl"
        }
        return Patterns.WEB_URL.matcher(testUrl).matches()
    }

    fun openUrlSafely(context: Context, url: String) {
        try {
            var target = url.trim()
            if (!target.startsWith("http://", ignoreCase = true) &&
                !target.startsWith("https://", ignoreCase = true)
            ) {
                target = "https://$target"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link: $url", Toast.LENGTH_SHORT).show()
        }
    }
}
