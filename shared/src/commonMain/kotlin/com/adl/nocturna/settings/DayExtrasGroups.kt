package com.adl.nocturna.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adl.nocturna.data.Countdown
import com.adl.nocturna.data.Settings
import com.adl.nocturna.data.SettingsStore
import com.adl.nocturna.day.AddTodoDialog
import com.adl.nocturna.day.addTodo
import com.adl.nocturna.day.monthDayDate
import com.adl.nocturna.day.removeTodo
import com.adl.nocturna.day.todosForDay
import com.adl.nocturna.resources.*
import com.adl.nocturna.text.DateTimePattern
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.bestDateTimePattern
import com.adl.nocturna.text.strings
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/*
 * Settings › Day mode's newer groups: countdowns, the to-do list, breaks and reminders, and the birthday
 * for the holiday touches.
 */

private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** [date] as "Sat 20 Dec" in [locale]'s words and order. */
private fun shortDate(date: LocalDate, locale: PlatformLocale): String =
    DateTimePattern(bestDateTimePattern("EEEdMMM", locale), locale).format(date.atTime(0, 0))

/** The days the desk clock counts down to, each removable, a way to add one, and the days off from the calendar. */
@Composable
fun CountdownsGroup(s: Settings, locale: PlatformLocale, calendarAccess: Boolean, askCalendar: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    if (adding) {
        CountdownDialog(locale, onDismiss = { adding = false }) { countdown ->
            adding = false
            SettingsStore.update { it.copy(countdowns = (it.countdowns + countdown).take(Settings.MAX_COUNTDOWNS)) }
        }
    }
    SettingsGroup(stringResource(Res.string.daypage_countdowns)) {
        val today = today()
        s.countdowns.forEachIndexed { i, c ->
            val date = runCatching { LocalDate.parse(c.date) }.getOrNull()
            RemovableRow(c.label, date?.let { shortDate(it, locale) }.orEmpty(), past = date != null && date < today) {
                SettingsStore.update { st -> st.copy(countdowns = st.countdowns.filterIndexed { j, _ -> j != i }) }
            }
        }
        if (s.countdowns.size < Settings.MAX_COUNTDOWNS) {
            ActionButton(stringResource(if (s.countdowns.isEmpty()) Res.string.daypage_countdown_add else Res.string.daypage_countdown_add_another)) { adding = true }
        }
        Hint(stringResource(Res.string.daypage_countdown_hint, Settings.MAX_COUNTDOWNS), Modifier.padding(bottom = 6.dp))
        SwitchRow(
            stringResource(Res.string.daypage_countdown_days_off), s.countdownDaysOff,
            if (s.countdownDaysOff && !calendarAccess) stringResource(Res.string.daypage_needs_calendar) else stringResource(Res.string.daypage_countdown_days_off_detail),
        ) { v ->
            if (v && !calendarAccess) askCalendar() else SettingsStore.update { it.copy(countdownDaysOff = v) }
        }
    }
}

/** A row with a title, a detail under it and a Remove button; a [past] one is crossed out. */
@Composable
private fun RemovableRow(title: String, detail: String, past: Boolean = false, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (past) TextDecoration.LineThrough else null))
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.daypage_remove), color = MaterialTheme.colorScheme.error) }
    }
}

