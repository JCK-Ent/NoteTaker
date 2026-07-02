# NoteTaker

Android note-taking app with voice-to-text, audio recording, recording transcription, and share to SMS/email.

## Features

| Feature | Status |
|---|---|
| Text notes — type, edit, save | Shipped |
| Voice-to-text (continuous, stops on demand) | Shipped |
| Audio recording (mic → M4A file) | Shipped |
| Attach recording to a note | Shipped |
| Transcribe recording → note text (offline via Vosk) | Shipped |
| Transcription — cloud providers (Whisper, AssemblyAI, Deepgram) | Shipped |
| Low-confidence word highlighting in transcription | Shipped |
| Share note via SMS / email | Shipped |
| Dark / light / system theme toggle | Shipped |
| Auto-record mic during active phone calls | Shipped |
| Call recording foreground service + API 31+ TelephonyCallback | Planned — #10 |
| Note search / filter | Planned — #11 |
| Folder / tag organization | Planned — #12 |

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Architecture:** MVVM + LiveData + Navigation Component
- **Database:** Room (SQLite)
- **UI:** Material 3 (XML layouts)
- **Offline transcription:** Vosk (alphacephei/vosk-android, ~40 MB model bundled in APK)

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35

### Setup

1. Clone the repo
   ```
   git clone https://github.com/JCK-Ent/NoteTaker.git
   ```
2. Open in Android Studio (`File → Open`). Gradle will sync and download dependencies automatically.
3. The first build downloads the Vosk English model (~40 MB) into `app/src/main/assets/` — this only happens once and is bundled into the APK so users don't download anything separately.
4. Connect a device or start an emulator running Android 8.0+, then run (`Shift+F10`).

## Transcription Providers

Tap ⋮ → **Transcription Settings** to choose a provider and enter an API key.

| Provider | Speaker labels | Internet | Cost |
|---|---|---|---|
| Vosk (default) | No | No | Free |
| OpenAI Whisper | No | Yes | ~$0.006/min |
| AssemblyAI | Yes | Yes | Free 5 hr/month |
| Deepgram | Yes | Yes | Free $200 credit |

Words Vosk or Deepgram flag as low-confidence are highlighted in yellow. Tap and correct them.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice-to-text and mic recording |
| `READ_PHONE_STATE` | Detect active calls for in-call recording |
| `INTERNET` | Cloud transcription providers (optional) |
| `WRITE_EXTERNAL_STORAGE` | Save recordings on Android 9 and below |

## Call Recording Notice

Recording audio during a phone call captures **microphone input only**. Android 9+ blocks access to the call audio stream for non-system apps. When the other party is on speakerphone their voice leaks into the mic — this is the only method available without root. Always obtain consent from all parties; laws vary by jurisdiction.

## Project Structure

```
app/src/main/
├── java/com/jckent/notetaker/
│   ├── data/               # Room entity, DAO, Database
│   ├── ui/
│   │   ├── notes/          # Note list (RecyclerView + ViewModel + Adapter)
│   │   ├── editor/         # Note editor, VoiceInputHelper, NoteSharer,
│   │   │                   # TranscriptionProvider/Manager + providers
│   │   └── recording/      # Audio recorder, CallRecordingManager
│   ├── MainActivity.kt     # Nav host, theme toggle, transcription settings
│   └── ThemePreference.kt
└── res/
    ├── layout/             # XML layouts
    ├── navigation/         # Nav graph
    ├── menu/
    └── values/             # Strings, themes, colors
```

## License

MIT
