import time
from typing import Optional

from game_state import GameState, StartMode, ManhuntGameMode
from match import Match
from player_role import PlayerRole


class GameManager:
    def __init__(self):
        self.match = Match()
        self.force_start: bool = False
        self.headstart_remaining: int = -1
        self._countdown_active: bool = False
        self._headstart_active: bool = False

    def start_game(self, owner_uuid: Optional[str] = None) -> bool:
        if self.match.state != GameState.WAITING:
            return False

        if not self.match.runner_uuids:
            return False
        if not self.match.hunter_uuids:
            return False

        self.match.owner_uuid = owner_uuid
        self.match.state = GameState.COUNTDOWN
        return True

    def start_game_force(self, owner_uuid: Optional[str] = None) -> bool:
        if self.match.state != GameState.WAITING:
            return False

        self.match.owner_uuid = owner_uuid
        self.force_start = True
        self.match.state = GameState.COUNTDOWN
        return True

    def finish_countdown(self) -> None:
        self.match.stronghold_discovered = False
        self.match.fortress_discovered = False
        self.match.blaze_rod_obtained = False
        self.match.bastion_discovered = False

        self._reset_match_timing()

        if self.match.start_mode == StartMode.HEADSTART:
            self.match.state = GameState.HEADSTART
            self._headstart_active = True
        elif self.force_start:
            self.force_start = False
            self.match.state = GameState.RUNNING
            self.match.start_time = int(time.time() * 1000)
        else:
            self.match.state = GameState.PRE_HUNT

    def start_hunt(self) -> bool:
        if self.match.state != GameState.PRE_HUNT:
            return False

        self.match.state = GameState.RUNNING
        self._reset_match_timing()
        self.match.start_time = int(time.time() * 1000)
        self.match.stronghold_discovered = False
        self.match.fortress_discovered = False
        self.match.blaze_rod_obtained = False
        self.match.bastion_discovered = False
        return True

    def stop_game(self) -> None:
        self.match.accumulate_paused_time()
        self.match.state = GameState.FINISHED
        self.match.end_time = int(time.time() * 1000)
        self._countdown_active = False
        self._headstart_active = False

        self.match.state = GameState.WAITING
        self.match.seed = None
        self.match.world_name = None
        self.match.clear_all_players()

    def runner_wins(self) -> None:
        self.match.state = GameState.FINISHED
        self.match.end_time = int(time.time() * 1000)
        self.match.runner_wins = True

        self.match.state = GameState.WAITING
        self.match.seed = None
        self.match.world_name = None
        self.match.clear_all_players()

    def hunters_win(self) -> None:
        self.match.state = GameState.FINISHED
        self.match.end_time = int(time.time() * 1000)
        self.match.runner_wins = False

        self.match.state = GameState.WAITING
        self.match.seed = None
        self.match.world_name = None
        self.match.clear_all_players()

    def is_game_active(self) -> bool:
        return self.match.state in (
            GameState.RUNNING,
            GameState.PRE_HUNT,
            GameState.HEADSTART,
            GameState.PAUSED,
        )

    def is_game_paused(self) -> bool:
        return self.match.state == GameState.PAUSED

    def pause_game(self, owner_uuid: Optional[str] = None) -> bool:
        if self.match.state not in (
            GameState.RUNNING,
            GameState.PRE_HUNT,
            GameState.HEADSTART,
        ):
            return False

        if owner_uuid is not None and not self.match.is_owner(owner_uuid):
            return False

        self.match.pre_pause_state = self.match.state
        self.match.state = GameState.PAUSED
        self.match.paused_at = int(time.time() * 1000)
        self._headstart_active = False
        return True

    def resume_game(self, owner_uuid: Optional[str] = None) -> bool:
        if self.match.state != GameState.PAUSED:
            return False

        if owner_uuid is not None and not self.match.is_owner(owner_uuid):
            return False

        self.match.accumulate_paused_time()
        previous = self.match.pre_pause_state
        if previous is not None:
            self.match.state = previous
        return True

    def infect_player(self, runner_uuid: str) -> bool:
        if self.match.game_mode != ManhuntGameMode.INFECTION:
            return False
        if self.match.state != GameState.RUNNING:
            return False
        if runner_uuid not in self.match.runner_uuids:
            return False

        self.match.remove_runner(runner_uuid)
        self.match.add_hunter(runner_uuid)

        if not self.match.runner_uuids:
            self.hunters_win()
            return True
        return True

    def _reset_match_timing(self) -> None:
        self.match.end_time = 0
        self.match.paused_at = 0
        self.match.total_paused_duration = 0
        self.match.start_time = 0
        self.match.headstart_remaining = -1
