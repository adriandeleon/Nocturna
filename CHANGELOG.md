# Changelog

All notable changes to Nocturna are documented in this file.

The format is based on [Keep a Changelog 1.1](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each entry opens with a bold
one-line summary written for users.

## [Unreleased]

### Added

- **A big bedside clock for the nightstand:** digital, in six font styles, 12 or 24 hours, with or without
  seconds, or analog, in a colour you pick.
- **An info line under the clock** with the date, the weather (from Open-Meteo, no account needed), the battery
  and charging state, the next alarm and the radio station playing.
- **A short forecast next to the weather:** today's high and low during the day, and from 18:00 tomorrow's
  weather, high and low, for a look before bed. Switch it off under *Show on the clock › Forecast*.
- **Notification icons under the clock,** once you give Nocturna notification access.
- **Brightness that follows the room,** between the darkest and brightest levels you set, and a switch to turn
  dimming off.
- **Night mode:** when the room goes dark the clock turns dim red; without a light sensor it follows the time
  (22:00–07:00).
- **Burn-in protection:** the clock drifts slowly around the screen.
- **Day mode, a cheerful desk set for while you work:** a desk clock, a spiral-bound calendar page for the
  month in its own colour, with dots on the days that have events, and a sticky note that says hello and lists
  the rest of today's events from the calendars on your phone, with the one in progress marked *Now*. Behind
  them is a sunny scene that changes every month (blossoms in April, sunflowers in August, snow in December),
  or a photo of your own. Turn it on from the clock's controls or in Settings; today's events need calendar
  access, and without it you get the page alone. Day mode never turns night red. Because it stays up all day,
  burn-in protection moves it around more, swaps the clock and the page every hour in landscape, and keeps
  the background slowly moving.
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
- **Wake up to the radio:** each alarm can play one of your stations instead of the alarm tone. If the
  station doesn't play within 15 seconds, or stops while ringing, the alarm tone takes over. The wake-up
  screen and the alarm notification name the station only while it plays, and *Keep listening* on the
  wake-up screen leaves the station playing after you dismiss the alarm.
- **Skip the next alarm:** a repeating alarm can skip its next ring, for a day off, without being switched
  off and back on. *Undo skip* brings it back, and changing the alarm's time or days drops the skip.
- **Starts on its own while charging,** as the Android screen saver or with *Open when charging*, which opens
  the clock when you plug in a locked phone.
- **An About screen in Settings** with the version, links to the source code, bug reports and the licence, and
  credits for the open-source libraries and the weather data.
