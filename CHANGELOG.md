# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-05-14

### Added
- Append-Only File (AOF) persistence with sequential command logging.
- Hybrid recovery: snapshot loads bulk state, AOF replays commands after snapshot's sequence number.
- `SnapshotPersistence` and `AppendOnlyPersistence` interfaces for pluggable persistence strategies.
- `EXPIREAT` command for absolute timestamp expiry (used internally by AOF replay).
- `StoreState` record to decouple store data from persistence concerns.
- `exportState()` and `loadState()` methods on `InMemoryStore`.
- AOF, Snapshot, and InMemoryStore test suites expanded with edge case coverage.

### Changed
- Decoupled `InMemoryStore` from `SnapshotManager` — store no longer knows about persistence.
- `BabyRedisServer` now receives persistence implementations via constructor (dependency injection).
- `SnapshotManager.save()` now takes `SnapshotData` and a sequence number.
- Renamed `snapshot` package to `persistence`.
- Snapshot files moved to `persistence/` directory.
- Snapshot temp file path derived from snapshot file location instead of hardcoded.

### Fixed
- Added `maven-surefire-plugin` 3.2.5 to fix JUnit 5 test discovery.

## [0.3.0] - 2026-05-04
- Added FLUSHDB command with support for full and pattern-based (prefix*) key deletion.
- Enhanced KEYS command to support prefix-based pattern matching (KEYS prefix*).

## [0.2.0] - 2026-05-01
- Initial public release.
- Implemented RESP-inspired wire protocol for typed client-server communication.

---
Replace `YYYY-MM-DD` with the actual release date when you publish a new version.
