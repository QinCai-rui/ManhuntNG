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

---

## Why ManhuntNG

I made this plugin because I wanted a more accurate tracking system for Minecraft Manhunt. Many existing plugins/datapacks have issues with cross-dimension tracking, and I wanted to create a solution that was reliable, easy to use, and configurable.

ManhuntNG is designed to be simple to set up and use, and also having advanced features for those who want more control over their Manhunt games.

TODO: add teamchat support

### Better Tracking

- Live runner tracking in the same dimension  
- Dimension‑aware lastknown location tracking  
- Compass safely disables when data is missing  
- Event‑driven updates (death, respawn, dimension change)

### Match Lifecycle

- Working pause/resume system. Prevents movement, world time, block & entity interaction, furnace burning/smelting, and crafting. (TODO: add mob AI freezing)
- Auto‑pause when runner disconnects  

### Respawn Handling

- Automatic compass removal on death (prevents duplication)
- Automatic compass re‑giving on respawn  
- Optional partial/complete inventory restoration

### World Management

- Custom world selection  
- Custom seed selection  
- Multi‑match support on the same server  
- Clean world teardown/reset between matches

### Better Player UI/UX

- Action bar tracking messages (dimension‑aware)  
- Scoreboard with live match info  
- Clear state transitions  
- Fully configurable messages

### Simple Config

- potion effects  
- respawn rules  
- Clean tracking options  
- No complex twist scripting required

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
