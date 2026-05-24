package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

object ReminderManager {
    private const val CHANNEL_ID = "transition_reminders"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "open_transition_prefs"
    private const val KEY_REMINDER_ENABLED = "daily_reminder_enabled"
    private const val KEY_REMINDER_HOUR = "daily_reminder_hour" // Default 20 (8 PM)
    private const val KEY_REMINDER_MINUTE = "daily_reminder_minute" // Default 0

    fun scheduleReminder(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Always cancel existing
        alarmManager.cancel(pendingIntent)

        if (!enabled) return

        val hour = prefs.getInt(KEY_REMINDER_HOUR, 20)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time is before now, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log your transition progress"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Adjust icon as needed
            .setContentTitle("Time to log!")
            .setContentText("Don't forget to track your transition milestones for today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reschedule for next day after showing
        scheduleReminder(context)
    }

    fun isReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        
        scheduleReminder(context)
    }
}
