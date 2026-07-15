# ManhuntNG — Pumpkin Plugin Roadmap

## Implemented ✅

### Core Game Logic
- Match state machine (WAITING → COUNTDOWN → HEADSTART → PRE_HUNT → RUNNING → PAUSED → FINISHED)
- Runner / Hunter / Spectator role system
- Game start, stop, pause, resume
- Infection mode (hunter kills → becomes runner)
- Headstart mode (hunters frozen, runners get head start)
- Dreamstart mode (teleport to Nether portal)
- Force start (skip validation)
- Role shuffle with configurable runner count
- Owner system (pause/resume permissions)
- Seed and world name configuration

### Commands
- `/manhunt join|leave|start|stop|reload|forcestart`
- `/manhunt runner|hunter|remove|kick <player>`
- `/manhunt shuffle <count>`
- `/manhunt mode <normal|infection> <dreamstart|headstart>`
- `/manhunt pause|resume`
- `/manhunt owner [player]`
- `/manhunt seed [value]`
- `/manhunt world [name]`
- `/manhunt chat <global|team>`
- `/g <message>` — global chat shortcut
- `/t <message>` — team chat shortcut

### Events (25+ registered)
- Join, leave, login
- Chat (intercepted and rerouted)
- Move, teleport (freeze during restricted phases)
- Interact, block-break, block-place (freeze during restricted phases)
- Toggle sneak/flight/sprint (freeze during restricted phases)
- Inventory click/close (freeze during restricted phases)
- Item held (freeze during restricted phases)
- Change world, gamemode change
- Fish, egg throw, exp change
- Spawn change, server command, server broadcast

### UI & Display
- Scoreboard (runners, hunters, elapsed time)
- Action bar phase display
- Titles for phase transitions, pause/resume
- Display name with role suffixes `[R]` / `[H]`
- Tab list header/footer

### Chat
- Global vs team chat modes
- Per-player chat mode toggle
- Chat log to console

### Permissions
- `manhunt.admin` (Op)
- `manhunt.play` (Allow)
- `manhunt.spectate` (Allow)

### Other
- Compass tracker (last-known runner locations)
- Kill/death/win statistics tracking
- Config and messages YAML loading
- Loot table JSON loading
- Scheduled UI tick (every 20 ticks)

---

## Not Implemented ❌

### Blocked by Upstream API (no events/features in pumpkin-api-py)

| Feature | Status | Notes |
|---------|--------|-------|
| **Player death handling** | Blocked | No `PlayerDeathEvent` or `EntityDeathEvent`. Required for infection mode kill detection, runner death → hunter conversion. See [GitHub #1609](https://github.com/Pumpkin-MC/Pumpkin/issues/1609). |
| **Advancement listening** | Blocked | No advancement event. Needed to detect Nether portal, fortress, bastion, end portal discovery. |
| **Furnace interaction** | Blocked | No furnace/crafting event. Needed for blaze rod tracking. |
| **Game rule manipulation** | Blocked | No `setGameRule` API. Needed for `keepInventory`, `doDaylightCycle`, etc. |
| **Respawn event** | Blocked | No respawn event. Needed for post-death role assignment in infection mode. |
| **Persistent data container** | Blocked | No PDC API. Needed for cross-session stat persistence. |
| **World creation with seed** | Blocked | `Server.create_world()` exists but no seed parameter visible in API. |
| **Entity damage/cancel** | Blocked | No entity damage event. Needed for invincibility during pre-hunt. |
| **Projectile launch** | Blocked | No projectile launch event (only egg throw). Needed for ender pearl tracking. |
| **Player velocity** | Blocked | No velocity API. Needed for freeze enforcement (current approach uses move cancel which can be laggy). |

### Not Yet Implemented (code exists but incomplete)

| Feature | Status | Notes |
|---------|--------|-------|
| **Full infection mode** | Partial | Role assignment on join works, but kill-based conversion blocked by missing death event. |
| **Headstart countdown** | Partial | State exists but no timed countdown task wired up. |
| **Pause timeout** | Partial | State fields exist but no timeout enforcement. |
| **Loot drop on death** | Not started | Loot tables loaded but no death event to trigger drops. |
| **Potion effects** | Not started | `potion_manager.py` exists but empty. |
| **Nametag updates** | Partial | Applied on role set, but not updated on phase change. |
| **World teleport on start** | Not started | Need world creation + teleport all players. |
| **Debug command** | Not started | Java has `/manhuntdebug` — not ported. |

### Java Features Not Ported

| Feature | Reason |
|---------|--------|
| `FormationManager` | Depends on world creation API |
| `LootListener` | Depends on death event |
| `ManhuntDebugCommand` | Low priority |
| `WorldManager` full impl | Limited world API upstream |

---

## Upstream Dependencies

The plugin requires changes to the Pumpkin ecosystem to unlock remaining features:

1. **pumpkin-plugin-wit** — Add `player-death`, `entity-death`, `advancement`, `game-rule`, `respawn` events
2. **Pumpkin server (Rust)** — Implement the new events in the server core
3. **pumpkin-api-py** — Auto-regenerate Python bindings from updated WIT

---

## Build

```bash
cd pumpkin-plugin
./build.sh
# Output: manhuntng.wasm → place in Pumpkin server's plugins/
```

Requires: `pip install pumpkin-api-py componentize-py`

---

## File Structure

```
pumpkin-plugin/
├── main.py              # Plugin entry, commands, events, UI
├── match.py             # Match state data
├── game_manager.py      # Game lifecycle
├── player_manager.py    # Roles, respawns
├── config_manager.py    # YAML config + messages
├── chat_manager.py      # Global/team chat
├── ui_manager.py        # Phase detection, time formatting
├── tracker_manager.py   # Compass tracking
├── stats_manager.py     # Kill/death/win stats
├── nametag_manager.py   # Name tag rendering
├── potion_manager.py    # (placeholder)
├── loot_manager.py      # Loot table parsing
├── game_state.py        # GameState, StartMode, ManhuntGameMode enums
├── game_phase.py        # GamePhase display enum
├── player_role.py       # PlayerRole enum
├── chat_mode.py         # ChatMode enum
├── resources/
│   ├── config.yml
│   ├── messages.yml
│   └── loot.json
├── build.sh
├── requirements.txt
└── manhuntng.wasm       # Compiled output
```
