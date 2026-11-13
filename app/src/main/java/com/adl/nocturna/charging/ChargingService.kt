package com.adl.nocturna.charging

import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.adl.nocturna.NocturnaApp
import com.adl.nocturna.R
import com.adl.nocturna.clock.ClockActivity

/**
 * Android no longer delivers "power connected" to manifest receivers, so this small foreground
 * service listens for it and opens the clock when the phone is put on the charger while idle.
 *
 * Opening an activity from the background needs "Display over other apps"; without it we fall back
 * to a full-screen notification, which Android shows as the clock when the screen is off.
 */
class ChargingService : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_POWER_CONNECTED) onPowerConnected()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, ClockActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, NocturnaApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Waiting for the charger")
            .setContentText("Nocturna opens when you plug in your phone")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter(Intent.ACTION_POWER_CONNECTED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun onPowerConnected() {
        // Don't pop up over whatever the user is doing; only take over an idle/locked phone.
        val interactive = getSystemService(PowerManager::class.java).isInteractive
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (interactive && !locked) return

        val intent = Intent(this, ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Settings.canDrawOverlays(this)) {
            startActivity(intent)
            return
        }
        val pi = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(this, NocturnaApp.CHANNEL_LAUNCH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nocturna")
            .setContentText("Tap to open the clock")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        val canNotify = android.os.Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (canNotify) NotificationManagerCompat.from(this).notify(LAUNCH_NOTIFICATION_ID, n)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 7
        private const val LAUNCH_NOTIFICATION_ID = 8

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, ChargingService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChargingService::class.java))
        }
    }
}
