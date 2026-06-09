package com.saavdhan.app.system.watchdog

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.saavdhan.app.MainActivity
import com.saavdhan.app.R
import com.saavdhan.app.data.scanner.AssessedApp

/** Builds and posts the "a new app looks dangerous" notification. */
object ThreatNotifier {

    private const val CHANNEL_ID = "saavdhan_threats"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.watchdog_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.watchdog_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPost(context: Context): Boolean {
        // POST_NOTIFICATIONS is a runtime permission only on Android 13+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Show a notification for a risky newly-installed app. No-ops if notifications aren't allowed.
     *
     * We DO check the POST_NOTIFICATIONS permission first via [canPost]; lint can't trace through a
     * helper method, so the MissingPermission warning is suppressed here with that justification.
     */
    @SuppressLint("MissingPermission")
    fun notifyThreat(context: Context, assessed: AssessedApp) {
        if (!canPost(context)) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = android.app.PendingIntent.getActivity(
            context,
            assessed.app.packageName.hashCode(),
            openApp,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.watchdog_title))
            .setContentText(context.getString(R.string.watchdog_text, assessed.app.label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context)
            .notify(assessed.app.packageName.hashCode(), notification)
    }
}
