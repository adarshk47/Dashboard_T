package com.messageorganizer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.messageorganizer.data.SmsMessage
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    fun exportToCsv(
        context: Context,
        messages: List<SmsMessage>,
        groupName: String
    ): Intent {
        val fileName = "${groupName.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Sender,Date,Message\n")
            messages.forEach { msg ->
                val date = dateFormat.format(Date(msg.timestamp))
                val body = msg.body.replace("\"", "'").replace("\n", " ")
                writer.append("\"${msg.sender}\",\"$date\",\"$body\"\n")
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Messages - $groupName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun backupAllToDrive(context: Context, messages: List<SmsMessage>): Intent {
        val file = File(context.cacheDir, "MessageOrganizer_Backup_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("Group,Sender,Date,Message\n")
            messages.forEach { msg ->
                val date = dateFormat.format(Date(msg.timestamp))
                val body = msg.body.replace("\"", "'").replace("\n", " ")
                writer.append("\"All\",\"${msg.sender}\",\"$date\",\"$body\"\n")
            }
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // ACTION_CREATE_DOCUMENT opens Google Drive / local storage picker
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, file.name)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
