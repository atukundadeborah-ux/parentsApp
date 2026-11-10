# ParentsApp

An offline Android application that helps parents/guardians, caregivers and institutional
care staff coordinate the day-to-day care of children: child profiles, activities,
reminders with alarm notifications, caregiver tasks, parenting tips, a Uganda referral
directory (Sauti 116 pinned first) and basic reports, with role-based access for four roles.

Built with Kotlin, Jetpack Compose (Material 3), Room, DataStore and Navigation Compose.

## Requirements

| Item | Requirement |
|---|---|
| IDE | Android Studio (Narwhal or later, with Android Gradle Plugin 9.1) |
| Java | JDK 17 or later (the JDK bundled with Android Studio is sufficient) |
| Android SDK | SDK Platform 36; build tools are installed by Gradle sync |
| Device | Android 7.0 (API 24) or later, physical device or emulator |

## Build and run

```bash
git clone https://github.com/atukundadeborah-ux/parentsApp.git
cd parentsApp
git checkout v1.0.0

./gradlew assembleDebug                 # Windows: gradlew.bat assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio, wait for Gradle sync, and choose **Run 'app'**.

## Tests

```bash
./gradlew testDebugUnitTest            # JVM unit tests (21)
./gradlew connectedDebugAndroidTest    # instrumented tests on a device/emulator (65)
```

| Suite | Location | IDs |
|---|---|---|
| Module unit tests | `app/src/androidTest/.../unit/` and `app/src/test/` | UT-01 – UT-42 |
| Integration tests | `app/src/androidTest/.../integration/IntegrationTest.kt` | IT-01 – IT-10 |
| Black-box UI tests | `app/src/androidTest/.../blackbox/BlackBoxTest.kt` | BB-01 – BB-07, BB-09, BB-10, BB-12 |

Each test method name starts with its ID (for example `ut27_overdueReminderIsMarkedMissed`).
BB-08 (alarm at the configured time) and BB-11 (dialler opens) are run manually on a device.
HTML reports are written to `app/build/reports/`.

## Demonstration accounts

The app seeds fictitious demo data on first launch (Settings → *Reset demo data* restores it).

| Role | Email | Password |
|---|---|---|
| Parent / Guardian | parent@example.com | Password123! |
| Caregiver | caregiver@example.com | Password123! |
| Institutional user | institution@example.com | Password123! |
| Administrator | admin@example.com | Password123! |

All data is stored only on the device. No real child data is included.
