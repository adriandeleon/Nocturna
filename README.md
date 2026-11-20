# Nocturna

[![CI](https://github.com/adriandeleon/Nocturna/actions/workflows/ci.yml/badge.svg)](https://github.com/adriandeleon/Nocturna/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/github/license/adriandeleon/Nocturna)](LICENSE)
![Android](https://img.shields.io/badge/Android-8.1%2B%20%28API%2027%29-3ddc84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7f52ff?logo=kotlin&logoColor=white)

A bedside clock for Android: a big, dimmable clock for the nightstand while your phone charges.

Nocturna is in English and Spanish (Mexican usage); it follows your phone's language, and any other language
gets English.

![Nocturna showing the time, date and battery level on a black screen](docs/images/clock.png)

## Features

- **Clock**: digital (six font styles, 12/24 h, optional seconds) or analog, in a colour you pick.
- **Material You**: on Android 12+, the clock can take its colour from your wallpaper (Settings › Clock face › *From wallpaper*), and the settings screens follow the wallpaper's colours.
- **Info line**: date, weather with today's high and low, or tomorrow's forecast in the evening (Open-Meteo, no API key), battery and charging state, next alarm, now-playing radio station.
- **Notification icons**: icons of pending notifications under the clock (needs notification access).
- **Brightness**: follows the room's light sensor between a darkest and brightest level you set.
- **Night mode**: when the room goes dark the clock turns dim red. Without a light sensor it follows the time (22:00–07:00).
- **Battery at night**: Settings › Brightness and night mode › *Battery at night* shows how much charge the last night took while the night clock showed off the charger (*Sun 15 Nov: 12% in 7 h 40 m*), and what 8 hours take on average over the last seven nights. Time on the charger doesn't count; the log stays on the phone and isn't backed up.
- **Tap to peek**: tap the dim clock and it brightens for about six seconds, to 40% of your brightest level and a soft warm colour, then fades back; a second tap shows the controls (in the screen saver, closes it). Before an alarm the brighter of the peek and the sunrise wins; Day mode taps go straight to the controls. Turn it off under Settings › Brightness and night mode.
- **Wave to peek and wave to snooze**: on phones with a proximity sensor, a wave of the hand over the top of the phone can peek at the night clock like a tap (Settings › Brightness and night mode › *Wave to peek*), or snooze a ringing alarm like the Snooze button (Settings › Alarms › *Wave to snooze*); a wave never stops an alarm. A covered phone (face down, in a pocket) doesn't count. Both are off by default, and the sensor is on only while the wake-up screen or the night clock shows.
- **Do Not Disturb at night**: while the clock shows at night (22:00–07:00, or from when night mode starts in the evening), Nocturna can turn on priority-only Do Not Disturb, so alarms still ring, and turn it off again when the clock leaves the screen or in the morning. It never undoes a change you made yourself, and Day mode doesn't touch it. Settings › Starting the clock › *Do Not Disturb at night*; needs Do Not Disturb access.
- **Burn-in protection**: the clock drifts slowly around the screen.
- **Day mode**: a cheerful desk clock, calendar page and sticky note for a phone on a desk stand while you work. See [Day mode](#day-mode).
- **Alarms**: repeat days, labels, snooze, a gentle volume ramp, vibration, and a full-screen wake-up screen over the lock screen. An alarm can wake you with a radio station, and falls back to the alarm tone if the station doesn't play. A repeating alarm can skip its next ring, and a one-time alarm can be set for a date (*Fri 21 Nov*). To stop an alarm you can just tap, solve one to three sums, or hold the button for 3 seconds; snooze is always one tap.
- **Sunrise before alarms**: in the 10, 20 or 30 minutes before an alarm (not a snooze) the clock brightens to your brightest level and warms from night red through amber to a soft white.
- **Sleep sounds and ambient music**: white, pink and brown noise, rain and surf, and three pieces of ambient music (slow chords, night chimes, a deep drone), made on the phone so they play offline; they loop seamlessly and play from the Radio button like a station, sleep timer included.
- **Music on your phone**: add your own audio files, or a folder of them, as stations, through the system's file picker (no storage permission). A file repeats; a folder plays in name order and starts again. Alarms can wake you with them too.
- **Weather on waking**: the wake-up screen shows the current conditions and today's high and low under the time, refreshed in the background if they are old (Settings › Alarms › *Show weather*).
- **Moon phase**: the info line can show the moon's phase on the night clock, a small drawing with its name (*Waxing gibbous*), worked out offline (*Show on the clock › Moon phase*, off by default).
- **Time until the alarm**: within 12 hours of the next alarm the info line counts down to it (*06:30 · in 7 h 20 m*, in steps of 5 minutes), switchable under *Show on the clock*.
- **Bedtime reminder**: a quiet notification the evening before a morning alarm, 15 minutes before you'd need to sleep to get 6 to 9 hours (Settings › Alarms › *Bedtime reminder*, off by default). Not sent while the clock is showing; needs notifications.
- **Skip on days off**: a repeating alarm can stay quiet on days with an all-day calendar event titled as a day off (holiday, vacation, PTO, día libre…, editable), and says which ring it skips. Needs calendar access; without it the alarm rings as usual.
- **Web radio**: any MP3/AAC/HLS stream, with a sleep timer that fades out. The Radio button starts it with the timer set in Settings › Radio stations › *Sleep timer* (off, or 15 minutes to 2 hours; 30 minutes by default), and the clock's *Sleep* button changes it while it plays.
- **Backup**: export settings, radio stations and alarms to one JSON file and import them on this or another phone (Settings › About › Backup). The Day mode photo isn't included. No storage permission: you pick the file.
- **Quick Settings tiles**: *Nocturna* opens the clock; *Sleep radio* plays your selected station with your fading sleep timer, or stops it, and lights up while it plays. To add them, pull down Quick Settings, tap the pencil (Edit), and drag the tiles into place.
- **Home-screen widget**: the time and your next alarm in the app's dark colours, with *Clock* and *Sleep radio* buttons. Long-press the home screen, tap *Widgets*, find Nocturna and drag *Nocturna clock* onto the screen; long-press it to resize.
- **Starts on its own**:
  - as the Android screen saver (Settings › Display › Screen saver › Nocturna, "while charging"), or
  - with **Open when charging**, which opens the clock when you plug in a locked phone. This needs the "Display over other apps" permission; without it the app shows a full-screen notification instead.

Tap the clock to show the controls (Day mode, Alarms, Radio, Sleep timer, Settings).

## Day mode

![Day mode on a desk stand, in landscape and portrait: the weather on a pane of glass over the sky, a navy desk clock, a spiral-bound November calendar page and a yellow sticky note with the day's to-dos, over sunny hills](docs/images/day-mode.png)

The bedside clock is for the night; Day mode is for the desk, when your phone sits on a charging stand while you work.

- **Desk clock**: the time, date, battery and next alarm, in white on a navy panel that reads over any background.
- **Weather**: over the sky just above the desk clock, on a pane of see-through navy glass: the conditions and temperature, today's high and low (tomorrow's in the evening), the glance line's weather and the next hours. It drifts with the clock and moves with it when the sides swap.
- **World clock**: up to two other time zones under the desk clock's time, e.g. *London 19:42 · Tokyo 04:42 +1*, in your 12 or 24-hour format; *+1* or *−1* marks a different day there. Add them in Settings › Day mode › *Other time zones*, from a searchable list of about 160 cities or by time zone name (*America/Argentina/Cordoba*), and remove them there. They drift with the clock and are left out when the screen has no room: in landscape the sticky note keeps its place first, and in portrait the desk clock never takes more than two-fifths of the height.
- **Next hours**: at the bottom of the weather over the sky, the next six hours' weather and temperature (*14h ☀ 22° · 15h ⛅ 23° …*), with the chance of rain when it's 30% or more, and 12-hour labels on a 12-hour clock. Where the screen is narrow it shows fewer hours, or none rather than crowd the clock, and it drifts with the clock for burn-in protection. It comes in the same Open-Meteo request as the rest of the weather. Settings › Day mode › *Next hours* (on by default) shows once the weather is on and has a place.
- **Calendar page**: a spiral-bound page in the month's own colour, in one of three views. **Month** has today circled, weekends highlighted and a dot under each day that has events. **Week** shows this week a day per row, with each day's events. **Day** is a tear-off page: today's big date beside the day's schedule: its all-day events, then the last event to have finished (faded) and the next three, moving along as the day goes. Swipe the page left for more detail (month, week, day) and right to go back, or tap *Month · Week · Day* under it. Settings › Day mode › *Calendar view* picks the view it starts with. In the day view the sticky note steps aside, since the page already shows the day's schedule.
- **Sticky note**: a greeting for the time of day and the rest of today's events. The one happening now is marked *Now · until …*; finished ones drop off; if they don't all fit, the note says how many more there are.
- **Next-meeting countdown**: in the hour before a timed event, the note puts it at the top, counting down (*In 12 min · 10:00*, then *In 1 min*) until it turns into *Now · until …*. All-day events don't count down. Where the note isn't showing (the day view, or a phone held sideways with no room under the clock), *Standup in 12 min* hangs in a small tag under the desk clock instead. Tap either to open the event. Settings › Day mode › *Chime before meetings* adds one soft tone 1, 2 or 5 minutes before (Off by default): at the notification volume, never the alarm's, once per meeting, only while Day mode is showing, and not when the phone is on silent, vibrate or Do Not Disturb.
- **Focus timer**: tap the clock and pick *Focus* for a Pomodoro: a 25-minute focus block, a 5-minute break, and after every fourth block a 15-minute break, round and round. The desk clock shows *Focus · 18:42 left* or *Break · 3:10 left* under its info line, beside a thin ring of the time gone (white for focus, green for a break), and drifts with it. Tap that line for *Pause*, *Skip* and *Stop*, or tap the clock and *Focus*, lit up while it runs, to stop. Each switch between focus and a break plays the meeting chime's soft tone, with the same rules: notification volume, not on silent or Do Not Disturb. A meeting countdown keeps its place on the note or under the clock. Settings › Day mode › *Focus timer* sets the three lengths. The timer goes on when you turn the phone or open Nocturna's settings, and stops when you leave Day mode or the app or turn the screen off: there's no background service.
- **Glance line**: under the temperature in the weather over the sky, whichever of these you turn on in Settings › Day mode › *Weather* (all off by default): *Rain from 15:00* in the next six hours, or *Rain until 17:00* while it rains, from the hourly forecast; *Sunset 17:48 · 2 h 10 m of light*, or the next sunrise after dark; *UV 7 · High* while the sun is up and the index is 1 or more; and *Air: good (42)*, the US air quality index from Copernicus (CAMS) through Open-Meteo, fetched once an hour and only while it's on. The countdowns go under the desk clock. In landscape the sticky note keeps its place: where there isn't room for everything, the world clock line goes first, then all but the first weather detail and countdown, then the rest, then the next hours' weather, and last of all the weather shrinks to a single line of the temperature and the day's high and low.
- **Countdowns**: up to three days you add in Settings › Day mode › *Countdowns*, the nearest two at a time (*Vacation in 12 days*, *Party tomorrow*, *Trip today!*). *Count down to days off* adds the next all-day event titled as a day off, with the same words as the alarms' *Days off*. Each goes once its day has passed.
- **To-do list**: Settings › Day mode › *To-do list* puts a short list (up to eight) on the sticky note, under the day's events; add items there or with *+ Add a to-do* on the note, and tap one to tick it off. Ticked items stay crossed off for the rest of the day and are gone the next. With the list on, the note stays in the day view too, for the list and the next meeting's countdown.
- **Breaks and reminders**: every 30 minutes to 2 hours, a yellow note in the desk clock to stand up and stretch, with a few footsteps on gravel, or to drink some water, with a few drops falling into a glass (made on the phone, at the notification volume, with the meeting chime's rules). They wait during a focus block or a meeting and come as soon as it's over; tap one away, or it goes after five minutes. *Battery care* says *Charged to 85% · you can unplug* once the phone on the stand has charged to 80, 85 or 90%, until it's unplugged or tapped away. All off by default.
- **Week numbers and birthdays**: week numbers down the side of the month view, and a 🎂 on birthdays (all-day events with *birthday*, *b-day* or *cumpleaños* in the title, such as a synced contacts' birthdays calendar) on the page and the note. Settings › Day mode › *Calendar*; both off by default.
- **Wave to start or pause**: with a proximity sensor, a wave over the top of the phone starts the focus timer, and pauses or resumes it (Settings › Day mode › *Focus timer*, off by default).
- **Background**: a sunny scene that changes every month, from snow in January to blossoms in April, sunflowers in August, autumn hills in October and fir trees in December. It follows the time of day: a peachy dawn in the morning, the month's bright sky at midday, golden light in the afternoon and a starry night with the moon after dark, blending slowly from one to the next. The clouds drift and the sun's rays turn slowly. With *Weather in the scene* (Settings › Day mode › *Weather*, on by default) it also follows the weather outside: more and greyer clouds and a hidden sun when it's overcast, rain streaks, snowflakes, banks of fog, and a dark sky for a storm. On a few days it dresses up (Settings › Day mode › *Background* › *Holidays*, on by default): confetti on New Year's Eve and Day and on your birthday (pick it there), hearts on Valentine's Day, bats and pumpkins on Halloween and a garland of lights on Christmas Eve and Day, with the sticky note's greeting to match; with the phone's region set to Mexico, papel picado on 15 and 16 September and on the Day of the Dead, when the hills fill with cempasúchil. To use a photo of your own instead, go to Settings › Day mode › *Background* and tap *Monthly pictures*. Nocturna keeps its own smaller copy, so it needs no storage permission and keeps working if you delete the original. *Use the monthly pictures* switches back.

**Tap to open your calendar**: tap a day on the page to open your calendar app on that date, tap an event on the sticky note to open the event, or tap anywhere else on the note to open today. If the phone is locked, it asks you to unlock first. Tapping anywhere else shows the clock's buttons, as usual; the screen saver is for looking only.

Side by side in landscape, stacked in portrait. Day mode never turns night red; brightness still follows the room light if *Adjust to room light* is on, but stops at 50%, so a screen that's lit all day wears more slowly. Change it in Settings › Day mode › *Screen care*.

**Burn-in protection**: Day mode stays on screen all day, so on OLED screens nothing in it stays put. With *Burn-in protection* on, the layout drifts around a tenth of the screen, the clock, the calendar page and the sticky note each drift on their own as well, and every other hour the clock and the page trade places (sides in landscape, top and bottom in portrait). The page's paper, the brightest thing on screen, dims by about a quarter over its first four hours up, too slowly to notice. The background keeps moving whatever the setting: the sun crosses the sky from dawn to dusk and the moon from dusk to dawn, the stars turn, the clouds float up and down as they pass, the hills scroll, swell and change shape, the flowers and trees sway in the breeze, wander about and grow and shrink a little, and are replanted every hour, and your own photo pans gently. The page and the note use warm tones a touch below white.

**Turning it on**: in Settings › Day mode, pick *Always*, or *On a schedule* with the hours and days you work, for example weekdays 09:00–18:00 (hours can run past midnight). On the clock, tap it, then *Day mode* or *Clock only*; with a schedule, that lasts until the schedule's next start or end. The screen saver follows the same setting.

**Today's events** (Settings › Day mode › *Today's events*): read from the calendars synced to the phone, such as Google Calendar. Hidden calendars and invitations you declined are left out. Nocturna asks for calendar access when you turn this on and only ever reads; without access you get the calendar page alone.

## Build

Requirements: JDK 17+ and the Android SDK (platform 37, build-tools) with `sdk.dir` set in `local.properties`.

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug build installs as `com.adl.nocturna.debug`, so it can sit next to a release build.

The project has two Gradle modules: `app`, the Android app, and `shared`, a Kotlin Multiplatform module with
the code that doesn't depend on Android, ready for an iOS app later. Both use the packages below.

## Layout

| Package | What's in it |
|---|---|
| `clock` | `ClockScreen` and the clock faces (`shared`), which reach the phone through `ClockPlatform`; `ClockActivity`, `AndroidClockPlatform` and its sensor/battery/alarm state (`app`); the brightness curve, the battery used at night (`NightBattery`, kept by `NightBatteryStore` in `data`), the sunrise before alarms, tap to peek, telling a wave from the proximity sensor (`shared`, `Wave.kt`; `AndroidProximity` in `app`) and the moon phase; Do Not Disturb while it shows (`app`) |
| `day` | Day mode: the desk calendar page and sticky note with its to-do list, the monthly scenes with their weather and holidays, the focus timer, the world clock with its city list, the weather over the sky with its glance line (rain, sun, UV, air), the countdowns and the reminders (`shared`); the background photo and today's events from the calendar provider (`app`); the schedule |
| `dream` | `NocturnaDream`, the screen saver |
| `alarm` | The wake-up screen, the rules for skipping days off, the bedtime reminder's timing and the countdown to the next alarm, when a dated one-time alarm rings, the dismiss challenges with their sums and screens, and wave to snooze (`shared`); scheduling (`AlarmManager.setAlarmClock`), the ringing service and its activity, boot receiver, days off read from the calendar and the bedtime reminder (`app`) |
| `radio` | Media3 `RadioService` (streams, sleep sounds and music on the phone) and the `Radio` controller with the sleep timer |
| `sound` | The sleep sounds: seeded noise, rain and surf generators that loop seamlessly, the ambient music synthesizer (`AmbientMusic`), the WAV encoder, and which files a folder of the user's music plays and in what order (`PhoneMusic`) (`shared`); their cached WAV files and the picked music's files (`PhoneMusicFiles`) (`app`, in `radio`) |
| `weather` | Open-Meteo current conditions, daily and hourly forecast, air quality and city search; the phone's location (`app`) |
| `notify` | Notification listener that mirrors notification icons |
| `charging` | Foreground service that opens the clock when the charger is connected |
| `widget` | Quick Settings tiles, the home-screen widget and its one-tap sleep radio (`app`); the widget's next-alarm text and clock size (`shared`) |
| `settings` | The Settings hub and its pages: clock face, info line, brightness, day mode, weather, starting, alarms, stations, About and backup (`shared`), which reach the phone through `SettingsPlatform`; `SettingsActivity`, its Material You theme and `AndroidSettingsPlatform` (`app`) |
| `data` | Settings and alarms and their stores, kept as JSON in device-protected SharedPreferences on Android; the backup file format (`shared`) |
| `text`, `ui` | Dates and names in the phone's language, the words that plain functions put together (`Strings`), the theme and the icons (`shared`); the screens' own text is in `shared/src/commonMain/composeResources`, the notifications', tiles' and widget's in `app/src/main/res` |

## License

MIT, see [LICENSE](LICENSE). Third-party components and the weather data's attribution are in [NOTICE](NOTICE);
what changed is in [CHANGELOG.md](CHANGELOG.md).
