"""
ManhuntNG - Pumpkin MC Python Plugin

A feature-complete Minecraft Manhunt server plugin.
Hunters track and chase the Runner, who must defeat the Ender Dragon.

Uses all available upstream pumpkin-api-py features:
  Events: join, leave, chat, move, teleport, interact, block-break,
          block-place, toggle-sneak/flight/sprint, change-world,
          gamemode-change, inventory-click/close, item-held, fish,
          egg-throw, spawn-change, server-command, server-broadcast
  Commands: /manhunt (with subcommands), /g, /t
  Scoreboard, titles, action bar, teleport, gamemode, health, food,
  display name, tab list, permissions, scheduler, world creation,
  entity invulnerability, text formatting
"""

from pumpkin_api import (
    Plugin, register_plugin,
    server, event, command, text, context, logging, permission, scoreboard, world, common
)
from pumpkin_api import scheduler as sched
from pumpkin_api import metadata as plugin_metadata

from game_state import GameState, StartMode, ManhuntGameMode
from player_role import PlayerRole
from chat_mode import ChatMode
from game_phase import GamePhase
from game_manager import GameManager
from player_manager import PlayerManager
from config_manager import ConfigManager
from chat_manager import ChatManager
from ui_manager import UIManager
from tracker_manager import TrackerManager
from stats_manager import StatisticsManager


# ── helpers ──────────────────────────────────────────────────────────

def tc(raw: str) -> "text.TextComponent":
    """Shortcut: build a TextComponent from a raw string."""
    return text.TextComponent.text(raw)


def tc_bold(raw: str) -> "text.TextComponent":
    return text.TextComponent.text(raw).bold(True)


def tc_colored(raw: str, color) -> "text.TextComponent":
    return text.TextComponent.text(raw).color_named(color)


def _get_player_uuid(evt_data) -> str:
    """Extract UUID string from any event data that has a .player field."""
    return evt_data.player.get_uuid()


def _get_player_name(evt_data) -> str:
    return evt_data.player.get_name()


# ── plugin ───────────────────────────────────────────────────────────

