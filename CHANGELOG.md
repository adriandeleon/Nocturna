# Changelog

All notable changes to Nocturna are documented in this file.

The format is based on [Keep a Changelog 1.1](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each entry opens with a bold
one-line summary written for users.

## [Unreleased]

### Added

- **See how much battery the night clock uses:** Settings › Brightness and night mode › *Battery at night*
  shows the last night's use (*Sun 15 Nov: 12% in 7 h 40 m*) and what 8 hours take on average over the last
  seven nights (*About 13% for 8 hours*). It counts only while the night clock shows and the phone isn't
  charging, so a phone left on its charger all night adds nothing. It stays on the phone and isn't backed up.
- **Ambient music and your own music for falling asleep, all without the internet:** Settings › Radio
  stations has three pieces of *Ambient music*: *Slow chords* (warm chords drifting from one to the next),
  *Night chimes* (bells here and there over a low hum) and *Deep drone* (a low hum that swells and fades).
  Like the sleep sounds, Nocturna makes them on your phone, so they add nothing to the app's size, and they loop
  without a break. Under *Music on your phone*, *Add files* or *Add a folder* to play your own audio: a file
  repeats, a folder plays its tracks (subfolders too) in name order and starts again. No storage permission is
  needed: you pick what Nocturna may play. Both play from the Radio button with the sleep timer fading them
  out, and an alarm can wake you with your own music. A backup keeps your music stations, but on another phone
  you need to add the files again.
- **Choose how long the radio and sleep sounds play:** Settings › Radio stations › *Sleep timer* sets how long
  the clock's Radio button, the *Sleep radio* tile and the widget play before fading out: off, or 15 minutes
  to 2 hours. It starts at 30 minutes, so the Radio button now stops on its own too; pick *Off* to have it play
  until you stop it. The clock's *Sleep* button still changes the timer for what's playing.
- **Nocturna habla español:** with your phone in Spanish, the clock, Day mode, Settings, the wake-up screen,
  the notifications, the tiles and the widget are all in Mexican Spanish (*alarma*, *posponer*, *modo noche*,
  *No molestar*…). Dates and times follow your phone's region as before, and any other language stays in
  English.
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
- **One-time alarms on a date:** an alarm without repeat days can be set for a day of your choice, such as a
  flight on Friday, with *Date* on its card; the list shows the day (*Fri 21 Nov*). It rings once, at its time
  on that day, and then switches itself off. Without a date a one-time alarm rings the next time its time comes,
  as before. A date and repeat days don't go together: picking days takes the date off. Past dates can't be
  picked, days off don't silence it, and switching it back on after its day takes the date off.
- **Alarms that make sure you're awake:** each alarm has *To stop it*: *Just tap* (as before), *Solve a sum*
  or *Hold the button 3 s*. *Solve a sum* asks for one to three additions such as 37 + 48, typed on a numeric
  keypad (*Sums to stop an alarm* under Settings › Alarms); a wrong answer just brings a new sum, never a wait,
  and TalkBack reads each sum aloud. Snooze is always one tap, and with a challenge the notification's button
  opens the wake-up screen instead of dismissing. If the challenge can't be shown, the alarm falls back to a
  tap, so it can always be stopped.
- **Wave to snooze, and wave to peek:** with Settings › Alarms › *Wave to snooze* on, a wave of the hand over
  the top of the phone snoozes a ringing alarm, just like the Snooze button; it never stops the alarm, so a
  dismiss challenge still stands. With Settings › Brightness and night mode › *Wave to peek* on (next to *Tap to
  peek*), a wave peeks at the dim night clock, in the app and the screen saver, the same as a tap; Day mode
  isn't affected. A wave is the hand passing over and away within about a second and a half, so a phone lying
  face down, in a pocket or under a pillow doesn't count, and one wave is one snooze or peek. Both are off by
  default and only show on phones with a proximity sensor, which is on only while the wake-up screen or the
  night clock is showing.
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
- **A world clock on Day mode's desk clock:** add up to two other time zones in Settings › Day mode › *Other
  time zones*, picked from a searchable list of about 160 cities, or by time zone name. The desk clock shows
  them under its time, e.g. *London 19:42 · Tokyo 04:42 +1*, in your 12 or 24-hour format, with +1 or −1 when
  it's already another day there. They drift with the clock and step aside when the screen has no room, so
  the sticky note keeps its place. None by default.
- **Quick Settings tiles and a home-screen widget:** the *Nocturna* tile opens the clock, and the *Sleep
  radio* tile shows your station, lights up while it plays, and with one tap plays it with a 30-minute sleep
  timer that fades out; tap again to stop. The dark *Nocturna clock* widget shows the time and your next
  alarm, with *Clock* and *Sleep radio* buttons, and resizes to fit.
- **Today's weather on the wake-up screen:** when an alarm rings, the current conditions and today's high and
  low show under the time, in the clock's units. They show at once from what the clock already has, and are
  refreshed in the background if that is old and the phone is online; the alarm never waits for them. Switch
  it off under *Alarms › Show weather*.
- **The next few hours' weather in Day mode:** under the desk clock's weather line, a strip of the next six
  hours with each hour's weather and temperature (*14h ☀ 22° · 15h ⛅ 23° …*), and the chance of rain when
  it's 30% or more. It uses 12-hour labels on a 12-hour clock, shows fewer hours where the screen is narrow
  and steps aside rather than crowd the clock. It comes in the same single weather request as before. Switch
  it off under Settings › Day mode › *Weather › Next hours*, which shows once the weather is on and has a
  place.
- **Reminders to stretch and drink water in Day mode:** every 30 minutes to 2 hours, a soft chime and a yellow
  note in the desk clock: *Time to stand up and stretch*, *Time for a glass of water*. Tap it away, or it
  goes after five minutes. They wait during a focus block or a meeting and come right after. Settings › Day
  mode › *Breaks and reminders*; off by default.
- **A to-do list on the sticky note:** add a few things to do in Settings › Day mode › *To-do list* or with
  *+ Add a to-do* on the note itself, and tap one to tick it off. Ticked items stay crossed off for the rest of
  the day and are gone the next. The note shows it under today's events, and stays in the calendar's day view
  just for the list; where only some items fit, it says how many more there are. Off by default; the list goes in your backups.
- **Battery care:** with the phone charging on the stand, Day mode says *Charged to 85% · you can unplug* once
  it reaches 80, 85 or 90%, whichever you pick under *Breaks and reminders › Battery care*. Off by default.
- **A glance line under Day mode's desk clock:** each one switched on by itself in Settings › Day mode ›
  *Weather*, all off by default. *Rain alert*: *Rain from 15:00* in the next six hours, or *Rain until 17:00*
  while it rains, from the hourly forecast the clock already has. *Sunrise and sunset*: *Sunset 17:48 · 2 h
  10 m of light*, and the next sunrise after dark. *UV index*: *UV 7 · High* while the sun is up. *Air
  quality*: *Air: good (42)* on the US air quality index, from Copernicus through Open-Meteo, in one more
  request an hour, made only while it's on. In landscape, where the sticky note needs the room, the desk clock
  drops the glance line, and then the next hours, rather than the note.
- **Countdowns:** Day mode counts down to up to three days you add in Settings › Day mode › *Countdowns*
  (*Vacation in 12 days*, *Party tomorrow*), the nearest two at a time, and with *Count down to days off* to the
  next all-day calendar event titled as a day off. Each goes once its day has passed.
- **Week numbers and birthdays on the calendar page:** week numbers down the side of the month view, and a 🎂
  on birthdays (all-day events with *birthday* or *cumpleaños* in the title, like your contacts' birthdays) on
  the page and the note, instead of the dot. Settings › Day mode › *Calendar*; both off by default.
- **The weather in Day mode's scene:** the monthly pictures cloud over, turn grey, rain, snow or fill with fog
  to match the weather outside, by day and by night. On by default with the weather on; turn it off under
  Settings › Day mode › *Weather › Weather in the scene*.
- **Holiday touches in Day mode:** confetti on New Year's Eve and Day, hearts on Valentine's Day, bats and
  pumpkins on Halloween, a garland of lights on Christmas Eve and Day, and the note greets you for the day
  (*Merry Christmas!*). With the phone's region set to Mexico, papel picado for *¡Viva México!* on 15 and 16
  September and for the Day of the Dead on 1 and 2 November, with the hills full of cempasúchil. Pick your
  birthday for confetti and a *Happy birthday!* of your own. Settings › Day mode › *Background › Holidays*; on
  by default.
- **Wave to start or pause the focus timer:** on a phone with a proximity sensor, a wave over the top of the
  phone starts Day mode's focus timer, and pauses or resumes it. Settings › Day mode › *Focus timer*; off by
  default.

### Changed

- **The screen dims behind the clock's buttons:** while Settings, Alarms, Radio, Day mode or Focus and the
  other buttons show, the clock (and Day mode's photo, calendar and notes) fades to a darker shade, so the
  buttons stand out. A tap on the dimmed part hides the buttons, as before; it no longer opens a calendar event
  under them.
- **Day mode's weather moves out of the desk clock, onto the sky above it:** the conditions, temperature,
  today's high and low, the next hours and the rain, sun, UV and air quality line now sit on a pane of
  see-through navy glass over the background, just above the clock, and drift and swap sides with it. The desk
  clock keeps the time, date, battery, next alarm and countdowns. Where the sticky note needs the room, the
  weather shrinks to one line of the temperature and the day's high and low. The night clock is unchanged.
- **Clearer buttons on the clock and the wake-up screen:** each button on the clock has an icon beside its
  name and is taller, so it's easier to hit in the dark. Dimming, the radio, the sleep timer and Day mode's
  focus timer light up while they're on, instead of changing their wording, and TalkBack reads them as
  switches. In Day mode the buttons are navy, and turn warm yellow while on. Snooze and Dismiss are larger,
  with an icon above the words, and the sums keypad has rounded square keys.
- **Groundwork for an iOS version:** the code that doesn't depend on Android now lives in a shared Kotlin
  Multiplatform module that can also build for iOS: the settings and alarms and how they are stored, when
  alarms ring, the Day mode schedule and calendar, the sky, the weather and its forecast, the Settings
  summaries, the burn-in timings and the icons, and now every screen: the clock with Day mode's calendar
  page, sticky note and scenes, all of Settings, and the alarm's wake-up screen. The Android app works
  exactly as before, and your settings and alarms carry over as they are. The About screen now credits Compose Multiplatform (with its Material 3),
  kotlinx-datetime, kotlinx.serialization, kotlinx.coroutines and atomicfu.

### Fixed

- **The calendar's day view keeps up with the day:** instead of the day's first few events, even long after
  they're over, it lists the all-day events and then the last event to have finished with the next three. Before
  the first event ends it shows the next four, and late in the day the last four.
- **Day mode's sticky note no longer shrinks to a thin yellow strip** on a phone held sideways. When there
  isn't room under the clock for its hello and at least one event, the note is left out; your events still
  show as dots on the month page and in the week and day views.
