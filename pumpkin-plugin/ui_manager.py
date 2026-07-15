import time
import math
from typing import Optional

from game_manager import GameManager
from game_phase import GamePhase


class UIManager:
    def __init__(self, game_manager: GameManager):
        self._game_manager = game_manager
        self.current_phase: GamePhase = GamePhase.OVERWORLD_PREP

    def update_phase(self) -> None:
        match = self._game_manager.match
        if match.game_world is None:
            return

        runner_uuid = match.get_runner_uuid()
        if runner_uuid is None:
            return

        if match.blaze_rod_obtained:
            if match.stronghold_discovered:
                self.current_phase = GamePhase.STRONGHOLD_DIVE
            else:
                self.current_phase = GamePhase.RETURN_EYES
        elif match.fortress_discovered:
            if match.blaze_rod_obtained:
                self.current_phase = GamePhase.BLAZE_ROD_RUN
            else:
                self.current_phase = GamePhase.FORTRESS_RUN
        elif match.bastion_discovered:
            self.current_phase = GamePhase.BASTION_ROUTE
        else:
            if match.stronghold_discovered:
                self.current_phase = GamePhase.STRONGHOLD_DIVE
            else:
                self.current_phase = GamePhase.OVERWORLD_PREP

    def format_time(self, elapsed_seconds: int) -> str:
        hours = elapsed_seconds // 3600
        minutes = (elapsed_seconds % 3600) // 60
        seconds = elapsed_seconds % 60
        if hours > 0:
            return f"{hours}:{minutes:02d}:{seconds:02d}"
        return f"{minutes:02d}:{seconds:02d}"

    def get_elapsed_seconds(self) -> int:
        match = self._game_manager.match
        if match.start_time == 0:
            return 0
        end = match.end_time if match.end_time != 0 else int(time.time() * 1000)
        paused = (int(time.time() * 1000) - match.paused_at) if match.paused_at != 0 else 0
        return (end - match.start_time - match.total_paused_duration - paused) // 1000
