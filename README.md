# NoteTaker

Android note-taking app with voice-to-text, audio recording, folder organization, and share to SMS/email.

## Features

| Feature | Status | Branch |
|---|---|---|
| Text notes (type / paste) | Skeleton | `feature/note-editor` |
| Voice-to-text | Planned | `feature/voice-to-text` |
| Audio recording (mic) | Planned | `feature/audio-recording` |
| Folder organization | Skeleton | `feature/note-editor` |
| Share via SMS / email | Planned | `feature/sharing` |
| Record during calls | Planned | `feature/call-recording` |

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0) — covers ~95% of active devices
- **Architecture:** MVVM + LiveData + Navigation Component
- **Database:** Room (SQLite)
- **UI:** Material 3

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35

### Setup

1. Clone the repo
   ```
   git clone https://github.com/JCK-Ent/NoteTaker.git
   ```
2. Open the project in **Android Studio** (`File → Open`). Android Studio will download Gradle and sync dependencies automatically.
3. Connect a device or start an emulator running Android 8.0+.
4. Run the app (`Shift+F10`).

> **Note on Gradle wrapper:** The first sync will download Gradle 8.9 (~100 MB). This only happens once.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice-to-text and mic recording |
| `READ_PHONE_STATE` | Detect active calls for in-call recording |
| `WRITE_EXTERNAL_STORAGE` | Save recordings on Android 9 and below |

## Call Recording Notice

Recording audio **during a phone call** captures microphone input only. Recording the other party's audio is blocked by Android 9+ system policy. When the other party is on speaker, their voice leaks into the mic — this is the only cross-device method available without root. Always obtain consent from all parties before recording a call; laws vary by jurisdiction.

## Project Structure

```
app/src/main/
├── java/com/jckent/notetaker/
│   ├── data/               # Room entities, DAO, Database
│   ├── ui/
│   │   ├── notes/          # Note list fragment + ViewModel
│   │   ├── editor/         # Note editor fragment
│   │   └── recording/      # Audio recording fragment
│   └── MainActivity.kt
└── res/
    ├── layout/             # XML layouts
    ├── navigation/         # Nav graph
    ├── menu/               # Bottom nav menu
    └── values/             # Strings, themes, colors
```

## License

MIT
