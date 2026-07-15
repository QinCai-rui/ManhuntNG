from typing import Any

from game_manager import GameManager
from player_manager import PlayerManager


class PotionEffectManager:
    def __init__(self, config_manager: Any, game_manager: GameManager):
        self._config_manager = config_manager
        self._game_manager = game_manager

    def get_runner_effects(self) -> list[dict[str, Any]]:
        return self._config_manager.get_runner_potion_effects()

    def get_hunter_effects(self) -> list[dict[str, Any]]:
        return self._config_manager.get_hunter_potion_effects()

    def get_effects_for_player(self, uuid: str) -> list[dict[str, Any]]:
        match = self._game_manager.match
        if uuid in match.runner_uuids:
            return self.get_runner_effects()
        elif uuid in match.hunter_uuids:
            return self.get_hunter_effects()
        return []
