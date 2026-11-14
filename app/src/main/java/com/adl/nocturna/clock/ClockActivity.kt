package com.adl.nocturna.clock

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.alarm.AlarmActivity
import com.adl.nocturna.alarm.AlarmService
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.adl.nocturna.data.Storage
import com.adl.nocturna.settings.SettingsActivity
import com.adl.nocturna.ui.NocturnaTheme
import com.adl.nocturna.ui.enableDarkEdgeToEdge

class ClockActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDarkEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        askForNotificationsOnce()

        setContent {
            NocturnaTheme {
                // If the alarm's own screen couldn't open (e.g. notifications are off), show it as soon
                // as the clock is visible, so there's always a way to stop a ringing alarm.
                val ringing by AlarmService.ringing.collectAsStateWithLifecycle()
                LaunchedEffect(ringing?.alarmId) {
                    if (ringing != null) startActivity(Intent(this@ClockActivity, AlarmActivity::class.java))
                }
                ClockScreen(
                    interactive = true,
                    onBrightness = { window.setBrightness(it) },
                    onOpenSettings = { openUnlocked(Intent(this, SettingsActivity::class.java)) },
                    onOpen = { intent ->
                        openUnlocked(intent) {
                            Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenAlarms = {
                        openUnlocked(
                            Intent(this, SettingsActivity::class.java)
                                .putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.SCREEN_ALARMS)
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back to the clock while an alarm rings (e.g. after switching away from its screen)
        // must bring the Stop/Snooze screen back to the front.
        if (AlarmService.ringing.value != null) startActivity(Intent(this, AlarmActivity::class.java))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        val prefs = Storage.localPrefs(this)
        if (prefs.getBoolean("askedNotifications", false)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit { putBoolean("askedNotifications", true) }
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * The clock may be showing over the lock screen; unlock first before opening anything else.
 * [onNoApp] runs instead of crashing when no installed app can open [intent].
 */
fun Activity.openUnlocked(intent: Intent, onNoApp: () -> Unit = {}) {
    fun start() {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            onNoApp()
        }
    }
    val km = getSystemService(KeyguardManager::class.java)
    if (km.isKeyguardLocked) {
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = start()
        })
    } else {
        start()
    }
}

/** Applies a brightness from [ClockScreen]; negative means "use the system setting". */
fun Window.setBrightness(value: Float) {
    attributes = attributes.apply {
        screenBrightness = if (value < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else value.coerceIn(0.01f, 1f)
    }
}
