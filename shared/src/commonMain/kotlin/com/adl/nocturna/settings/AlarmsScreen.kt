package com.adl.nocturna.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adl.nocturna.alarm.DAYS_OFF_LOOK_AHEAD
import com.adl.nocturna.alarm.DEFAULT_DAY_OFF_KEYWORDS
import com.adl.nocturna.alarm.DaysOff
import com.adl.nocturna.alarm.DismissChallenge
import com.adl.nocturna.alarm.STREAM_TIMEOUT_MS
import com.adl.nocturna.alarm.SUM_COUNTS
import com.adl.nocturna.alarm.datePassed
import com.adl.nocturna.alarm.daysOff
import com.adl.nocturna.alarm.nextDayOffSkip
import com.adl.nocturna.alarm.nextRing
import com.adl.nocturna.alarm.parseDayOffKeywords
import com.adl.nocturna.alarm.skipping
import com.adl.nocturna.alarm.sleepHoursLabel
import com.adl.nocturna.alarm.switched
import com.adl.nocturna.alarm.toggleSkip
import com.adl.nocturna.alarm.withDate
import com.adl.nocturna.alarm.withDays
import com.adl.nocturna.alarm.withTime
import com.adl.nocturna.clock.LocalClockPlatform
import com.adl.nocturna.clock.Moment
import com.adl.nocturna.data.Alarm
import com.adl.nocturna.data.AlarmStore
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.data.Station
import com.adl.nocturna.resources.*
import com.adl.nocturna.sound.displayName
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.formatTime
import com.adl.nocturna.text.strings
import com.adl.nocturna.ui.currentLocale
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlarmsScreen(padding: PaddingValues) {
    val alarms by AlarmStore.alarms.collectAsStateWithLifecycle()
    val snooze by AlarmStore.snooze.collectAsStateWithLifecycle()
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    val platform = LocalSettingsPlatform.current
    val hasProximity = LocalClockPlatform.current.proximity != null
    val access = platform.access()
    val notificationsAllowed = access.notifications
    val askNotifications = platform.rememberPermissionRequest(Permission.NOTIFICATIONS) { }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val locale = currentLocale()
    val resources = rememberResourceEnvironment()
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var adding by remember { mutableStateOf(false) }
    var dating by remember { mutableStateOf<Alarm?>(null) }
    fun tell(message: suspend () -> String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message())
        }
    }
    // Rescheduling also when access is refused: the alarm then rings as usual, and its time may change.
    val askCalendar = platform.rememberPermissionRequest(Permission.CALENDAR) { platform.rescheduleAlarms() }
    val daysOff = rememberDaysOff(alarms.any { it.enabled && it.repeats && it.skipDaysOff } && access.calendar, s.dayOffKeywords)
    // The bedtime reminder is set only while notifications are allowed: set it again when they're turned on.
    val askBedtimeNotifications = platform.rememberPermissionRequest(Permission.NOTIFICATIONS) { platform.rescheduleAlarms() }
    LaunchedEffect(notificationsAllowed) { if (s.bedtimeSleepHours > 0f) platform.rescheduleAlarms() }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (!notificationsAllowed) item { NotificationsOffWarning() }
            item {
                Button(
                    onClick = {
                        adding = true
                        if (!notificationsAllowed) askNotifications()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(Res.string.alarms_add)) }
            }
            snooze?.let { sn ->
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val t = Instant.fromEpochMilliseconds(sn.until).toLocalDateTime(TimeZone.currentSystemDefault()).time
                        Text(stringResource(Res.string.alarms_snoozed_until, formatTime(t, s.use24h, locale)), modifier = Modifier.weight(1f))
                        TextButton(onClick = { AlarmStore.clearSnooze() }) { Text(stringResource(Res.string.alarms_cancel_snooze)) }
                    }
                }
            }
            if (alarms.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.alarms_none),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm, s.use24h, locale, s.stations, daysOff, access.calendar,
                    onAskCalendar = askCalendar,
                    onEditTime = { editing = alarm },
                    onPickDate = { dating = alarm },
                    onDelete = {
                        val removed = AlarmStore.delete(alarm.id) ?: return@AlarmCard
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar(
                                getString(resources, Res.string.alarms_deleted, formatTime(LocalTime(removed.hour, removed.minute), s.use24h, locale)),
                                actionLabel = getString(resources, Res.string.alarms_undo),
                                duration = SnackbarDuration.Long,
                            )
                            if (result == SnackbarResult.ActionPerformed) AlarmStore.restore(removed)
                        }
                    },
                )
            }
            item { SectionHeader(stringResource(Res.string.alarms_behaviour)) }
            item {
                SliderRow(stringResource(Res.string.alarms_snooze_length), s.snoozeMinutes.toFloat(), 1f..30f, { strings(locale).minutesShort(it.roundToInt()) }, steps = 28) { v ->
                    SettingsStore.update { it.copy(snoozeMinutes = v.roundToInt()) }
                }
            }
            item {
                SliderRow(
                    stringResource(Res.string.alarms_gentle), s.alarmRampSeconds.toFloat(), 0f..120f,
                    { strings(locale).rampLabel(it.roundToInt()) },
                    steps = 11,
                ) { v -> SettingsStore.update { it.copy(alarmRampSeconds = v.roundToInt()) } }
            }
            item { SwitchRow(stringResource(Res.string.alarms_vibrate), s.alarmVibrate) { v -> SettingsStore.update { it.copy(alarmVibrate = v) } } }
            if (hasProximity) {
                item {
                    SwitchRow(
                        stringResource(Res.string.alarms_wave), s.waveToSnooze,
                        stringResource(Res.string.alarms_wave_detail),
                    ) { v -> SettingsStore.update { it.copy(waveToSnooze = v) } }
                }
            }
            if (alarms.any { it.challenge == DismissChallenge.SUM }) {
                item {
                    ChoiceRow(stringResource(Res.string.alarms_sums), SUM_COUNTS.toList(), s.alarmSums, { sumsLabel(it, locale) }) { v ->
                        SettingsStore.update { it.copy(alarmSums = v) }
                    }
                }
            }
            item {
                SwitchRow(stringResource(Res.string.alarms_show_weather), s.alarmShowWeather, stringResource(Res.string.alarms_show_weather_detail)) { v ->
                    SettingsStore.update { it.copy(alarmShowWeather = v) }
                }
            }
            item {
                ChoiceRow(
                    stringResource(Res.string.alarms_sunrise), Settings.SUNRISE_CHOICES, s.sunriseMinutes,
                    { if (it == 0) strings(locale).off else strings(locale).minutesShort(it) },
                ) { v -> SettingsStore.update { it.copy(sunriseMinutes = v) } }
            }
            if (s.sunriseMinutes > 0) {
                item { Hint(stringResource(Res.string.alarms_sunrise_detail)) }
            }
            item {
                ChoiceRow(stringResource(Res.string.alarms_bedtime), Settings.BEDTIME_SLEEP_HOURS, s.bedtimeSleepHours, { sleepHoursLabel(it, locale) }) { v ->
                    SettingsStore.update { it.copy(bedtimeSleepHours = v) }
                    platform.rescheduleAlarms()
                    // Asks only where the system has a notifications permission (Android 13 and later).
                    if (v > 0f && !notificationsAllowed) askBedtimeNotifications()
                }
            }
            if (s.bedtimeSleepHours > 0f) {
                item {
                    Hint(
                        stringResource(Res.string.alarms_bedtime_detail, sleepHoursLabel(s.bedtimeSleepHours, locale)) +
                            if (notificationsAllowed) "" else " " + stringResource(Res.string.alarms_needs_notifications),
                    )
                }
            }
            item { DayOffKeywordsField(s.dayOffKeywords) }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (adding) {
        val nextHour = (Moment.now().hour + 1) % 24
        TimeDialog(nextHour, 0, s.use24h, onDismiss = { adding = false }) { h, m ->
            AlarmStore.add(h, m)
            adding = false
        }
    }
    editing?.let { alarm ->
        TimeDialog(alarm.hour, alarm.minute, s.use24h, onDismiss = { editing = null }) { h, m ->
            val now = Clock.System.now()
            val zone = TimeZone.currentSystemDefault()
            val current = AlarmStore.get(alarm.id)
            val date = current?.ringDate
            if (current != null && date != null && current.withTime(h, m, now, zone) == null) {
                tell { getString(resources, Res.string.alarms_already_passed, formatTime(LocalTime(h, m), s.use24h, locale), dayLabel(date, locale)) }
            } else {
                AlarmStore.update(alarm.id) { it.withTime(h, m, now, zone) ?: it }
            }
            editing = null
        }
    }
    dating?.let { alarm ->
        DateDialog(alarm.ringDate, onDismiss = { dating = null }) { date ->
            dating = null
            val now = Clock.System.now()
            val zone = TimeZone.currentSystemDefault()
            val current = AlarmStore.get(alarm.id) ?: return@DateDialog
            if (current.withDate(date, now, zone) == null) {
                tell { getString(resources, Res.string.alarms_already_passed, formatTime(LocalTime(current.hour, current.minute), s.use24h, locale), dayLabel(date, locale)) }
            } else {
                AlarmStore.update(alarm.id) { it.withDate(date, now, zone) ?: it }
            }
        }
    }
}

