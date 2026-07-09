# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added support for multiple runners in a match
- Added a new Infection gamemode, where hunters can infect runners and turn them
 into hunters
- Added a runner keepInventory option
- Added the ability to have late joiners in a match

### Changed

- Refactored join flow. Now players need to run `/manhunt join` to join a match.

### Fixed

- Tracking compass now works properly by tracking nearest runners
- Fixed stale hunter and runner uuid entries after game end
- Fixed a bug where player would respawn in the lobby after death instead of the
 current game's world spawn

## [1.2.1] - 2026-07-08

### Deprecated

- Legacy bukkit chat formatting

### Changed

- Updated in-game messages, titles, countdowns, and broadcasts to use Adventure'
s Minimessages
- `messages.yml` is changed to the new formatting style

## [1.2.0] - 2026-07-08

### Added

- Added a new way to start the game. `/manhunt mode headstart`

### Changed

- THe old start method is now identified as `dreamStart`

## [1.1.0] - 2026-07-08

### Added

- Added support for Modrinth releases. [`dadcfe2`](https://github.com/QinCai-rui/ManhuntNG/commit/dadcfe273975e3391157a41b95775dfa78dc606d)

### Changed

- Updated the release workflow to publish to Modrinth using the `cloudnode-pro/m
odrinth-publish` action. [`dadcfe2`](https://github.com/QinCai-rui/ManhuntNG/commit/dadcfe273975e3391157a41b95775dfa78dc606d)

## [1.0.1] - 2026-07-08

### Fixed

- Inconsistency in death message formatting (Runner) [`4dacd8a`](https://github.com/QinCai-rui/ManhuntNG/commit/4dacd8aeb6f574a017a034199306d12e6e26cfb3)

## [1.0.0] - 2026-07-08

Initial public stable release of ManhuntNG.
