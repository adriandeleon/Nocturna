package com.adl.nocturna.clock

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Storage
import com.adl.nocturna.day.isDayModeActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Settings.dndWhileShowing: priority-only Do Not Disturb while the clock (the app or the screen saver) is
 * on screen at night, put back when it leaves the screen or at the morning edge. The decisions are the
 * shared [shouldDndBeOn] and [dndStep]; this applies them and keeps their [DndMemory] in device-protected
 * storage, so a restart still knows what Nocturna changed. Call everything on the main thread.
 */
object DndWhileShowing {
    private val scope = MainScope()
    private var ticker: Job? = null
    private var surfaces = 0
    private var night = false

    /** Whether the user has given Nocturna Do Not Disturb access. */
    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /**
     * Whether the phone's own Do Not Disturb rules let alarms through priority mode. If they don't,
     * Nocturna never turns it on, since its alarms must ring.
     */
    fun alarmsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true // alarms always come through before Android 9
        val policy = context.getSystemService(NotificationManager::class.java).notificationPolicy
        return policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
    }

    /** The clock came on screen; checks again every minute, for the night window's edges, until it leaves. */
    fun shown(context: Context) {
        val app = context.applicationContext
        surfaces++
        if (ticker == null) ticker = scope.launch {
            while (isActive) {
                check(app)
                delay(60_000)
            }
        }
    }

    /**
     * The clock left the screen: puts DND back if Nocturna set it. It waits a moment first, so a clock
     * that comes straight back (the screen saver taking over from the app, or the app recreated) doesn't
     * switch DND off and on.
     */
    fun hidden(context: Context) {
        val app = context.applicationContext
        surfaces = (surfaces - 1).coerceAtLeast(0)
        if (surfaces > 0) return
        ticker?.cancel()
        ticker = null
        night = false
        scope.launch {
            delay(HIDE_GRACE_MS)
            if (surfaces == 0) check(app)
        }
    }

    /** The clock's night mode (the dim red look) went on or off. */
    fun nightChanged(context: Context, night: Boolean) {
        if (this.night == night) return
        this.night = night
        check(context.applicationContext)
    }

    /**
     * Brings DND in line with the setting and the night; at app start (nothing on screen yet) it puts back
     * DND left on by a clock that went away with the app.
     */
    fun check(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val prefs = Storage.prefs(context, PREFS)
        val memory = DndMemory(prefs.getBoolean(KEY_SET, false), prefs.getBoolean(KEY_STANDING_DOWN, false))
        val access = nm.isNotificationPolicyAccessGranted
        if (!access && memory == DndMemory()) return // nothing Nocturna can or need do

        val settings = SettingsStore.current
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val wanted = access && shouldDndBeOn(
            enabled = settings.dndWhileShowing,
            showing = surfaces > 0,
            day = isDayModeActive(settings, now, zone),
            night = night,
            alarmsAllowed = alarmsAllowed(context),
            now = now,
            zone = zone,
        )
        val current = when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> DndFilter.OFF
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> DndFilter.PRIORITY
            else -> DndFilter.OTHER
        }
        val step = dndStep(wanted, current, memory)
        // Without access (taken back while DND was ours) there's nothing to put back: forget it.
        if (access) {
            try {
                when (step.action) {
                    DndAction.TURN_ON -> nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    DndAction.RESTORE -> nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    DndAction.NONE -> Unit
                }
            } catch (_: SecurityException) {
                // Access was taken back just now.
                if (step.action == DndAction.TURN_ON) return
            }
        }
        if (step.memory != memory) {
            prefs.edit {
                putBoolean(KEY_SET, step.memory.setByUs)
                putBoolean(KEY_STANDING_DOWN, step.memory.standingDown)
            }
        }
    }

    /** Device-protected, and left out of backups: it describes this phone's DND only. */
    private const val PREFS = "nocturna_dnd"
    private const val KEY_SET = "setByUs"
    private const val KEY_STANDING_DOWN = "standingDown"
    private const val HIDE_GRACE_MS = 2_000L
}
