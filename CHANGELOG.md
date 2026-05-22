# Changelog

All notable changes to TugaMedia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-17

### Added
- Initial release as part of the Tuga ecosystem v0.2.0 utility bundle for Geely Tugella head unit (Android 5.1, API 22).
- Scans connected USB OTG flash drive (`/storage/usbotg/usbotg-otg1`) for media files.
- Supported audio formats: `mp3`, `m4a`, `flac`, `ogg`, `wav`.
- Supported video formats: `mp4`, `mkv`, `avi`, `webm`, `mov`.
- Filter chips to switch between Music and Video views.
- Fallback scan path: `/sdcard/Music` when no USB drive is mounted (useful in emulator development).
- Tapping an item fires `Intent.ACTION_VIEW` so the head unit's stock player handles playback.
- Landscape-locked UI matching head unit hardware.
- Shared design tokens (colours, typography, drawables) consumed from `tuga-design` workspace library (added in Phase 1).
- Package: `com.miktuga.media` (renamed from `com.example.tugastore.media` in Phase 1).
- Signed with the shared `_signing/tuga-release.jks` keystore (v1+v2+v3) so it installs as a sibling alongside TugaStore without signature conflicts.