/** Asks for a countdown's name and its day. */
@Composable
private fun CountdownDialog(locale: PlatformLocale, onDismiss: () -> Unit, onAdd: (Countdown) -> Unit) {
    var label by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        DateDialog(date ?: today(), onDismiss = { picking = false }) { picked -> picking = false; date = picked }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.daypage_countdown_new)) },
        text = {
            Column {
                OutlinedTextField(
                    label, { label = it.take(40) },
                    label = { Text(stringResource(Res.string.daypage_countdown_label)) },
                    placeholder = { Text(stringResource(Res.string.daypage_countdown_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { picking = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(date?.let { shortDate(it, locale) } ?: stringResource(Res.string.daypage_countdown_pick_date))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { date?.let { d -> onAdd(Countdown(label.trim(), d.toString())) } },
                enabled = label.isNotBlank() && date != null,
            ) { Text(stringResource(Res.string.day_todo_add_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Material's date picker, starting on [initial]; [onPick] gets the day picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    // The picker works in UTC midnights.
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date) } ?: onDismiss()
            }) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) { DatePicker(state) }
}

/** The to-do list on the sticky note: on or off, its items, each removable, and a way to add one. */
@Composable
fun TodosGroup(s: Settings) {
    var adding by remember { mutableStateOf(false) }
    if (adding) {
        AddTodoDialog(onDismiss = { adding = false }) { text ->
            adding = false
            SettingsStore.update { it.copy(todos = addTodo(it.todos, text, today())) }
        }
    }
    SettingsGroup(stringResource(Res.string.daypage_todos)) {
        SwitchRow(stringResource(Res.string.daypage_todos_switch), s.dayTodos, stringResource(Res.string.daypage_todos_detail)) { v ->
            SettingsStore.update { it.copy(dayTodos = v) }
        }
        if (s.dayTodos) {
            val shown = todosForDay(s.todos, today())
            shown.forEachIndexed { i, item ->
                RemovableRow(item.text, "", past = item.done) {
                    SettingsStore.update { st -> st.copy(todos = removeTodo(todosForDay(st.todos, today()), i)) }
                }
            }
            if (shown.size < Settings.MAX_TODOS) ActionButton(stringResource(Res.string.daypage_todo_add)) { adding = true }
            Hint(stringResource(Res.string.daypage_todos_hint, Settings.MAX_TODOS), Modifier.padding(bottom = 6.dp))
        }
    }
}

/** How often to stretch and drink some water, and when to unplug. */
@Composable
fun RemindersGroup(s: Settings, locale: PlatformLocale) {
    val t = strings(locale)
    fun minutes(m: Int) = if (m == 0) t.off else t.minutesShort(m)
    SettingsGroup(stringResource(Res.string.daypage_reminders)) {
        ChoiceRow(stringResource(Res.string.daypage_stretch), Settings.REMINDER_MINUTES, s.stretchReminderMinutes, ::minutes) { v ->
            SettingsStore.update { it.copy(stretchReminderMinutes = v) }
        }
        ChoiceRow(stringResource(Res.string.daypage_water), Settings.REMINDER_MINUTES, s.waterReminderMinutes, ::minutes) { v ->
            SettingsStore.update { it.copy(waterReminderMinutes = v) }
        }
        Hint(stringResource(Res.string.daypage_reminders_hint), Modifier.padding(bottom = 6.dp))
        ChoiceRow(stringResource(Res.string.daypage_battery_care), Settings.BATTERY_CARE_PERCENT, s.batteryCarePercent, { if (it == 0) t.off else "$it%" }) { v ->
            SettingsStore.update { it.copy(batteryCarePercent = v) }
        }
        Hint(stringResource(Res.string.daypage_battery_care_hint), Modifier.padding(bottom = 6.dp))
    }
}

/** The user's birthday, for confetti on the day: picked on a calendar, its year ignored. */
@Composable
fun BirthdayRows(s: Settings, locale: PlatformLocale) {
    var picking by remember { mutableStateOf(false) }
    val date = monthDayDate(s.birthday)
    if (picking) {
        DateDialog(date ?: today(), onDismiss = { picking = false }) { picked ->
            picking = false
            SettingsStore.update { it.copy(birthday = picked.month.number * 100 + picked.day) }
        }
    }
    NavRow(
        stringResource(Res.string.daypage_birthday),
        date?.let { DateTimePattern(bestDateTimePattern("dMMMM", locale), locale).format(it.atTime(0, 0)) } ?: stringResource(Res.string.daypage_birthday_not_set),
    ) { picking = true }
    if (date != null) ActionButton(stringResource(Res.string.daypage_birthday_clear)) { SettingsStore.update { it.copy(birthday = 0) } }
}
