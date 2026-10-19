package com.example

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.Note
import com.example.utils.DateTimeFormatter
import com.example.utils.RichTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Note", appName)
    }

    @Test
    fun `test note entity creation`() {
        val note = Note(
            id = 1L,
            title = "Design System",
            contentHtml = "<p>Test <b>bold</b> text</p>",
            plainText = "Test bold text",
            isPinned = true
        )
        assertEquals("Design System", note.title)
        assertTrue(note.isPinned)
    }

    @Test
    fun `test rich text formatting helper`() {
        val ssb = SpannableStringBuilder("Hello Android World")
        RichTextHelper.toggleBold(ssb, 0, 5)
        assertTrue(RichTextHelper.isBoldActive(ssb, 0, 5))

        val html = RichTextHelper.toHtml(ssb)
        assertNotNull(html)
    }

    @Test
    fun `test date time formatter`() {
        val formatted = DateTimeFormatter.formatNoteDate(System.currentTimeMillis())
        assertEquals("Just now", formatted)
    }
}
