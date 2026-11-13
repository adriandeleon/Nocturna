package com.adl.nocturna.clock

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
                ClockScreen(
                    interactive = true,
                    onBrightness = { window.setBrightness(it) },
                    onOpenSettings = { openUnlocked(Intent(this, SettingsActivity::class.java)) },
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
        val prefs = getSharedPreferences("nocturna", MODE_PRIVATE)
        if (prefs.getBoolean("askedNotifications", false)) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        prefs.edit { putBoolean("askedNotifications", true) }
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** The clock may be showing over the lock screen; unlock first before opening anything else. */
fun Activity.openUnlocked(intent: Intent) {
    val km = getSystemService(KeyguardManager::class.java)
    if (km.isKeyguardLocked) {
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = startActivity(intent)
        })
    } else {
        startActivity(intent)
    }
}

/** Applies a brightness from [ClockScreen]; negative means "use the system setting". */
fun Window.setBrightness(value: Float) {
    attributes = attributes.apply {
        screenBrightness = if (value < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else value.coerceIn(0.01f, 1f)
    }
}
