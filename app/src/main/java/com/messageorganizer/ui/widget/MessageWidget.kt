package com.messageorganizer.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.RemoteViews
import com.messageorganizer.MainActivity
import com.messageorganizer.R

class MessageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_message)

        val stats = getStats(context)
        views.setTextViewText(R.id.widget_otp_count, "OTP: ${stats.first}")
        views.setTextViewText(R.id.widget_tx_count, "Transactions: ${stats.second}")
        views.setTextViewText(R.id.widget_title, "Message Organizer")

        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, pending)

        manager.updateAppWidget(widgetId, views)
    }

    private fun getStats(context: Context): Pair<Int, Int> {
        var otpCount = 0
        var txCount = 0
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY), null, null,
                "${Telephony.Sms.DATE} DESC LIMIT 500"
            )
            cursor?.use {
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                while (it.moveToNext()) {
                    val body = it.getString(bodyIdx)?.lowercase() ?: continue
                    if (body.contains("otp") || body.contains("verification code")) otpCount++
                    if (body.contains("debited") || body.contains("credited") || body.contains("₹")) txCount++
                }
            }
        } catch (_: Exception) {}
        return Pair(otpCount, txCount)
    }
}
