# Changelog

All notable changes to Nocturna are documented in this file.

The format is based on [Keep a Changelog 1.1](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each entry opens with a bold
one-line summary written for users.

## [Unreleased]

### Added

- **A big bedside clock for the nightstand:** digital, in six font styles, 12 or 24 hours, with or without
  seconds, or analog, in a colour you pick.
- **A clock in your wallpaper's colours (Material You):** on Android 12 and later, pick *From wallpaper* in
  Settings › Clock face › *Colour* and the clock and its info line take a light accent from your wallpaper, and
  change with it. Night mode still turns the clock red, and Day mode keeps its own look. The settings screens
  follow your wallpaper's colours too.
- **An info line under the clock** with the date, the weather (from Open-Meteo, no account needed), the battery
  and charging state, the next alarm and the radio station playing.
- **A short forecast next to the weather:** today's high and low during the day, and from 18:00 tomorrow's
  weather, high and low, for a look before bed. Switch it off under *Show on the clock › Forecast*.
- **Notification icons under the clock,** once you give Nocturna notification access.
- **Brightness that follows the room,** between the darkest and brightest levels you set, and a switch to turn
  dimming off.
- **Night mode:** when the room goes dark the clock turns dim red; without a light sensor it follows the time
  (22:00–07:00).
- **The moon's phase on the night clock:** Settings › Show on the clock › *Moon phase* (off by default) adds
  the moon's phase to the info line, a small drawing of the lit part with its name (*Waxing gibbous*), worked
  out on the phone to within about a day, no network needed. It follows night mode's red, and Day mode keeps
  its own moon.
- **Tap to peek at the dim clock:** in the dark, a tap on the dim red clock brightens it for about six seconds,
  to a comfortable 40% of your brightest level and a soft warm colour, then it fades back; tap again during the
  peek for the controls. The screen saver peeks too, and a second tap closes it. Before an alarm, whichever is
  brighter of the peek and the sunrise wins, and Day mode isn't affected. Turn it off under Settings ›
  Brightness and night mode › *Tap to peek*.
- **Do Not Disturb at night:** Settings › Starting the clock › *Do Not Disturb at night* (off by default)
  turns on priority-only Do Not Disturb while the clock or its screen saver is on screen from 22:00 to 07:00,
  or from when night mode starts in the evening, and turns it off again when the clock leaves the screen or
  at 07:00. Alarms still ring. It only undoes what it changed: Do Not Disturb that was already on, or that you
  change yourself, is left alone, and Day mode never touches it. It needs Do Not Disturb access, which the
  switch asks for; without it nothing changes. If your Do Not Disturb settings would silence alarms, Nocturna
  doesn't turn it on.
- **Burn-in protection:** the clock drifts slowly around the screen.
- **Day mode, a cheerful desk set for while you work:** a desk clock, a spiral-bound calendar page for the
  month in its own colour, with dots on the days that have events, and a sticky note that says hello and lists
  the rest of today's events from the calendars on your phone, with the one in progress marked *Now*. Tap a
  day to open it in your calendar app, or an event to open the event. Swipe the page to switch between the
  month, this week and today (or tap *Month · Week · Day* under it), and pick the view it starts with in
  Settings. Behind them is a sunny scene that changes every month (blossoms in April, sunflowers in August,
  snow in December) and with the time of day, from a peachy dawn to golden afternoons and a starry night, or
  a photo of your own. Turn it on from the clock's controls or in Settings; today's events need calendar
  access, and without it you get the page alone. Day mode never turns night red.
- **Nothing in Day mode stays put, to spare OLED screens:** with burn-in protection on, the clock, the
  calendar page and the sticky note each drift on their own, as the night clock does, while the whole layout
  drifts too, and every other hour the clock and the page trade places: sides in landscape, top and bottom in
  portrait. The page's bright paper dims so slowly you won't see it happen, by about a quarter over its first
  four hours on screen. When the brightness follows the room, Day mode stops at 50% instead of the clock's
  80%, since a dimmer screen wears much more slowly; change it under Settings › Day mode › *Screen care*. The
  scene behind them never stops moving: the sun crosses the sky from dawn to dusk and the moon from dusk to
  dawn, slowly turning, the stars wheel around the sky, the clouds float up and down as they pass, the hills
  scroll, swell and change shape, and the flowers (or December's trees) sway in the breeze, wander about,
  grow and shrink a little, and are replanted somewhere new every hour. Your own photo pans gently.
- **Day mode on a schedule:** set Day mode to *On a schedule* with the hours and days you work, for example
  weekdays 09:00–18:00, and the clock switches to the calendar and back on its own. Hours can run past
  midnight. The *Day mode* / *Clock only* button on the clock overrides the schedule until its next start or
  end, and Settings shows what the schedule is doing and until when.
- **Alarms** with repeat days, labels, snooze, a gentle volume ramp, vibration and a full-screen wake-up screen
  over the lock screen. A deleted alarm can be brought back with *Undo*.
- **Alarms you can count on:** they still ring after the phone restarts overnight, even before you unlock it; if
  your alarm sound can't play, a soft built-in chime rings instead; and if notifications are off, Nocturna warns
  you, because a locked phone then can't show the screen to stop an alarm. Opening Nocturna always brings that
  screen back while an alarm rings.
- **Web radio:** play any MP3, AAC or HLS stream, with a sleep timer that fades out after 15, 30, 60 or 90
  minutes. Monterrey stations come ready to pick.
- **Sleep sounds that play offline:** White noise, Pink noise, Brown noise, Rain and Surf (slow waves rolling
  in) sit under their own *Sleep sounds* heading in Settings › Radio stations. Nocturna makes them on your
  phone the first time you play one, so they need no internet and add almost nothing to the app's size. They
  loop without a break or a click and play like a station: from the clock's Radio button, with the sleep timer
  fading them out, in the info line and in the media notification. Alarms still wake you with a web station
  or the alarm tone.
- **Wake up to the radio:** each alarm can play one of your stations instead of the alarm tone. If the
  station doesn't play within 15 seconds, or stops while ringing, the alarm tone takes over. The wake-up
  screen and the alarm notification name the station only while it plays, and *Keep listening* on the
  wake-up screen leaves the station playing after you dismiss the alarm.
- **Skip the next alarm:** a repeating alarm can skip its next ring, for a day off, without being switched
  off and back on. *Undo skip* brings it back, and changing the alarm's time or days drops the skip.
- **A sunrise before your alarms:** in the 20 minutes before an alarm the clock slowly brightens, up to the
  brightest level you allow, and warms from night red through amber to a soft white, so the room is light
  when the alarm rings. It starts gently and speeds up, like a real dawn, and ends when the alarm rings. It
  doesn't run before a snooze, before a skipped ring or in Day mode. Pick 10, 20 or 30 minutes, or turn it
  off, under Settings › Alarms › *Sunrise before alarms*.
- **Alarms that sleep in on holidays:** turn on *Skip on days off* for a repeating alarm and it stays quiet on
  days with an all-day event in your calendars whose title says it's a day off, such as *Holiday*,
  *Vacation*, *PTO*, *OOO* or *Día libre*. The alarm list says which ring it skips and why (*Skips Mon 16 Nov
  · Revolution Day holiday*), and the next alarm everywhere, including the clock and Android's own, is the
  first day that isn't off. Change the words under Settings › Alarms › *Days off*; case and accents don't
  matter. It needs calendar access and asks for it; without it, or if the calendar can't be read, alarms ring
  as usual.
- **How long until your alarm, on the clock:** when the next alarm is within 12 hours, the info line says
  *⏰ 06:30 · in 7 h 20 m*, or *in 45 m* in the last hour, stepping every 5 minutes so it doesn't flicker.
  Further off it shows the day and time as before. Switch it off under *Show on the clock › Time until the
  alarm*.
- **A bedtime reminder:** pick how much sleep you want (6 to 9 hours, in half hours) under Settings › Alarms
  › *Bedtime reminder*, and the evening before a morning alarm a quiet notification says *Bedtime: 8 h before
  your 06:30 alarm*, a quarter of an hour before you need to be asleep. It stays quiet if the clock is already
  showing, isn't sent late, and follows your alarms as they change, skipped days off included. Off by
  default; it needs notifications and does nothing without them.
- **Starts on its own while charging,** as the Android screen saver or with *Open when charging*, which opens
  the clock when you plug in a locked phone.
- **An About screen in Settings** with the version, links to the source code, bug reports, the licence and
  the author's homepage, and credits for the open-source libraries and the weather data.
- **Settings in tidy pages:** the first Settings page is now a short list (Clock face, Show on the clock,
  Brightness and night mode, Day mode, Alarms, Radio stations, Weather, Starting the clock, About), each with a
  line saying how it is set, and each opening its own page. *Clock face* shows a live preview of the clock as you
  change it, and anything Android hasn't allowed yet, such as notification or calendar access, is gathered in a
  *Needs your attention* card at the top with the button that fixes it.
- **Back up your settings, radio stations and alarms to a file,** and bring them back on this phone or a new
  one: *Settings › About › Backup › Export…* saves a file such as `nocturna-backup-2026-11-15.json` wherever you
  pick, and *Import…* shows what a backup holds before it replaces your settings, stations and alarms. The Day
  mode photo isn't included, and a file that isn't a Nocturna backup changes nothing.
- **A countdown to your next meeting in Day mode:** in the hour before a timed event, the sticky note puts it
  at the top, *In 12 min · 10:00*, counting down to *Now · until …* once it starts. Tap it to open the event.
  When the note isn't showing (the day view, or no room under the clock), *Standup in 12 min* hangs in a small
  tag under the desk clock instead. For a soft chime 1, 2 or 5 minutes before, set *Chime before meetings* in
  Settings › Day mode: one quiet tone at the notification volume, once per meeting, only while Day mode is
  showing and never on silent or Do Not Disturb.
- **A focus timer (Pomodoro) in Day mode:** tap the clock and pick *Focus* for 25 minutes of work, then a
  5-minute break, with a 15-minute break after every fourth block. The desk clock shows *Focus · 18:42 left* or
  *Break · 3:10 left* beside a thin ring of the time gone; tap it to pause, skip or stop. A soft chime (the
  meeting chime's tone, never the alarm's) marks each switch. Set the lengths in Settings › Day mode › *Focus
  timer*. Turning the phone keeps it going; leaving Day mode or the app stops it.
- **Quick Settings tiles and a home-screen widget:** the *Nocturna* tile opens the clock, and the *Sleep
  radio* tile shows your station, lights up while it plays, and with one tap plays it with a 30-minute sleep
  timer that fades out; tap again to stop. The dark *Nocturna clock* widget shows the time and your next
  alarm, with *Clock* and *Sleep radio* buttons, and resizes to fit.
- **Today's weather on the wake-up screen:** when an alarm rings, the current conditions and today's high and
  low show under the time, in the clock's units. They show at once from what the clock already has, and are
  refreshed in the background if that is old and the phone is online; the alarm never waits for them. Switch
  it off under *Alarms › Show weather*.

### Changed

- **Groundwork for an iOS version:** the code that doesn't depend on Android now lives in a shared Kotlin
  Multiplatform module that can also build for iOS: the settings and alarms and how they are stored, when
  alarms ring, the Day mode schedule and calendar, the sky, the weather and its forecast, the Settings
  summaries, the burn-in timings and the icons, and now every screen: the clock with Day mode's calendar
  page, sticky note and scenes, all of Settings, and the alarm's wake-up screen. The Android app works
  exactly as before, and your settings and alarms carry over as they are. The About screen now credits Compose Multiplatform (with its Material 3),
  kotlinx-datetime, kotlinx.serialization, kotlinx.coroutines and atomicfu.

### Fixed

- **Day mode's sticky note no longer shrinks to a thin yellow strip** on a phone held sideways. When there
  isn't room under the clock for its hello and at least one event, the note is left out; your events still
  show as dots on the month page and in the week and day views.
