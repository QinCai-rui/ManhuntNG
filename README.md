# ManhuntNG

[![CI](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/ci.yml/badge.svg)](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/ci.yml) [![Release](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/release.yml/badge.svg)](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/release.yml)

A (n *almost*) feature complete Minecraft Manhunt plugin for PaperMC. ManhuntNG provides enjoyable manhunt gameplay, cross-dimension **compass tracking**, "twists" such as potion effects (*more to come*), and controls for running multiple Manhunt games on Minecraft servers.

## Features

- Tracking compasses that follow the runner across dimensions
- Automatic potion effects and respawn handling for hunters and runner
- Pause/resume and owner/admin controls to manage games
- Configurable messages and options via `config.yml` and `messages.yml`

## Requirements

- Java 17+ (match your server runtime. Use Java 25 for latest Minecraft versions)
- Paper server compatible with the plugin's target Minecraft version (1.21.4+)

## Installation

Download the latest release JAR from the [Releases](https://github.com/QinCai-rui/ManhuntNG/releases/latest), or build from source (see Building & Development).

## Quick Start

1. Place the JAR in the server `plugins/` folder and start the server.
2. Configure matches and roles using the `/manhunt` command group (see Commands below).

Example:

```minecraft-console
/manhunt runner <player>
/manhunt hunter <player>
/manhunt start
```

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/manhunt join` | `manhunt.play` | Join the lobby |
| `/manhunt leave` | `manhunt.play` | Leave the lobby |
| `/manhunt pause` | `manhunt.play` (owner) | Pause the current match |
| `/manhunt resume` | `manhunt.play` (owner) | Resume a paused match |
| `/manhunt start` | `manhunt.admin` | Start a match (requires runner + hunter(s)) |
| `/manhunt forcestart` | `manhunt.admin` | Force-start skipping validation |
| `/manhunt stop` | `manhunt.admin` | Force stop the current match |
| `/manhunt runner <player>` | `manhunt.admin` | Set the runner |
| `/manhunt hunter <player>` | `manhunt.admin` | Add a hunter |
| `/manhunt remove <player>` | `manhunt.admin` | Remove a player from the match |
| `/manhunt owner [player]` | `manhunt.admin` | View or set the game owner |
| `/manhunt seed [value]` | `manhunt.admin` | View or set the world seed |
| `/manhunt world [name]` | `manhunt.admin` | Use an existing world instead of generating one |
| `/manhunt reload` | `manhunt.admin` | Reload plugin config and messages |

## Permissions

- `manhunt.admin` — administrative actions (OP by default)
- `manhunt.play` — play and basic controls (true by default)
- `manhunt.spectate` — spectator access (true by default)

## Configuration & Messages

On first run the plugin creates a `plugins/ManhuntNG/` folder with `config.yml` and `messages.yml`.

Edit these files to adjust to what you desire. See the default configuration in [src/main/resources/config.yml](src/main/resources/config.yml) for available options.

Messages support `&` colour codes and `{player}` placeholders.

## Gameplay (Overview)

1. "Lobby": players join and the admin assigns runner/hunters.
2. Countdown: players are frozen while the match readies.
3. Pre-hunt: players are teleported to the match world; hunters form around the runner.
4. Running: hunters receive tracking compasses; potion effects (if used) are applied.
5. Pause/Resume: owner/admin may pause or resume; world and mob behaviour are handled safely. Game automatically pauses when the runner leaves during a game.
6. Win conditions: runner wins by killing the Ender Dragon; hunters win by eliminating the runner (or if the runner dies).

## Tracking Behavior

Hunters receive a compass that points to the runner when in the same dimension. When across dimensions the compass points to the runner's last known location or the relevant portal. Compasses are destroyed on death to avoid duplication and re-given on respawn.

Action bar examples (with `tracking.showDistance: true`):

- Same dimension: `Runner — 123m`
- Runner in Nether: `Tracking Nether Portal — 234m`
- Runner in The End: `Tracking End Portal — 567m`

## Building & Development

Build with:

```bash
mvn clean package
```

## Contributing

Contributions, bug reports, and feature requests are welcome.

## Support

For support or questions please [open an issue](https://github.com/QinCai-rui/ManhuntNG/issues/new/choose)

## License

This project is licensed under GNU GPL-3.0 (see [LICENSE](LICENSE)).
