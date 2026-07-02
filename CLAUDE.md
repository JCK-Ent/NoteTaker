# NoteTaker — Project Brief for Claude

## What this is
Android note-taking app (Kotlin, min SDK 26). Built by Jonathan (JCK-Ent on GitHub).

## Tech stack
- Kotlin + XML layouts (no Compose in use yet — Compose deps are present but not wired up)
- Room database (Note entity, NoteDao, NoteDatabase)
- Navigation Component with BottomNavigationView (Notes tab, Record tab)
- MVVM: ViewModel + LiveData
- Material 3 theme
- Vosk offline speech recognition (alphacephei/vosk-android 0.3.47) — model bundled in APK via Gradle download task
- Multi-provider transcription: Vosk (default), OpenAI Whisper, AssemblyAI, Deepgram

## Package: `com.jckent.notetaker`

## Key files
- `app/src/main/java/com/jckent/notetaker/data/` — Room entity, DAO, database
- `app/src/main/java/com/jckent/notetaker/ui/notes/` — Note list fragment + ViewModel + Adapter
- `app/src/main/java/com/jckent/notetaker/ui/editor/` — Note editor, VoiceInputHelper (continuous STT), NoteSharer, TranscriptionProvider interface + VoskProvider / WhisperProvider / AssemblyAiProvider / DeepgramProvider, TranscriptionManager
- `app/src/main/java/com/jckent/notetaker/ui/recording/` — Recording fragment, AudioRecorder, CallRecordingManager
- `app/src/main/res/navigation/nav_graph.xml` — Navigation graph
- `gradle/libs.versions.toml` — Version catalog (Gradle 9.4.1, AGP 8.7.3, Kotlin 2.0.21)
- `app/build.gradle.kts` — downloadVoskModel task runs at build time, bundles model ZIP into assets

## Shipped features (all merged to main)
- Text notes: create, edit, save (Room)
- Voice-to-text: continuous SpeechRecognizer loop, stops on user tap
- Audio recording: mic → M4A via AudioRecorder, elapsed timer
- Attach recording to a note (picker: existing note or new note)
- Transcribe recording → note text, offline via Vosk, or cloud via Whisper / AssemblyAI / Deepgram
- Low-confidence word highlighting (yellow background) for Vosk and Deepgram output
- Speaker diarization output for AssemblyAI and Deepgram
- Share note via SMS / email / other intent
- Dark / light / system theme toggle (overflow menu)
- Auto-record mic during active phone calls (CallRecordingManager + PhoneStateListener)
- Transcription Settings UI in overflow menu (provider picker + API key input)

## Open PRs (work to be done)
| PR | Branch | What's planned |
|---|---|---|
| #10 | `feature/call-recording-service` | Wrap CallRecordingManager in a foreground Service with persistent notification; migrate PhoneStateListener → TelephonyCallback for API 31+ |
| #11 | `feature/note-search` | Search bar in NoteListFragment filtering by title + content in real time |
| #12 | `feature/note-folders` | Folder/tag entity, many-to-many with Note, folder picker in editor, folder filter in list |

## Call recording limitation
Android 9+ blocks `CAPTURE_AUDIO_OUTPUT` for non-system apps. Only mic (AudioSource.MIC) is available. Recording both sides only works if the other party is on speakerphone. Always prompt user for consent before enabling call recording.

## Transcription notes
- Vosk small English model (~40 MB) is downloaded at `./gradlew build` time into `app/src/main/assets/` — users install it in the APK, no post-install download
- First `Transcribe Recording` tap extracts the model ZIP from assets to `filesDir/vosk_model/` (~one-time, a few seconds)
- `acceptWaveForm()` returns true at utterance boundaries — must call `rec.result` at each boundary to avoid losing text before a pause
- AssemblyAI and Deepgram support `speaker_labels`/`diarize` for Speaker A / Speaker B output

## Workflow
- One feature per branch, merged to main via PR
- GitHub: https://github.com/JCK-Ent/NoteTaker
- Run `gh pr list` to see open PRs