/** A date picker for a one-time alarm, which offers no day before today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate?, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    // The picker counts days as midnight UTC, whatever the phone's zone.
    val now = Moment.now()
    val today = now.instant.toLocalDateTime(now.zone).date
    val todayUtc = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: today).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            override fun isSelectableYear(year: Int) = year >= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton onDismiss()
                    onConfirm(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) { DatePicker(state) }
}

/** A one-time alarm's date, or a button to give it one; repeat days and a date don't go together. */
@Composable
private fun DateRow(alarm: Alarm, locale: PlatformLocale, onPickDate: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val date = alarm.ringDate
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.alarms_date), style = MaterialTheme.typography.bodyMedium)
            Text(
                if (date == null) stringResource(Res.string.alarms_date_none) else stringResource(Res.string.alarms_date_set),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        TextButton(onClick = onPickDate) { Text(date?.let { dayLabel(it, locale) } ?: stringResource(Res.string.alarms_pick_date)) }
        if (date != null) {
            TextButton(onClick = { AlarmStore.update(alarm.id) { it.copy(date = null) } }) { Text(stringResource(Res.string.alarms_clear)) }
        }
    }
}

/** A day as the alarm list shows it, "Fri 21 Nov" in British English. */
private fun dayLabel(date: LocalDate, locale: PlatformLocale): String =
    DateTimePattern(bestDateTimePattern("EEEdMMM", locale), locale).format(date.atTime(0, 0))

