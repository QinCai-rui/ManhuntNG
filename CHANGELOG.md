# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-07-09

### Added

- Added configurable role name tag suffixes (red `[H]` for hunters, green `[R]` for runners) using MiniMessage
- toggleable for the overhead nametag and tab list via `nameTags` in `config.yml`

## [1.3.0] - 2026-07-09

### Added

- Added support for multiple runners in a match [`b0e6e5d`](https://github.com/QinCai-rui/ManhuntNG/commit/b0e6e5d93b95c739bf829e687b8067a6ba58b3aa)
- Added multi-runner support to Normal mode (runners spawn in a center circle
  with hunters surrounding them in a larger ring) [`21a4b36`](https://github.com/QinCai-rui/ManhuntNG/commit/21a4b36d479b0eda3ecd88995d3956ac91a6c675)
- Added a new Infection gamemode, where hunters can infect runners and turn them
  into hunters [`3f387ee`](https://github.com/QinCai-rui/ManhuntNG/commit/3f387eef96affb8cdedd8c686ca4306c6d061f92)
- Added a runner keepInventory option [`3f387ee`](https://github.com/QinCai-rui/ManhuntNG/commit/3f387eef96affb8cdedd8c686ca4306c6d061f92)
- Added the ability to have late joiners in a match [`ab0c994`](https://github.com/QinCai-rui/ManhuntNG/commit/ab0c99462a87bc81e7862098c806cfa01b7d2677)
- Players who disconnect during an active game now have their location saved and
  are teleported back to that exact spot on rejoin (fixes players being stranded
  in the main world after a disconnect) [`737cdcc`](https://github.com/QinCai-rui/ManhuntNG/commit/737cdcc77d2231d99aa093bebd9c82925e9d0203)
- Added a configurable pause timeout: when the game is paused because an entire
  team disconnected, the **opposing** team automatically wins after
  `pauseTimeout.duration` seconds. The countdown is shown in the pause action bar
  (`pauseTimeout.enabled` / `pauseTimeout.duration` in `config.yml`) [`4c7fc2d`](https://github.com/QinCai-rui/ManhuntNG/commit/4c7fc2d47f9291a4fa198c7f9b2d0ff37bf75c3e)

### Changed

- Refactored join flow. Now players need to run `/manhunt join` to join a match. [`ab0c994`](https://github.com/QinCai-rui/ManhuntNG/commit/ab0c99462a87bc81e7862098c806cfa01b7d2677)
- In Normal mode, hunters now win only when the last runner is eliminated
  (previously any runner death ended the game) [`b0e6e5d`](https://github.com/QinCai-rui/ManhuntNG/commit/b0e6e5d93b95c739bf829e687b8067a6ba58b3aa)
- The game now only auto-pauses when **all** runners or **all** hunters have
  disconnected (previously any single runner disconnect paused the game); a game
  with remaining runners/hunters keeps running [`66ecc10`](https://github.com/QinCai-rui/ManhuntNG/commit/66ecc1057f95cfe2723014c228082d44707a9f1d)

### Fixed

- Tracking compass now works properly by tracking nearest runners [`3f387ee`](https://github.com/QinCai-rui/ManhuntNG/commit/3f387eef96affb8cdedd8c686ca4306c6d061f92)
- Fixed stale hunter and runner uuid entries after game end [`0e77cd2`](https://github.com/QinCai-rui/ManhuntNG/commit/0e77cd2dade027ae33f4ec2fddf32970a119b2fe)
- Fixed a bug where player would respawn in the lobby after death instead of the
  current game's world spawn [`0e77cd2`](https://github.com/QinCai-rui/ManhuntNG/commit/0e77cd2dade027ae33f4ec2fddf32970a119b2fe)
- Fixed a crash on game start caused by `setArmorContents(null)` (now clears armor
  with an empty array) in `GameManager.clearPlayerState` [`737cdcc`](https://github.com/QinCai-rui/ManhuntNG/commit/737cdcc77d2231d99aa093bebd9c82925e9d0203)
- `eliminateRunner` and `eliminateHunter` now keep `playerRoles` and the match's
  runner/hunter/spectator sets in sync, so chat routing, combat/movement checks
  and team-chat logic no longer use a stale role after a player is eliminated [`737cdcc`](https://github.com/QinCai-rui/ManhuntNG/commit/737cdcc77d2231d99aa093bebd9c82925e9d0203)
- Eliminated hunters are now removed from the hunter set and moved to spectators
  (previously they lingered in the hunter set and kept receiving game broadcasts)
  [`737cdcc`](https://github.com/QinCai-rui/ManhuntNG/commit/737cdcc77d2231d99aa093bebd9c82925e9d0203)
- Runner deaths no longer incorrectly increment the hunter respawn counter [`737cdcc`](https://github.com/QinCai-rui/ManhuntNG/commit/737cdcc77d2231d99aa093bebd9c82925e9d0203)

## [1.2.1] - 2026-07-08

### Deprecated

- Legacy bukkit chat formatting [`b7baaec`](https://github.com/QinCai-rui/ManhuntNG/commit/b7baaeca13dd6795517230c7379200583e04d42b)

### Changed

- Updated in-game messages, titles, countdowns, and broadcasts to use Adventure's
  Minimessages [`b7baaec`](https://github.com/QinCai-rui/ManhuntNG/commit/b7baaeca13dd6795517230c7379200583e04d42b)
- `messages.yml` is changed to the new formatting style [`b7baaec`](https://github.com/QinCai-rui/ManhuntNG/commit/b7baaeca13dd6795517230c7379200583e04d42b)

## [1.2.0] - 2026-07-08

### Added

- Added a new way to start the game. `/manhunt mode headstart` [`3d24f1f`](https://github.com/QinCai-rui/ManhuntNG/commit/3d24f1fce86ab1a247827d7145128c8c785c3482)

### Changed

- THe old start method is now identified as `dreamStart` [`3d24f1f`](https://github.com/QinCai-rui/ManhuntNG/commit/3d24f1fce86ab1a247827d7145128c8c785c3482)

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
