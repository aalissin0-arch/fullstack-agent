# Fullstack Agent (Android)

An Android (Kotlin + Jetpack Compose + Material 3) rewrite of `fullstack-agent`, bringing the full agent stack—guided setup wizard, persistent memory vault, voice bus, living face visualizers, spatial hands board, seam wiring inspector, and mechanic troubleshooter—into a native mobile and tablet experience.

## Features

- **Guided Setup & Adoption Wizard**: Plain-English step-by-step configuration for home folder adoption, stack piece selection (`ai-memory-vault`, `backtalk`, `ai-visualizer`, `barehands`), Three Doors identity presets, vault registration, mic/voice/permission modes, and face selection.
- **Stage & Launchpad**: Switch across `Full Stack`, `Talk`, `Hands`, and `Chat` modes with live `.voice_state` and `.voice_waveform` bus synchronization, Android `TextToSpeech` spoken greetings/replies, Push-to-Talk (`RECORD_AUDIO` with runtime permission handling) or Hands-Free mode, and four custom Canvas visualizers (`Board`, `Radial`, `Rain`, `Neural`).
- **Barehands Spatial Board**: Draggable glass cards with interactive `present` actions from chat or the vault.
- **Memory Vault & `CLAUDE.md` Editor**: Create, search, edit, and delete persistent Markdown notes alongside the home `CLAUDE.md` identity and mechanic directive.
- **Wiring & Seam Inspector**: Live `.voice_state` / `.voice_waveform` telemetry, runtime switches (`mic_mode`, `permission_mode`, `voice_engine`), generated JSON previews (`backtalk.json`, `ai-visualizer.json`, `barehands.json`), and seam break/repair testing.
- **Mechanic & Updater**: Non-destructive stack update runner (`update.sh` workflow), automated health diagnostics, launcher overview, and searchable troubleshooting guides.