@Composable
private fun AlarmCard(
    alarm: Alarm,
    use24h: Boolean,
    locale: PlatformLocale,
    stations: List<Station>,
    daysOff: DaysOff,
    calendarAllowed: Boolean,
    onAskCalendar: () -> Unit,
    onEditTime: () -> Unit,
    onPickDate: () -> Unit,
    onDelete: () -> Unit,
) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(LocalTime(alarm.hour, alarm.minute), use24h, locale),
                    fontSize = 44.sp,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f).clickable(onClick = onEditTime),
                )
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = { on -> AlarmStore.update(alarm.id) { it.switched(on, Clock.System.now(), TimeZone.currentSystemDefault()) } },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    describe(alarm, use24h, locale, daysOff),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.weight(1f),
                )
                if (alarm.enabled && alarm.repeats) {
                    val skipping = alarm.skipping(Clock.System.now(), TimeZone.currentSystemDefault(), daysOff) != null
                    TextButton(onClick = { AlarmStore.update(alarm.id) { it.toggleSkip(Clock.System.now(), TimeZone.currentSystemDefault(), daysOff) } }) {
                        Text(if (skipping) stringResource(Res.string.alarms_undo_skip) else stringResource(Res.string.alarms_skip_next))
                    }
                }
            }
            if (alarm.enabled) DayOffSkipLine(alarm, locale, daysOff)
            Spacer(Modifier.height(8.dp))
            DayToggles(alarm.days, locale) { days -> AlarmStore.update(alarm.id) { it.withDays(days) } }
            if (alarm.repeats) SkipDaysOffRow(alarm, calendarAllowed, onAskCalendar) else DateRow(alarm, locale, onPickDate)
            SoundPicker(alarm, stations, locale)
            ChallengePicker(alarm, locale)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LabelField(alarm, Modifier.weight(1f).padding(top = 8.dp))
                TextButton(onClick = onDelete) { Text(stringResource(Res.string.alarms_delete), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

/** Chooses what the alarm plays: the alarm tone or one of the radio stations. */
@Composable
private fun SoundPicker(alarm: Alarm, stations: List<Station>, locale: PlatformLocale) {
    var open by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    fun choose(station: Station?) {
        open = false
        AlarmStore.update(alarm.id) { it.copy(station = station) }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.alarms_sound), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { open = true }) { Text(alarm.station?.displayName(locale) ?: stringResource(Res.string.alarms_tone)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.alarms_tone)) }, onClick = { choose(null) })
                stations.forEach { st -> DropdownMenuItem(text = { Text(st.displayName(locale)) }, onClick = { choose(st) }) }
            }
        }
    }
    if (alarm.station != null) {
        Text(
            stringResource(Res.string.alarms_stream_fallback, STREAM_TIMEOUT_MS / 1000),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )
    }
}

/** *To stop it*: what it takes to stop the alarm when it rings. Snoozing is always one tap. */
@Composable
private fun ChallengePicker(alarm: Alarm, locale: PlatformLocale) {
    var open by remember { mutableStateOf(false) }
    fun choose(challenge: DismissChallenge) {
        open = false
        AlarmStore.update(alarm.id) { it.copy(challenge = challenge) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.alarms_to_stop), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { open = true }) { Text(alarm.challenge.label(locale)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DismissChallenge.entries.forEach { c -> DropdownMenuItem(text = { Text(c.label(locale)) }, onClick = { choose(c) }) }
            }
        }
    }
}

private fun sumsLabel(n: Int, locale: PlatformLocale) =
    strings(locale).sumsLabel(n, easiest = n == SUM_COUNTS.first, hardest = n == SUM_COUNTS.last)