class ManhuntPlugin(Plugin):

    # ── metadata ─────────────────────────────────────────────────────
    def metadata(self) -> "plugin_metadata.PluginMetadata":
        return plugin_metadata.PluginMetadata(
            name="ManhuntNG",
            version="1.0.0",
            authors=["QinCai"],
            description="A feature-complete Minecraft Manhunt plugin for Pumpkin MC",
            dependencies=[],
            permissions=[
                "manhunt.admin",
                "manhunt.play",
                "manhunt.spectate",
            ],
        )

    # ── lifecycle ────────────────────────────────────────────────────
    def on_load(self, ctx: context.Context) -> None:
        # --- config ---
        self.config_manager = ConfigManager(ctx.get_data_folder())
        self.config_manager.load_configs()

        # --- managers ---
        self.game_manager = GameManager()
        self.game_manager.config = self.config_manager
        self.player_manager = PlayerManager(self.game_manager)
        self.chat_manager = ChatManager(self.game_manager, self.player_manager)
        self.ui_manager = UIManager(self.game_manager, self.config_manager)
        self.tracker_manager = TrackerManager(self.game_manager, self.player_manager)
        self.stats_manager = StatisticsManager()

        # --- permissions ---
        self._register_permissions(ctx)

        # --- commands ---
        self._register_commands(ctx)

        # --- events ---
        self._register_events(ctx)

        # --- scheduled tasks (1 second / 20 ticks interval) ---
        self._ui_task_id = self.schedule_repeating_task(
            20, 20, self._tick_ui
        )

        logging.log(logging.Level.INFO, "ManhuntNG loaded!")

    def on_unload(self, ctx: context.Context) -> None:
        try:
            sched.cancel_task(self._ui_task_id)
        except Exception:
            pass
        logging.log(logging.Level.INFO, "ManhuntNG unloaded.")

    # ── permissions ──────────────────────────────────────────────────
    def _register_permissions(self, ctx: context.Context) -> None:
        for node, desc, default in [
            ("manhunt.admin", "ManhuntNG admin commands", permission.PermissionDefault_Op()),
            ("manhunt.play", "Allows playing Manhunt", permission.PermissionDefault_Allow()),
            ("manhunt.spectate", "Allows spectating Manhunt", permission.PermissionDefault_Allow()),
        ]:
            ctx.register_permission(permission.Permission(
                node=node, description=desc, default=default, children=[],
            ))

    # ── commands ─────────────────────────────────────────────────────
    def _register_commands(self, ctx: context.Context) -> None:
        # /manhunt <subcommand> [args...]
        # GreedyString captures everything after /manhunt; we split manually
        # so that subcommands with extra args (runner Foo, shuffle 3, etc.) work.
        mh = command.Command(["manhunt"], "ManhuntNG main command")
        mh = mh.then(
            command.CommandNode.argument("subcommand", command.ArgumentType_GreedyString())
        )
        self.register_command(ctx, mh, self._cmd_manhunt, "manhunt.admin")

        # /g <message> — global chat shortcut
        g_cmd = command.Command(["g"], "Send a global message")
        g_cmd = g_cmd.then(
            command.CommandNode.argument("message", command.ArgumentType_GreedyString())
        )
        self.register_command(ctx, g_cmd, self._cmd_global_chat, "manhunt.play")

        # /t <message> — team chat shortcut
        t_cmd = command.Command(["t"], "Send a team message")
        t_cmd = t_cmd.then(
            command.CommandNode.argument("message", command.ArgumentType_GreedyString())
        )
        self.register_command(ctx, t_cmd, self._cmd_team_chat, "manhunt.play")

    # ── command handlers ─────────────────────────────────────────────

    def _cmd_manhunt(
        self,
        sender: command.CommandSender,
        srv: server.Server,
        args: command.ConsumedArgs,
    ) -> int:
        raw = (args.get_value("subcommand") or "").strip()
        if not raw:
            self._send_help(sender)
            return 1

        parts = raw.split(None, 1)
        sub = parts[0].lower()
        extra = parts[1].strip() if len(parts) > 1 else ""

        player_uuid = None
        player_name = None
        if sender.is_player():
            p = sender.as_player()
            player_uuid = p.get_uuid()
            player_name = p.get_name()

        handlers = {
            "join": lambda: self._mh_join(sender, player_uuid, player_name),
            "leave": lambda: self._mh_leave(sender, player_uuid, player_name),
            "start": lambda: self._mh_start(sender, player_uuid),
            "stop": lambda: self._mh_stop(sender),
            "reload": lambda: self._mh_reload(sender),
            "runner": lambda: self._mh_set_role(sender, srv, extra, PlayerRole.RUNNER),
            "hunter": lambda: self._mh_set_role(sender, srv, extra, PlayerRole.HUNTER),
            "remove": lambda: self._mh_remove_role(sender, srv, extra),
            "kick": lambda: self._mh_kick(sender, srv, extra),
            "forcestart": lambda: self._mh_force_start(sender, player_uuid),
            "shuffle": lambda: self._mh_shuffle(sender, srv, extra),
            "mode": lambda: self._mh_mode(sender, extra),
            "pause": lambda: self._mh_pause(sender, player_uuid),
            "resume": lambda: self._mh_resume(sender, player_uuid),
            "owner": lambda: self._mh_owner(sender, srv, extra),
            "seed": lambda: self._mh_seed(sender, extra),
            "world": lambda: self._mh_world(sender, extra),
            "chat": lambda: self._mh_chat_mode(sender, extra),
        }

        if sub in handlers:
            handlers[sub]()
        elif sub == "help":
            self._send_help(sender)
        else:
            sender.send_message(tc(f"Unknown subcommand: {sub}. Use /manhunt help"))
        return 1

    def _cmd_global_chat(
        self, sender: command.CommandSender, srv: server.Server, args: command.ConsumedArgs,
    ) -> int:
        if not sender.is_player():
            sender.send_message(tc("Only players can use this!"))
            return 1
        p = sender.as_player()
        msg = args.get_value("message") or ""
        if not msg.strip():
            sender.send_message(tc("Message cannot be empty!"))
            return 1
        formatted = self.chat_manager.send_global_message(
            p.get_uuid(), p.get_name(), msg
        )
        for uuid in self.chat_manager.get_all_participants():
            target = srv.get_player_by_uuid(uuid)
            if target:
                target.send_message(tc(formatted), False)
        return 1

    def _cmd_team_chat(
        self, sender: command.CommandSender, srv: server.Server, args: command.ConsumedArgs,
    ) -> int:
        if not sender.is_player():
            sender.send_message(tc("Only players can use this!"))
            return 1
        p = sender.as_player()
        msg = args.get_value("message") or ""
        if not msg.strip():
            sender.send_message(tc("Message cannot be empty!"))
            return 1
        role = self.player_manager.get_role(p.get_uuid())
        if role == PlayerRole.SPECTATOR:
            sender.send_message(tc("Spectators can only use global chat!"))
            return 1
        formatted = self.chat_manager.send_team_message(
            p.get_uuid(), p.get_name(), msg
        )
        for uuid in self.chat_manager.get_recipients_for_team(p.get_uuid()):
            target = srv.get_player_by_uuid(uuid)
            if target:
                target.send_message(tc(formatted), False)
        return 1

    # ── /manhunt subcommand helpers ──────────────────────────────────

    def _mh_join(self, sender, uuid, name):
        if not uuid:
            sender.send_message(tc("Only players can use this!"))
            return
        match = self.game_manager.match
        if match.is_participant(uuid):
            sender.send_message(tc("You have already joined!"))
            return
        self.player_manager.set_role(uuid, PlayerRole.SPECTATOR)
        match.add_spectator(uuid)
        if self.game_manager.is_game_active():
            sender.send_message(tc("You joined as a spectator!"))
        else:
            sender.send_message(tc("You joined the manhunt lobby!"))
        self._broadcast(cfg_msg("join.broadcast", player=name))

    def _mh_leave(self, sender, uuid, name):
        if not uuid:
            sender.send_message(tc("Only players can use this!"))
            return
        match = self.game_manager.match
        if not match.is_participant(uuid):
            sender.send_message(tc(f"{name} is not in the game!"))
            return
        self.player_manager.remove_player_from_game(uuid)
        sender.send_message(tc("You left the manhunt lobby."))
        self._broadcast(cfg_msg("leave.broadcast", player=name))

    def _mh_start(self, sender, uuid):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        if self.game_manager.start_game(uuid):
            self._broadcast(cfg_msg("game.started"))
        else:
            sender.send_message(tc("Cannot start (no runners/hunters or game active)!"))

    def _mh_stop(self, sender):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        self.game_manager.stop_game()
        sender.send_message(tc("Game stopped."))

    def _mh_reload(self, sender):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        self.config_manager.reload_configs()
        sender.send_message(tc("Configuration reloaded!"))

    def _mh_set_role(self, sender, srv, extra: str, role: PlayerRole):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        target_name = extra.strip()
        if not target_name:
            sender.send_message(tc("Usage: /manhunt runner <player>"))
            return
        target = srv.get_player_by_name(target_name)
        if not target:
            sender.send_message(tc("Player not found!"))
            return
        if self.game_manager.is_game_active():
            sender.send_message(tc("Cannot change roles during an active game!"))
            return
        target_uuid = target.get_uuid()
        self.player_manager.apply_role_to_player(target_uuid, role)
        role_name = "Runner" if role == PlayerRole.RUNNER else "Hunter"
        sender.send_message(tc(f"{target_name} is now a {role_name}!"))
        target.send_message(tc(f"You are now a {role_name}!"))
        self._apply_nametag(target, target_uuid)

    def _mh_remove_role(self, sender, srv, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        target_name = extra.strip()
        if not target_name:
            sender.send_message(tc("Usage: /manhunt remove <player>"))
            return
        target = srv.get_player_by_name(target_name)
        if not target:
            sender.send_message(tc("Player not found!"))
            return
        if self.game_manager.is_game_active():
            sender.send_message(tc("Cannot change roles during an active game!"))
            return
        self.player_manager.remove_player_from_game(target.get_uuid())
        sender.send_message(tc(f"{target_name}'s role has been removed!"))
        target.send_message(tc("Your role has been removed!"))

    def _mh_kick(self, sender, srv, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        target_name = extra.strip()
        if not target_name:
            sender.send_message(tc("Usage: /manhunt kick <player>"))
            return
        target = srv.get_player_by_name(target_name)
        if not target:
            sender.send_message(tc("Player not found!"))
            return
        self.player_manager.remove_player_from_game(target.get_uuid())
        sender.send_message(tc(f"{target_name} has been kicked from the game!"))
        target.send_message(tc("You have been kicked from the game!"))
        self._broadcast(cfg_msg("admin.kick-broadcast", player=target_name))

    def _mh_force_start(self, sender, uuid):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        if self.game_manager.start_game_force(uuid):
            self._broadcast(cfg_msg("forcestart.broadcast"))
        else:
            sender.send_message(tc("Cannot force start!"))

    def _mh_shuffle(self, sender, srv, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        try:
            runner_count = int(extra)
        except (ValueError, TypeError):
            sender.send_message(tc("Usage: /manhunt shuffle <count>"))
            return
        if runner_count < 1:
            sender.send_message(tc("Invalid number!"))
            return
        if self.game_manager.is_game_active():
            sender.send_message(tc("Cannot change roles during an active game!"))
            return

        match = self.game_manager.match
        all_uuids = list(match.runner_uuids | match.hunter_uuids | match.spectator_uuids)
        online = [u for u in all_uuids if srv.get_player_by_uuid(u) is not None]

        if runner_count >= len(online) or len(online) < runner_count + 1:
            sender.send_message(tc(f"Not enough online players! Need at least {runner_count + 1}."))
            return

        import random
        random.shuffle(online)
        for i, uuid in enumerate(online):
            role = PlayerRole.RUNNER if i < runner_count else PlayerRole.HUNTER
            self.player_manager.apply_role_to_player(uuid, role)
            p = srv.get_player_by_uuid(uuid)
            if p:
                self._apply_nametag(p, uuid)

        hunters = len(online) - runner_count
        sender.send_message(tc(f"Randomly assigned {runner_count} runner(s) and {hunters} hunter(s)!"))

    def _mh_mode(self, sender, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        if self.game_manager.is_game_active():
            sender.send_message(tc("Cannot change mode during an active game!"))
            return
        parts = extra.split()
        gm_str = parts[0].lower() if len(parts) > 0 else ""
        sm_str = parts[1].lower() if len(parts) > 1 else ""
        if not gm_str or not sm_str:
            sender.send_message(tc("Usage: /manhunt mode <normal|infection> <dreamstart|headstart>"))
            return
        gm = ManhuntGameMode.INFECTION if gm_str == "infection" else ManhuntGameMode.NORMAL
        sm = StartMode.HEADSTART if sm_str == "headstart" else StartMode.DREAMSTART
        self.game_manager.match.game_mode = gm
        self.game_manager.match.start_mode = sm
        sender.send_message(tc(f"Game mode set to {gm.value}, Start mode set to {sm.value}"))

    def _mh_pause(self, sender, uuid):
        if not uuid:
            sender.send_message(tc("Only players can use this!"))
            return
        if self.game_manager.match.state not in (GameState.RUNNING, GameState.PRE_HUNT, GameState.HEADSTART):
            sender.send_message(tc("No active game to pause!"))
            return
        if not self.game_manager.match.is_owner(uuid) and not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("Only the game owner can pause the game!"))
            return
        if self.game_manager.pause_game(uuid):
            self._broadcast(cfg_msg("pause.broadcast"))
            self._send_title_to_all("GAME PAUSED", "Use /manhunt resume to continue")
        else:
            sender.send_message(tc("Failed to pause the game!"))

    def _mh_resume(self, sender, uuid):
        if not uuid:
            sender.send_message(tc("Only players can use this!"))
            return
        if self.game_manager.match.state != GameState.PAUSED:
            sender.send_message(tc("No paused game to resume!"))
            return
        if not self.game_manager.match.is_owner(uuid) and not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("Only the game owner can resume the game!"))
            return
        if self.game_manager.resume_game(uuid):
            self._broadcast(cfg_msg("resume.broadcast"))
            self._send_title_to_all("Game Resumed", "The game has been resumed")
        else:
            sender.send_message(tc("Failed to resume the game!"))

    def _mh_owner(self, sender, srv, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        target_name = extra.strip()
        if not target_name:
            owner = self.game_manager.match.owner_uuid
            if owner:
                p = srv.get_player_by_uuid(owner)
                name = p.get_name() if p else "Unknown"
                sender.send_message(tc(f"Game owner: {name}"))
            else:
                sender.send_message(tc("No game owner set."))
            return
        target = srv.get_player_by_name(target_name)
        if not target:
            sender.send_message(tc("Player not found!"))
            return
        self.game_manager.match.owner_uuid = target.get_uuid()
        sender.send_message(tc(f"{target_name} is now the game owner!"))
        target.send_message(tc("You are now the game owner!"))

    def _mh_seed(self, sender, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        val = extra.strip()
        if not val:
            seed = self.game_manager.match.seed
            if seed is not None:
                sender.send_message(tc(f"Current seed: {seed}"))
            else:
                sender.send_message(tc("No seed set (will use random)."))
            return
        try:
            seed = int(val)
        except ValueError:
            seed = hash(val) & 0xFFFFFFFF
        self.game_manager.match.seed = seed
        sender.send_message(tc(f"World seed set to {seed} (\"{val}\")"))

    def _mh_world(self, sender, extra: str):
        if not sender.has_permission("manhunt.admin"):
            sender.send_message(tc("You don't have permission!"))
            return
        if self.game_manager.is_game_active():
            sender.send_message(tc("Cannot change world during an active game!"))
            return
        name = extra.strip()
        if not name:
            wn = self.game_manager.match.world_name
            if wn:
                sender.send_message(tc(f"Current world: {wn}"))
            else:
                sender.send_message(tc("No world set (will generate a new world)."))
            return
        if name.lower() in ("clear", "reset"):
            self.game_manager.match.world_name = None
            sender.send_message(tc("World cleared. Will generate a new world on start."))
        else:
            self.game_manager.match.world_name = name
            sender.send_message(tc(f"World set to {name}."))

    def _mh_chat_mode(self, sender, extra: str):
        if not sender.is_player():
            sender.send_message(tc("Only players can use this!"))
            return
        p = sender.as_player()
        mode_str = extra.strip().lower()
        if not mode_str:
            mode = self.chat_manager.get_chat_mode(p.get_uuid())
            label = "GLOBAL" if mode == ChatMode.GLOBAL else "TEAM"
            sender.send_message(tc(f"Current chat mode: {label}"))
            return
        if mode_str in ("global", "g"):
            self.chat_manager.set_chat_mode(p.get_uuid(), ChatMode.GLOBAL)
            sender.send_message(tc("Chat mode set to GLOBAL"))
        elif mode_str in ("team", "t"):
            if self.chat_manager.is_team_single_player(p.get_uuid()):
                sender.send_message(tc("Cannot use team chat - you're the only one on your team."))
            else:
                self.chat_manager.set_chat_mode(p.get_uuid(), ChatMode.TEAM)
                sender.send_message(tc("Chat mode set to TEAM"))
        else:
            sender.send_message(tc("Usage: /manhunt chat <global|team>"))

    def _send_help(self, sender):
        state = self.game_manager.match.state
        state_name = self._get_state_name(state)
        sender.send_message(tc(""))
        sender.send_message(tc_bold(f"ManhuntNG — {state_name}"))
        sender.send_message(tc("─────────────────────────────────────"))
        sender.send_message(tc_bold("  Player"))
        for line in [
            "/manhunt join — Join the lobby",
            "/manhunt leave — Leave the lobby",
            "/manhunt pause — Pause the game (owner only)",
            "/manhunt resume — Resume the game (owner only)",
            "/manhunt chat <global|team> — Switch chat mode",
            "/g <message> — Global chat shortcut",
            "/t <message> — Team chat shortcut",
        ]:
            sender.send_message(tc(f"  {line}"))
        if sender.has_permission("manhunt.admin"):
            sender.send_message(tc(""))
            sender.send_message(tc_bold("  Admin"))
            for line in [
                "/manhunt start — Start the match",
                "/manhunt stop — Force stop the match",
                "/manhunt runner <player> — Set the Runner",
                "/manhunt hunter <player> — Add a Hunter",
                "/manhunt remove <player> — Remove a player's role",
                "/manhunt kick <player> — Kick a player from the game",
                "/manhunt owner [player] — View/set game owner",
                "/manhunt seed [value] — View/set world seed",
                "/manhunt world [name] — View/set existing world",
                "/manhunt mode <normal|infection> <dreamstart|headstart>",
                "/manhunt forcestart — Skip validation & start",
                "/manhunt shuffle <count> — Randomly assign roles",
                "/manhunt reload — Reload configuration",
            ]:
                sender.send_message(tc(f"  {line}"))

    def _get_state_name(self, state: GameState) -> str:
        return {
            GameState.WAITING: "Waiting",
            GameState.COUNTDOWN: "Countdown",
            GameState.HEADSTART: "Head Start",
            GameState.PRE_HUNT: "Pre-Hunt",
            GameState.RUNNING: "Running",
            GameState.PAUSED: "Paused",
            GameState.FINISHED: "Finished",
        }.get(state, "Unknown")

    # ── events ───────────────────────────────────────────────────────
    def _register_events(self, ctx: context.Context) -> None:
        self.register_event(ctx, event.EventType.PLAYER_JOIN_EVENT, self.on_player_join)
        self.register_event(ctx, event.EventType.PLAYER_LEAVE_EVENT, self.on_player_leave)
        self.register_event(ctx, event.EventType.PLAYER_CHAT_EVENT, self.on_player_chat)
        self.register_event(ctx, event.EventType.PLAYER_MOVE_EVENT, self.on_player_move)
        self.register_event(ctx, event.EventType.PLAYER_TELEPORT_EVENT, self.on_player_teleport)
        self.register_event(ctx, event.EventType.PLAYER_INTERACT_EVENT, self.on_player_interact)
        self.register_event(ctx, event.EventType.BLOCK_BREAK_EVENT, self.on_block_break)
        self.register_event(ctx, event.EventType.BLOCK_PLACE_EVENT, self.on_block_place)
        self.register_event(ctx, event.EventType.PLAYER_TOGGLE_SNEAK_EVENT, self.on_toggle_sneak)
        self.register_event(ctx, event.EventType.PLAYER_TOGGLE_FLIGHT_EVENT, self.on_toggle_flight)
        self.register_event(ctx, event.EventType.PLAYER_TOGGLE_SPRINT_EVENT, self.on_toggle_sprint)
        self.register_event(ctx, event.EventType.PLAYER_CHANGE_WORLD_EVENT, self.on_change_world)
        self.register_event(ctx, event.EventType.PLAYER_GAMEMODE_CHANGE_EVENT, self.on_gamemode_change)
        self.register_event(ctx, event.EventType.INVENTORY_CLICK_EVENT, self.on_inventory_click)
        self.register_event(ctx, event.EventType.INVENTORY_CLOSE_EVENT, self.on_inventory_close)
        self.register_event(ctx, event.EventType.PLAYER_ITEM_HELD_EVENT, self.on_item_held)
        self.register_event(ctx, event.EventType.PLAYER_FISH_EVENT, self.on_fish)
        self.register_event(ctx, event.EventType.PLAYER_EGG_THROW_EVENT, self.on_egg_throw)
        self.register_event(ctx, event.EventType.PLAYER_EXP_CHANGE_EVENT, self.on_exp_change)
        self.register_event(ctx, event.EventType.SPAWN_CHANGE_EVENT, self.on_spawn_change)
        self.register_event(ctx, event.EventType.SERVER_COMMAND_EVENT, self.on_server_command)
        self.register_event(ctx, event.EventType.SERVER_BROADCAST_EVENT, self.on_server_broadcast)
        self.register_event(ctx, event.EventType.PLAYER_LOGIN_EVENT, self.on_player_login)

    # ── event: join ──────────────────────────────────────────────────
    def on_player_join(self, srv: server.Server, evt: event.PlayerJoinEventData) -> event.PlayerJoinEventData:
        self._store_server(srv)
        uuid = _get_player_uuid(evt)
        name = _get_player_name(evt)

        if not self.game_manager.is_game_active():
            self.player_manager.set_role(uuid, PlayerRole.SPECTATOR)
            self.game_manager.match.add_spectator(uuid)
        elif self.game_manager.match.is_participant(uuid):
            if self.player_manager.is_hunter(uuid):
                if self.game_manager.match.state == GameState.RUNNING:
                    self.tracker_manager.update_runner_last_known(uuid, "overworld", 0, 0, 0)
            p = srv.get_player_by_uuid(uuid)
            if p:
                self._apply_nametag(p, uuid)

        self._broadcast(cfg_msg("join.broadcast", player=name))
        return evt

    def on_player_leave(self, srv: server.Server, evt: event.PlayerLeaveEventData) -> event.PlayerLeaveEventData:
        uuid = _get_player_uuid(evt)
        name = _get_player_name(evt)

        if self.game_manager.is_game_active() and self.game_manager.match.is_participant(uuid):
            self._broadcast(cfg_msg("leave.broadcast", player=name))

        if self.game_manager.match.state in (GameState.RUNNING, GameState.HEADSTART):
            if not self.game_manager.match.runner_uuids or not self.game_manager.match.hunter_uuids:
                return evt
            any_runner = any(uid != uuid for uid in self.game_manager.match.runner_uuids)
            any_hunter = any(uid != uuid for uid in self.game_manager.match.hunter_uuids)
            if not any_runner:
                self._broadcast(cfg_msg("pause.runners-disconnected"))
                self.game_manager.pause_game()
            elif not any_hunter:
                self._broadcast(cfg_msg("pause.hunters-disconnected"))
                self.game_manager.pause_game()

        return evt

    # ── event: chat ──────────────────────────────────────────────────
    def on_player_chat(self, srv: server.Server, evt: event.PlayerChatEventData) -> event.PlayerChatEventData:
        if not self.game_manager.is_game_active():
            return evt

        uuid = _get_player_uuid(evt)
        role = self.player_manager.get_role(uuid)
        match = self.game_manager.match

        is_participant = (
            role in (PlayerRole.HUNTER, PlayerRole.RUNNER) or
            (role == PlayerRole.SPECTATOR and uuid in match.spectator_uuids)
        )
        if not is_participant:
            return evt

        mode = self.chat_manager.get_chat_mode(uuid)
        if mode == ChatMode.TEAM and self.chat_manager.is_team_single_player(uuid):
            mode = ChatMode.GLOBAL

        name = _get_player_name(evt)
        msg = evt.message

        if mode == ChatMode.GLOBAL:
            formatted = self.chat_manager.send_global_message(uuid, name, msg)
            for rid in self.chat_manager.get_all_participants():
                target = srv.get_player_by_uuid(rid)
                if target:
                    target.send_message(tc(formatted), False)
        else:
            formatted = self.chat_manager.send_team_message(uuid, name, msg)
            for rid in self.chat_manager.get_recipients_for_team(uuid):
                target = srv.get_player_by_uuid(rid)
                if target:
                    target.send_message(tc(formatted), False)

        if self.config_manager.is_chat_log_to_console():
            logging.log(logging.Level.INFO, f"[Manhunt Chat] {formatted}")

        evt.cancelled = True
        return evt

    # ── event: move (freeze during restricted phases) ────────────────
    def on_player_move(self, srv: server.Server, evt: event.PlayerMoveEventData) -> event.PlayerMoveEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state

        if state in (GameState.PAUSED, GameState.COUNTDOWN):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        elif state == GameState.PRE_HUNT:
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        elif state == GameState.HEADSTART:
            if self.player_manager.is_hunter(uuid):
                evt.cancelled = True

        return evt

    # ── event: teleport ──────────────────────────────────────────────
    def on_player_teleport(self, srv: server.Server, evt: event.PlayerTeleportEventData) -> event.PlayerTeleportEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        elif state == GameState.HEADSTART:
            if self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: interact ──────────────────────────────────────────────
    def on_player_interact(self, srv: server.Server, evt: event.PlayerInteractEventData) -> event.PlayerInteractEventData:
        uuid = _get_player_uuid(evt)
        self._cancel_if_restricted(uuid, evt)
        return evt

    # ── event: block break ───────────────────────────────────────────
    def on_block_break(self, srv: server.Server, evt: event.BlockBreakEventData) -> event.BlockBreakEventData:
        if evt.player is None:
            return evt
        uuid = evt.player.get_uuid()
        self._cancel_if_restricted(uuid, evt)
        return evt

    # ── event: block place ───────────────────────────────────────────
    def on_block_place(self, srv: server.Server, evt: event.BlockPlaceEventData) -> event.BlockPlaceEventData:
        uuid = _get_player_uuid(evt)
        self._cancel_if_restricted(uuid, evt)
        return evt

    # ── event: toggle sneak ──────────────────────────────────────────
    def on_toggle_sneak(self, srv: server.Server, evt: event.PlayerToggleSneakEventData) -> event.PlayerToggleSneakEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: toggle flight ─────────────────────────────────────────
    def on_toggle_flight(self, srv: server.Server, evt: event.PlayerToggleFlightEventData) -> event.PlayerToggleFlightEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: toggle sprint ─────────────────────────────────────────
    def on_toggle_sprint(self, srv: server.Server, evt: event.PlayerToggleSprintEventData) -> event.PlayerToggleSprintEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: change world ──────────────────────────────────────────
    def on_change_world(self, srv: server.Server, evt: event.PlayerChangeWorldEventData) -> event.PlayerChangeWorldEventData:
        uuid = _get_player_uuid(evt)
        if self.game_manager.match.state == GameState.RUNNING and self.player_manager.is_runner(uuid):
            pos = evt.position
            self.tracker_manager.update_runner_last_known(uuid, "overworld", pos[0], pos[1], pos[2])
        return evt

    # ── event: gamemode change ───────────────────────────────────────
    def on_gamemode_change(self, srv: server.Server, evt: event.PlayerGamemodeChangeEventData) -> event.PlayerGamemodeChangeEventData:
        return evt

    # ── event: inventory click ───────────────────────────────────────
    def on_inventory_click(self, srv: server.Server, evt: event.InventoryClickEventData) -> event.InventoryClickEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: inventory close ───────────────────────────────────────
    def on_inventory_close(self, srv: server.Server, evt: event.InventoryCloseEventData) -> event.InventoryCloseEventData:
        return evt

    # ── event: item held ─────────────────────────────────────────────
    def on_item_held(self, srv: server.Server, evt: event.PlayerItemHeldEventData) -> event.PlayerItemHeldEventData:
        uuid = _get_player_uuid(evt)
        state = self.game_manager.match.state
        if state in (GameState.PAUSED, GameState.COUNTDOWN, GameState.PRE_HUNT):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        return evt

    # ── event: fish ──────────────────────────────────────────────────
    def on_fish(self, srv: server.Server, evt: event.PlayerFishEventData) -> event.PlayerFishEventData:
        return evt

    # ── event: egg throw ─────────────────────────────────────────────
    def on_egg_throw(self, srv: server.Server, evt: event.PlayerEggThrowEventData) -> event.PlayerEggThrowEventData:
        return evt

    # ── event: exp change ────────────────────────────────────────────
    def on_exp_change(self, srv: server.Server, evt: event.PlayerExpChangeEventData) -> event.PlayerExpChangeEventData:
        return evt

    # ── event: spawn change ──────────────────────────────────────────
    def on_spawn_change(self, srv: server.Server, evt: event.SpawnChangeEventData) -> event.SpawnChangeEventData:
        return evt

    # ── event: server command ────────────────────────────────────────
    def on_server_command(self, srv: server.Server, evt: event.ServerCommandEventData) -> event.ServerCommandEventData:
        return evt

    # ── event: server broadcast ──────────────────────────────────────
    def on_server_broadcast(self, srv: server.Server, evt: event.ServerBroadcastEventData) -> event.ServerBroadcastEventData:
        return evt

    # ── event: login ─────────────────────────────────────────────────
    def on_player_login(self, srv: server.Server, evt: event.PlayerLoginEventData) -> event.PlayerLoginEventData:
        return evt

    # ── helpers: restriction cancellation ─────────────────────────────
    def _cancel_if_restricted(self, uuid: str, evt) -> None:
        state = self.game_manager.match.state
        if state == GameState.HEADSTART:
            if self.player_manager.is_hunter(uuid):
                evt.cancelled = True
        elif state in (GameState.PRE_HUNT, GameState.COUNTDOWN, GameState.PAUSED):
            if self.player_manager.is_runner(uuid) or self.player_manager.is_hunter(uuid):
                evt.cancelled = True

    def _store_server(self, srv: server.Server) -> None:
        """Cache the server reference so _broadcast / _send_title_to_all work."""
        self.game_manager.match._server = srv

    # ── helpers: broadcast / title / nametag ──────────────────────────
    def _broadcast(self, msg: str) -> None:
        """Send a message to ALL online players."""
        srv = self.game_manager.match._server
        if srv is None:
            return
        comp = tc(msg)
        for p in srv.get_all_players():
            p.send_message(comp, False)

    def _send_title_to_all(self, title: str, subtitle: str) -> None:
        srv = self.game_manager.match._server
        if srv is None:
            return
        match = self.game_manager.match
        for uid in match.runner_uuids | match.hunter_uuids:
            p = srv.get_player_by_uuid(uid)
            if p:
                p.show_title(tc_bold(title))
                p.show_subtitle(tc(subtitle))

    def _send_actionbar_to_all(self, msg: str) -> None:
        srv = self.game_manager.match._server
        if srv is None:
            return
        match = self.game_manager.match
        for uid in match.runner_uuids | match.hunter_uuids:
            p = srv.get_player_by_uuid(uid)
            if p:
                p.show_actionbar(tc(msg))

    def _apply_nametag(self, player, uuid: str) -> None:
        role = self.player_manager.get_role(uuid)
        if role == PlayerRole.SPECTATOR:
            return
        suffix = "<red>[H]</red>" if role == PlayerRole.HUNTER else "<green>[R]</green>"
        name = player.get_name()
        full = tc(f"{name} {suffix}")
        player.set_display_name(full)
        player.set_tab_list_header_footer(
            tc_bold("ManhuntNG"),
            tc(f"Playing Manhunt — {self._get_state_name(self.game_manager.match.state)}")
        )

    def _update_scoreboard(self, srv: server.Server) -> None:
        match = self.game_manager.match
        world = None
        for uid in match.runner_uuids:
            p = srv.get_player_by_uuid(uid)
            if p:
                world = p.get_world()
                break
        if world is None:
            return
        sb = world.get_scoreboard()

        obj_name = "manhunt"
        try:
            sb.remove_objective(obj_name)
        except Exception:
            pass
        sb.add_objective(obj_name, tc_bold("Manhunt"), scoreboard.RenderType.INTEGER)
        sb.set_display_slot(scoreboard.DisplaySlot.SIDEBAR, obj_name)

        runners = match.runner_uuids
        runner_str = f"Runners: {len(runners)}" if runners else "Runner: None"
        hunters = f"Hunters: {len(match.hunter_uuids)}"
        elapsed = self.ui_manager.get_elapsed_seconds()
        time_str = self.ui_manager.format_time(elapsed)

        for i, line in enumerate([runner_str, hunters, f"Time: {time_str}"]):
            sb.update_score(line, obj_name, 10 - i)

    # ── scheduled tick ───────────────────────────────────────────────
    def _tick_ui(self, srv: server.Server) -> None:
        if not self.game_manager.is_game_active():
            return

        self.ui_manager.update_phase()
        phase_text = self.ui_manager.current_phase.display()

        match = self.game_manager.match
        for uid in match.runner_uuids | match.hunter_uuids:
            p = srv.get_player_by_uuid(uid)
            if p:
                p.show_actionbar(tc_colored(phase_text, common.NamedColor.GOLD))

        self._update_scoreboard(srv)


# ── module-level config message helper ────────────────────────────────
_config_manager = None  # set by ManhuntPlugin.on_load


def cfg_msg(key: str, **kw) -> str:
    """Read a localized message from the config.  Falls back to the raw key."""
    if _config_manager is None:
        return key
    placeholders = {f"{{{k}}}": v for k, v in kw.items()}
    return _config_manager.get_message(key, **placeholders)


register_plugin(ManhuntPlugin)
