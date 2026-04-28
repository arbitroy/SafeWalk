# SafeWalk

A personal safety Android app that lets you walk alone with confidence. Start a timed check-in session before you head out — if you don't check in on time, your emergency contacts get an automatic SMS alert with your location. A companion Wear OS app keeps everything on your wrist.

---

## What It Does

**Check-in Sessions** — Start a timed session (default 30 min) before a solo walk. Check in periodically to confirm you're safe. Miss a check-in and the app escalates automatically.

**SOS Alerts** — One tap sends an emergency SMS to your contacts with your current address and a Google Maps link. Medical info you've stored is included in the message.

**Wear OS Companion** — Pair your smartwatch via a 6-digit session code. The watch mirrors the session timer, lets you check in from your wrist, and receives real-time sync from the phone over Firebase.

**Emergency Contacts** — Add contacts with names, phone numbers, and relationships. Mark primary contacts to have them alerted first.

**Session History** — Every session is logged with its outcome: completed, missed check-in, or SOS triggered.

**Guest Mode** — Try the app without creating an account.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt 2.59.2 |
| Local DB | Room 2.8.4 |
| Preferences | DataStore 1.2.1 |
| Background work | WorkManager 2.11.2 |
| Phone ↔ Watch sync | Firebase Realtime Database 34.11.0 |
| Location | Google Play Services Location (fused provider) |
| SMS alerts | Android SmsManager |
| Bluetooth | Android RFCOMM sockets |
| Build | AGP 9.1.0, Gradle Kotlin DSL, KSP 2.3.6 |
| Min SDK | 26 (phone) / 24 (Wear OS) |

---

## Architecture

The project is a multi-module Gradle build:

```
SafeWalk/
├── app/          # Phone app
└── wear/         # Wear OS companion app
```

### Phone App

```
com.example.safewalk/
├── ui/
│   ├── screens/      # 9 Compose screens (Dashboard, Alert, Contacts, History…)
│   ├── viewmodel/    # 7 ViewModels (Auth, Dashboard, Alert, Contacts…)
│   └── components/   # Reusable Compose components
├── data/
│   ├── local/        # Room database — CheckIn & EmergencyContact entities
│   ├── preferences/  # DataStore — user accounts, session state, settings
│   └── firebase/     # PhoneFirebaseSyncManager — real-time watch ↔ phone sync
├── communication/    # BluetoothCommunicationManager (RFCOMM + JSON protocol)
├── location/         # LocationService — fused GPS + reverse geocoding
├── sms/              # SmsAlertSender — multi-part SMS with location payload
└── di/               # Hilt modules (Database, Communication, Repository)
```

### Wear OS App

```
com.wear/
├── ui/screen/        # WearPairingScreen — pairing + session timer display
├── viewmodel/        # WearPairingViewModel
└── data/             # WearFirebaseSyncManager — listens for timer & contacts
```

### Data Flow

```
Phone App  ──Firebase RTDB──▶  Wear OS App
    │                               │
    └─ session code ────────────────┘
         (6-digit pairing)
```

State is managed with `StateFlow` / `Flow` throughout. `SafeWalkSession` is a sealed class (`Idle | Active`) so every screen always knows exactly what state the app is in.

---

## How to Run

### Prerequisites

- Android Studio (latest stable)
- JDK 21
- Android SDK API 26+
- A Firebase project with Realtime Database enabled
- `google-services.json` placed in `app/` and `wear/`

### Firebase Setup

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add two Android apps — one for `com.example.safewalk`, one for `com.wear`.
3. Download each `google-services.json` and place them in `app/` and `wear/` respectively.
4. Enable **Realtime Database** in test mode (or add security rules for production).

### Build & Install

```bash
# Clone
git clone https://github.com/arbitroy/SafeWalk.git
cd SafeWalk

# Install phone app on a connected device or emulator
./gradlew app:installDebug

# Install Wear OS app on a paired watch
./gradlew wear:installDebug

# Run unit tests
./gradlew test
```

### Required Permissions

The app will request these at runtime:

- **Location** — for GPS coordinates in SOS alerts
- **Send SMS** — for emergency contact notifications
- **Bluetooth** — for watch pairing
- **Notifications** — for session timer and check-in reminders
- **Contacts** — for importing contacts from the phone's address book

### Pairing the Watch

1. Open SafeWalk on the phone and go to **Pairing**.
2. A 6-digit session code is displayed.
3. Enter that code on the Wear OS app to link the two devices.