/** The ring the alarm skips for a day off, and the calendar event that makes it one. */
@Composable
private fun DayOffSkipLine(alarm: Alarm, locale: PlatformLocale, daysOff: DaysOff) {
    val now = Moment.now()
    val skip = alarm.nextDayOffSkip(now.instant, now.zone, daysOff) ?: return
    val day = dayLabel(skip.date, locale)
    Text(
        stringResource(Res.string.alarms_skips_day, day, skip.title),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** *Skip on days off*, which needs calendar access and says so while it doesn't have it. */
@Composable
private fun SkipDaysOffRow(alarm: Alarm, calendarAllowed: Boolean, onAskCalendar: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.alarms_skip_days_off), style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    !alarm.skipDaysOff -> stringResource(Res.string.alarms_skip_days_off_off)
                    calendarAllowed -> stringResource(Res.string.alarms_skip_days_off_on)
                    else -> stringResource(Res.string.alarms_skip_days_off_no_access)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (alarm.skipDaysOff && !calendarAllowed) MaterialTheme.colorScheme.error else muted,
            )
        }
        Switch(
            checked = alarm.skipDaysOff,
            onCheckedChange = { on ->
                AlarmStore.update(alarm.id) { it.copy(skipDaysOff = on) }
                if (on && !calendarAllowed) onAskCalendar()
            },
        )
    }
    if (alarm.skipDaysOff && !calendarAllowed) TextButton(onClick = onAskCalendar) { Text(stringResource(Res.string.daypage_allow_calendar)) }
}

/** The words that make an all-day event a day off, saved once typing pauses. */
@Composable
private fun DayOffKeywordsField(saved: String) {
    val platform = LocalSettingsPlatform.current
    var text by remember { mutableStateOf(saved) }
    LaunchedEffect(text) {
        if (text == saved) return@LaunchedEffect
        delay(600)
        SettingsStore.update { it.copy(dayOffKeywords = text) }
        platform.rescheduleAlarms()
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(Res.string.alarms_days_off), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(Res.string.alarms_days_off_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(DEFAULT_DAY_OFF_KEYWORDS) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (text != DEFAULT_DAY_OFF_KEYWORDS) {
            TextButton(onClick = { text = DEFAULT_DAY_OFF_KEYWORDS }) { Text(stringResource(Res.string.alarms_default_words)) }
        }
    }
}

/** Days off in the phone's calendars for the next [DAYS_OFF_LOOK_AHEAD] days, kept current; empty when [enabled] is off. */
@Composable
private fun rememberDaysOff(enabled: Boolean, keywordText: String): DaysOff {
    val now = Moment.now()
    val today = now.instant.toLocalDateTime(now.zone).date
    val last = today.plus(DAYS_OFF_LOOK_AHEAD, DateTimeUnit.DAY)
    val events = LocalClockPlatform.current.calendarEvents(enabled, today, last, now.zone)
    val keywords = remember(keywordText) { parseDayOffKeywords(keywordText) }
    return remember(events, keywords, today) { events?.let { daysOff(it, keywords, today, last) } ?: emptyMap() }
}

/** Edits the label locally and saves once typing pauses, instead of on every keystroke. */
@Composable
private fun LabelField(alarm: Alarm, modifier: Modifier) {
    var label by remember(alarm.id) { mutableStateOf(alarm.label) }
    LaunchedEffect(label) {
        if (label == alarm.label) return@LaunchedEffect
        delay(600)
        AlarmStore.update(alarm.id) { it.copy(label = label) }
    }
    OutlinedTextField(
        value = label,
        onValueChange = { label = it },
        placeholder = { Text(stringResource(Res.string.alarms_label)) },
        singleLine = true,
        modifier = modifier,
    )
}

private fun describe(alarm: Alarm, use24h: Boolean, locale: PlatformLocale, daysOff: DaysOff): String {
    val t = strings(locale)
    if (!alarm.enabled) return t.off
    val now = Moment.now()
    val pattern = DateTimePattern(bestDateTimePattern(if (use24h) "EEEdMMMHHmm" else "EEEdMMMhmma", locale), locale)
    alarm.ringDate?.let { date ->
        return if (alarm.datePassed(now.instant, now.zone)) t.datePassed(dayLabel(date, locale)) else dayLabel(date, locale)
    }
    val next = alarm.nextRing(now.instant, now.zone, daysOff)?.toLocalDateTime(now.zone) ?: return t.once
    val skipped = alarm.skipping(now.instant, now.zone, daysOff)?.let { dayLabel(it, locale) }
    val repeat = when (alarm.days) {
        0 -> t.once
        Alarm.ALL_DAYS -> t.everyDay
        0b0011111 -> t.weekdays
        0b1100000 -> t.weekends
        else -> t.repeats
    }
    return t.alarmDescription(repeat, skipped, pattern.format(next))
}
