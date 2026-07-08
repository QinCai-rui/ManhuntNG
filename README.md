# ManhuntNG

[![CI](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/ci.yml/badge.svg)](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/ci.yml) [![Release](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/release.yml/badge.svg)](https://github.com/QinCai-rui/ManhuntNG/actions/workflows/release.yml)

A (n *almost*) feature complete Minecraft Manhunt plugin for PaperMC. ManhuntNG provides enjoyable manhunt gameplay, cross-dimension **compass tracking**, "twists" such as potion effects (*more to come*), and controls for running multiple Manhunt games on Minecraft servers.

---

## Installation

1. Download the latest release JAR from the [Releases](https://github.com/QinCai-rui/ManhuntNG/releases/latest), or build from source (see Building & Development).
2. Place the JAR in your server's `plugins/` folder.
3. Start the server to generate the default configuration files.
4. Configure `config.yml` and `messages.yml` to your liking, then run `/manhunt reload` or restart the server to apply changes.

---

## Screenshots

![ManhuntNG help message (`/manhunt help`)](assets/screenshots/help-page.png)

---

## Features

### Tracking & Compass System

- Hunters receive a compass that points to the runner when in the same dimension.
- When across dimensions, the compass points to the runner's *last known location* in the tracking hunter's dimension
- Automatic compass regeneration on respawn
- Compass destruction on death to prevent duplication
- **Action bar** distance indicators (configurable)

### Gameplay

- Fully automated match (`"lobby"` -> `countdown` -> `pre‑hunt` -> `running` -> `finish`)
- Team/Global chat toggle. By default, hunters' messages are only visible by other hunters.
- Runner/hunter role assignment
- Automatic respawn handling for hunters
- Configurable inventory restoration (keepinventory, or keeparmour & keepoffhand) on respawn
- Automatic potion effects for runner/hunters (configurable)

### Match Management & Admin Tools

- Pause/resume system with working world freezing
- Auto pause when runner disconnects
- Owner‑based match control (admin with permissions can override)
- Force‑start option for debugging or other reasons
- Custom, preexisting world & seed selection for matches
- Ability to run multiple matches on the same server

### UI

- Action bar tracking messages
- Scoreboard with game info and timer
- Clickable help menu with hover/click actions (Adventure API)
- Configurable messages via `messages.yml`

## Requirements

- Java 17+ (match your server runtime. Use Java 25 for latest Minecraft versions)
- Bukkit/Spigot/**PaperMC** server (1.21.4+, I try to keep this plugin up-to-date for newer versions. I have tested on 26.1.2)

>[!NOTE]
> Although Bukkit/Spigot/PaperMC is supported, this plugin is primarily tested on PaperMC. Some features may not work as expected on non-PaperMC implementations. All servers *based on* PaperMC (such as PurpurMC) should work fine, but if you encounter issues on other server types, please [report them](https://github.com/QinCai-rui/ManhuntNG/issues).

## Permissions

- `manhunt.admin` — admin permissions (OP by default)
- `manhunt.play` — play and basic controls (true by default)
- `manhunt.spectate` — spectator access (true by default)

## Configuration & Messages

On first run the plugin creates a `plugins/ManhuntNG/` folder with `config.yml` and `messages.yml`.

Edit these files to adjust to what you desire. See the default configuration in [src/main/resources/config.yml](src/main/resources/config.yml) for available options.

The messages support legacy `&` colour codes and `{player}` placeholders. (TODO: change to Adventure text components/MiniMessage)

## Gameplay (Overview)

1. "Lobby": players join and the admin assigns runner/hunters.
2. Countdown: players are frozen for 5 seconds (configurable).
3. Pre-hunt: players are teleported to the match world; hunters form  circle around the runner.
4. Running: hunters receive tracking compasses; potion effects (if used) are applied.
5. Pause/Resume: owner/admin may pause or resume; world and mob behaviour are handled properly. Game automatically pauses when the runner leaves during a game.
6. **Win conditions**: runner wins by killing the Ender Dragon; hunters win by eliminating the runner (or if the runner dies).

## Tracking Behavior

Hunters receive a compass that points to the runner when in the same dimension. When across dimensions the compass points to the runner's last known location or the relevant portal. Compasses are destroyed on death to avoid duplication and re-given on respawn.

Action bar examples (with `tracking.showDistance: true`):

- Same dimension: `Runner — 123m`
- Runner in Nether: `Tracking Nether Portal — 234m`
- Runner in The End: `Tracking End Portal — 567m`

---

## Why ManhuntNG

I made this plugin because I wanted a more accurate tracking system for Minecraft Manhunt. Many existing plugins/datapacks have issues with cross-dimension tracking, and I wanted to create a solution that was reliable, easy to use, and configurable.

ManhuntNG is designed to be simple to set up and use, and also having advanced features for those who want more control over their Manhunt games.

### Team Chat

By default, your messages are only visible to your teammates, unless you are a runner (because as of right now there is no multi runner support)

### Better Tracking

Runner tracking is live within the same dimension, updates last‑known locations correctly across dimensions, and automatically disables the compass when data isn’t available.

### Match Lifecycle

The pause/resume system freezes movement (including camera angle), world time, block & entity interactions, furnace activity, and crafting (mob AI freeze: TODO). Matches also pause automatically whenever the runner disconnects.

### Respawn Handling

Respawns are handled cleanly: compasses are removed on death to avoid duplication and automatically returned on respawn; optional partial or full inventory restoration.

### World Management

The plugin allows for preexisting worlds or new worlds with custom seeds to be used for matches, and allows for multiple matches to run on the same server.

### Better Player UI/UX

Players get a clean, simple UI with action bar messages and a scoreboard. The help menu is clickable with hover/click actions (Adventure API).

### Simple Config

Potion effects, inventory restoration, and other options are configurable in `config.yml`. Messages are configurable in `messages.yml`.

### Modern-ish

(pls dont tell me to use gradle, i tried. trust me)

- Built with Adventure API  
- Modular managers (tracking, players, world, UI)  
- Clean command system  

---

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
