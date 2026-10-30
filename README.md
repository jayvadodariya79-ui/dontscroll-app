# SwipeStop

Android app that counts the Reels and Shorts you swipe on **Instagram, YouTube, Snapchat and Facebook**, shows the time you spend there, and stops you at your limit. Free, no login, no internet permission; everything is stored on the phone.

## Build

**Android Studio:** File → Open → this folder → wait for Gradle sync → Run ▶ on your phone (USB debugging on), or Build → Build APK(s).

**Command line:** `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

**GitHub:** push to `main` and the workflow in `.github/workflows/build.yml` builds the APK and attaches it to a release called `latest`.

Toolchain: AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Gradle 8.11.1, JDK 17+, minSdk 26, targetSdk 35.
Debug and release builds are signed with the committed **test** key (`app/dontscroll-test.keystore`), so a new build installs over the old one without losing data. Make a private upload key before publishing on Play.

## Features

| Area | What it does |
| --- | --- |
| Tracking | Counts each reel/short swiped; time in the player; time in each app; sessions |
| Apps | Instagram Reels, YouTube Shorts, Snapchat Spotlight (beta), Facebook Reels (beta) |
| You tab | Brain mascot + health %, today's reels and time vs limits, per-app list, quick switches |
| Battle tab | You vs the Scroll Monster each day, streaks, levels/XP, daily challenges |
| Progress tab | Reels per hour chart, Total Reels + Time spent, Top apps, 7-day chart |
| Blocking (free) | Limit screen at daily reel/time limit, block an app's Reels completely, schedules (bedtime, focus), snoozes |
| Mindful | Live counter bubble, pause-before-reels screen, 80% warning |
| Other | Nightly summary notification, home-screen widget, CSV export, reset, Detection check screen |

## Code map

| Path | Purpose |
| --- | --- |
| `data/Apps.kt` | The four apps and the **view IDs** used to recognise their players (update here when an app changes) |
| `service/ReelService.kt` | Accessibility service: events, 1 s tick, counting, limits, overlays |
| `service/ReelCounter.kt` | Counting rules (entry, swipes with/without position, new item, re-entry grace) |
| `service/Detector.kt` | Finds the player by view ID; Facebook by layout (action rail) |
| `service/Overlays.kt` | Live counter, warning banner, pause screen, limit screen |
| `data/Repo.kt`, `data/Db.kt` | SQLite storage (reels, usage per day/app, day budgets/challenges) |
| `data/Prefs.kt` | Settings, schedules, snoozes |
| `data/Battle.kt` | Battle results, streaks, XP, challenges |
| `ui/screens/*` | Compose screens |

## If counting breaks after an Instagram/YouTube update

Open **Settings → Detection check**, open Reels/Shorts and swipe, then **Copy log**. The `Last scroll view` and `Found by` lines show the IDs the app uses now; add them to `data/Apps.kt`.
