package com.adl.nocturna.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.adl.nocturna.R
import com.adl.nocturna.clock.ClockActivity
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.radio.RadioService
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.bestDateTimePattern
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Home-screen widget: the time, the next alarm and buttons for the clock and the sleep radio. The time is a
 * TextClock, which the launcher keeps ticking; everything else is redrawn when the next alarm, the time zone,
 * the language, the 12/24-hour setting, the selected station or the radio's state changes, never on a timer.
 */
class ClockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, manager.getAppWidgetOptions(it))) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        manager.updateAppWidget(id, views(context, options))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) updateAll(context)
    }

    companion object {
        private val REFRESH_ACTIONS = setOf(
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidget::class.java))
            if (ids.isNotEmpty()) ClockWidget().onUpdate(context, manager, ids)
        }

        /** Redraws the widgets whenever the radio or the settings they show change, while the app runs. */
        fun watch(context: Context) {
            val app = context.applicationContext
            MainScope().launch {
                combine(RadioService.nowPlaying, SettingsStore.settings) { playing, s -> Triple(playing, s.use24h, s.currentStation) }
                    .distinctUntilChanged()
                    .collect { updateAll(app) }
            }
        }

        private fun views(context: Context, options: Bundle?): RemoteViews {
            val settings = SettingsStore.current
            val locale = context.resources.configuration.locales[0]
            val views = RemoteViews(context.packageName, R.layout.widget_clock)

            val clockPattern = if (settings.use24h) "HH:mm" else "h:mm"
            views.setCharSequence(R.id.widget_time, "setFormat12Hour", clockPattern)
            views.setCharSequence(R.id.widget_time, "setFormat24Hour", clockPattern)
            val width = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
            if (width > 0 && height > 0) {
                views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_SP, widgetClockSizeSp(width, height).toFloat())
            }

            val alarmAt = context.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime
            val alarm = nextAlarmText(
                alarmAt?.let { Instant.fromEpochMilliseconds(it) },
                Clock.System.now(),
                TimeZone.currentSystemDefault(),
                settings.use24h,
                locale,
                DateTimePattern(bestDateTimePattern("EEEdMMM", locale), locale),
            )
            views.setTextViewText(R.id.widget_alarm, alarm.orEmpty())
            views.setViewVisibility(R.id.widget_alarm, if (alarm == null) View.GONE else View.VISIBLE)

            val playing = RadioService.nowPlaying.value
            val nowPlaying = playing?.ifEmpty { settings.currentStation?.name }
            views.setTextViewText(R.id.widget_playing, nowPlaying?.let { "♪ $it" }.orEmpty())
            views.setViewVisibility(R.id.widget_playing, if (nowPlaying == null) View.GONE else View.VISIBLE)
            views.setTextViewText(
                R.id.widget_radio,
                context.getString(if (playing != null) R.string.widget_stop_radio else R.string.tile_sleep_radio),
            )

            val openClock = PendingIntent.getActivity(
                context, 0,
                Intent(context, ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_time, openClock)
            views.setOnClickPendingIntent(R.id.widget_clock, openClock)
            views.setOnClickPendingIntent(
                R.id.widget_radio,
                PendingIntent.getBroadcast(
                    context, 0,
                    Intent(context, SleepRadioReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            return views
        }
    }
}
