# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.7.0] - 2026-07-18

### Added

- Added configurable loot table modifications via `loot.json`. Admins can now
  modify mob drops, piglin bartering `outcomes`, and structure chest loot to give
  runners/hunters (configurable, see below) an advantage.
  [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)
- Added three loot modification sources:
  - **Mob drops**: Boost ender pearl, blaze rod, arrow, and string drops from
    killed mobs (`EntityDeathEvent`)
  - **Piglin bartering**: Replace bartering outcomes with configured items
    (`PiglinBarterEvent`)
  - **Structure chest loot**: Add extra items to bastion, fortress, and end city
    chests (`LootGenerateEvent`)
    [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)
- Added per-entry role filtering (`"all"`, `"runner"`, `"hunter"`) so admins
  can control which team benefits from each loot modification
  [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)
- Added potion item support with `potion-type` and `potion-form` fields for
  configuring splash, lingering, or drinkable potions (eg. fire res)
  [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)
- Added item control: material, min/max amount, drop chance/weight,
  display name, and potion properties
  [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)
- **NOTE**: Mob drops and structure chest loot are additive - custom items are
  added alongside vanilla drops. Piglin bartering outcomes, however, replace
  vanilla bartering results when configured

### Changed

- `/manhunt reload` now also reloads `loot.json` to hot-reload loot-table changes
  [`c5f3ac0`](https://github.com/QinCai-rui/ManhuntNG/commit/c5f3ac0eed558c34ef9ce5da10d5feba0f343c91)

## [1.6.0] - 2026-07-11

### Added

- Added `/manhunt shuffle <runners_count>` command to randomly assign a given
  number of players as runners, and the rest as hunters [`b52d9ca`](https://github.com/QinCai-rui/ManhuntNG/commit/b52d9ca0c06e773ab43df681b2b1a15c4b2708cb)

## [1.5.1] - 2026-07-10

### Added

- Added `chat.logToConsole` config option (default `true`) to log game chat
  messages to the server console with format `[Manhunt Chat] [Global/Team] Play
  r: message` [`f564955`](https://github.com/QinCai-rui/ManhuntNG/commit/f564955583073d869edcc0398b56065149920f0c)

## [1.5.0] - 2026-07-10

### Added

- Runner respawn system: runners now support multiple lives via `runner.respawnLimit`
  in `config.yml` (default `0`: first death = elimination).
  When a runner dies with lives remaining, they respawn at world spawn with potion
  effects reapplied instead of being converted to spectator. Death and respawn messages
  display remaining lives. Runners respect `runner.keepInventory` on respawn.
  [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)
- Added `applyRunnerEffects()` to `PotionEffectManager` for reapplying runner
  potion effects on respawn [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)
- Added runner death/respawn/elimination messages to `messages.yml`:
  `death.runner-lives`, `death.runner-eliminated`, `respawn.runner`
  [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)

### Changed (*BREAKING*)

- Hunter respawn limit check now uses `respawnLimit == -1` for infinite instead
  of a separate boolean flag [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)

### Removed

- Removed `runner.lives` config option [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)
- Removed `hunters.infiniteRespawns` config option (See _Changed_ section above)
  [`f77245b`](https://github.com/QinCai-rui/ManhuntNG/commit/f77245b5157593f28975d405aa942b11af404625)

## [1.4.1] - 2026-07-10

### Changed

- All hardcoded messages now read from `messages.yml`, making every
  message configurable and translatable (bumped `config-version` to 3)
  [`d005dd9`](https://github.com/QinCai-rui/ManhuntNG/commit/d005dd95554bd9e9cdf90eaa7ec4c00d774de208)
  
### Added

- Added `getMessageComponent()` helpers to `ConfigManager` to return
  Minimessage-deserialised `Component` objects
  [`d005dd9`](https://github.com/QinCai-rui/ManhuntNG/commit/d005dd95554bd9e9cdf90eaa7ec4c00d774de208)
- Externalised enum display names (`GamePhase`, `ManhuntGameMode`, `StartMode`,
`PlayerRole`) to the `advanced` section of `messages.yml`
[`d005dd9`](https://github.com/QinCai-rui/ManhuntNG/commit/d005dd95554bd9e9cdf90eaa7ec4c00d774de208)

## [1.4.0] - 2026-07-09

### Added

- Added configurable role name tag suffixes (red `[H]` for hunters, green `[R]` for runners) using MiniMessage
  [`dfca4b5`](https://github.com/QinCai-rui/ManhuntNG/commit/dfca4b5846afef678d84849c45283b507d28354f)
- toggleable for the overhead nametag and tab list via `nameTags` in `config.yml`
  [`dfca4b5`](https://github.com/QinCai-rui/ManhuntNG/commit/dfca4b5846afef678d84849c45283b507d28354f)

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
