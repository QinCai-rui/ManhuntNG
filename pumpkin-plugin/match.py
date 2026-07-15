from typing import Optional
from game_state import GameState, StartMode, ManhuntGameMode
from player_role import PlayerRole


class Match:
    def __init__(self):
        self.state: GameState = GameState.WAITING
        self.start_mode: StartMode = StartMode.DREAMSTART
        self.game_mode: ManhuntGameMode = ManhuntGameMode.NORMAL

        self.runner_uuids: set[str] = set()
        self.hunter_uuids: set[str] = set()
        self.spectator_uuids: set[str] = set()
        self.previous_roles: dict[str, PlayerRole] = {}

        self.owner_uuid: Optional[str] = None
        self.start_time: int = 0
        self.end_time: int = 0
        self.paused_at: int = 0
        self.total_paused_duration: int = 0
        self.pre_pause_state: Optional[GameState] = None

        self.game_world: Optional[str] = None
        self.nether_world: Optional[str] = None
        self.end_world: Optional[str] = None
        self.winner_uuid: Optional[str] = None
        self.runner_wins: bool = False

        self.stronghold_discovered: bool = False
        self.fortress_discovered: bool = False
        self.blaze_rod_obtained: bool = False
        self.bastion_discovered: bool = False

        self.headstart_remaining: int = -1
        self.pause_timeout_remaining: int = -1
        self.pause_timeout_hunters_win: bool = False

        self.seed: Optional[int] = None
        self.world_name: Optional[str] = None

        self._server: object = None  # set by plugin event handlers

    def add_runner(self, uuid: str) -> None:
        self.runner_uuids.add(uuid)

    def remove_runner(self, uuid: str) -> None:
        self.runner_uuids.discard(uuid)

    def is_runner(self, uuid: str) -> bool:
        return uuid in self.runner_uuids

    def get_runner_uuid(self) -> Optional[str]:
        if not self.runner_uuids:
            return None
        return next(iter(self.runner_uuids))

    def add_hunter(self, uuid: str) -> None:
        self.hunter_uuids.add(uuid)

    def remove_hunter(self, uuid: str) -> None:
        self.hunter_uuids.discard(uuid)

    def is_hunter(self, uuid: str) -> bool:
        return uuid in self.hunter_uuids

    def add_spectator(self, uuid: str) -> None:
        self.spectator_uuids.add(uuid)

    def remove_spectator(self, uuid: str) -> None:
        self.spectator_uuids.discard(uuid)

    def is_spectator(self, uuid: str) -> bool:
        return uuid in self.spectator_uuids

    def is_player(self, uuid: str) -> bool:
        return uuid in self.runner_uuids or uuid in self.hunter_uuids

    def is_participant(self, uuid: str) -> bool:
        return self.is_player(uuid) or uuid in self.spectator_uuids

    def is_owner(self, uuid: str) -> bool:
        return self.owner_uuid is not None and self.owner_uuid == uuid

    def clear_all_players(self) -> None:
        self.runner_uuids.clear()
        self.hunter_uuids.clear()
        self.spectator_uuids.clear()
        self.previous_roles.clear()

    def is_using_existing_world(self) -> bool:
        return self.world_name is not None and len(self.world_name) > 0
