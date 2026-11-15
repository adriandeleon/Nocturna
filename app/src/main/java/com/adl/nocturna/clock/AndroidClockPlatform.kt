package com.adl.nocturna.clock

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.data.Station
import com.adl.nocturna.day.CalendarEvent
import com.adl.nocturna.day.CalendarEvents
import com.adl.nocturna.day.DayPhoto
import com.adl.nocturna.day.MeetingChime
import com.adl.nocturna.day.rememberCalendarEvents
import com.adl.nocturna.notify.NotificationIconService
import com.adl.nocturna.radio.Radio
import com.adl.nocturna.weather.DeviceLocation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** The shared clock's view of the phone: its sensors, battery, alarms, notifications, radio, location, calendars and photo. */
class AndroidClockPlatform(context: Context) : ClockPlatform {
    private val app = context.applicationContext

    @Composable
    override fun ambientLux(): Float? = rememberAmbientLux().value

    @Composable
    override fun battery(): BatteryInfo? = rememberBattery().value

    @Composable
    override fun nextAlarm(): Long? = rememberNextAlarm().value

    @Composable
    override fun notificationIcons(): List<NotificationIcon> {
        val icons by NotificationIconService.icons.collectAsStateWithLifecycle()
        return remember(icons) {
            icons.map { n ->
                NotificationIcon(n.key) { px -> runCatching { n.icon.loadDrawable(app)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull() }
            }
        }
    }

    override val radio: RadioControl = object : RadioControl {
        override val isOn = Radio.isOn
        override val title = Radio.title
        override val error = Radio.error
        override val sleepAt = Radio.sleepAt

        override fun toggle(station: Station?) = Radio.toggle(app, station)

        override fun setSleepTimer(minutes: Int) = Radio.setSleepTimer(minutes)
    }

    override suspend fun deviceLocation(): Pair<Double, Double>? = DeviceLocation.get(app)

    @Composable
    override fun calendarEvents(enabled: Boolean, first: LocalDate, last: LocalDate, zone: TimeZone): List<CalendarEvent>? =
        rememberCalendarEvents(enabled, first, last, zone).value

    override fun hasCalendarAccess(): Boolean = CalendarEvents.hasPermission(app)

    override suspend fun loadDayPhoto(): ImageBitmap? = DayPhoto.load(app)

    override fun playMeetingChime() = MeetingChime.play(app)
}
