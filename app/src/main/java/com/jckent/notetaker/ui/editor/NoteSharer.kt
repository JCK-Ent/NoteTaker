package com.jckent.notetaker.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jckent.notetaker.data.Note

object NoteSharer {

    fun shareText(context: Context, note: Note) {
        val text = buildShareText(note)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share note via…"))
    }

    fun shareViaSms(context: Context, note: Note, phoneNumber: String = "") {
        val text = buildShareText(note)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", text)
        }
        context.startActivity(intent)
    }

    fun shareViaEmail(context: Context, note: Note, to: String = "") {
        val text = buildShareText(note)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$to")
            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(intent)
    }

    private fun buildShareText(note: Note): String =
        if (note.title.isBlank()) note.content
        else "${note.title}\n\n${note.content}"
}
