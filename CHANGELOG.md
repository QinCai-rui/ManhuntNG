# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-07-09

### Added

- Added support for multiple runners in a match
- Added multi-runner support to Normal mode (runners spawn in a center circle
  with hunters surrounding them in a larger ring)
- Added a new Infection gamemode, where hunters can infect runners and turn them
  into hunters
- Added a runner keepInventory option
- Added the ability to have late joiners in a match
- Players who disconnect during an active game now have their location saved and
  are teleported back to that exact spot on rejoin (fixes players being stranded
  in the main world after a disconnect)
- Added a configurable pause timeout: when the game is paused because an entire
  team disconnected, the **opposing** team automatically wins after
  `pauseTimeout.duration` seconds. The countdown is shown in the pause action bar
  (`pauseTimeout.enabled` / `pauseTimeout.duration` in `config.yml`)

### Changed

- Refactored join flow. Now players need to run `/manhunt join` to join a match.
- In Normal mode, hunters now win only when the last runner is eliminated
  (previously any runner death ended the game)
- The game now only auto-pauses when **all** runners or **all** hunters have
  disconnected (previously any single runner disconnect paused the game); a game
  with remaining runners/hunters keeps running

### Fixed

- Tracking compass now works properly by tracking nearest runners
- Fixed stale hunter and runner uuid entries after game end
- Fixed a bug where player would respawn in the lobby after death instead of the
  current game's world spawn
- Fixed a crash on game start caused by `setArmorContents(null)` (now clears armor
  with an empty array) in `GameManager.clearPlayerState`
- `eliminateRunner` and `eliminateHunter` now keep `playerRoles` and the match's
  runner/hunter/spectator sets in sync, so chat routing, combat/movement checks
  and team-chat logic no longer use a stale role after a player is eliminated
- Eliminated hunters are now removed from the hunter set and moved to spectators
  (previously they lingered in the hunter set and kept receiving game broadcasts)
- Runner deaths no longer incorrectly increment the hunter respawn counter

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
