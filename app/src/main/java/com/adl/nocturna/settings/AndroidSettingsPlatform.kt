package com.adl.nocturna.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.adl.nocturna.BuildConfig
import com.adl.nocturna.alarm.AlarmScheduler
import com.adl.nocturna.charging.ChargingService
import com.adl.nocturna.clock.DndWhileShowing
import com.adl.nocturna.day.CalendarEvents
import com.adl.nocturna.day.DayPhoto
import com.adl.nocturna.notify.NotificationIconService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * The Settings pages' view of an Android phone: its permissions, system settings pages, photo and file
 * pickers and services. [activity] opens the system's pages, so they sit on top of Settings and Back returns to it.
 */
class AndroidSettingsPlatform(private val activity: Context) : SettingsPlatform {
    private val app = activity.applicationContext

    @Composable
    override fun access(): SystemAccess {
        // The user changes these in the system's settings, so read them again on every return.
        var refresh by remember { mutableIntStateOf(0) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
        return remember(refresh) {
            SystemAccess(
                notifications = NotificationManagerCompat.from(app).areNotificationsEnabled(),
                notificationAccess = NotificationIconService.isEnabled(app),
                overlay = Settings.canDrawOverlays(app),
                fullScreen = Build.VERSION.SDK_INT < 34 ||
                    app.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
                calendar = CalendarEvents.hasPermission(app),
                dndAccess = DndWhileShowing.hasAccess(app),
                dndAllowsAlarms = DndWhileShowing.hasAccess(app) && DndWhileShowing.alarmsAllowed(app),
            )
        }
    }

    override fun isGranted(permission: Permission): Boolean = when (permission) {
        Permission.NOTIFICATIONS -> NotificationManagerCompat.from(app).areNotificationsEnabled()
        else -> ContextCompat.checkSelfPermission(app, manifestName(permission)!!) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    override fun rememberPermissionRequest(permission: Permission, onResult: (Boolean) -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
        val name = manifestName(permission)
        return { if (name != null) launcher.launch(name) }
    }

    /** The permission to ask for; null where Android grants it without asking (notifications before 13). */
    private fun manifestName(permission: Permission): String? = when (permission) {
        Permission.CALENDAR -> Manifest.permission.READ_CALENDAR
        Permission.LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
        Permission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    }

    override fun open(page: SystemPage) {
        val intent = when (page) {
            SystemPage.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
            SystemPage.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            SystemPage.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${app.packageName}".toUri())
            SystemPage.FULL_SCREEN_ALARMS ->
                if (Build.VERSION.SDK_INT < 34) return
                else Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${app.packageName}".toUri())
            SystemPage.SCREEN_SAVER -> Intent(Settings.ACTION_DREAM_SETTINGS)
            SystemPage.DND_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        runCatching { activity.startActivity(intent) }
    }

    @Composable
    override fun rememberPhotoPicker(onSaved: (Boolean) -> Unit): () -> Unit {
        val scope = rememberCoroutineScope()
        // The system photo picker needs no storage permission.
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch { onSaved(DayPhoto.save(app, uri)) }
        }
        return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }

    override fun clearDayPhoto() = DayPhoto.clear(app)

    override fun setOpenOnCharge(on: Boolean) {
        if (on) ChargingService.start(app) else ChargingService.stop(app)
    }

    override fun rescheduleAlarms() = AlarmScheduler.reschedule(app)

    @Composable
    override fun rememberSaveFile(mimeType: String, onResult: (saved: Boolean) -> Unit): (String, String) -> Unit {
        val scope = rememberCoroutineScope()
        // The text waits here while the user picks a place.
        val pending = remember { arrayOfNulls<String>(1) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
            val text = pending[0]
            pending[0] = null
            if (uri == null || text == null) return@rememberLauncherForActivityResult
            scope.launch { onResult(withContext(Dispatchers.IO) { runCatching { write(uri, text) }.isSuccess }) }
        }
        return { fileName, text ->
            pending[0] = text
            launcher.launch(fileName)
        }
    }

    @Composable
    override fun rememberOpenFile(maxBytes: Int, onResult: (OpenedFile) -> Unit): () -> Unit {
        val scope = rememberCoroutineScope()
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { runCatching { read(uri, maxBytes) } }
                onResult(
                    bytes.fold(
                        onSuccess = { if (it == null) OpenedFile.TooBig else OpenedFile.Text(it.decodeToString()) },
                        onFailure = { OpenedFile.Unreadable },
                    ),
                )
            }
        }
        // Some file managers don't know .json as application/json, so any file can be picked and is checked.
        return { launcher.launch(arrayOf("*/*")) }
    }

    private fun write(uri: Uri, text: String) {
        val out = app.contentResolver.openOutputStream(uri, "w") ?: throw IOException("No stream for $uri")
        out.use { it.write(text.encodeToByteArray()) }
    }

    /** The file's bytes, or null when there are more than [limit]. */
    private fun read(uri: Uri, limit: Int): ByteArray? {
        val input = app.contentResolver.openInputStream(uri) ?: throw IOException("No stream for $uri")
        return input.use { it.readAtMost(limit) }
    }

    private fun InputStream.readAtMost(limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = read(buffer)
            if (n < 0) return out.toByteArray()
            out.write(buffer, 0, n)
            if (out.size() > limit) return null
        }
    }

    override fun showMessage(text: String) = Toast.makeText(app, text, Toast.LENGTH_SHORT).show()

    override val versionName: String = BuildConfig.VERSION_NAME

    override val versionLabel: String = versionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.DEBUG)
}
