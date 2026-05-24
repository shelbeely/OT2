package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule reminder on boot if enabled
            ReminderManager.scheduleReminder(context)
        } else {
            // Show the notification
            ReminderManager.showNotification(context)
        }
    }
}
